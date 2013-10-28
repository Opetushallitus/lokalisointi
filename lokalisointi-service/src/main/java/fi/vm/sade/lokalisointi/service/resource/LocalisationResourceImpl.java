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

import com.sun.jersey.api.NotFoundException;
import fi.vm.sade.lokalisointi.api.LocalisationResource;
import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.dao.LocalisationDao;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.cxf.jaxrs.cors.CrossOriginResourceSharing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implement necessary services for localisations.
 *
 * @author mlyly
 */
@Transactional(readOnly = false)
@CrossOriginResourceSharing(allowAllOrigins = true)
public class LocalisationResourceImpl implements LocalisationResource {

    private static final Logger LOG = LoggerFactory.getLogger(LocalisationResourceImpl.class);

    @Autowired
    private LocalisationDao localisationDao;

    @Override
    public List<LocalisationRDTO> getLocalisations(LocalisationRDTO query) {
        LOG.info("getLocalisations({})", query);

        List<Localisation> l = null;

        if (query != null) {
            l = localisationDao.findBy(query.getCategory(), query.getLocale(), query.getKey());
        } else {
            l = localisationDao.findAll();
        }

        List<LocalisationRDTO> result = convert(l);
        LOG.info("  --> result = {}", result);
        return result;
    }

    @Override
    public LocalisationRDTO updateLocalisation(LocalisationRDTO data) {
        if (data == null) {
            throw new NotFoundException("Invalud null input for update!");
        }

        Localisation l = convert(data);
        if (l.getId() == null) {
            throw new NotFoundException("Localisation not found: " + data.getCategory() + ", " + data.getKey() + ", " + data.getLocale());
        }

        l = localisationDao.save(l);

        return convert(l);
    }

    @Override
    public LocalisationRDTO updateLocalisationAccessed(LocalisationRDTO data) {

        Localisation l = localisationDao.findOne(data.getCategory(), data.getKey(), data.getLocale());
        if (l != null) {
            l.setAccessed(new Date());
            l = localisationDao.save(l);
        }

        return convert(l);
    }

    @Override
    public LocalisationRDTO createLocalisation(LocalisationRDTO data) {

        Localisation l = convert(data);
        if (l.getId() == null) {
            l = localisationDao.save(l);
        }

        return convert(l);
    }

    @Override
    public LocalisationRDTO deleteLocalisation(LocalisationRDTO data) {
        LocalisationRDTO result = null;

        Localisation l = localisationDao.findOne(data.getCategory(), data.getKey(), data.getLocale());
        if (l != null) {
            result = convert(l);
            localisationDao.remove(l);
        }

        return result;
    }

    private List<LocalisationRDTO> convert(List<Localisation> l) {
        List<LocalisationRDTO> result = new ArrayList<LocalisationRDTO>();

        if (l != null) {
            for (Localisation localisation : l) {
                result.add(convert(localisation));
            }
        }

        return result;
    }

    private LocalisationRDTO convert(Localisation s) {
        if (s == null) {
            return null;
        }

        LocalisationRDTO t = new LocalisationRDTO();

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

    private Localisation convert(LocalisationRDTO s) {
        if (s == null) {
            return null;
        }

        Localisation t = localisationDao.findOne(s.getCategory(), s.getKey(), s.getLocale());

        if (t == null) {
            t = new Localisation();
        }

        t.setAccessed(s.getAccessed());
        t.setCategory(s.getCategory());
        t.setCreated(s.getCreated());
        t.setCreatedBy(s.getCreatedBy());
        t.setDescription(s.getDescription());
        t.setKey(s.getKey());
        t.setLanguage(s.getLocale());
        t.setModified(s.getModified());
        t.setModifiedBy(s.getModifiedBy());
        t.setValue(s.getValue());

        return t;
    }

}
