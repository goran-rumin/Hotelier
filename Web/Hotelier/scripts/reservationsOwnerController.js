
var apl_resOwner = angular.module('res_owner', ['ngCookies']);

apl_resOwner.controller('reservationsOwnerController', function($scope, $cookieStore){
	$scope.kolacic = $cookieStore.get('SESSION');
	var position = 0;
	if($scope.kolacic==null){
		location.href = "#/";
		return;
	}
	
	$scope.generate_dates = function(delay){
		$scope.dates = [];
		var date=new Date();
		for(var i=0;i<14;i++){
			date=new Date();
			date.setTime(date.getTime()+delay*1209600000+i*86400000);
			$scope.dates[i]=date.getDate()+'.'+(date.getMonth()+1)+'.';
		}
	};
	
	$scope.previous = function(){
		position-=1;
		$scope.generate_dates(position);
		$scope.getReservations();
		$scope.getReservationsCanceled();
	};
	
	$scope.next = function(){
		position+=1;
		$scope.generate_dates(position);
		$scope.getReservations();
		$scope.getReservationsCanceled();
	};
	
	$scope.getReservations = function(){
		$.post( PATH+'reservation/all/owner', 'session_id='+$scope.kolacic+'&index='+position, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.data = main_json.data;
				$scope.reservations = {};
				for(object in main_json.data){
					for(accommodation in main_json.data[object]){
						var reservations = main_json.data[object][accommodation];
						var new_reservations = [];
						var res_current = 0;
						var day_current = new Date();
						day_current.setDate(day_current.getDate()+position*14);
						var u_rezervaciji = false;
						var sljedeci = false;
						var rezervacija = {};
						for(var i=0;i<14;i++){
							if(u_rezervaciji==true){
								rezervacija.id = reservations[res_current].id;
								if(!$scope.reservations.hasOwnProperty(reservations[res_current].id)){
									var temp = {};
									for(podatak in reservations[res_current]){
										temp[podatak] = reservations[res_current][podatak];
									}
									var datum = $scope.createDate(temp.date_until);
									datum.setDate(datum.getDate()+1);
									temp.date_until = datum.toLocaleDateString();
									$scope.reservations[reservations[res_current].id] = temp;
								}
								if(reservations[res_current].type=='Confirmed' || reservations[res_current].type=='Completed'){
									rezervacija.class = "zauzeto";
								}
								else if(reservations[res_current].type=='Pending'){
									rezervacija.class = "warning";
								}
							}
							else{
								rezervacija.class = "success";
							}
							if(sljedeci==true){
								if(reservations[res_current-1].type=='Confirmed' || reservations[res_current-1].type=='Completed'){
									rezervacija.left_d = true;
								}
								else if(reservations[res_current-1].type=='Pending'){
									rezervacija.left_w = true;
								}
								sljedeci = false;
							}

							if(reservations[res_current]!=null && reservations[res_current].hasOwnProperty('date_from') && 
							day_current.toLocaleDateString()==$scope.convertDate(reservations[res_current].date_from_draw)){
								u_rezervaciji = true;
								
								rezervacija.id = reservations[res_current].id;
								if(!$scope.reservations.hasOwnProperty(reservations[res_current].id)){
									var temp = {};
									for(podatak in reservations[res_current]){
										temp[podatak] = reservations[res_current][podatak];
									}
									var datum = $scope.createDate(temp.date_until);
									datum.setDate(datum.getDate()+1);
									temp.date_until = datum.toLocaleDateString();
									$scope.reservations[reservations[res_current].id] = temp;
								}
								
								if(reservations[res_current].date_from_draw.localeCompare(reservations[res_current].date_from)>0){
									if(reservations[res_current].type=='Confirmed' || reservations[res_current].type=='Completed'){
										rezervacija.class = "zauzeto";
									}
									else if(reservations[res_current].type=='Pending'){
										rezervacija.class = "warning";
									}
								}
								if(reservations[res_current].type=='Confirmed' || reservations[res_current].type=='Completed'){
									rezervacija.right_d = true;
								}
								else if(reservations[res_current].type=='Pending'){
									rezervacija.right_w = true;
								}
							}
							if(reservations[res_current]!=null && reservations[res_current].hasOwnProperty('date_from') && 
							day_current.toLocaleDateString()==$scope.convertDate(reservations[res_current].date_until_draw)){
								u_rezervaciji = false;
								sljedeci = true;
								res_current+=1;
							}
							day_current.setDate(day_current.getDate()+1);
							new_reservations.push(rezervacija);
							rezervacija = {};
						}
						main_json.data[object][accommodation] = new_reservations;
					}
				}
			}
			$scope.$apply();
		});
	};
	
	$scope.getReservationsCanceled = function(){
		$.post( PATH+'reservation/all/owner/canceled', 'session_id='+$scope.kolacic+'&index='+position, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.canceled = main_json.data;
			}
			$scope.$apply();
		});
	};
	
	$scope.showData = function($event){
		if($($event.currentTarget).data('id')!=''){
			var reservation = $scope.reservations[$($event.currentTarget).data('id')];
			$($event.currentTarget).popover({
    			placement: "top",
    			container: 'body',
        		title: "Reservation",
        		content: "Guest: "+reservation.name+" "+reservation.surname+"<br/>"+$scope.convertDate(reservation.date_from)+" - "+
        			reservation.date_until+"<br/>Price: "+reservation.price+" - "+reservation.discount+" %<br/>"+
        			(reservation.remark==null ? '' : reservation.remark)+"<br/>"+reservation.type,
        		html: true
    		}).popover('show');
		}
	};
	
	$scope.hideData = function($event){
		setTimeout(function(){
			$($event.currentTarget).popover('hide');
			$scope.popover_showed = false;
		}, 0);
	};
	
	function getLog(id){
		$.post( PATH+'reservation/log', 'session_id='+$scope.kolacic+'&res_id='+id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.log = main_json.data;
			}
			$scope.$apply();
		});
	}
	
	$scope.cannot_edit = true;
	$scope.getReservation = function(id, canceled){
		if(id==null){
			return;
		}
		$scope.cannot_edit = canceled;
		$.post( PATH+'reservation/one/owner', 'session_id='+$scope.kolacic+'&res_id='+id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.reservation = main_json.data;
				if($scope.reservation.validity_date!=null){
					$scope.reservation.validity_date = $scope.reservation.validity_date.substring(0, $scope.reservation.validity_date.length-2);
				}
				else{
					$scope.reservation.validity_date = 'NULL';
				}
			}
			getLog(id);
			$scope.$apply();
		});
	};
	
	$scope.edit = function(){
		if($scope.reservation.id!=null){
			$.post( PATH+'reservation/edit/owner', 'session_id='+$scope.kolacic+'&res_id='+$scope.reservation.id+'&acc_id='+$scope.reservation.acc_id+'&date_from='+$scope.reservation.date_from
			+'&date_until='+$scope.reservation.date_until+'&ppl_adults='+$scope.reservation.ppl_adults+'&ppl_children='+$scope.reservation.ppl_children+'&price='+$scope.reservation.price
			+'&discount='+$scope.reservation.discount+'&advmoney='+$scope.reservation.advmoney+'&remark='+$scope.reservation.remark+'&validity_date='+$scope.reservation.validity_date, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
				}
				else{
					$scope.saved = true;
				}
				$scope.reservation = {};
				$scope.$apply();
			});
		}
		else{
			$.post( PATH+'reservation/add/owner', 'session_id='+$scope.kolacic+'&acc_id='+$scope.reservation.acc_id+'&date_from='+$scope.reservation.date_from
			+'&date_until='+$scope.reservation.date_until+'&ppl_adults='+$scope.reservation.ppl_adults+'&ppl_children='+$scope.reservation.ppl_children+'&price='+$scope.reservation.price
			+'&discount='+$scope.reservation.discount+'&advmoney='+$scope.reservation.advmoney+'&remark='+$scope.reservation.remark, function( data ) {
				var main_json = angular.fromJson(data);
				if(main_json.hasOwnProperty('error')){
					alert('Error '+main_json.error.id+' - '+main_json.error.description);
				}
				else{
					$scope.saved = true;
				}
				$scope.reservation = {};
				$scope.getReservations();
				$scope.$apply();
			});
		}
	};
	
	$scope.add = function(){
		$scope.reservation = {};
		$scope.reservation.discount = 0;
		$scope.reservation.advmoney = 0;
		$scope.reservation.validity_date = 'NULL';
		$scope.reservation.remark = '';
		$scope.cannot_edit = false;
	};
	
	$scope.remove = function(){
		$.post( PATH+'reservation/delete/owner', 'session_id='+$scope.kolacic+'&res_id='+$scope.reservation.id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.getReservations();
				$scope.getReservationsCanceled();
			}
			$scope.reservation = {};
			$scope.$apply();
		});
	};
	
	$scope.confirmReservation = function(){
		$.post( PATH+'reservation/confirm', 'session_id='+$scope.kolacic+'&res_id='+$scope.reservation.id, function( data ) {
			var main_json = angular.fromJson(data);
			if(main_json.hasOwnProperty('error')){
				alert('Error '+main_json.error.id+' - '+main_json.error.description);
				$scope.loggedOut();
			}
			else{
				$scope.getReservations();
			}
			$scope.reservation = {};
			$scope.$apply();
		});
	};
	
	$scope.generate_dates(position);
	$scope.getAccommodations($scope.kolacic);
	$scope.getReservations();
	$scope.getReservationsCanceled();
});