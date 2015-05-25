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
	
	private PreparedStatement stat_cleanup_expired;
	private PreparedStatement stat_cleanup_completed;
	
	public Reservation(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		format = new SimpleDateFormat("yyyy-MM-dd");
		
		stat_accommodation = connect.prepareStatement("SELECT date_from, date_until, name FROM reservation "
				+ "JOIN res_status ON res_status_id = res_status.id "
				+ "WHERE (date_from >= ? AND date_from <= ? OR date_until >= ? AND date_until <= ?) AND acc_id = ? " 
				+ "AND (name='Confirmed' OR name='Pending') "
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
