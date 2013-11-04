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

angular.module('app',
        [
            'app.directives',
            'app.filters',
            'app.services',
            'app.controllers',
            'loading',
            'ngRoute',
            'ngResource',
            'ngSanitize',
            'ngAnimate',
            'ui.bootstrap',
        ]);

angular.module('app').value("globalConfig", window.CONFIG);

/**
 * Main controller to manage translations.
 */
angular.module('app').controller('AppCtrl', ['$scope', '$q', '$log', '$modal', 'LocalisationService',
    function($scope, $q, $log, $modal, LocalisationService) {

        $log.info("AppCtrl()");

        $scope.model = {
            localisations: LocalisationService.getTranslations(),
            filterCategory: "tarjonta",
            filterLocale: "fi",
            filterKey: "",
            filterValue: "",
            info: []
        };

        $scope.filterCategoryWithCategory = function(item) {
            var result = ($scope.model.filterCategory === undefined) || item.category === undefined || (item.category.indexOf($scope.model.filterCategory) != -1);
            // $log.info("filterCategoryWithCategory()", result);
            return result;
        };

        $scope.filterKeyWithKey = function(item) {
            var result = ($scope.model.filterKey === undefined) || item.key === undefined || (item.key.indexOf($scope.model.filterKey) != -1);
            // $log.info("filterKeyWithKey()", result);
            return result;
        };

        $scope.filterLocaleWithLocale = function(item) {
            var result = ($scope.model.filterLocale === undefined) || item.locale === undefined || (item.locale.indexOf($scope.model.filterLocale) != -1);
            // $log.info("filterLocaleWithLocale()", result);
            return result;
        };

        $scope.filterValueWithValue = function(item) {
            var result = ($scope.model.filterValue === undefined) || item.value === undefined || (item.value.indexOf($scope.model.filterValue) != -1);
            // $log.info("filterValueWithValue()", result);
            return result;
        };

        $scope.doFilter = new function() {
            return function(item) {
                var result = true;
                result = result && $scope.filterCategoryWithCategory(item);
                result = result && $scope.filterKeyWithKey(item);
                result = result && $scope.filterLocaleWithLocale(item);
                result = result && $scope.filterValueWithValue(item);

                result = item.uiChanged || result;

                // $log.info("doFilter() -------------> ", result);

                return result;
            }
        };

        $scope.reloadData = function() {
            $log.info("reloadData()");
            LocalisationService.reload().then(function(data) {
                $scope.model.localisations = data;
            }, function(aa, bb, cc, dd) {
                $scope.model.info.push({status: "ERROR", aa: aa, bb: bb, cc: cc, dd: dd});
            });
        };

        $scope.saveAllModified = function() {
            $log.info("saveAllModified()", $scope.localisationsForm);

            var promises = [];

            for (var i = 0; i < $scope.model.localisations.length; i++) {
                var item = $scope.model.localisations[i];

                if (item.uiDelete) {
                    $log.info("  -- DELETE:", item);
                    promises.push(
                            item.$delete({id: item.id}, function() {
                                $scope.pushResult({status: "OK"});
                            }, function(aa, bb, cc, dd, ee) {
                                $scope.pushResult({status: "ERROR", aa: aa, bb: bb, cc: cc, dd: dd, ee: ee});
                            }));
                    item.uiDelete = false;
                    item.uiChanged = false;
                }

                if (item.uiChanged) {
                    $log.info("  -- SAVE:", item);
                    promises.push(
                            item.$update(function(data, headers) {
                                $scope.pushResult({status: "OK", data: data, headers: headers});
                            }, function(data, headers, xxx) {
                                $scope.pushResult({statusx: "ERROR", datax: data, headersx: headers, xxx : xxx});
                            }));
                }
            }

            $scope.localisationsForm.$setPristine();

            // Trigger reload, ONLY AFTER WAITING UPDATES TO FINNISH - please, $q.all().then ....
            // $scope.reloadData();

            $q.all(promises).then(function() {
                $log.info("  all operations completed... reload data.");
                $scope.reloadData();
            });

        };

        $scope.createNew = function() {
            $log.info("createNew()");
        };

        $scope.pushResult = function(info) {
            $scope.model.info.push(info);
        };

        // Trigger reload
        $scope.reloadData();
    }]);


//
// "Production" mode
//
angular.module('app').config(function($logProvider) {
    $logProvider.debugEnabled(true);
});