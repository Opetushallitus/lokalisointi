/*
 * Copyright (c) 2013 The Finnish Board of Education - Opetushallitus
 *
 * This program is free software:  Licensed under the EUPL, Version 1.1 or - as
 * soon as they will be approved by the European Commission - subsequent versions
 * of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 */
package fi.vm.sade.lokalisointi.service.resource;

import com.sun.jersey.api.MessageException;
import com.sun.jersey.api.NotFoundException;
import fi.vm.sade.lokalisointi.api.LocalisationResource;
import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.dao.LocalisationDao;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implement necessary services to manage localisation and internationalization support for other applications.
 *
 * @author mlyly
 * @see CORS fi.vm.sade.lokalisointi.service.resource.AllowAllCorsRequestsFilter in web.xml
 */
@Transactional(readOnly = false)
// @CrossOriginResourceSharing -- CONFIGURED IN WEB.XML
public class LocalisationResourceImpl implements LocalisationResource {

    private static final String ROLE_READ = "ROLE_APP_LOKALISOINTI_READ";
    private static final String ROLE_UPDATE = "ROLE_APP_LOKALISOINTI_READ_UPDATE";
    private static final String ROLE_CRUD = "ROLE_APP_LOKALISOINTI_CRUD";

    private static final Logger LOG = LoggerFactory.getLogger(LocalisationResourceImpl.class);

    @Autowired
    private LocalisationDao localisationDao;

    // GET /localisation/authorize
    @Secured({ROLE_READ})
    @Override
    public String authorize() {
        LOG.info("authorize()");
        return getCurrentUserName();
    }

    // GET /localisation?category=tarjonta
    @Override
    public List<LocalisationRDTO> getLocalisations(LocalisationRDTO query) {
        LOG.info("getLocalisations({})", query);

        try {
            List<Localisation> l;

            if (query != null) {
                l = localisationDao.findBy(query.getId(), query.getCategory(), query.getKey(), query.getLocale());
            } else {
                l = localisationDao.findAll();
            }

            List<LocalisationRDTO> result = convert(l);
            LOG.info("  --> result.size = {}", result.size());
            return result;
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex);
        }
    }

    // PUT localisation/{id}
    @Secured({ROLE_UPDATE, ROLE_CRUD})
    @Override
    public LocalisationRDTO updateLocalisation(Long id, LocalisationRDTO data) {
        LOG.info("updateLocalisation({})", data);
        if (data == null) {
            throw new NotFoundException("Invalid null input for update!");
        }

        try {
            // Find localisations only by the composite primary key
            Localisation t = localisationDao.findOne((Long) null, data.getCategory(), data.getKey(), data.getLocale());
            update(t, data);
            return convert(t);
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /localisation/{id}/accessed
    @Secured({ROLE_UPDATE, ROLE_CRUD})
    @Override
    public LocalisationRDTO updateLocalisationAccessed(Long id, LocalisationRDTO data) {
        LOG.info("updateLocalisationAccessed({})", data);

        try {
            // Find localisations only by the composite primary key
            Localisation l = localisationDao.findOne((Long) null, data.getCategory(), data.getKey(), data.getLocale());
            if (l != null) {
                l.setAccessed(new Date());
                l = localisationDao.save(l);
            }

            return convert(l);
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /localisation
    @Override
    public LocalisationRDTO createLocalisation(LocalisationRDTO data) {
        LOG.info("createLocalisation({})", data);

        // Just require logged in user so that we can create missing translations in any application, VIA angular apps too
        if (SecurityContextHolder.getContext() == null
                || SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication().isAuthenticated() == false) {
            LOG.warn("  cannot create Localisations with non-logged in user, localisation = {}", data);
            throw new MessageException("NOT AUTHORIZED, only logged in users can create (initial) translations.");
        }

        try {
            // Find already existing? (shoud not be found)
            Localisation t = localisationDao.findOne((Long) null, data.getCategory(), data.getKey(), data.getLocale());

            if (t == null) {
                t = new Localisation();

                t.setCreated(new Date());
                t.setCreatedBy(getCurrentUserName());

                t.setModified(new Date());
                t.setModifiedBy(getCurrentUserName());

                t.setAccessed(new Date());

                t.setCategory(data.getCategory());
                t.setKey(data.getKey());
                t.setLanguage(data.getLocale());
                t.setAccessed(new Date());

                t.setDescription(data.getDescription());

                // If no value has been given for localisation, use "[gategory-key-locale]" format as value.
                if (data.getValue() == null) {
                    t.setValue("[" + t.getCategory() + "-" + t.getKey() + "-" + t.getLanguage() + "]");
                } else {
                    t.setValue(data.getValue());
                }

                // Save it
                localisationDao.save(t);
            } else {
                LOG.info("createLocalisation() --- Cannot create new localisation since it already exists!");
                throw new NotFoundException("Localisation should not have been found: " + data);
            }

            return convert(t);
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /localisation/{id}
    @Secured({ROLE_CRUD})
    @Override
    public LocalisationRDTO deleteLocalisation(Long id) {
        LOG.info("deleteLocalisation({})", id);
        try {
            LocalisationRDTO result = null;

            // Use db key to find the localisation
            Localisation l = localisationDao.findOne(id, null, null, null);
            if (l != null) {
                result = convert(l);
                localisationDao.remove(l);
            }

            return result;
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convert list of localisations to list of transferobjects.
     *
     * @param l
     * @return
     */
    private List<LocalisationRDTO> convert(List<Localisation> l) {
        List<LocalisationRDTO> result = new ArrayList<LocalisationRDTO>();

        if (l != null) {
            for (Localisation localisation : l) {
                result.add(convert(localisation));
            }
        }

        return result;
    }

    /**
     * Convert the localisation to transfer object.
     *
     * @param s
     * @return
     */
    private LocalisationRDTO convert(Localisation s) {
        if (s == null) {
            return null;
        }

        LocalisationRDTO t = new LocalisationRDTO();

        t.setId(s.getId());
        t.setAccessed(s.getAccessed());
        t.setCategory(s.getCategory());
        t.setCreated(s.getCreated());
        t.setCreatedBy(s.getCreatedBy());
        t.setDescription(s.getDescription());
        t.setKey(s.getKey());
        t.setLocale(s.getLanguage());
        t.setModified(s.getModified());
        t.setModifiedBy(s.getModifiedBy());
        t.setValue(s.getValue());

        return t;
    }

    /**
     * Update db localisation from transfer object.
     *
     * @param t
     * @param data
     */
    private void update(Localisation t, LocalisationRDTO data) {
        if (t == null) {
            throw new NotFoundException("Cannot find localisation for: " + data);
        }

        if (data.getId() != null) {
            // Update exact match by id... So that we can edit cat/key/locale too :)
            t.setCategory(data.getCategory());
            t.setKey(data.getKey());
            t.setLanguage(data.getLocale());
        }

        t.setAccessed(new Date());

        t.setModified(new Date());
        t.setModifiedBy(getCurrentUserName());

        t.setDescription(data.getDescription());
        t.setValue(data.getValue());
    }

    /**
     * Gets current username - for "auditing" purposes.
     *
     * @return
     */
    private String getCurrentUserName() {
        if (SecurityContextHolder.getContext() == null
                || SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication().getName() == null) {
            return "NA";
        }

        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
