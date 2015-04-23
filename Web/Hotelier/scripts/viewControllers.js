/**
 * @author Goran
 */

var aplikacija = angular.module('kontroleri', ['ngCookies']);

aplikacija.controller('userDataController', function($scope,$cookieStore){
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
				location.href = "#/";
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
			}
        	$scope.$apply();
		});
	};
	$scope.getUserData();
	$scope.getCountries();
	$scope.getStats();
});

aplikacija.controller('registerController', function ($scope, $cookieStore){
	$scope.kolacic = $cookieStore.get('SESSION');
	if($scope.kolacic!=null){
		location.href='#/';
	}
	$scope.data={};
	$scope.data.type='Guest';
	$scope.data.oib='N/A';
	$scope.getCountries();
	$('#guest_button').toggleClass('active');
	$scope.selectType= function(type){
		if($scope.data.type!=type){
			$scope.data={};
			$scope.data.type=type;
			if(type=='Guest'){
				$scope.data.oib='N/A';
			}
			$('#guest_button').toggleClass('active');
			$('#owner_button').toggleClass('active');
		}
	};
	$scope.register= function(){
		if($scope.data.username==null || $scope.data.password==null || $scope.data.name==null || $scope.data.surname==null || $scope.data.country_id==null || $scope.data.oib==null || $scope.data.date_birth==null){
			$scope.message='Please fill out the requiered form fields';
		}
		else{
			$scope.password=CryptoJS.SHA1($scope.data.password);
			if($scope.data.address==null){
				$scope.data.address='N/A';
			}
			if($scope.data.city==null){
				$scope.data.city='N/A';
			}
			if($scope.data.phone==null){
				$scope.data.phone='N/A';
			}
			$.post( PATH+'user/register/'+$scope.data.type.toLowerCase(), 'username='+$scope.data.username+'&password='+$scope.password+'&name='+$scope.data.name+'&surname='+$scope.data.surname+'&oib='+$scope.data.oib+'&address='+$scope.data.address+'&city='+$scope.data.city+'&country_id='+$scope.data.country_id+'&phone='+$scope.data.phone+'&email='+$scope.data.email+'&date_birth='+$scope.data.date_birth, function( data ) {
				var main_json = angular.fromJson(data);
				console.log(""+data);
				if(main_json.hasOwnProperty('error')){
					$scope.message=main_json.error.id+' - '+main_json.error.description;
				}
				else{
					$cookieStore.put('SESSION',main_json.data.id);
					location.href='#/';
					location.reload(true);
				}
        		$scope.$apply();
			});
		}
	};
});

aplikacija.controller('accomodationController', function ($scope){
	$scope.path = PATH;
	$scope.class_n = '';
	$scope.class_p = 'disabled';
	$scope.cpage = 1;
	$scope.npage = 1;
	$scope.proba = function(){
		alert('proba2');
	};
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
		}
		$scope.check();
	};
	$scope.previous = function(){
		if($scope.class_p==''){
			$scope.cpage-=1;
		}
		$scope.check();
	};
	$scope.getData = function(index){
		$.post( PATH+'accommodation/all/guest', 'index='+$scope.cpage, function( data ) {
			var main_json = angular.fromJson(data);
			console.log(""+data);
			$scope.npage = main_json.data.pages;
			if(main_json.hasOwnProperty('error')){
				$scope.message=main_json.error.id+' - '+main_json.error.description;
			}
			else{
				$scope.accommodation = main_json.data.acc;
			}
			$scope.check();
        	$scope.$apply();
		});
	};
	$scope.getData($scope.cpage);
});

