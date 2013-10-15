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

import fi.vm.sade.lokalisointi.api.LocalisationResource;
import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.dao.LocalisationDao;
import java.util.List;
import org.apache.cxf.jaxrs.cors.CrossOriginResourceSharing;
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

    @Autowired
    private LocalisationDao localisationDao;

    @Override
    public List<LocalisationRDTO> getLocalisations(String category, String locale, String keyPrefix) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocalisationRDTO updateLocalisation(LocalisationRDTO data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocalisationRDTO updateLocalisationAccessed(LocalisationRDTO data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocalisationRDTO createLocalisation(LocalisationRDTO data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocalisationRDTO deleteLocalisation(LocalisationRDTO data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
