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


var app = angular.module('app.services', ['ngResource']);

app.factory('Localisations', function($log, $resource, globalConfig) {

    globalConfig.env.localisations = [];

    var uri = globalConfig.env.lokalisointiRestUrl + "/:id";

    $log.info("Localisations() - uri = ", uri);

    return $resource(uri, {id : "@id"}, {
        update: {
            method: 'PUT',
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
        },
        save: {
            method: 'POST',
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
        }
    });

});

app.service('LocalisationService', function($log, $q, Localisations, globalConfig) {
    $log.log("LocalisationService()");

    this.delete = function(entry) {
        $log.info("delete()", entry);

        var deferred = $q.defer();

        Localisations.delete(entry, function(data, status, headers, config) {
            $log.info("  delete() - OK", data, status, headers, config);
            deferred.resolve(entry);
        }, function(data, status, headers, config) {
            $log.error("  delete() - ERROR", data, status, headers, config, entry);
            deferred.reject(entry);
        });

        return deferred.promise;
    };

    this.save = function(entry) {
        $log.info("save()", entry);

        var deferred = $q.defer();

        // Try to save to the server
        Localisations.save(entry, function(data, status, headers, config) {
            $log.info("  save() - OK", data, status, headers, config);
            deferred.resolve(entry);
        }, function(data, status, headers, config) {
            $log.error("  save() - ERROR", data, status, headers, config, entry);
            deferred.reject(entry);
        });

        return deferred.promise;
    };

    this.reload = function() {
        $log.info("reload()");

        var deferred = $q.defer();

        Localisations.query({}, function(data) {
            $log.log("  reload() - successfull", data);
            globalConfig.env.localisations = data;
            deferred.resolve(data);
        }, function(data) {
            $log.error("  reload() - FAILED", data);
            globalConfig.env.localisations = [];
            deferred.reject(data);
        });

        return deferred.promise;
    };

    this.getTranslations = function() {
        $log.info("getTranslations()");
        return globalConfig.env.localisations;
    };

    // Initial load of translations
    // this.reload();

});
