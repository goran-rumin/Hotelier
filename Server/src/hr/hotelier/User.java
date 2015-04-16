package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class User {

	private Connection connect = null;
	private Security sec;
	
	private PreparedStatement stat_login;
	private PreparedStatement stat_login_active;
	private PreparedStatement stat_register_check;
	private PreparedStatement stat_register_guest;
	private PreparedStatement stat_register_owner;
	private PreparedStatement stat_edit;
	private PreparedStatement stat_data;
	
	public User(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		stat_login = connect.prepareStatement("SELECT user.id, user_type_id, user_type.name FROM user JOIN user_type ON user.user_type_id=user_type.id WHERE username=? AND password=?;");
		stat_login_active = connect.prepareStatement("UPDATE user SET last_activity=? WHERE id=?;");
		stat_register_check = connect.prepareStatement("SELECT id FROM user WHERE username=?;");
		stat_register_guest = connect.prepareStatement("INSERT INTO user (username, password, name, surname, address, city, country_id, phone, email, date_birth, user_type_id) VALUES (?,?,?,?,?,?,?,?,?,?,1);");
		stat_register_owner = connect.prepareStatement("INSERT INTO user (username, password, name, surname, oib, address, city, country_id, phone, email, date_birth, user_type_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,2);");
		stat_edit = connect.prepareStatement("UPDATE user SET username=?, password=?, name=?, surname=?, oib=?, address=?, city=?, country_id=?, phone=?, email=?, date_birth=? WHERE id=?;");
		stat_data = connect.prepareStatement("SELECT username, user.name, surname, oib, address, city, country_id, phone, email, date_birth, user_type.name FROM user JOIN user_type ON user.user_type_id=user_type.id WHERE user.id=?;");
	}
	
	/**
	 * 
	 * @param username korisnicko ime korisnika
	 * @param password lozinka korisnika kodirana u SHA1 zapisu
	 * @return SHA1 kljuc sesije u JSON zapisu
	 * @throws SQLException
	 * @throws JSONException
	 */
	public String login(String username, String password) throws SQLException, JSONException{
		String odgovor;
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		if(!Security.areParamsOK(username, password)){
    		odgovor = Security.prepareErrorJson(2, Security.ERROR2);
    		return odgovor;
    	}
		stat_login.setString(1, username);
    	stat_login.setString(2, password);
    	ResultSet rezultati = stat_login.executeQuery();
    	
    	if(rezultati.first()){
    		int id = rezultati.getInt("user.id");
    		String type = rezultati.getString("user_type.name");
    		String session_id = sec.createSession(id);
    		data.put("id", session_id);
    		data.put("type", type);
    		main.put("data", data);
    		odgovor=main.toString();
    		Date now = new Date();
    		String datum = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
    		stat_login_active.setString(1, datum);
    		stat_login_active.setInt(2, id);
    		stat_login_active.executeUpdate();
    	}else{
    		odgovor=Security.prepareErrorJson(2,Security.ERROR2);
    	}
    	rezultati.close();
    	return odgovor;
	}
	
	/**
	 * 
	 * @param SHA1key SHA1 kljuc sesije
	 * @return JSON zapis uspjesnosti
	 * @throws JSONException
	 */
	public String logout(String SHA1key) throws JSONException{
		String odgovor="";
    	JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
    	if(sec.deleteSession(SHA1key)){
    		data.put("success", 1);
    		main.put("data", data);
    		odgovor = main.toString();
    	}
    	else{
    		odgovor=Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	return odgovor;
	}
	
	/**
	 * funkcija koja se zove za registraciju gosta
	 * @param username - obavezan
	 * @param password - obavezan
	 * @param name - obavezan
	 * @param surname - obavezan
	 * @param address - opcionalan
	 * @param city - opcionalan
	 * @param country_id - obavezan
	 * @param phone - opcionalan
	 * @param email - obavezan
	 * @param date_birth - obavezan
	 * @return
	 * @throws Exception
	 */
	public String registerGuest(String username, String password, String name, String surname, String address, String city, String country_id, String phone, String email, String date_birth) throws Exception{
		String odgovor="";
		if(!Security.areParamsOK(username, password, name, surname, country_id, email, date_birth)){
    		odgovor = Security.prepareErrorJson(3, Security.ERROR3);
    		return odgovor;
    	}
		stat_register_check.setString(1, username);
		ResultSet rezultati = stat_register_check.executeQuery();
		if(rezultati.first()){
			odgovor = Security.prepareErrorJson(4, Security.ERROR4);
    		return odgovor;
		}
		
		int country = Integer.parseInt(country_id);
		//Date date = new Date(DateFormat.getDateInstance().parse(date_birth).getTime());
		stat_register_guest.setString(1, username);
		stat_register_guest.setString(2, password);
		stat_register_guest.setString(3, name);
		stat_register_guest.setString(4, surname);
		stat_register_guest.setString(5, address);
		stat_register_guest.setString(6, city);
		stat_register_guest.setInt(7, country);
		stat_register_guest.setString(8, phone);
		stat_register_guest.setString(9, email);
		stat_register_guest.setString(10, date_birth);
		stat_register_guest.executeUpdate();
		odgovor = login(username, password);
		return odgovor;
	}
	
	/**
	 * 
	 * @param username - obavezan
	 * @param password - obavezan
	 * @param name - obavezan
	 * @param surname - obavezan
	 * @param oib - obavezan
	 * @param address - opcionalan
	 * @param city - opcionalan
	 * @param country_id - obavezan
	 * @param phone - opcionalan
	 * @param email - obavezan
	 * @param date_birth - obavezan
	 * @return
	 * @throws Exception
	 */
	public String registerOwner(String username, String password, String name, String surname, String oib, String address, String city, String country_id, String phone, String email, String date_birth) throws Exception{
		String odgovor="";
		if(!Security.areParamsOK(username, password, name, surname, oib, country_id, email, date_birth)){
    		odgovor = Security.prepareErrorJson(3, Security.ERROR3);
    		return odgovor;
    	}
		stat_register_check.setString(1, username);
		ResultSet rezultati = stat_register_check.executeQuery();
		if(rezultati.first()){
			odgovor = Security.prepareErrorJson(4, Security.ERROR4);
    		return odgovor;
		}
		
		int country = Integer.parseInt(country_id);
		stat_register_owner.setString(1, username);
		stat_register_owner.setString(2, password);
		stat_register_owner.setString(3, name);
		stat_register_owner.setString(4, surname);
		stat_register_owner.setString(5, oib);
		stat_register_owner.setString(6, address);
		stat_register_owner.setString(7, city);
		stat_register_owner.setInt(8, country);
		stat_register_owner.setString(9, phone);
		stat_register_owner.setString(10, email);
		stat_register_owner.setString(11, date_birth);
		stat_register_owner.executeUpdate();
		odgovor = login(username, password);
		return odgovor;
	}
	
	public String edit(String SHA1key, String username, String password, String name, String surname, String oib, String address, String city, String country_id, String phone, String email, String date_birth) throws Exception{
		String odgovor="";
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		if(!Security.areParamsOK(username, password, name, surname, oib, country_id, email, date_birth)){
    		odgovor = Security.prepareErrorJson(5, Security.ERROR5);
    		return odgovor;
    	}
		int id = sec.getSession(SHA1key);
    	if(id==-1){
    		odgovor=Security.prepareErrorJson(1,Security.ERROR1);
    		return odgovor;
    	}
		int country = Integer.parseInt(country_id);
		stat_edit.setString(1, username);
		stat_edit.setString(2, password);
		stat_edit.setString(3, name);
		stat_edit.setString(4, surname);
		stat_edit.setString(5, oib);
		stat_edit.setString(6, address);
		stat_edit.setString(7, city);
		stat_edit.setInt(8, country);
		stat_edit.setString(9, phone);
		stat_edit.setString(10, email);
		stat_edit.setString(11, date_birth);
		stat_edit.setInt(12, id);
		int status = stat_edit.executeUpdate();
		if(status==1){
    		data.put("success", 1);
    		main.put("data", data);
    		odgovor = main.toString();
    	}
    	else{
    		odgovor=Security.prepareErrorJson(5,Security.ERROR5);
    	}
		return odgovor;
	}
	
	/**
	 * funkcija koja vraca podatke o pojedinom korisniku, ako je korisnik owner, vraca se i oib
	 * @param SHA1key - SHA1 kljuc sesije
	 * @return JSON kodirane podatke o korisniku
	 * @throws Exception
	 */
	public String data(String SHA1key) throws Exception{
		String odgovor="";
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
    	
		int id = sec.getSession(SHA1key);
    	if(id==-1){
    		odgovor=Security.prepareErrorJson(1,Security.ERROR1);
    		return odgovor;
    	}
    	stat_data.setInt(1, id);
    	ResultSet rezultati = stat_data.executeQuery();
    	if(rezultati.first()){
    		String username = rezultati.getString("username");
    		String name = rezultati.getString("name");
    		String surname = rezultati.getString("surname");
    		String oib = rezultati.getString("oib");
    		String address = rezultati.getString("address");
    		String city = rezultati.getString("city");
    		String country_id = rezultati.getString("country_id");
    		String phone = rezultati.getString("phone");
    		String email = rezultati.getString("email");
    		String date_birth = rezultati.getString("date_birth");
    		String type = rezultati.getString("user_type.name");
    		if(oib!=null){
    			data.put("oib", oib);
    		}
    		data.put("username", username);
    		data.put("name", name);
    		data.put("surname", surname);
    		data.put("address", address);
    		data.put("city", city);
    		data.put("country_id", country_id);
    		data.put("phone", phone);
    		data.put("email", email);
    		data.put("date_birth", date_birth);
    		data.put("type", type);
    		main.put("data", data);
    		odgovor=main.toString();
    	}
    	rezultati.close();
		return odgovor;
	}
}
