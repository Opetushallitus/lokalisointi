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
 *//*
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
package fi.vm.sade.lokalisointi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.spring.Autowire;
import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.dao.LocalisationDao;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple utility to automatically transfer translations from one environment to another.
 *
 * Reads translations via given URL (for example: "https://itest-virkailija.oph.ware.fi/lokalisointi/cxf/rest/v1/localisation"),
 * searches for local translations and updates any changes to local database.
 * NOTE: no deletions detected.
 *
 * Configuration:
 * <pre>
 * #
 * # Copy from luokka, default EMPTY - will not do any copying by default without any configuration!
 * #
 * lokalisointi.copy.from=https://itest-virkailija.oph.ware.fi/lokalisointi/cxf/rest/v1/localisation
 * # Every full hour by default
 * lokalisointi.copy.cron=0 0 * * * *
 * # Forced copy - ie. always overwrite even newer translations, default false
 * lokalisointi.copy.force=false
 * </pre>
 *
 *
 * @author mlyly
 */
public class CopyLocalisationsFrom {

    private static final Logger LOG = LoggerFactory.getLogger(CopyLocalisationsFrom.class);

    public CopyLocalisationsFrom() {
        LOG.info("CopyLocalisationsFrom()");
    }

    // @Value("${lokalisointi.copy.from:https://itest-virkailija.oph.ware.fi/lokalisointi/cxf/rest/v1/localisation}")
    @Value("${lokalisointi.copy.from:}")
    private String _copyLocalisationsFromUri;

    @Value("${lokalisointi.copy.force:false}")
    private boolean _forced = false;

    @Value("${lokalisointi.copy.cron:0 0 * * * *}")
    private String _cron;

    @Autowired
    private LocalisationDao localisationDao;

    @Scheduled(cron = "${lokalisointi.copy.cron:0 0 * * * *}")
    @Transactional(readOnly = false)
    public void copyLocalisationsFrom() {
        LOG.debug("copyLocalisationsFrom() force={}, cron='{}', uri='{}'", _forced, _cron, _copyLocalisationsFromUri);

        if (_copyLocalisationsFromUri == null || _copyLocalisationsFromUri.isEmpty()) {
            LOG.info("Copying of localisations is not enabled, set environment variable 'lokalisointi.copy.from' to enable it.");
            return;
        }

        try {
            // Get localisations
            URL url = new URL(_copyLocalisationsFromUri);
            InputStream in = url.openStream();

            // Map to list of localisation objects
            TypeReference<List<LocalisationRDTO>> typeref = new TypeReference<List<LocalisationRDTO>>() {
            };

            ObjectMapper mapper = new ObjectMapper();
            List<LocalisationRDTO> localisations = mapper.readValue(in, typeref);

            in.close();

            int totalCount = 0;
            int createCount = 0;
            int updateCount = 0;

            // Process all localisations
            for (LocalisationRDTO localisationRemote : localisations) {
                totalCount++;

                Localisation localisationLocal = localisationDao.findOne(null, localisationRemote.getCategory(), localisationRemote.getKey(), localisationRemote.getLocale());

                if (localisationLocal == null) {
                    localisationLocal = new Localisation();
                    localisationLocal.setAccesscount((int) localisationRemote.getAccesscount());
                    localisationLocal.setAccessed(localisationRemote.getAccessed());
                    localisationLocal.setCategory(localisationRemote.getCategory());
                    localisationLocal.setCreated(localisationRemote.getCreated());
                    localisationLocal.setCreatedBy(localisationRemote.getCreatedBy());
                    localisationLocal.setDescription(localisationRemote.getDescription());
                    localisationLocal.setKey(localisationRemote.getKey());
                    localisationLocal.setLanguage(localisationRemote.getLocale());
                    localisationLocal.setModified(localisationRemote.getModified());
                    localisationLocal.setModifiedBy(localisationRemote.getModifiedBy());
                    localisationLocal.setValue(localisationRemote.getValue());

                    localisationDao.insert(localisationLocal);

                    createCount++;
                } else {
                    long timeDiffRemoteLocal = localisationRemote.getModified().getTime() - localisationLocal.getModified().getTime();

                    if (_forced || timeDiffRemoteLocal > 0) {
                        // Ok, forced OR localisation is newer in the remote location - anyways - do the update

                        LOG.info("remote: {} > local: {} -- diff: {}", localisationRemote.getModified(), localisationLocal.getModified(),
                                localisationRemote.getModified().getTime() - localisationLocal.getModified().getTime());

                        localisationLocal.setDescription(localisationRemote.getDescription());
                        localisationLocal.setValue(localisationRemote.getValue());
                        localisationLocal.setModified(localisationRemote.getModified());
                        localisationLocal.setModifiedBy(localisationRemote.getModifiedBy());

                        localisationDao.update(localisationLocal);

                        updateCount++;
                    }
                }
            }

            LOG.info("Total: {} -- Updated: {}, created {} translations.", totalCount, updateCount, createCount);

        } catch (Throwable ex) {
            // Is the configuread uri really the localisation uri to get translations from?
            LOG.error("Failed to transfer localisations from:" + _copyLocalisationsFromUri, ex);
        }
    }

}
