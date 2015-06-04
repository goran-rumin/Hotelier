
var apl_reps = angular.module('reports', ['ngCookies']);

apl_reps.controller('reportsController', function($scope, $cookieStore){
	$scope.kolacic = $cookieStore.get('SESSION');
	$scope.rep_type = 'rep1';
	$scope.report1 = {};
	$scope.report2 = {};
	$scope.report3 = {};
	$scope.params = {};
	$scope.guest = {};
	if($scope.kolacic==null){
			location.href = "#/";
			return;
	}
	
	$scope.getArrivals = function(){
		$.post( PATH+'report/arrivals', 'session_id='+$scope.kolacic+'&date_from='+$scope.params.date_from+'&date_until='+$scope.params.date_until, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.report1 = main_json.data;
			}
			$scope.$apply();
		});
	};
	
	$scope.getRevenue = function(){
		$.post( PATH+'report/revenue', 'session_id='+$scope.kolacic+'&date_from='+$scope.params.date_from+'&date_until='+$scope.params.date_until, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.report2 = main_json.data;
			}
			$scope.$apply();
			drawChart();
		});
	};
	
	function drawChart() {
    	var data = new google.visualization.DataTable();
        data.addColumn('string', 'Accommodation');
        data.addColumn('number', 'Revenue (€)');
        for(var i=0;i<$scope.report2.length;i++){
        	data.addRow([$scope.report2[i].name, $scope.report2[i].revenue]);
        }
        
        var options = {'title':'Revenue per accommodation',
                       'width': '100%',
                       'height': 500};
        
        var chart = new google.visualization.BarChart(document.getElementById('chart_div'));
        chart.draw(data, options);	
  	}
  	
  	setTimeout(function(){
  		google.load('visualization', '1', {'callback':'', 'packages':['corechart']});
  	}, 500);

	$scope.getGuests = function(){
		$.post( PATH+'report/guests', 'session_id='+$scope.kolacic, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.guests = main_json.data;
			}
			$scope.$apply();
		});
	};
	
	$scope.addGuest = function(){
		$.post( PATH+'report/guests/add', 'session_id='+$scope.kolacic+'&name='+$scope.guest.name+'&surname='+$scope.guest.surname+'&country_id='+$scope.guest.country_id+'&doc_num='+$scope.guest.doc_num+'&email='+$scope.guest.email+'&date_birth='+$scope.guest.date_birth, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.getGuests();
				$scope.guest = {};
			}
			$scope.$apply();
		});
	};
	
	$scope.getGuestList = function(){
		$.post( PATH+'report/guestlist', 'session_id='+$scope.kolacic, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.report3 = main_json.data;
			}
			$scope.$apply();
		});
	};
	
	$scope.saveList = function(){
		$.post( PATH+'report/guestlist/add', 'session_id='+$scope.kolacic+'&guest_id='+$scope.params.guest_id+'&date_from='+$scope.params.date_from+'&date_until='+$scope.params.date_until, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.getGuestList();
				$scope.params = {};
			}
			$scope.$apply();
		});
	};
	
	$scope.getCountries();
	$scope.getGuests();
	$scope.getGuestList();
});
