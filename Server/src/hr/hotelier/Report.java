package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Report {
	
	private Connection connect = null;
	private Security sec;
	
	private PreparedStatement stat_rep_arrivals;
	private PreparedStatement stat_rep_revenue;
	private PreparedStatement stat_rep_guests;
	private PreparedStatement stat_rep_add_guest;
	private PreparedStatement stat_rep_guestlist;
	private PreparedStatement stat_rep_add_guest_to_list;
	
	public Report(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		
		stat_rep_arrivals = connect.prepareStatement("SELECT reservation.id, accommodation.name, user.name, user.surname, "
				+ "country.name, date_from, date_until, ppl_adults, ppl_children, price, discount, advmoney, remark "
				+ "FROM reservation JOIN user ON reservation.user_id=user.id "
				+ "JOIN res_status ON res_status_id=res_status.id "
				+ "JOIN country ON user.country_id=country.id "
				+ "JOIN accommodation ON reservation.acc_id=accommodation.id "
				+ "JOIN object ON accommodation.object_id=object.id "
				+ "JOIN owner ON object.id=owner.object_id AND owner.user_id=? "
				+ "WHERE (date_from>=? AND date_from<=?) AND res_status.name='Confirmed'"
				+ "ORDER BY date_from;");
		stat_rep_revenue = connect.prepareStatement("SELECT accommodation.name, SUM((DATEDIFF(date_until,date_from)+1)*price) AS revenue "
				+ "FROM reservation JOIN res_status ON res_status_id=res_status.id "
				+ "RIGHT OUTER JOIN accommodation ON reservation.acc_id=accommodation.id AND res_status.name='Completed' AND (date_from>=? AND date_from <=?) "
				+ "JOIN object ON accommodation.object_id=object.id "
				+ "JOIN owner ON object.id=owner.object_id AND owner.user_id=? "
				+ "GROUP BY accommodation.id "
				+ "ORDER BY revenue DESC;");
		stat_rep_guests = connect.prepareStatement("SELECT * FROM guest WHERE user_id=?;");
		stat_rep_add_guest = connect.prepareStatement("INSERT INTO guest(user_id, name, surname, country_id, doc_num, email, date_birth) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?);");
		stat_rep_guestlist = connect.prepareStatement("SELECT name, surname, date_from, DATE_ADD(date_until,INTERVAL 1 DAY) AS date_until "
				+ "FROM guest_list JOIN guest ON guest_id=guest.id "
				+ "WHERE user_id=? "
				+ "ORDER BY date_from;");
		stat_rep_add_guest_to_list = connect.prepareStatement("INSERT INTO guest_list(guest_id, date_from, date_until) VALUES (?, ?, DATE_SUB(?,INTERVAL 1 DAY));");
	}
	
	public String arrivals(String session_id, String date_from, String date_until) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(date_from, date_until)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_rep_arrivals.setInt(1, id);
    	stat_rep_arrivals.setString(2, date_from);
    	stat_rep_arrivals.setString(3, date_until);
    	ResultSet rezultati = stat_rep_arrivals.executeQuery();
    	JSONObject rep_item;
    	while(rezultati.next()){
    		rep_item = new JSONObject();
    		rep_item.put("id", rezultati.getString("reservation.id"));
    		rep_item.put("acc_name", rezultati.getString("accommodation.name"));
    		rep_item.put("name", rezultati.getString("user.name"));
    		rep_item.put("surname", rezultati.getString("user.surname"));
    		rep_item.put("country", rezultati.getString("country.name"));
    		rep_item.put("date_from", rezultati.getString("date_from"));
    		rep_item.put("date_until", rezultati.getString("date_until"));
    		rep_item.put("ppl_adults", rezultati.getString("ppl_adults"));
    		rep_item.put("ppl_children", rezultati.getString("ppl_children"));
    		rep_item.put("price", rezultati.getString("price"));
    		rep_item.put("discount", rezultati.getString("discount"));
    		rep_item.put("advmoney", rezultati.getString("advmoney"));
    		rep_item.put("remark", rezultati.getString("remark"));
    		data.put(rep_item);
    	}
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String revenue(String session_id, String date_from, String date_until) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(date_from, date_until)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_rep_revenue.setString(1, date_from);
    	stat_rep_revenue.setString(2, date_until);
    	stat_rep_revenue.setInt(3, id);
    	ResultSet rezultati = stat_rep_revenue.executeQuery();
    	JSONObject app_data;
    	while(rezultati.next()){
    		app_data = new JSONObject();
    		app_data.put("name", rezultati.getString("accommodation.name"));
    		if(rezultati.getString("revenue")==null)
    			app_data.put("revenue", 0);
    		else
    			app_data.put("revenue", rezultati.getInt("revenue"));
    		data.put(app_data);
    	}
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String guests(String session_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_rep_guests.setInt(1, id);
    	ResultSet rezultati = stat_rep_guests.executeQuery();
    	JSONObject guest;
    	while(rezultati.next()){
    		guest = new JSONObject();
    		guest.put("id", rezultati.getInt("id"));
    		guest.put("name", rezultati.getString("name"));
    		guest.put("surname", rezultati.getString("surname"));
    		guest.put("country_id", rezultati.getInt("country_id"));
    		guest.put("doc_num", rezultati.getString("doc_num"));
    		guest.put("email", rezultati.getString("email"));
    		guest.put("date_birth", rezultati.getString("date_birth"));
    		data.put(guest);
    	}
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String addGuest(String session_id, String name, String surname, String country_id, String doc_num, String email, String date_birth) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(name, surname, country_id, doc_num, date_birth)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_rep_add_guest.setInt(1, id);
    	stat_rep_add_guest.setString(2, name);
    	stat_rep_add_guest.setString(3, surname);
    	stat_rep_add_guest.setString(4, country_id);
    	stat_rep_add_guest.setString(5, doc_num);
    	stat_rep_add_guest.setString(6, email);
    	stat_rep_add_guest.setString(7, date_birth);
    	stat_rep_add_guest.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String guestList(String session_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_rep_guestlist.setInt(1, id);
    	ResultSet rezultati = stat_rep_guestlist.executeQuery();
    	JSONObject list_element;
    	while(rezultati.next()){
    		list_element = new JSONObject();
    		list_element.put("name", rezultati.getString("name")+" "+rezultati.getString("surname"));
    		list_element.put("date_from", rezultati.getString("date_from"));
    		list_element.put("date_until", rezultati.getString("date_until"));
    		data.put(list_element);
    	}
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String guestListAdd(String session_id, String guest_id, String date_from, String date_until) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(guest_id, date_from, date_until)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_rep_add_guest_to_list.setString(1, guest_id);
    	stat_rep_add_guest_to_list.setString(2, date_from);
    	stat_rep_add_guest_to_list.setString(3, date_until);
    	stat_rep_add_guest_to_list.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
}
