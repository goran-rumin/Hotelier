var aplikacija = angular.module('hotelier', ['ngRoute', 'ngCookies', 'kontroleri']);

var PATH = "http://localhost:4567/";
var DOMAIN = "http://127.0.0.1:8020/Hotelier/";
var IMAGES = "http://localhost:4567/";
var IMAGES_SMALL = "http://localhost:4567/small/";
var BR_APP_PO_RETKU = 3;

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
      when('/accm/:id', {
        templateUrl: 'views/accommodation.html',
        controller: 'accOneController'
      }).
      otherwise({
        redirectTo: '/'
      });	
  }]);

aplikacija.controller('commonController', function ($scope, $cookieStore){
	$scope.go = function(path){
		location.href=DOMAIN+path;
	};
	$scope.getCountries = function(){
		$.post( PATH+'country/all', '', function( data ) {
			var main_json = angular.fromJson(data);
        	$scope.countries=main_json.data;
        	$scope.$apply();
		});
	};
	$scope.getAtypes = function(){
		$.post( PATH+'atype/all', '', function( data ) {
			var main_json = angular.fromJson(data);
        	$scope.atypes=main_json.data;
        	$scope.atypes.push({id: 0, name: "All"});
        	$scope.$apply();
		});
	};
	$scope.getName = function (id){
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
			console.log("ime  "+id);
			var name = main_json.data.name+' '+main_json.data.surname;
			var object={"name":name, "type":main_json.data.type};
        	$scope.user = object;
        	$scope.$apply();
        	console.log("kraj");
		});
	};
	$scope.convertDate = function(date){
		var djelovi = date.split(/[- :]/);
		var datum = new Date(djelovi[0], djelovi[1]-1, djelovi[2], djelovi[3], djelovi[4], djelovi[5]);
		return datum.toLocaleString();
	};
	$scope.kolacic = $cookieStore.get('SESSION');
});
aplikacija.controller('loginController', function ($scope, $cookieStore, $route){
	if($scope.kolacic!=null){
		$scope.getName($scope.kolacic);
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
        		$scope.getName(json_main.data.id);
        		console.log("pocetak");
        		$route.reload();
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
		console.log("logout  "+id);
		$.post( PATH+'user/logout', 'session_id='+id, function( data ) {
		});
		$cookieStore.remove('SESSION');
		$scope.kolacic=null;
		$route.reload();
	};
});
