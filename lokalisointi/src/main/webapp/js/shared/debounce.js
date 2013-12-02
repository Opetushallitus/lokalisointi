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

// This solution based on:
// see: http://stackoverflow.com/questions/13320015/how-to-write-a-debounce-service-in-angularjs

var app = angular.module('debounce', []);

app.factory('debounce', ['$timeout', '$log', function($timeout, $log) {
        $log.info("debounce initializing...");

        var laters = {};

        /**
         * Delay execution for given function.
         *
         * Usage: debounce("autosave", function() { doAutosave(my, args, here) }, 5000);
         *
         *
         * @param string key if the debounce exisis with given key, then it will be removed and re-created
         * @param function fn function to be delayed
         * @param int timeout milliseconds
         * @param boolean enableDirtyCheck true by default, enables dirty check in digest cycle
         * @returns {undefined}
         */
        var debounce = function(key, fn, timeout, enableDirtyCheck) {
            timeout = angular.isUndefined(timeout) ? 1000 : timeout;
            enableDirtyCheck = angular.isUndefined(enableDirtyCheck) ? true : enableDirtyCheck;

            $log.info("debounce(): ", key, fn, timeout, enableDirtyCheck);

            var later = function() {
                $log.debug("later()", key);

                // call the FN? How to get parameters without creating lambada out of the caller?
                fn();

                // Remove this from to be called array
                delete laters[key];
            };

            if (laters[key] !== undefined) {
                $log.info("  cancelling existing entry: ", key, laters[key]);

                // Cancel and remove
                $timeout.cancel(laters[key]);
                delete laters[key];
            }

            // Add timeout function call to executed later
            laters[key] = $timeout(later, timeout, enableDirtyCheck);
        };

        $log.info("debounce initializing... done.");

        return debounce;
    }]);
