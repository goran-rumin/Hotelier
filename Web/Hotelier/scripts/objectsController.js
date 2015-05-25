
var apl_obj = angular.module('objects', ['ngCookies']);

apl_obj.controller('objectsController', function($scope, $cookieStore){
	$scope.kolacic = $cookieStore.get('SESSION');
	if($scope.kolacic==null){
			location.href = "#/";
			return;
	}
	
	$scope.getObjects = function(){
		$.post( PATH+'object/all', 'session_id='+$scope.kolacic, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.objects = main_json.data;
			}
			$scope.$apply();
			$scope.getObject($scope.objects[0].id);
		});
	};
	$scope.getObject = function(id){
		if(id==null){
			return;
		}
		$.post( PATH+'object/one', 'session_id='+$scope.kolacic+'&object_id='+id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.object = main_json.data;
				$scope.object.id = id;
			}
			$scope.$apply();
		});
	};
	
	$scope.saved = false;
	$scope.edit = function(){
		if($scope.object.id!=null){
			$.post( PATH+'object/edit', 'session_id='+$scope.kolacic+'&object_id='+$scope.object.id+'&name='+$scope.object.name+'&desc='+$scope.object.desc+'&addr='+$scope.object.addr+'&city='+$scope.object.city+'&country_id='+$scope.object.country_id+'&lat='+$scope.object.lat+'&long='+$scope.object.long, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
					$scope.saved = false;
				}
				else{
					$scope.saved = true;
				}
				$scope.$apply();
			});
		}
		else{
			$.post( PATH+'object/add', 'session_id='+$scope.kolacic+'&name='+$scope.object.name+'&desc='+$scope.object.desc+'&addr='+$scope.object.addr+'&city='+$scope.object.city+'&country_id='+$scope.object.country_id+'&lat='+$scope.object.lat+'&long='+$scope.object.long, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
					$scope.saved = false;
				}
				else{
					$scope.saved = true;
				}
				$scope.$apply();
				$scope.getObjects();
			});
		}
	};
	
	$scope.saved_owners = false;
	$scope.saveOwners = function(){
		var owners = {};
		var suma = 0;
		for(var i=0; i<$scope.object.owners.length; i++){
			if(isNaN($scope.object.owners[i].percentage)){
				alert('Percentages are numbers...');
				return;
			}
			owners[$scope.object.owners[i].id] = $scope.object.owners[i].percentage;
			suma += $scope.object.owners[i].percentage;
		}
		if(suma!=100){
			alert('Sum of percentages needs to be 100%');
			return;
		}
		$.post( PATH+'object/edit/owners', 'session_id='+$scope.kolacic+'&object_id='+$scope.object.id+'&owners='+JSON.stringify(owners), function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				$scope.saved_owners = false;
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.saved_owners = true;
			}
			$scope.$apply();
			$scope.getObject($scope.object.id);
		});
	};
	
	$scope.addOwner = function(){
		$("#dialog_owner").dialog("open");
		$('#oib').val('');
		$('#percentage').val('');
	};
	
	$scope.deleteOwner = function(owner_id, percentage){
		var status = confirm("Are you sure you want to delete this owner?");
		if (status == true) {
			$.post( PATH+'object/delete/owner', 'session_id='+$scope.kolacic+'&object_id='+$scope.object.id+'&user_id='+owner_id+'&percentage='+percentage, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
				}
				else{
					
				}
				$scope.$apply();
				$scope.getObject($scope.object.id);
			});
		}
	};
	
	$scope.add = function(){
		$scope.object = {};
	};
	
	$("#saveOwner").click(function(){
		var oib = $('#oib').val();
		var percentage = $('#percentage').val();
		if(isNaN(oib) || isNaN(percentage)){
			alert('Please enter numbers');
			return;
		}
		if(percentage>$scope.object.owners[0].percentage){
			alert("You can't give more ownership than you have. Enter number below "+$scope.object.owners[0].percentage+" %.");
			return;
		}
		if(percentage==0){
			alert('No one can be 0% owner...');
			return;
		}
		$.post( PATH+'object/add/owner', 'session_id='+$scope.kolacic+'&object_id='+$scope.object.id+'&oib='+oib+'&percentage='+percentage, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$("#dialog_owner").dialog("close");
			}
			$scope.$apply();
			$scope.getObject($scope.object.id);
		});
	});
	
    var map;
    var marker_options;
	var marker;
	
	$scope.pickCoords = function(){
    	$("#dialog_objects").dialog("open");
    	initializeMap();
    	//google.maps.event.trigger(map, 'resize');
    	if($scope.object.lat!=null){
    		map.panTo(new google.maps.LatLng($scope.object.lat, $scope.object.long));
    	}
    	else{
    		map.panTo(new google.maps.LatLng(44, 14));
    	}
    	
    };
    
    $('#saveMapObjects').click(function() {
    	$scope.object.lat = marker_options.position.lat();
    	$scope.object.long = marker_options.position.lng();
    	$("#dialog_objects").dialog("close");
    	$scope.$apply();
    });
	
	function initializeMap() {
		var myLatlng;		
		if($scope.object.lat!=null){
			myLatlng = new google.maps.LatLng($scope.object.lat, $scope.object.long);
		}
		else{
			myLatlng = new google.maps.LatLng(44, 14);
		}
    	var mapOptions = {
      		center: myLatlng,
        	zoom: 8
        };
        map = new google.maps.Map(document.getElementById('map-canvas-objects'), mapOptions);
        
        marker_options = {
      		position: myLatlng,
      		map: map,
      		title: 'Object'
  		};
  		if($scope.object.lat!=null){
        	marker = new google.maps.Marker(marker_options);
       	}
  		
  		google.maps.event.addListener(map, 'click', function(e) {
  			marker.setMap(null);
  			marker_options.position = e.latLng;
  			marker = new google.maps.Marker(marker_options);
  			map.panTo(e.latLng);
  		});
  	}
  	
  	setTimeout(initializeMap, 1000);
    
	$scope.getObjects();
	$scope.getCountries();
});
