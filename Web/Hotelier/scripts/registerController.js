
var apl_reg = angular.module('register', ['ngCookies']);

apl_reg.controller('registerController', function ($scope, $cookieStore){
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

