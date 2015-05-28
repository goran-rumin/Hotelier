package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Reservation {
	
	private Connection connect = null;
	private Security sec;
	Timer timer = new Timer();
	
	private int RES_STATUS_CONFIRMED = 1;
	private int RES_STATUS_PENDING = 2;
	private int RES_STATUS_CANCELED_BY_GUEST = 3;
	private int RES_STATUS_CANCELED_BY_OWNER = 4;
	private int RES_STATUS_TIME_OUT = 5;
	private int RES_STATUS_COMPLETED = 6;
	
	private SimpleDateFormat format;
	
	private PreparedStatement stat_accommodation;
	private PreparedStatement stat_add_prices;
	private PreparedStatement stat_add_check_if_on_sale;
	private PreparedStatement stat_add_check_if_free;
	private PreparedStatement stat_add_reservation;
	private PreparedStatement stat_my_reservations;
	private PreparedStatement stat_reservation_id;
	private PreparedStatement stat_reservation_log;
	private PreparedStatement stat_reservation_remove;
	private PreparedStatement stat_owner_all;
	private PreparedStatement stat_owner_all_canceled;
	private PreparedStatement stat_reservation_one;
	private PreparedStatement stat_edit;
	private PreparedStatement stat_get_log;
	private PreparedStatement stat_owner_add;
	
	private PreparedStatement stat_cleanup_expired;
	private PreparedStatement stat_cleanup_completed;
	
	public Reservation(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		format = new SimpleDateFormat("yyyy-MM-dd");
		
		stat_accommodation = connect.prepareStatement("SELECT date_from, date_until, name FROM reservation "
				+ "JOIN res_status ON res_status_id = res_status.id "
				+ "WHERE (date_from >= ? AND date_from <= ? OR date_until >= ? AND date_until <= ?) AND acc_id = ? " 
				+ "AND (name='Confirmed' OR name='Pending' OR name='Completed') "
				+ "ORDER BY date_from;");
		stat_add_prices = connect.prepareStatement("SELECT date_from, date_until, price FROM prices "
				+"WHERE acc_id=? AND date_until>? AND date_from<DATE_SUB(?,INTERVAL 1 DAY) "  //dolazak, odlazak
				+"ORDER BY date_from;");
		stat_add_check_if_on_sale = connect.prepareStatement("SELECT DISTINCT acc_id FROM prices "
				+ "WHERE acc_id=? AND acc_id IN (SELECT acc_id FROM prices WHERE date_from<=? AND date_until>=?) "
				+ "AND acc_id IN (SELECT acc_id FROM prices WHERE date_from<=DATE_SUB(?,INTERVAL 1 DAY) "
				+ "AND date_until>=DATE_SUB(?,INTERVAL 1 DAY));");
		stat_add_check_if_free = connect.prepareStatement("SELECT DISTINCT acc_id FROM reservation JOIN res_status ON res_status_id=res_status.id "
				+ "WHERE acc_id=? AND ((date_from<=? AND date_until>=?) OR (date_from<=DATE_SUB(?,INTERVAL 1 DAY) "
				+ "AND date_until>=DATE_SUB(?,INTERVAL 1 DAY)) OR (date_from>? AND date_until<DATE_SUB(?,INTERVAL 1 DAY))) "
				+ "AND (name='Confirmed' OR name='Pending');");  //provjeri da li je slobodan u trazenom rasponu
		stat_add_reservation = connect.prepareStatement("INSERT INTO reservation(acc_id, user_id, date_from, date_until, ppl_adults, ppl_children, price, validity_date, res_status_id) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, DATE_ADD(NOW() ,INTERVAL 1 DAY), 2);");
		stat_my_reservations = connect.prepareStatement("SELECT reservation.id, acc_id, accommodation.name, date_from, "
				+ "DATE_ADD(date_until, INTERVAL 1 DAY) AS date_until, ppl_adults, ppl_children, price, "
				+ "discount, advmoney, validity_date, res_status.name FROM reservation "
				+ "JOIN res_status ON res_status_id=res_status.id "
				+ "JOIN accommodation ON acc_id=accommodation.id "
				+ "WHERE user_id=? AND date_from > DATE_SUB(NOW(), INTERVAL 1 YEAR) "
				+ "ORDER BY date_from;");
		stat_reservation_id = connect.prepareStatement("SELECT id FROM reservation WHERE acc_id=? AND user_id=? AND date_from=? AND date_until=?;");
		stat_reservation_log = connect.prepareStatement("INSERT INTO reservation_log(res_id, time, `desc`) VALUES (?, NOW(), ?);");
		stat_reservation_remove = connect.prepareStatement("UPDATE reservation SET res_status_id=?, validity_date=NULL WHERE id=?;");
		stat_owner_all = connect.prepareStatement("SELECT reservation.id, object.name, accommodation.name, user.name, user.surname, date_from, date_until, price, discount, remark, res_status.name "
				+ "FROM reservation JOIN res_status ON res_status_id=res_status.id "
				+ "RIGHT OUTER JOIN accommodation ON reservation.acc_id=accommodation.id "
				+ "AND (date_from IS NULL OR (date_from>=? AND date_from<=?) OR (date_until>=? AND date_until<=?)) "
				+ "AND (res_status.name IS NULL OR res_status.name='Completed' OR res_status.name='Confirmed' OR res_status.name='Pending') "
				+ "JOIN object ON accommodation.object_id=object.id "
				+ "JOIN owner ON object.id=owner.object_id "
				+ "LEFT OUTER JOIN user ON reservation.user_id=user.id "
				+ "WHERE owner.user_id=? "
				+ "ORDER BY object.name, accommodation.name, date_from;");
		stat_owner_all_canceled = connect.prepareStatement("SELECT reservation.id, object.name, accommodation.name, user.name, user.surname, date_from, date_until, price, discount, remark, res_status.name "
				+ "FROM reservation JOIN res_status ON res_status_id=res_status.id "
				+ "JOIN accommodation ON reservation.acc_id=accommodation.id "
				+ "JOIN object ON accommodation.object_id=object.id "
				+ "JOIN owner ON object.id=owner.object_id "
				+ "JOIN user ON reservation.user_id=user.id "
				+ "WHERE owner.user_id=? "
				+ "AND (date_from IS NULL OR (date_from>=? AND date_from<=?) OR (date_until>=? AND date_until<=?)) "
				+ "AND (res_status.name IS NULL OR res_status.name='Canceled by guest' OR res_status.name='Canceled by owner' OR res_status.name='Validity ended') "
				+ "ORDER BY object.name, accommodation.name, date_from;");
		stat_reservation_one = connect.prepareStatement("SELECT reservation.id, user.name, user.surname, country.name, phone, email, acc_id, date_from, date_until, "
				+ "ppl_adults, ppl_children, price, discount, advmoney, remark, validity_date, res_status.name "
				+ "FROM reservation "
				+ "JOIN res_status ON res_status_id=res_status.id "
				+ "JOIN user ON reservation.user_id=user.id "
				+ "JOIN country ON user.country_id=country.id "
				+ "WHERE reservation.id=?;");
		stat_edit = connect.prepareStatement("UPDATE reservation SET acc_id=?, date_from=?, date_until=?, ppl_adults=?, ppl_children=?, price=?, discount=?, advmoney=?, "
				+ "remark=?, validity_date=? WHERE id=?;");
		stat_get_log = connect.prepareStatement("SELECT time, `desc` FROM reservation_log WHERE res_id=? ORDER BY time DESC;");
		stat_owner_add = connect.prepareStatement("INSERT INTO reservation(acc_id, user_id, date_from, date_until, ppl_adults, ppl_children, price, discount, advmoney, remark, res_status_id) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1);");
		stat_cleanup_expired = connect.prepareStatement("UPDATE reservation SET res_status_id="+RES_STATUS_TIME_OUT+" WHERE validity_date<NOW();");
		stat_cleanup_completed = connect.prepareStatement("UPDATE reservation SET res_status_id="+RES_STATUS_COMPLETED+" WHERE date_until<NOW();");
		
		timer.scheduleAtFixedRate(res_cleanup_expired, (long) 0, (long) 60*60*1000);
		timer.scheduleAtFixedRate(res_cleanup_completed, (long) 0, (long) 24*60*60*1000);
	}
	
	public String accommodation(String acc_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -c.get(Calendar.DATE)+1);
		String datum_pocetak = format.format(c.getTime());
		stat_accommodation.setString(1, datum_pocetak);
		stat_accommodation.setString(3, datum_pocetak);
		c.add(Calendar.YEAR, 1);
		String datum_kraj = format.format(c.getTime());
		stat_accommodation.setString(2, datum_kraj);
		stat_accommodation.setString(4, datum_kraj);
		stat_accommodation.setString(5, acc_id);
		ResultSet rezultati = stat_accommodation.executeQuery();
		
		JSONObject rezervacija;
		while(rezultati.next()){
			rezervacija = new JSONObject();
			if(datum_pocetak.compareTo(rezultati.getString("date_from"))<0)
				rezervacija.put("date_from", rezultati.getString("date_from"));
			else
				rezervacija.put("date_from", datum_pocetak);
			if(datum_kraj.compareTo(rezultati.getString("date_until"))>0)
				rezervacija.put("date_until", rezultati.getString("date_until"));
			else
				rezervacija.put("date_until", datum_kraj);
			rezervacija.put("type", rezultati.getString("name"));
			data.put(rezervacija);
		}
		main.put("data", data);
		return main.toString();
	}
	
	public String guestAdd(String session_id, String date_from, String date_until, String ppl_adults, String ppl_children, String acc_id) throws SQLException, JSONException{
		
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(date_from, date_until, ppl_adults, ppl_children)){
    		return Security.prepareErrorJson(10, Security.ERROR10);
    	}
    	
    	stat_add_check_if_on_sale.setString(1, acc_id);
    	stat_add_check_if_on_sale.setString(2, date_from);
    	stat_add_check_if_on_sale.setString(3, date_from);
    	stat_add_check_if_on_sale.setString(4, date_until);
    	stat_add_check_if_on_sale.setString(5, date_until);
    	ResultSet rezultati = stat_add_check_if_on_sale.executeQuery();
    	if(!rezultati.first()){
    		return Security.prepareErrorJson(8,Security.ERROR8);
    	}
    	
    	stat_add_check_if_free.setString(1, acc_id);
    	stat_add_check_if_free.setString(2, date_from);
    	stat_add_check_if_free.setString(3, date_from);
    	stat_add_check_if_free.setString(4, date_until);
    	stat_add_check_if_free.setString(5, date_until);
    	stat_add_check_if_free.setString(6, date_from);
    	stat_add_check_if_free.setString(7, date_until);
    	rezultati = stat_add_check_if_free.executeQuery();
    	if(rezultati.first()){
    		return Security.prepareErrorJson(9,Security.ERROR9);
    	}
    	
    	stat_add_prices.setString(1, acc_id);
    	stat_add_prices.setString(2, date_from);
    	stat_add_prices.setString(3, date_until);
    	rezultati = stat_add_prices.executeQuery();
    	Calendar c = Calendar.getInstance();
    	String[] datum = date_until.split("-");
    	c.set(Integer.parseInt(datum[0]), Integer.parseInt(datum[1])-1, Integer.parseInt(datum[2]));
    	c.add(Calendar.DATE, -1);
    	date_until = format.format(c.getTime());
    	String prvi_datum, zadnji_datum;
    	while(rezultati.next()){
    		if(rezultati.isFirst()){
    			prvi_datum = date_from;
    		}
    		else{
    			prvi_datum = rezultati.getString("date_from");
    		}
    		if(rezultati.isLast()){
    			zadnji_datum = date_until;
    		}
    		else{
    			zadnji_datum = rezultati.getString("date_until");
    		}
    		stat_add_reservation.setString(1, acc_id);
    		stat_add_reservation.setInt(2, id);
    		stat_add_reservation.setString(3, prvi_datum);
    		stat_add_reservation.setString(4, zadnji_datum);
    		stat_add_reservation.setString(5, ppl_adults);
    		stat_add_reservation.setString(6, ppl_children);
    		stat_add_reservation.setString(7, rezultati.getString("price"));
    		stat_add_reservation.setInt(8, RES_STATUS_PENDING);
    		stat_add_reservation.executeUpdate();
    		
    		stat_reservation_id.setString(1, acc_id);
    		stat_reservation_id.setInt(2, id);
    		stat_reservation_id.setString(3, prvi_datum);
    		stat_reservation_id.setString(4, zadnji_datum);
    		ResultSet res_id_upit = stat_reservation_id.executeQuery();
    		res_id_upit.first();
    		int res_id = res_id_upit.getInt("id");
    		stat_reservation_log.setInt(1, res_id);
    		stat_reservation_log.setString(2, "Reservation added by guest");
    		stat_reservation_log.executeUpdate();
    	}
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	public String guestAll(String session_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_my_reservations.setInt(1, id);
    	ResultSet rezultati = stat_my_reservations.executeQuery();
    	JSONObject rezervacija;
    	while(rezultati.next()){
    		rezervacija = new JSONObject();
    		rezervacija.put("id", rezultati.getInt("reservation.id"));
    		rezervacija.put("acc_id", rezultati.getInt("acc_id"));
    		rezervacija.put("acc_name", rezultati.getString("accommodation.name"));
    		rezervacija.put("date_from", rezultati.getString("date_from"));
    		rezervacija.put("date_until", rezultati.getString("date_until"));
    		rezervacija.put("ppl_adults", rezultati.getInt("ppl_adults"));
    		rezervacija.put("ppl_children", rezultati.getInt("ppl_children"));
    		rezervacija.put("price", rezultati.getInt("price"));
    		rezervacija.put("discount", rezultati.getInt("discount"));
    		rezervacija.put("advmoney", rezultati.getInt("advmoney"));
    		rezervacija.put("validity_date", rezultati.getString("validity_date"));
    		rezervacija.put("status", rezultati.getString("res_status.name"));
    		data.put(rezervacija);
    	}
		main.put("data", data);
		return main.toString();
	}
	
	public String guestDelete(String session_id, String res_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_reservation_remove.setInt(1, RES_STATUS_CANCELED_BY_GUEST);
    	stat_reservation_remove.setString(2, res_id);
    	stat_reservation_remove.executeUpdate();
    	
    	stat_reservation_log.setString(1, res_id);
		stat_reservation_log.setString(2, "Reservation canceled by guest");
		stat_reservation_log.executeUpdate();
    	
		data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerAllOccupied(String session_id, String current_span) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	int index = Integer.parseInt(current_span);
    	
    	Calendar c = Calendar.getInstance();
    	c.add(Calendar.DATE, index*14);
		String datum_pocetak = format.format(c.getTime());
		stat_owner_all.setString(1, datum_pocetak);
		stat_owner_all.setString(3, datum_pocetak);
		c.add(Calendar.DATE, 14);
		String datum_kraj = format.format(c.getTime());
		stat_owner_all.setString(2, datum_kraj);
		stat_owner_all.setString(4, datum_kraj);
		stat_owner_all.setInt(5, id);
		ResultSet rezultati = stat_owner_all.executeQuery();
		
		JSONObject object = new JSONObject();
		JSONArray reservations = new JSONArray();
		JSONObject reservation;
		String trenutacni_accm = "", trenutacni_objekt = "";
		while(rezultati.next()){
			reservation = new JSONObject();
			if(!trenutacni_accm.equals(rezultati.getString("accommodation.name"))){
				if(!trenutacni_accm.equals("")){
					object.put(trenutacni_accm, reservations);
					reservations = new JSONArray();
				}
				trenutacni_accm = rezultati.getString("accommodation.name");
			}
			if(!trenutacni_objekt.equals(rezultati.getString("object.name"))){
				if(!trenutacni_objekt.equals("")){
					if(reservations.length()!=0){
						object.put(trenutacni_accm, reservations);
						reservations = new JSONArray();
					}
					data.put(trenutacni_objekt, object);
					object = new JSONObject();
				}
				trenutacni_objekt = rezultati.getString("object.name");
			}
			reservation.put("id", rezultati.getString("reservation.id"));
			reservation.put("name", rezultati.getString("user.name"));
			reservation.put("surname", rezultati.getString("user.surname"));
			reservation.put("date_from", rezultati.getString("date_from"));
			reservation.put("date_until", rezultati.getString("date_until"));
			if(rezultati.getString("date_from") == null || datum_pocetak.compareTo(rezultati.getString("date_from"))<0)
				reservation.put("date_from_draw", rezultati.getString("date_from"));
			else
				reservation.put("date_from_draw", datum_pocetak);
			if(rezultati.getString("date_until")==null || datum_kraj.compareTo(rezultati.getString("date_until"))>0)
				reservation.put("date_until_draw", rezultati.getString("date_until"));
			else
				reservation.put("date_until_draw", datum_kraj);
			reservation.put("price", rezultati.getString("price"));
			reservation.put("discount", rezultati.getString("discount"));
			reservation.put("remark", rezultati.getString("remark"));
			reservation.put("type", rezultati.getString("res_status.name"));
			reservations.put(reservation);
		}
		object.put(trenutacni_accm, reservations);
		data.put(trenutacni_objekt, object);
		main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerAllCanceled(String session_id, String current_span) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	int index = Integer.parseInt(current_span);
    	
    	Calendar c = Calendar.getInstance();
    	c.add(Calendar.DATE, index*14);
		String datum_pocetak = format.format(c.getTime());
		stat_owner_all_canceled.setString(2, datum_pocetak);
		stat_owner_all_canceled.setString(4, datum_pocetak);
		c.add(Calendar.DATE, 14);
		String datum_kraj = format.format(c.getTime());
		stat_owner_all_canceled.setString(3, datum_kraj);
		stat_owner_all_canceled.setString(5, datum_kraj);
		stat_owner_all_canceled.setInt(1, id);
		ResultSet rezultati = stat_owner_all_canceled.executeQuery();
		
		JSONObject object = new JSONObject();
		JSONArray reservations = new JSONArray();
		JSONObject reservation;
		String trenutacni_accm = "", trenutacni_objekt = "";
		while(rezultati.next()){
			reservation = new JSONObject();
			if(!trenutacni_accm.equals(rezultati.getString("accommodation.name"))){
				if(!trenutacni_accm.equals("")){
					object.put(trenutacni_accm, reservations);
					reservations = new JSONArray();
				}
				trenutacni_accm = rezultati.getString("accommodation.name");
			}
			if(!trenutacni_objekt.equals(rezultati.getString("object.name"))){
				if(!trenutacni_objekt.equals("")){
					if(reservations.length()!=0){
						object.put(trenutacni_accm, reservations);
						reservations = new JSONArray();
					}
					data.put(trenutacni_objekt, object);
					object = new JSONObject();
				}
				trenutacni_objekt = rezultati.getString("object.name");
			}
			reservation.put("id", rezultati.getString("reservation.id"));
			reservation.put("name", rezultati.getString("user.name"));
			reservation.put("surname", rezultati.getString("user.surname"));
			reservation.put("date_from", rezultati.getString("date_from"));
			reservation.put("date_until", rezultati.getString("date_until"));
			reservation.put("price", rezultati.getString("price"));
			reservation.put("discount", rezultati.getString("discount"));
			reservation.put("remark", rezultati.getString("remark"));
			reservation.put("type", rezultati.getString("res_status.name"));
			reservations.put(reservation);
		}
		object.put(trenutacni_accm, reservations);
		data.put(trenutacni_objekt, object);
		main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerOne(String session_id, String res_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_reservation_one.setString(1, res_id);
    	ResultSet rezultati = stat_reservation_one.executeQuery();
    	rezultati.first();
    	data.put("id", rezultati.getString("reservation.id"));
    	data.put("name", rezultati.getString("user.name"));
    	data.put("surname", rezultati.getString("user.surname"));
    	data.put("country", rezultati.getString("country.name"));
    	data.put("phone", rezultati.getString("phone"));
    	data.put("email", rezultati.getString("email"));
    	data.put("acc_id", rezultati.getInt("acc_id"));
    	data.put("date_from", rezultati.getString("date_from"));
    	data.put("date_until", rezultati.getString("date_until"));
    	data.put("ppl_adults", rezultati.getString("ppl_adults"));
    	data.put("ppl_children", rezultati.getString("ppl_children"));
    	data.put("price", rezultati.getString("price"));
    	data.put("discount", rezultati.getString("discount"));
    	data.put("advmoney", rezultati.getString("advmoney"));
    	data.put("remark", rezultati.getString("remark"));
    	data.put("validity_date", rezultati.getString("validity_date"));
    	data.put("type", rezultati.getString("res_status.name"));
    	
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerEdit(String session_id, String res_id, String acc_id, String date_from, String date_until, String ppl_adults,
			String ppl_children, String price, String discount, String advmoney, String remark, String validity_date) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(res_id, acc_id, date_from, date_until, ppl_adults, ppl_children, price, validity_date)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_reservation_one.setString(1, res_id);
    	ResultSet rezultati = stat_reservation_one.executeQuery();
    	rezultati.first();
    	
    	stat_reservation_log.setString(1, res_id);
    	String edit_log = "Edit by owner: \n";
    	if(Integer.parseInt(acc_id)!=rezultati.getInt("acc_id"))
    		edit_log+="Accommodation "+rezultati.getInt("acc_id")+"->"+acc_id+"\n";
    	
    	if(!date_from.equals(rezultati.getString("date_from")))
    		edit_log+="Date from "+rezultati.getString("date_from")+"->"+date_from+"\n";
    	
    	if(!date_until.equals(rezultati.getString("date_until")))
    		edit_log+="Date until "+rezultati.getString("date_until")+"->"+date_until+"\n";
    	
    	if(!ppl_adults.equals(rezultati.getString("ppl_adults")))
    		edit_log+="Number of adults "+rezultati.getString("ppl_adults")+"->"+ppl_adults+"\n";
    	
    	if(!ppl_children.equals(rezultati.getString("ppl_children")))
    		edit_log+="Number of children "+rezultati.getString("ppl_children")+"->"+ppl_children+"\n";
    	
    	if(!price.equals(rezultati.getString("price")))
    		edit_log+="Price "+rezultati.getString("price")+"->"+price+"\n";
    	
    	if(!discount.equals(rezultati.getString("discount")))
    		edit_log+="Discount "+rezultati.getString("discount")+"->"+discount+"\n";
    	
    	if(!advmoney.equals(rezultati.getString("advmoney")))
    		edit_log+="Advance money "+rezultati.getString("advmoney")+"->"+advmoney+"\n";
    	
    	if(!remark.equals(rezultati.getString("remark")))
    		edit_log+="Remark "+rezultati.getString("remark")+"->"+remark+"\n";
    	
    	if(!validity_date.equals(rezultati.getString("validity_date")))
    		edit_log+="Validity date "+rezultati.getString("validity_date")+"->"+validity_date+"\n";
    	
    	stat_reservation_log.setString(2, edit_log);
    	stat_reservation_log.executeUpdate();
    	
    	stat_edit.setString(1, acc_id);
    	stat_edit.setString(2, date_from);
    	stat_edit.setString(3, date_until);
    	stat_edit.setString(4, ppl_adults);
    	stat_edit.setString(5, ppl_children);
    	stat_edit.setString(6, price);
    	stat_edit.setString(7, discount);
    	stat_edit.setString(8, advmoney);
    	stat_edit.setString(9, remark);
    	stat_edit.setString(10, validity_date);
    	stat_edit.setString(11, res_id);
    	stat_edit.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String log(String session_id, String res_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_get_log.setString(1, res_id);
    	ResultSet rezultati = stat_get_log.executeQuery();
    	JSONObject log_entry;
    	while(rezultati.next()){
    		log_entry = new JSONObject();
    		log_entry.put("time", rezultati.getString("time"));
    		log_entry.put("desc", rezultati.getString("desc"));
    		data.put(log_entry);
    	}
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerAdd(String session_id, String date_from, String date_until, String ppl_adults, String ppl_children, String acc_id, String price, String discount, String advmoney, String remark) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(acc_id, date_from, date_until, ppl_adults, ppl_children, price)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_add_check_if_on_sale.setString(1, acc_id);
    	stat_add_check_if_on_sale.setString(2, date_from);
    	stat_add_check_if_on_sale.setString(3, date_from);
    	stat_add_check_if_on_sale.setString(4, date_until);
    	stat_add_check_if_on_sale.setString(5, date_until);
    	ResultSet rezultati = stat_add_check_if_on_sale.executeQuery();
    	if(!rezultati.first()){
    		return Security.prepareErrorJson(8,Security.ERROR8);
    	}
    	
    	stat_add_check_if_free.setString(1, acc_id);
    	stat_add_check_if_free.setString(2, date_from);
    	stat_add_check_if_free.setString(3, date_from);
    	stat_add_check_if_free.setString(4, date_until);
    	stat_add_check_if_free.setString(5, date_until);
    	stat_add_check_if_free.setString(6, date_from);
    	stat_add_check_if_free.setString(7, date_until);
    	rezultati = stat_add_check_if_free.executeQuery();
    	if(rezultati.first()){
    		return Security.prepareErrorJson(9,Security.ERROR9);
    	}
    	
    	Calendar c = Calendar.getInstance();
    	String[] datum = date_until.split("-");
    	c.set(Integer.parseInt(datum[0]), Integer.parseInt(datum[1])-1, Integer.parseInt(datum[2]));
    	c.add(Calendar.DATE, -1);
    	date_until = format.format(c.getTime());
    	
    	stat_owner_add.setString(1, acc_id);
    	stat_owner_add.setInt(2, id);
    	stat_owner_add.setString(3, date_from);
    	stat_owner_add.setString(4, date_until);
    	stat_owner_add.setString(5, ppl_adults);
    	stat_owner_add.setString(6, ppl_children);
    	stat_owner_add.setString(7, price);
    	stat_owner_add.setString(8, discount);
    	stat_owner_add.setString(9, advmoney);
    	stat_owner_add.setString(10, remark);
    	stat_owner_add.executeUpdate();
    	
    	stat_reservation_id.setString(1, acc_id);
		stat_reservation_id.setInt(2, id);
		stat_reservation_id.setString(3, date_from);
		stat_reservation_id.setString(4, date_until);
    	ResultSet res_id_upit = stat_reservation_id.executeQuery();
		res_id_upit.first();
		int res_id = res_id_upit.getInt("id");
		stat_reservation_log.setInt(1, res_id);
		stat_reservation_log.setString(2, "Reservation added by owner");
		stat_reservation_log.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerConfirm(String session_id, String res_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_reservation_remove.setInt(1, RES_STATUS_CONFIRMED);
    	stat_reservation_remove.setString(2, res_id);
    	stat_reservation_remove.executeUpdate();
    	
    	stat_reservation_log.setString(1, res_id);
		stat_reservation_log.setString(2, "Reservation confirmed by owner");
		stat_reservation_log.executeUpdate();
    	
		data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerDelete(String session_id, String res_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	stat_reservation_remove.setInt(1, RES_STATUS_CANCELED_BY_OWNER);
    	stat_reservation_remove.setString(2, res_id);
    	stat_reservation_remove.executeUpdate();
    	
    	stat_reservation_log.setString(1, res_id);
		stat_reservation_log.setString(2, "Reservation canceled by owner");
		stat_reservation_log.executeUpdate();
    	
		data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
			
   	TimerTask res_cleanup_expired = new TimerTask() {
   		@Override
   		public void run() {
   			try {
				stat_cleanup_expired.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
   		}
   	};
   	
   	TimerTask res_cleanup_completed = new TimerTask() {
   		@Override
   		public void run() {
   			try {
				stat_cleanup_completed.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
   		}
   	};
}
