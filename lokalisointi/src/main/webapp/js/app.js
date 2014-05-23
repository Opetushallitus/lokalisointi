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
            'app.services',
            'loading',
            'ngRoute',
            'ngResource',
            'ngSanitize',
            'ngAnimate',
            'ui.bootstrap',
            'debounce',
            'ngGrid'
        ]);

angular.module('app').value("globalConfig", window.CONFIG);

/**
 * Main controller to manage translations.
 */
angular.module('app').controller('AppCtrl', ['$scope', '$q', '$log', '$modal', 'LocalisationService', 'debounce', '$filter', '$templateCache',
    function($scope, $q, $log, $modal, LocalisationService, debounce, $filter, $templateCache) {

        $log.info("AppCtrl()");

        $scope.model = {
            localisations: LocalisationService.getTranslations(),
            changedItems: {},
            filterCategory: "tarjonta",
            filterLocale: "fi",
            filterKey: "",
            filterValue: "",
            info: [],
            filteredList: [],
            gridOptions: {
                data : 'model.filteredList',
                // rowHeight: 50,
                selectedItems: [],
                showSelectionCheckbox: true,
                selectWithCheckboxOnly: true,
                enableCellEditOnFocus: true,
                beforeSelectionChange: function(row, event) {
                    return !(row instanceof Array);
                },
                showColumnMenu: true,
                // showFilter: true,
                showFooter: true,
                sortInfo: {
                    fields: ["key"],
                    directions: ['asc']
                },
                columnDefs: [
                    {
                        field: "category",
                        enableCellEdit: false,
                        width: 150
                    },
                    {
                        field: "key",
                        enableCellEdit: false,
                        width: 300
                    },
                    {
                        field: "locale",
                        enableCellEdit: false,
                        width: 50
                    },
                    {
                        field: "value",
                        enableCellEdit: true,
                        editableCellTemplate: '<input ng-class="\'colt\' + col.index" ng-input="COL_FIELD" ng-model="COL_FIELD" ng-change="uiChanged(row)" ng-dblclick="openLongTextDialog(row)">',
                        // cellTemplate: '<input ng-class="\'colt\' + col.index" ng-input="COL_FIELD" ng-model="COL_FIELD" ng-change="uiChanged(row)" ng-dblclick="openLongTextDialog(row)">',
                        minWidth : 1000,
                        resizable: true
                    },
                    {
                        field: "accessed",
                        enableCellEdit: false,
                        cellFilter: "date:'dd.MM.yyyy HH:mm:ss'",
                        width: 150
                    },
                    {
                        field: "accesscount",
                        enableCellEdit: false,
                        width: 50
                    },
                ]
            }
        };

        $scope.numChangedItems = function() {
           return Object.keys($scope.getChangedItems()).length;
        };

        $scope.getChangedItems = function() {
            return $scope.model.changedItems;
        };

        $scope.clearChangedItems = function() {
            $scope.model.changedItems = {};
        };

        $scope.numSelected = function() {
            return $scope.getSelectedItems().length;
        };

        $scope.getSelectedItems = function() {
            return $scope.model.gridOptions.selectedItems;
        };

        $scope.clearSelectedItems = function() {
            $scope.model.gridOptions.selectedItems.splice(0);
        };

        /**
         * Informational / error messages added here.
         *
         * @param {type} info
         * @returns {undefined}
         */
        $scope.pushMessage = function(info) {
            $scope.model.info.push(info);
        };

        /**
         * Informational messages handling
         *
         * @param {type} msg
         * @returns {undefined}
         */
        $scope.deleteMessage = function(msg) {
            $log.info("deleteMessage()", msg);

            var arr = $scope.model.info;
            for (var i = arr.length; i--; ) {
                if (arr[i] === msg) {
                    arr.splice(i, 1);
                }
            }
        };

        /**
         * Automatically delete messages
         *
         * @param {type} msg
         * @returns {undefined}
         */
        $scope.timedDeleteMessage = function(msg) {
            setTimeout(function() {
                $scope.deleteMessage(msg);
                $scope.$digest();
            }, 5000);
        };

        /*
         * FILTERING
         */

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

        /**
         * Returns function thats checks if the given item matches the criteria defined.
         *
         * @returns {Function}
         */
        $scope.doFilter = new function() {
            return function(item, index, list) {
                // $log.debug("  filter: ", item, index);

                if (item === undefined) {
                    return false;
                }

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

        /**
         * Filters translations in list, used to search and narrow scope down
         *
         * @returns {undefined}
         */
        $scope.doFiltering = function() {
            var scope = this;
            debounce("filterLocalisations", function() {
                // $scope.model.filteredList = $filter()($scope.model.localisations, $scope.doFilter());
                scope.model.filteredList = _.filter(scope.model.localisations, scope.doFilter);
            }, 500);
        };


        /**
         * Reloads data from server.
         *
         * @returns {undefined}
         */
        $scope.reloadData = function() {
            $log.info("reloadData()");
            LocalisationService.reload().then(function(data) {
                $scope.model.localisations = data;
                $scope.doFiltering();
                $scope.pushMessage({status: "OK", message: "Käännökset uudelleen ladattu, " + data.length + " käännöstä."});

                // Clear modifications and selections
                $scope.clearChangedItems();
                $scope.clearSelectedItems();
            }, function(aa, bb, cc, dd) {
                $scope.model.info.push({status: "ERROR", aa: aa, bb: bb, cc: cc, dd: dd});
            });
        };

        /**
         * Hook from UI, triggered on row cell edit. See gridOptions - cell template for "value".
         *
         * @param {type} ngGridRow
         * @returns {undefined}
         */
        $scope.uiChanged = function(ngGridRow) {
            $log.info("uiChanged()", ngGridRow);
            $scope.getChangedItems()[ngGridRow.entity.id] = ngGridRow.entity;
        };

        /**
         * Saves all modifications and performs deletions of selected items.
         *
         * @returns {undefined}
         */
        $scope.saveAllModifiedNew = function() {
            $log.info("saveAllModifiedNew()", $scope.localisationsForm);

            var promises = [];

            $scope.pushMessage({status: "INFO", message: "Tallennetaan " + $scope.numChangedItems() + " muutosta..."});

            angular.forEach($scope.getChangedItems(), function(item) {
                $log.info("Save: ", item);

                // Forces to use server side ts
                delete item.modified;

                promises.push(
                    item.$update(function(data, headers) {
                        $log.info("Saved: ", data);
                    }, function(data, headers, xxx) {
                        $scope.pushMessage({status: "ERROR", data: data, headers: headers, xxx: xxx});
                }));
            });

            $scope.pushMessage({status: "INFO", message: "Poistetaan " + $scope.model.gridOptions.selectedItems.length + " käännöstä..."});

            angular.forEach($scope.getSelectedItems(), function(item) {
                $log.info("Delete: ", item);

                promises.push(
                        item.$delete({id: item.id}, function() {
                            $log.info("Käännös poistettu...", item);
                        }, function(aa, bb, cc, dd, ee) {
                            $scope.pushMessage({status: "ERROR", message: "Poisto epäonnistui", aa: aa, bb: bb, cc: cc, dd: dd, ee: ee});
                        }));
            });

            $q.all(promises).then(function() {
                $log.info("  all items saved completed... reload data.");
                $scope.pushMessage({status: "OK", message: "Kaikki muutokset ja poistot tehty."});
                $scope.reloadData();
            });

            // Mark form unmodified
            $scope.localisationsForm.$setPristine();

            // Remove modification / selection information
            $scope.clearChangedItems();
            $scope.clearSelectedItems();
        };

        /**
         * Copy from uri to current environment.
         *
         * @returns {undefined}
         */
        $scope.openDialog = function() {
            var modalInstance = $modal.open({
                scope: $scope,
                templateUrl: 'localisationTransferDialog2.html',
                controller: 'AppCtrl:TransferController'
            }).result.then(function(res) {
                $log.info("CLOSED: ", res);
                $scope.reloadData();
            });
        };

        /**
         * Open long text edit component.
         *
         * @returns {undefined}
         */
        $scope.openLongTextDialog = function(row) {

            $scope.model.longText = row.entity.value;

            var modalInstance = $modal.open({
                scope: $scope,
                templateUrl: 'localisationLongTextDialog.html'
            });

            modalInstance.result.then(function (result) {
                // OK - make the changes
                row.entity.value = $scope.model.longText;
                $scope.uiChanged(row);
            }, function (result) {
                // Cancelled, no change to be had
            });

        };

        // Trigger (re)load of localisations
        $scope.reloadData();
    }]);



/**
 * Transferring localisations from one environment to another.
 */
angular.module('app').controller('AppCtrl:TransferController', ['$scope', '$log', '$resource', 'LocalisationService', '$modalInstance',
    function($scope, $log, $resource, LocalisationService, $modalInstance) {

        $scope.model = {
            // Reppu by default
            // copyFrom: "https://test-virkailija.oph.ware.fi/lokalisointi/cxf/rest/v1/localisation",
            copyFrom: "",
            result: "",
            force : false
        };

        $scope.transferDialogCancel = function() {
            $log.info("transferDialogCancel()", $modalInstance);
            $modalInstance.close();
        };

        $scope.transferDialogOk = function() {
            $log.info("transferDialogOk()");

            $scope.model.result = "Ladataan käännöksiä...";

            // Get data, prededined uri's have "?value=NOCACHE"
            $resource($scope.model.copyFrom).query({},

                    // OK, translations loaded
                    function(data) {
                        console.log("  Käännökset ladattu:", data);
                        $scope.model.result = "OK, käännökset luettu, käsittelen...";

                        LocalisationService.massUpdate($scope.model.force, data).then(
                            function(result) {
                                console.log("  Käännökset puskettu serverille: muokattu=" + result.updated + ", luotu: " + result.created);
                                $scope.model.result = "OK; muokattu: " + result.updated + ", luotu: " + result.created + ", ei muutettu: " + result.notModified;
                                setTimeout(function() {
                                    $modalInstance.close();
                                }, 5000);
                            },
                            function(result) {
                                console.log("err result = ", result);
                                $scope.model.result = "Odottamaton virhe: " + result;
                                setTimeout(function() {
                                    $modalInstance.close();
                                }, 5000);
                            });
                    },
                    
                    // Failure, cannot load translations
                    function(err) {
                        console.log("ERROR, failed to load translations.", err);
                        $scope.model.result = "Käännösten lataaminen epäonnistui.";
                        setTimeout(function() {
                            $modalInstance.close();
                        }, 5000);
                    });
        };
    }]);

//
// "Production" mode?
//
angular.module('app').config(function($logProvider) {
    $logProvider.debugEnabled(true);
});
