
var apl_acc = angular.module('accm', ['ngCookies']);

apl_acc.controller('accommodationController', function ($scope){
	
	$scope.path = IMAGES_SMALL;
	$scope.class_n = '';
	$scope.class_p = 'disabled';
	$scope.cpage = 1;
	$scope.npage = 1;
	$scope.search_data = {};
	$scope.check = function(){
		if($scope.cpage==$scope.npage){
			$scope.class_n = 'disabled';
			return false;
		}
		else if($scope.cpage==1){
			$scope.class_p = 'disabled';
			return false;
		}
		else{
			$scope.class_n = '';
			$scope.class_p = '';
		}
		return true;
	};
	$scope.next = function(){
		if($scope.class_n==''){
			$scope.cpage+=1;
			$scope.getData();
		}
		$scope.check();
	};
	$scope.previous = function(){
		if($scope.class_p==''){
			$scope.cpage-=1;
			$scope.getData();
		}
		$scope.check();
	};
	$scope.getData = function(){
		$.post( PATH+'accommodation/all/guest', 'index='+$scope.cpage+'&search_params='+JSON.stringify($scope.search_data), function( data ) {
			var main_json = angular.fromJson(data);
			$scope.npage = main_json.data.pages;
			if(main_json.hasOwnProperty('error')){
				$scope.message=main_json.error.id+' - '+main_json.error.description;
			}
			else{
				var i;
				$scope.accommodation=[];
				for(i=0;i<main_json.data.acc.length;i+=BR_APP_PO_RETKU){
					var field = [];
					for(var j=0;j<BR_APP_PO_RETKU;j++){
						if(main_json.data.acc[i+j]!=null){
							field[j] = main_json.data.acc[i+j];
						}
					}
					$scope.accommodation.push(field);
				}
			}
			$scope.check();
        	$scope.$apply();
		});
	};
	
	var map;
	var cityCircle;
	var circleOptions;
	
	function initializeMap() {
		var myLatlng = new google.maps.LatLng(45.8203455, 15.2078808);
    	var mapOptions = {
      		center: myLatlng,
        	zoom: 8
        };
        map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
        
        circleOptions = {
      		strokeColor: '#337AB7',
      		strokeOpacity: 0.8,
      		strokeWeight: 5,
      		fillColor: '#D9EDF7',
      		fillOpacity: 0.35,
      		map: map,
      		center: myLatlng,
      		radius: 2500
    	};
    	
    	cityCircle = new google.maps.Circle(circleOptions);
    	cityCircle.setMap(null);
    	
    	google.maps.event.addListener(map, 'click', function(e) {
    		cityCircle.setMap(null);
    		circleOptions.center = e.latLng;
    		cityCircle = new google.maps.Circle(circleOptions);
    		map.panTo(circleOptions.center);
  		});
  	}
  	initializeMap();
  	
  	$scope.pickCoords = function(){
    	$("#dialog").dialog("open");
    	google.maps.event.trigger(map, 'resize');
    };
    
    $('#saveMap').click(function() {
    	if(circleOptions.center!=null){
    		$scope.search_data.lat = circleOptions.center.lat();
    		$scope.search_data.long = circleOptions.center.lng();
    	}
    	else{
    		delete $scope.search_data.lat;
    		delete $scope.search_data.long;
    	}
    	$("#dialog").dialog("close");
    	$scope.$apply();
    });
    
    $('#clearMap').click(function() {
    	cityCircle.setMap(null);
    	circleOptions.center = null;
    });
    
	$scope.getData($scope.cpage);
	$scope.getAtypes(1);
});
