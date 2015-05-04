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
	$scope.search_data = {};
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
	$scope.getData($scope.cpage);
	$scope.getAtypes();
});

aplikacija.controller('accOneController', function ($scope, $routeParams, $cookieStore){
	$scope.acc_id = $routeParams.id;
	$scope.PATH_SMALL = IMAGES_SMALL;
	$scope.PATH = IMAGES;
	$scope.kolacic=$cookieStore.get('SESSION');
	$scope.comment={rating: 1};
	$scope.stars = ['glyphicon-star-empty', 'glyphicon-star-empty', 'glyphicon-star-empty', 'glyphicon-star-empty', 'glyphicon-star-empty'];
	$scope.monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
	
	$scope.getCalendar = function(){
		$scope.calendar = [];
		var date = new Date();
		var current = new Date();
		var display = 0;
		$scope.currentMonth = current.getMonth();
		while(display<3){
			var mjesec=[];
			var tjedan=[];
			date.setDate(1);
			var broj = date.getDay();
			for(var i=1;i<broj;i++){
				tjedan.push({class : "active"});
			}
			while(date.getMonth()==current.getMonth()){
				if(date.getDay()==1){
					mjesec.push(tjedan);
					tjedan=[];
				}
				tjedan.push({class : "success"});
				date.setDate(date.getDate()+1);
			}
			for(var i=tjedan.length;i<7;i++){
				tjedan.push({class : "active"});
			}
			display++;
			mjesec.push(tjedan);
			$scope.calendar.push(mjesec);
			current.setMonth(current.getMonth()+1);
		}
		//console.log(JSON.stringify($scope.calendar));
	};
	
	$scope.getData = function(){
		$.post( PATH+'accommodation/one/guest', 'acc_id='+$scope.acc_id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				location.href='#/';
			}
			else{
				$scope.data = main_json.data;
				var images = $scope.data.images;
				$scope.data.images = [];
				var subimages=[];
				for(var i=0;i<images.length;i++){
					subimages.push(images[i]);
					if(i%6==5){
						$scope.data.images.push(subimages);
						subimages=[];
					}
				}
				if(subimages.length!=0){
					$scope.data.images.push(subimages);
				}
				for(var i=0;i<$scope.data.comments.length;i++){
					$scope.data.comments[i].time = $scope.convertDate($scope.data.comments[i].time);
				}
				//console.log(JSON.stringify($scope.data));
			}
        	$scope.$apply();
		});
	};
	
	$scope.getMyComment = function(){
		$.post( PATH+'comment/my', 'session_id='+$scope.kolacic+'&acc_id='+$scope.acc_id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				location.href='#/';
			}
			else{
				$scope.comment = main_json.data;
				for(var i=0;i<$scope.comment.rating;i++){
					$scope.stars[i] = 'glyphicon-star';
				}
			}
        	$scope.$apply();
		});
	};
	
	$scope.forLoop = function(number){
		var array = [];
		for(var i=0;i<number;i++){
			array.push(i);
		}
		return array;
	};
	
	$scope.rate = function(rating){
		for(var i=0;i<=rating;i++){
			$scope.stars[i] = 'glyphicon-star';
		}
		for(var i=rating+1;i<5;i++){
			$scope.stars[i] = 'glyphicon-star-empty';
		}
		$scope.comment.rating=rating+1;
	};
	
	$scope.post = function(){
		$.post( PATH+'comment/add', 'session_id='+$scope.kolacic+'&acc_id='+$scope.acc_id+'&text='+$scope.comment.text+'&rating='+$scope.comment.rating, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert(main_json.error.id+' - '+main_json.error.description);
			}
			else{
				var datum = new Date();
				$scope.data.comments.unshift({name: $scope.user.name, text: $scope.comment.text, rating: $scope.comment.rating, time: datum.toLocaleString()});
				$scope.can_comment=false;
			}
        	$scope.$apply();
		});
	};
	
	$scope.getCalendar();
	$scope.getData();
	if($scope.kolacic!=null){
		$scope.getMyComment();
	}
	function prepareCommentRegion(){
		if($scope.kolacic==null){
			$scope.comment_placeholder='You have to be logged in to comment';
			$scope.can_comment=false;
		}
		else if($scope.user.type=='Owner'){
			$scope.comment_placeholder='Only guests can comment';
			$scope.can_comment=false;
		}
		else if($scope.comment.text==null){
			$scope.comment_placeholder='Enter your comment...';
			$scope.can_comment=true;
		}
		else{
			$scope.can_comment=false;
		}
		$scope.$apply();
	}
	setTimeout(prepareCommentRegion, 1000);
});
