
var apl_accOwner = angular.module('accm_owner', ['ngCookies']);

apl_accOwner.controller('accOwnerController', function($scope, $cookieStore){
	$scope.PATH_SMALL = IMAGES_SMALL;
	$scope.PATH = IMAGES;
	$scope.kolacic = $cookieStore.get('SESSION');
	if($scope.kolacic==null){
			location.href = "#/";
			return;
	}
	
	$scope.getAccommodations = function(){
		$.post( PATH+'accommodation/all/owner', 'session_id='+$scope.kolacic, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.accommodations = main_json.data;
			}
			$scope.$apply();
			var first;
			for (first in $scope.accommodations) break;
			$scope.getAccommodation($scope.accommodations[first][0].id);
		});
	};
	
	$scope.getAccommodation = function(id){
		$.post( PATH+'accommodation/one/owner', 'session_id='+$scope.kolacic+'&acc_id='+id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.accommodation = main_json.data;
			}
			$scope.$apply();
			$('.datum').datetimepicker({
				timepicker:false,
				closeOnDateSelect:true,
				dayOfWeekStart: 1,
				format:'Y-m-d',
			});
		});
	};
	
	$scope.upload_text = 'Upload';
	$scope.upload = function(){
		if($scope.picture==null){
			$scope.message = 'Select picture first';
			$scope.picture_success = true;
			return;
		}
		else if($scope.picture.filetype!='image/jpeg'){
			$scope.message = 'You can upload only .JPG pictures';
			$scope.picture_success = true;
			return;
		}
		else if($scope.picture.filesize>4096000){
			$scope.message = 'Maximum picture size is 4 MB';
			$scope.picture_success = true;
			return;
		}
		$scope.picture_success = false;
		$scope.upload_text = 'Uploading';
		
		var start_index = 0, end_index = 99900, increment = 99900, i = 1;
		var flag, dio_slike;
		broj_ponavljanja = Math.floor($scope.picture.base64.length/99900) + 1;
		if(end_index > $scope.picture.base64.length){
			end_index = $scope.picture.base64.length;
		}
		var petlja = setInterval(function(){
			if(i==broj_ponavljanja){
				flag = 'end';
				dio_slike = $scope.picture.base64.substring(start_index, end_index);
			}
			else{
				flag = 'sending';
				dio_slike = $scope.picture.base64.substring(start_index, end_index);
				start_index+=increment;
				end_index+=increment;
				if(end_index > $scope.picture.base64.length){
					end_index = $scope.picture.base64.length;
				}
			}
			$.post( PATH+'image/upload', 'session_id='+$scope.kolacic+'&base64='+dio_slike+'&flag='+flag, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
					$scope.loggedOut();
				}
				else{
					if(main_json.hasOwnProperty('data')){
						$scope.accommodation.main_pic = main_json.data.file;
						$scope.upload_text = 'Upload';
						$scope.$apply();
					}
				}
			});
			i+=1;
			if(i>broj_ponavljanja){
				clearInterval(petlja);
			}
		}, 1000);
	};
	
	$scope.saved = false;
	$scope.edit = function(){
		if($scope.accommodation.id==null){
			$.post( PATH+'accommodation/add', 'session_id='+$scope.kolacic+'&object_id='+$scope.accommodation.object_id+'&name='+$scope.accommodation.name+'&category='+$scope.accommodation.category
			+'&surface='+$scope.accommodation.surface+'&sea='+$scope.accommodation.sea+'&air='+$scope.accommodation.air+'&sattv='+$scope.accommodation.sattv+'&balcony='+$scope.accommodation.balcony+'&breakfast='+$scope.accommodation.breakfast+'&pets='+$scope.accommodation.pets
			+'&beach_distance='+$scope.accommodation.beach_distance+'&main_pic='+$scope.accommodation.main_pic+'&desc='+$scope.accommodation.desc+'&acc_type_id='+$scope.accommodation.acc_type_id, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
				}
				else{
					$scope.saved = true;
				}
				$scope.$apply();
				$scope.getAccommodations();
			});
		}
		else{
			$.post( PATH+'accommodation/edit', 'session_id='+$scope.kolacic+'&acc_id='+$scope.accommodation.id+'&object_id='+$scope.accommodation.object_id+'&name='+$scope.accommodation.name+'&category='+$scope.accommodation.category
			+'&surface='+$scope.accommodation.surface+'&sea='+$scope.accommodation.sea+'&air='+$scope.accommodation.air+'&sattv='+$scope.accommodation.sattv+'&balcony='+$scope.accommodation.balcony+'&breakfast='+$scope.accommodation.breakfast+'&pets='+$scope.accommodation.pets
			+'&beach_distance='+$scope.accommodation.beach_distance+'&main_pic='+$scope.accommodation.main_pic+'&desc='+$scope.accommodation.desc+'&acc_type_id='+$scope.accommodation.acc_type_id, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
				}
				else{
					$scope.saved = true;
				}
				$scope.$apply();
			});
		}
	};
	
	$scope.add = function(){
		$scope.accommodation = {};
		$scope.accommodation.sea = false;
		$scope.accommodation.air = false;
		$scope.accommodation.sattv = false;
		$scope.accommodation.balcony = false;
		$scope.accommodation.breakfast = false;
		$scope.accommodation.pets = false;
	};
	
	$scope.upload_text2 = 'Upload picture';
	$scope.addImage = function(){
		if($scope.image==null){
			$scope.message2 = 'Select picture first';
			$scope.picture_success2 = true;
			return;
		}
		else if($scope.image.filetype!='image/jpeg'){
			$scope.message2 = 'You can upload only .JPG pictures';
			$scope.picture_success2 = true;
			return;
		}
		else if($scope.image.filesize>4096000){
			$scope.message2 = 'Maximum picture size is 4 MB';
			$scope.picture_success2 = true;
			return;
		}
		$scope.picture_success2 = false;
		$scope.upload_text2 = 'Uploading';
		
		var start_index = 0, end_index = 99900, increment = 99900, i = 1;
		var flag, dio_slike;
		broj_ponavljanja = Math.floor($scope.image.base64.length/99900) + 1;
		if(end_index > $scope.image.base64.length){
			end_index = $scope.image.base64.length;
		}
		var petlja = setInterval(function(){
			if(i==broj_ponavljanja){
				flag = 'end';
				dio_slike = $scope.image.base64.substring(start_index, end_index);
			}
			else{
				flag = 'sending';
				dio_slike = $scope.image.base64.substring(start_index, end_index);
				start_index+=increment;
				end_index+=increment;
				if(end_index > $scope.image.base64.length){
					end_index = $scope.image.base64.length;
				}
			}
			$.post( PATH+'image/upload', 'session_id='+$scope.kolacic+'&base64='+dio_slike+'&flag='+flag, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
					$scope.loggedOut();
				}
				else{
					if(main_json.hasOwnProperty('data')){
						$scope.upload_text2 = 'Upload';
						$scope.accommodation.images.push({id:-1, value: main_json.data.file});
						$scope.$apply();
					}
				}
			});
			i+=1;
			if(i>broj_ponavljanja){
				clearInterval(petlja);
			}
		}, 1000);
	};
	
	$scope.saveImages = function(){
		$.post( PATH+'image/save', 'session_id='+$scope.kolacic+'&acc_id='+$scope.accommodation.id+'&images='+JSON.stringify($scope.accommodation.images), function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.getAccommodation($scope.accommodation.id);
			}
			$scope.$apply();
		});
	};
	
	$scope.deleteImage = function(id){
		$.post( PATH+'image/delete', 'session_id='+$scope.kolacic+'&image_id='+id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.getAccommodation($scope.accommodation.id);
			}
			$scope.$apply();
		});
	};
	
	$scope.savePrices = function(){
		var sada = new Date();
		for(var i=1; i<$scope.accommodation.prices.length; i++){
			var datum_prvi = $scope.createDate($scope.accommodation.prices[i].date_from);
			var datum_drugi = $scope.createDate($scope.accommodation.prices[i].date_until);
			if(datum_prvi.getTime()>datum_drugi.getTime()){
				alert('"Date from" cannot be bigger than "Date until"');
				return;
			}
			if(datum_prvi.getFullYear()!=sada.getFullYear() || datum_drugi.getFullYear()!=sada.getFullYear()){
				alert('Please enter prices for this year only');
				return;
			}
			datum_prvi = $scope.createDate($scope.accommodation.prices[i-1].date_until);
			datum_drugi = $scope.createDate($scope.accommodation.prices[i].date_from);
			if(datum_prvi.getTime()>datum_drugi.getTime()){
				alert('Price ranges cannot overlap');
				return;
			}
		}
		$.post( PATH+'prices/add', 'session_id='+$scope.kolacic+'&acc_id='+$scope.accommodation.id+'&prices='+JSON.stringify($scope.accommodation.prices), function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$scope.prices_success = true;
			}
			$scope.$apply();
		});
	};
	
	$scope.addType = function(){
		$("#dialog_addtype").dialog("open");
	};
	
	$("#saveType").click(function(){
		var name = $('#type_name').val();
		var ppl_min = $('#people_min').val();
		var ppl_max = $('#people_max').val();
		if(isNaN(ppl_min) || isNaN(ppl_max) || name.length==0){
			alert('Please enter correct data');
			return;
		}
		$.post( PATH+'atype/add', 'session_id='+$scope.kolacic+'&name='+name+'&min='+ppl_min+'&max='+ppl_max, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
			}
			else{
				$("#dialog_addtype").dialog("close");
			}
			$scope.$apply();
			$scope.getAtypes();
		});
	});
	
	$scope.getObjects($scope.kolacic);
	$scope.getAtypes();
	$scope.getAccommodations();
});
