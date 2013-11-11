

// TESTING
//
// <style>
// [editable] {width:220px; height:20px; font-size:15px; border:solid 1px #ddd; background-color:#eee;}
// </style>
//
// <div editable="" ng-model="content"></div>

angular.module('app', []).directive('editable', function() {
  return {
    require: 'ngModel',
    link: function(scope, element, attr, ctrl) {
      element.css({'cursor':'text', 'overflow':'hidden'});
      element.on('click', function(e) {
        var oldText = element.text();
        element.empty();
        var input = $('<input type="text"/>')
          .css({'padding':'0px', 'border':'none', 'margin':'0px'})
          .width(element.width())
          .height(element.height())
          .appendTo(element)
          .focus()
          .val(oldText)
          .on('blur', function(e) {
            element.text(input.val());
            input.remove();
            scope.$apply(function() {
              ctrl.$setViewValue(element.html());
            });
          });
      });
      ctrl.render = function(value) {
        element.html(value);
      };
      ctrl.$setViewValue(element.html());
    }
  };
});
