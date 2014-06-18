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

    return $resource(uri, {id: "@id"}, {
        query: {
            method: 'GET',
            withCredentials: true,
            isArray: true
        },
        update: {
            method: 'PUT',
            withCredentials: true,
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
        },
        save: {
            method: 'POST',
            withCredentials: true,
            headers: {'Content-Type': 'application/json; charset=UTF-8'}
        },
        massUpdate: {
            url: globalConfig.env.lokalisointiRestUrl + "/update",
            method: 'POST',
            withCredentials: true,
            headers: {'Content-Type': 'application/json; charset=UTF-8'},
            isArray: true
        }
    });

});

app.service('LocalisationService', function($log, $q, Localisations, globalConfig) {
    $log.log("LocalisationService()");

    this.massUpdate = function(forced, data) {
        $log.info("massUpdate()", forced, data);
        
        var deferred = $q.defer();

        // Remove all "id" fields and update "forced" info
        var tmp = angular.copy(data);
        angular.forEach(tmp, function(t) {
            delete t.id;
            t.force = forced;
        });

        Localisations.massUpdate(tmp, 
            function (result) {
                $log.info("  success!", result);
                deferred.resolve(result[0]);
            },
            function (err) {
                $log.info("  failed!", err);
                deferred.reject({ status: "ERROR", error: err });
            });
        
        return deferred.promise;
    };

    this.delete = function(entry) {
        $log.info("delete()", entry);

        var deferred = $q.defer();

        Localisations.delete(entry,
                function(data, status, headers, config) {
                    $log.info("  delete() - OK", data, status, headers, config);
                    deferred.resolve(entry);
                },
                function(data, status, headers, config) {
                    $log.error("  delete() - ERROR", data, status, headers, config, entry);
                    deferred.reject(entry);
                });

        return deferred.promise;
    };

    this.save = function(entry, forceUpdate) {
        $log.info("save()", entry, forceUpdate);

        var deferred = $q.defer();

        // Cannot post with ID -> method not allowed
        delete entry.id;

        // Try to save/insert to the server
        Localisations.save(entry,
                function(data, status, headers, config) {
                    $log.info("  save() - OK (inserted)", data, status, headers, config);

                    // OK - this is a new entry - so update the values to be correct
                    data.category = entry.category;
                    data.key = entry.key;
                    data.locale = entry.locale;
                    data.value = entry.value;
                    data.description = entry.description;

                    // And update it then
                    Localisations.update(data,
                            function(data2, status2, headers2, config2) {
                                $log.info("  save() - OK -> update() - OK", data2, status2, headers2, config2);
                                deferred.resolve(entry);
                            },
                            function(data2, status2, headers2, config2) {
                                $log.info("  save() - OK -> update() - ERROR", data2, status2, headers2, config2);
                                deferred.reject(entry);
                            });

                },
                function(data, status, headers, config) {
                    $log.info("  save() - insert failed, try to update still :)", data, status, headers, config, entry);

                    // "insert" failed - maybe existing translation - try to update
                    data = {}
                    data.id = -1;
                    data.category = entry.category;
                    data.key = entry.key;
                    data.locale = entry.locale;
                    data.value = entry.value;
                    data.description = entry.description;

                    // needed if we want to detect older forced translations...
                    data.modified = entry.modified;

                    data.force = forceUpdate ? true : false;

                    // Try to update it (== PUTs with id...)
                    Localisations.update(data,
                            function(data2, status2, headers2, config2) {
                                $log.info("  save() - ERROR - update() - OK", data2, status2, headers2, config2);
                                deferred.resolve(entry);
                            },
                            function(data2, status2, headers2, config2) {
                                $log.info("  save() - ERROR - update - ERROR", data2, status2, headers2, config2);
                                deferred.reject(entry);
                            });
                });

        return deferred.promise;
    };

    this.reload = function() {
        $log.info("reload()");

        var deferred = $q.defer();

        Localisations.query({ value : "NOCACHE" },
                function(data) {
                    $log.log("  reload() - successfull", data);
                    globalConfig.env.localisations = data;
                    deferred.resolve(data);
                },
                function(data) {
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
