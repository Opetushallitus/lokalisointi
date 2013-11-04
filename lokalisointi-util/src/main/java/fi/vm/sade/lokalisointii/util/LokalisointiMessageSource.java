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
package fi.vm.sade.lokalisointii.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractMessageSource;

/**
 * Configuratione example:
 * <pre>
 *
 * </pre>
 *
 * @author mlyly
 */
public class LokalisointiMessageSource extends AbstractMessageSource {

    private static final Logger LOG = LoggerFactory.getLogger(LokalisointiMessageSource.class);

    private String _serviceUrl;
    private String _category = "NONE";

    public String getServiceUrl() {
        return _serviceUrl;
    }

    public void setServiceUrl(String _serviceUrl) {
        this._serviceUrl = _serviceUrl;
    }

    public String getCategory() {
        return _category;
    }

    public void setCategory(String _category) {
        this._category = _category;
    }

    /**
     * Localisations "cached" here.
     *
     * TODO: cache management, expiration re-trieals etc.
     */
    private Map<String, Map<String, String>> _translationsByLocaleKey;

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        LOG.info("resolveCode({}, {})", code, locale);

        String result = getTranslationValue(code, locale.toString());
        if (result == null) {
            result = createNotFoundResult(code, locale.toString());
        }

        return createMessageFormat(result, locale);
    }

    /**
     * Get translation if it exists.
     *
     * @param code
     * @param locale
     * @return null if not found
     */
    synchronized private String getTranslationValue(String code, String locale) {
        LOG.info("getTranslationValue({} {})", code, locale);

        if (_translationsByLocaleKey == null) {
            _translationsByLocaleKey = getLocalisationsFromService();
        }

        Map<String, String> translationsByKey = _translationsByLocaleKey.get(locale != null ? locale.toString() : "");

        // Language not found?
        if (translationsByKey == null) {
            return null;
        }

        // Get the value (if any)
        return translationsByKey.get(code);
    }


    /**
     * Result was not found, create the "missing" trabslation.
     *
     * @param code
     * @param locale
     * @return
     */
    private String createNotFoundResult(String code, String locale) {
        String result = "[" + code + "_" + locale + "]";

        createTranslation(code, locale, result);

        return result;
    }

    /**
     * Create translation to the service (and in memory).
     *
     * @param code
     * @param locale
     * @param result
     */
    private void createTranslation(String code, String locale, String result) {
        LOG.info("createTranslation({}, {}, {})", new Object[] {code, locale, result});

        // TODO create POST to create the missing translation to the service
        // category, key, locale, result as value


        Map<String, String> translationsByKey = _translationsByLocaleKey.get(locale);
        if (translationsByKey == null) {
            translationsByKey = new HashMap<String, String>();
            _translationsByLocaleKey.put(locale, translationsByKey);
        }

        translationsByKey.put(code, result);
    }

    /**
     * Load translations from service.
     *
     * @return
     */
    private Map<String, Map<String, String>> getLocalisationsFromService() {
        LOG.info("getLocalisationsFromService()...");

        // Empty result created here
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

        try {
            String urlStr = getServiceUrl() + "?category=" + getCategory();

            LOG.info("  url = {}", urlStr);

            URL url = new URL(urlStr);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            JsonNode root = new ObjectMapper().readTree(in);

            in.close();

            LOG.info("  DATA == {}", root);
        } catch (Throwable ex) {
            LOG.error("  failed, got exception.", ex);
        }

        return result;
    }


}
