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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
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
        LOG.debug("authorize()");
        return getCurrentUserName();
    }

    // GET /localisation?category=tarjonta&value=cached
    @Override
    public List<LocalisationRDTO> getLocalisations(LocalisationRDTO query, HttpServletResponse httpServletResponse) {
        LOG.debug("getLocalisations({})", query);

        //
        // Add "?value=cached" to request to add cache headers cache
        //
        if (httpServletResponse != null && query != null && query.getValue() != null && query.getValue().equalsIgnoreCase("cached")) {
            LOG.debug("  adding 'Cache-Control' header for 600 s / ten minutes");
            httpServletResponse.addHeader("Cache-Control", "public, max-age=600");
        }

        try {
            List<Localisation> l;

            if (query != null) {
                l = localisationDao.findBy(query.getId(), query.getCategory(), query.getKey(), query.getLocale());
            } else {
                l = localisationDao.findAll();
            }

            List<LocalisationRDTO> result = convert(l);
            LOG.debug("  --> result.size = {}", result.size());
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
        LOG.debug("updateLocalisation({})", data);
        if (data == null) {
            throw new NotFoundException("Invalid null input for update!");
        }

        try {
            // NOTE! Find localisations only by the composite primary key
            Localisation t = localisationDao.findOne((Long) null, data.getCategory(), data.getKey(), data.getLocale());
            update(t, data);
            return convert(t);
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /localisation/access
    @Override
    public Map<String, Long> updateLocalisationAccessed(List<Long> ids) {
        // Access check updates can be made by anyone
        try {
            Map<String, Long> result = new HashMap<String, Long>();

            long count = 0;
            if (isLoggedInUser()) {
                count = localisationDao.updateAccessed(ids);
            }

            result.put("updated", count);
            return result;
        } catch (Throwable ex) {
            LOG.error("failed", ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /localisation
    @Override
    public LocalisationRDTO createLocalisation(LocalisationRDTO data) {
        LOG.debug("createLocalisation({})", data);

        // Just require logged in user so that we can create missing translations in any application, VIA angular apps too
        if (!isLoggedInUser()) {
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

                t.setDescription(data.getDescription());

                // If no value has been given for localisation, use "[gategory-key-locale]" format as value.
                if (data.getValue() == null || data.getValue().trim().isEmpty()) {
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
            if (!(ex instanceof NotFoundException)) {
                LOG.error("failed", ex);
            }
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /localisation/{id}
    @Secured({ROLE_CRUD})
    @Override
    public LocalisationRDTO deleteLocalisation(Long id) {
        LOG.debug("deleteLocalisation({})", id);
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

    // POST /localisation/update
    @Secured({ROLE_CRUD})
    @Override
    public List<Map> updateLocalisations(List<LocalisationRDTO> data) {
        LOG.info("updateLocalisations({} kpl)", data != null ? data.size() : 0);

        List<Map> tmp = new ArrayList<Map>();
        Map result = new HashMap();
        tmp.add(result);

        int updated = 0;
        int created = 0;
        int notModified = 0;
        
        for (LocalisationRDTO l : data) {
            LOG.info("processing: {}", l);

            // Find already existing?
            Localisation t = localisationDao.findOne((Long) null, l.getCategory(), l.getKey(), l.getLocale());

            boolean isNew = (t == null);
            
            if (t == null) {
                t = new Localisation();

                t.setCreated(new Date());
                t.setCreatedBy(getCurrentUserName());
                t.setModified(null);
                t.setModifiedBy(null);

                t.setCategory(l.getCategory());
                t.setKey(l.getKey());
                t.setLanguage(l.getLocale());
            }

            if (update(t, l)) {
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
                localisationDao.save(t);
            } else {
                notModified++;
            }
        }
            
        result.put("status", "OK");
        result.put("updated", updated);
        result.put("created", created);
        result.put("notModified", notModified);
                
        return tmp;
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
        t.setAccesscount(s.getAccesscount());

        return t;
    }

    /**
     * Update db localisation from transfer object.
     *
     * @param t
     * @param data
     * @param force if true no modified after check will be done
     * @return true if modifications done, false if not modified
     */
    private boolean update(Localisation t, LocalisationRDTO data) {
        if (t == null) {
            throw new NotFoundException("Cannot find localisation for: " + data);
        }

        // Forced update?
        if (!data.getForce()) {
            // No forced updated if db translation is newer
            if (t.getModified() != null && data.getModified() != null && t.getModified().after(data.getModified())) {
                LOG.debug("*** Cowardly refusing to 'downgrade' localisation for {}-{}-{}, modified: {} > ui modified: {}",
                        new Object[]{
                            t.getCategory(), t.getKey(), t.getLanguage(), t.getModified(), data.getModified()
                        });
                return false;
            }

            LOG.debug("  --> OK localisation for {}-{}-{}, modified: {} <= ui modified: {}",
                    new Object[]{
                        t.getCategory(), t.getKey(), t.getLanguage(), t.getModified(), data.getModified()
                    });

        }

        if (data.getId() != null) {
            // Update exact match by id... So that we can edit cat/key/locale too :)
            t.setCategory(data.getCategory());
            t.setKey(data.getKey());
            t.setLanguage(data.getLocale());
        }

        // Don't update accessed on update
        if (t.getAccessed() == null) {
            t.setAccessed(new Date());
        }

        // If data contains last modified data then use it for last modification ts (so we can store even older data to db)
        if (data.getModified() != null) {
            t.setModified(data.getModified());
        } else {
            t.setModified(new Date());
        }
        t.setModifiedBy(getCurrentUserName());

        t.setDescription(data.getDescription());
        t.setValue(data.getValue());

        LOG.debug("Updated!");
        return true;
    }

    /**
     * Gets current username - for "auditing" purposes.
     *
     * @return
     */
    private String getCurrentUserName() {
        if (isLoggedInUser()) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } else {
            return "NA";
        }
    }

    /**
     * @return true if user has been logged in AND is not "anonymousUser"
     */
    private boolean isLoggedInUser() {
        boolean result = true;

        result = result && SecurityContextHolder.getContext() != null;
        result = result && SecurityContextHolder.getContext().getAuthentication() != null;
        result = result && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
        result = result && SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null;
        result = result && SecurityContextHolder.getContext().getAuthentication().getAuthorities() != null;
        result = result && SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty() == false;

        // TODO how to make this check more robust?
        result = result && !"anonymousUser".equalsIgnoreCase(SecurityContextHolder.getContext().getAuthentication().getName());

        LOG.debug("isLoggedInUser(): {} - {}", result, result ? SecurityContextHolder.getContext().getAuthentication().getName() : "NA");
        // LOG.debug("  authorities = {}", result ? SecurityContextHolder.getContext().getAuthentication().getAuthorities() : null);

        return result;
    }

}
