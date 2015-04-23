var aplikacija = angular.module('hotelier', ['ngRoute', 'ngCookies', 'kontroleri']);

var PATH = "http://localhost:4567/";

aplikacija.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/', {
        templateUrl: 'views/all.html',
        controller: 'accomodationController'
      }).
      when('/mydata', {
        templateUrl: 'views/mydata.html',
        controller: 'userDataController'
      }).
      when('/register', {
        templateUrl: 'views/register.html',
        controller: 'registerController'
      }).
      otherwise({
        redirectTo: '/'
      });	
  }]);

aplikacija.controller('commonController', function ($scope){
	$scope.go = function(path){
		location.href=path;
	};
	$scope.getCountries = function(){
		$.post( PATH+'country/all', '', function( data ) {
			var main_json = angular.fromJson(data);
        	$scope.countries=main_json.data;
        	$scope.$apply();
		});
	};
});
aplikacija.controller('loginController', function ($scope, $cookieStore){
	$scope.kolacic = $cookieStore.get('SESSION');
	function getName(id){
		$.post( PATH+'user/data', 'session_id='+id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('You have been automatically logged out due to inactivity');
				$cookieStore.remove('SESSION');
				$scope.kolacic=null;
				$scope.$apply();
				location.href = "#/";
				return;
			}
			var name = main_json.data.name+' '+main_json.data.surname;
			var object={"name":name, "type":main_json.data.type};
        	$scope.user = object;
        	$scope.$apply();
		});
	};
	
	if($scope.kolacic!=null){
		getName($scope.kolacic);
	}
	
	$scope.login = function (){
		var password_hash = CryptoJS.SHA1($scope.password);
		$.post( PATH+'user/login', 'username='+$scope.username+'&password='+password_hash, function( data ) {
        	json_main=angular.fromJson(data);
        	if(json_main.hasOwnProperty('error')){
        		$('#loginForm').popover({
    				placement: "bottom",
    				container: 'body',
    				trigger: 'manual',
        			content: 'Error '+json_main.error.id+' - '+json_main.error.description
    			}).popover('show');
        	}
        	else{
        		$('#loginForm').popover('hide');
        		$scope.errorcode=null;
        		$cookieStore.put('SESSION', json_main.data.id);
        		$scope.kolacic = $cookieStore.get('SESSION');
        		getName(json_main.data.id);
        	}
        	$scope.$apply();
		});
	};
	$scope.logout = function (){
		var id = $cookieStore.get('SESSION');
		if(id==null){
			location.href = "#/";
			return;
		}
		$.post( PATH+'user/logout', 'session_id='+id, function( data ) {
		});
		$cookieStore.remove('SESSION');
		$scope.kolacic=null;
		location.href = "#/";
	};
});
