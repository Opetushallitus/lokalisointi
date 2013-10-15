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
package fi.vm.sade.lokalisointi.service.dao;

import fi.vm.sade.generic.dao.JpaDAO;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import java.util.List;

/**
 *
 * @author mlyly
 */
public interface LocalisationDao extends JpaDAO<Localisation, Long> {

    /**
     * Find localisations.
     *
     * @param category
     * @param locale
     * @param keyPrefix
     * @return
     */
    List<Localisation> findBy(String category, String locale, String keyPrefix);

    /**
     * Find by "primarey" keys.
     *
     * @param category
     * @param key
     * @param locale
     * @return
     */
    Localisation findOne(String category, String key, String locale);

    /**
     * Insert or update.
     *
     * @param localisation
     * @return
     */
    Localisation save(Localisation localisation);

    /**
     * Delete.
     *
     * @param localisation
     * @return
     */
    boolean delete(Localisation localisation);
}
