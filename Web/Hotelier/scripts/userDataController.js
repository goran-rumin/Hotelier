
var apl_data = angular.module('userdata', ['ngCookies']);

apl_data.controller('userDataController', function($scope,$cookieStore){
	$scope.message=null;
	$scope.edit_password=false;
	$scope.getUserData = function(){
		$scope.id = $cookieStore.get('SESSION');
		if($scope.id==null){
			location.href = "#/";
			return;
		}
		$.post( PATH+'user/data', 'session_id='+$scope.id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
				return;
			}
        	$scope.data=main_json.data;
        	$scope.$apply();
		});
	};
	$scope.edit = function(){
		if($scope.edit_password==false){
			$scope.password='N/A';
		}
		else{
			if($scope.password==null){
				$scope.password='N/A';
				$scope.edit_password=false;
			}
			else{
				$scope.password=CryptoJS.SHA1($scope.password);
			}
		}
		if($scope.data.type=='Guest'){
			$scope.data.oib='N/A';
		}
		$.post( PATH+'user/edit', 'session_id='+$scope.id+'&username='+$scope.data.username+'&password='+$scope.password+'&name='+$scope.data.name+'&surname='+$scope.data.surname+'&oib='+$scope.data.oib+'&address='+$scope.data.address+'&city='+$scope.data.city+'&country_id='+$scope.data.country_id+'&phone='+$scope.data.phone+'&email='+$scope.data.email+'&date_birth='+$scope.data.date_birth, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				$scope.message=main_json.error.id+' - '+main_json.error.description;
				$scope.message_class='alert-danger';
			}
			else{
				$scope.message='Changes saved successfully.';
				$scope.message_class='alert-success';
			}
			$scope.password=null;
        	$scope.$apply();
		});
	};
	$scope.getStats = function(){
		$.post( PATH+'user/stats', 'session_id='+$scope.id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				
			}
			else{
				$scope.stats = main_json.data;
				$scope.stats.last_activity=$scope.convertDateTime($scope.stats.last_activity);
			}
        	$scope.$apply();
		});
	};
	$scope.getUserData();
	$scope.getCountries();
	$scope.getStats();
});