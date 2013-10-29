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

//
// Routing
//
angular.module('app').controller('AppRoutingCtrl', function($scope, $route, $routeParams, $log) {

    $log.debug("app.AppRoutingCtrl()");

    $scope.count = 0;

    var render = function() {
        $log.debug("app.AppRoutingCtrl.render()");

        var renderAction = $route.current.action;
        var renderPath = renderAction ? renderAction.split(".") : [];

        // Store the values in the model.
        $scope.renderAction = renderAction;
        $scope.renderPath = renderPath;
        $scope.routeParams = $routeParams ? $routeParams : {};
        $scope.count++;

        $log.debug("  renderAction: ", $scope.renderAction);
        $log.debug("  renderPath: ", $scope.renderPath);
        $log.debug("  routeParams: ", $scope.routeParams);
        $log.debug("  count: ", $scope.count);
    };

    $scope.$on(
            "$routeChangeSuccess",
            function($currentRoute, $previousRoute) {
                $log.debug("app.AppRoutingCtrl.$routeChangeSuccess : from, to = ", $currentRoute, $previousRoute);
                render();
            }
    );

});


//
// "Production" mode
//
angular.module('app').config(function($logProvider) {
    $logProvider.debugEnabled(true);
});
