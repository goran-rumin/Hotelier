
var apl_accOne = angular.module('accm_one', ['ngCookies']);

apl_accOne.controller('accOneController', function ($scope, $routeParams, $cookieStore){
	$scope.acc_id = $routeParams.id;
	$scope.PATH_SMALL = IMAGES_SMALL;
	$scope.PATH = IMAGES;
	$scope.kolacic = $cookieStore.get('SESSION');
	$scope.res_data = {}; 
	$scope.comment={rating: 1};
	$scope.stars = ['glyphicon-star-empty', 'glyphicon-star-empty', 'glyphicon-star-empty', 'glyphicon-star-empty', 'glyphicon-star-empty'];
	$scope.monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
	
	$scope.getCalendar = function(){
		$.post( PATH+'reservation/accommodation', 'acc_id='+$scope.acc_id, function( data ) {
			var main_json = angular.fromJson(data);
			generateCalendar(main_json.data);
			$scope.$apply();
		});
	};
	
	function generateCalendar(rezervacije){
		$scope.calendar = [];
		var date = new Date();
		date.setHours(0,0,0,0);
		var current = new Date();
		var u_rezervaciji = false;  //da li se crta zauzet dio
		var index_rez = 0;  //koju rez se trenutacno obradjuje
		var sljedeci = false; //nakon zavrsetka rezervacije, gost odlazi sljedece jutro
		$scope.currentMonth = current.getMonth();
		for(var f=0;f<4;f++){
			var mjeseci = [];
			var display = 0;
			while(display<3){
				var mjesec=[];
				var tjedan=[];
				date.setDate(1);
				var broj = date.getDay();
				if(broj==0){
					broj=7;
				}
				for(var i=1;i<broj;i++){
					tjedan.push({class : "active"});
				}
				while(date.getMonth()==current.getMonth()){
					var dan = {};
					if(tjedan.length==7){  //date.getDay()==1
						mjesec.push(tjedan);
						tjedan=[];
					}
					if(u_rezervaciji){
						dan.class = "zauzeto";
						var a = $scope.convertDate(rezervacije[index_rez].date_until);
						var b = date.toLocaleDateString();
						if(a == b){
							sljedeci = true;
							u_rezervaciji = false;
							index_rez++;
						}
					}
					else{
						if(sljedeci){
							dan.left = true;
							sljedeci = false;
						}
						dan.class = "success";
						if(index_rez < rezervacije.length){
							var a = $scope.convertDate(rezervacije[index_rez].date_from);
							var b = date.toLocaleDateString();
							if(a == b){
								u_rezervaciji = true;
								if(dan.left!=null){
									dan.class = "zauzeto";
									dan.left = null;
								}
								else{
									dan.right = true;
								}
								var c = $scope.convertDate(rezervacije[index_rez].date_until);  //jednodnevne rezervacije, mora se odmah provjeriti jer se radi provjera dan po dan inace
								if(a==c){  //a kod jednodnevnih je taj dan isti
									u_rezervaciji = false;
									sljedeci = true;
									index_rez++;
								}
							}
						}
					}
					tjedan.push(dan);
					date.setDate(date.getDate()+1);
				}
				for(var i=tjedan.length;i<7;i++){
					tjedan.push({class : "active"});
				}
				display++;
				mjesec.push(tjedan);
				mjeseci.push(mjesec);
				current.setMonth(current.getMonth()+1);
			}
			$scope.calendar.push(mjeseci);
		}
		//console.log(JSON.stringify($scope.calendar));
	}
	
	$scope.getData = function(){
		$.post( PATH+'accommodation/one/guest', 'acc_id='+$scope.acc_id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				$scope.loggedOut();
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
					$scope.data.comments[i].time = $scope.convertDateTime($scope.data.comments[i].time);
				}
				for(var i=0;i<$scope.data.prices.length;i++){
					$scope.data.prices[i].date_from = $scope.convertDate($scope.data.prices[i].date_from);
					$scope.data.prices[i].date_until = $scope.convertDate($scope.data.prices[i].date_until);
				}
				//console.log(JSON.stringify($scope.data));
			}
        	$scope.$apply();
		});
	};
	
	$scope.res_save = {};
	$scope.res_save.show = false;
	$scope.saveReservation = function(){
		$.post( PATH+'reservation/add/guest', 'acc_id='+$scope.acc_id+'&session_id='+$scope.kolacic+'&date_from='+$scope.res_data.date_from+'&date_until='+$scope.res_data.date_until+'&ppl_adults='+$scope.res_data.ppl_adults+'&ppl_children='+$scope.res_data.ppl_children, function( data ) {
			console.log(data);
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				$scope.res_save.show = true;
				$scope.res_save.class = 'danger';
				$scope.res_save.message = 'Error '+main_json.error.id+' - '+main_json.error.description;
			}
			else{
				$scope.res_save.show = true;
				$scope.res_save.class = 'success';
				$scope.res_save.message = 'Reservation placed successfully';
				$scope.getCalendar();
			}
			$scope.$apply();
		});
	};
	
	$scope.getMyComment = function(){
		$.post( PATH+'comment/my', 'session_id='+$scope.kolacic+'&acc_id='+$scope.acc_id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				$scope.loggedOut();
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
	
	function initializeMap() {
		var myLatlng = new google.maps.LatLng($scope.data.lat, $scope.data.long);
    	var mapOptions = {
      		center: myLatlng,
        	zoom: 16
        };
        var map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);  //mora bit obican JS, ane jquery jer se maps api oslanja na to
        
        var marker = new google.maps.Marker({
      		position: myLatlng,
      		map: map,
      		title: 'Accommodation'
  		});
  		
  		var infowindow = new google.maps.InfoWindow({
      		content: 'Name: '+$scope.data.acc_name+'<br/>Coordinates: '+$scope.data.lat+','+$scope.data.long
  		});
  		
  		google.maps.event.addListener(marker, 'click', function() {
    		infowindow.open(map,marker);
  		});
  	}
	
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
	function prepareResRegion(){
		if($scope.kolacic==null || $scope.user.type=='Owner'){
			$scope.can_res=false;
		}
		else{
			$scope.can_res=true;
		}
		$scope.$apply();
	}
	
	setTimeout(prepareResRegion, 500);
	setTimeout(prepareCommentRegion, 1000);
	setTimeout(initializeMap, 1000);
});