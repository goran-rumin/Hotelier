
var apl_rez = angular.module('myreservations', ['ngCookies']);

apl_rez.controller('myReservationsController', function($scope, $cookieStore){
	$scope.kolacic = $cookieStore.get('SESSION');
	if($scope.kolacic==null){
			location.href = "#/";
			return;
	}
	$scope.getReservations = function(){
		$.post( PATH+'reservation/all/guest', 'session_id='+$scope.kolacic, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				for(var i=0; i<main_json.data.length; i++){
					main_json.data[i].date_from = $scope.convertDate(main_json.data[i].date_from);
					main_json.data[i].date_until = $scope.convertDate(main_json.data[i].date_until);
					main_json.data[i].validity_date = $scope.convertDateTime(main_json.data[i].validity_date);
					if(main_json.data[i].status=='Confirmed' || main_json.data[i].status=='Completed'){
						main_json.data[i].class = 'success';
					}
					else if(main_json.data[i].status=='Pending'){
						main_json.data[i].class = 'warning';
					}
					else{
						main_json.data[i].class = 'danger';
					}
				}
				$scope.reservations = main_json.data;
			}
			$scope.$apply();
		});
	};
	
	$scope.deleteReservation = function(res_id){
		var status = confirm("Are you sure you want to cancel this reservation?");
		if (status == true) {
    		$.post( PATH+'reservation/delete/guest', 'session_id='+$scope.kolacic+'&res_id='+res_id, function( data ) {
    			var main_json = angular.fromJson(data);
    			if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
					$scope.loggedOut();
				}
				else{
					$scope.getReservations();
				}
    		});
		}
	};
	
	$scope.getReservations();
});
