package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Object {
	
	private Connection connect = null;
	private Security sec;
	
	private PreparedStatement stat_all;
	private PreparedStatement stat_one;
	private PreparedStatement stat_can_edit_owners;
	private PreparedStatement stat_one_owners;
	private PreparedStatement stat_edit;
	private PreparedStatement stat_edit_owners;
	private PreparedStatement stat_add_owner;
	private PreparedStatement stat_add_owner_user;
	private PreparedStatement stat_owner_update_current;
	private PreparedStatement stat_delete_owner;
	private PreparedStatement stat_delete_user_for_percentage;
	private PreparedStatement stat_add;
	private PreparedStatement stat_add_object_id;
	
	public Object(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		
		stat_all = connect.prepareStatement("SELECT id, name, percentage FROM object JOIN owner ON object.id=object_id"
				+ " WHERE user_id=? ORDER BY percentage DESC;");
		stat_one = connect.prepareStatement("SELECT name, `desc`, addr, city, country_id, lat, `long` "
				+ "FROM object JOIN owner ON id=object_id AND id=? AND user_id=?;");
		stat_can_edit_owners = connect.prepareStatement("SELECT user_id=? AS provjera FROM owner "
				+ "WHERE object_id=? ORDER BY percentage DESC, user_id LIMIT 1;");
		stat_one_owners = connect.prepareStatement("SELECT user.id, user.name, surname, oib, phone, email, percentage "
				+ "FROM object JOIN owner ON object.id=object_id JOIN user ON user.id=user_id "
				+ "WHERE object_id=? ORDER BY percentage DESC, user_id;");
		stat_edit = connect.prepareStatement("UPDATE object SET name=?, `desc`=?, addr=?, city=?, country_id=?, lat=?, `long`=? WHERE id=?;");
		stat_edit_owners = connect.prepareStatement("UPDATE owner SET percentage=? WHERE user_id=? AND object_id=?;");
		stat_add_owner_user = connect.prepareStatement("SELECT user.id, user_type.name FROM user JOIN user_type ON user_type_id=user_type.id WHERE oib=?;");
		stat_add_owner = connect.prepareStatement("INSERT INTO owner(user_id, object_id, percentage) VALUES(?, ?, ?);");
		stat_owner_update_current = connect.prepareStatement("UPDATE owner SET percentage=percentage+? WHERE user_id=? AND object_id=?;");
		stat_delete_owner = connect.prepareStatement("DELETE FROM owner WHERE user_id=? AND object_id=?;");
		stat_delete_user_for_percentage = connect.prepareStatement("SELECT user_id FROM owner WHERE object_id=? ORDER BY percentage DESC LIMIT 1;");
		stat_add = connect.prepareStatement("INSERT INTO object(name, `desc`, addr, city, country_id, lat, `long`) VALUES (?, ?, ?, ?, ?, ?, ?);");
		stat_add_object_id = connect.prepareStatement("SELECT id FROM object WHERE name=? AND `desc`=? AND addr=? AND city=? AND country_id=? AND lat=? AND`long`=?;");
	}
	
	public String all(String session_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_all.setInt(1, id);
    	ResultSet rezultati = stat_all.executeQuery();
    	JSONObject objekt;
    	while(rezultati.next()){
    		objekt = new JSONObject();
    		objekt.put("id", rezultati.getInt("id"));
    		objekt.put("name", rezultati.getString("name"));
    		objekt.put("percentage", rezultati.getString("percentage"));
    		data.put(objekt);
    	}
    	main.put("data", data);
		return main.toString();
	}
	
	public String one(String session_id, String object_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_one.setString(1, object_id);
    	stat_one.setInt(2, id);
    	ResultSet rezultati = stat_one.executeQuery();
    	if(rezultati.first()){
    		data.put("name", rezultati.getString("name"));
    		data.put("desc", rezultati.getString("desc"));
    		data.put("addr", rezultati.getString("addr"));
    		data.put("city", rezultati.getString("city"));
    		data.put("country_id", rezultati.getInt("country_id"));
    		data.put("lat", rezultati.getDouble("lat"));
    		data.put("long", rezultati.getDouble("long"));
    	}
    	else{
    		return Security.prepareErrorJson(11,Security.ERROR11);
    	}
    	
    	stat_can_edit_owners.setInt(1, id);
    	stat_can_edit_owners.setString(2, object_id);
    	rezultati = stat_can_edit_owners.executeQuery();
    	rezultati.first();
    	data.put("can_edit_owners", rezultati.getBoolean("provjera"));
    	
    	stat_one_owners.setString(1, object_id);
    	rezultati = stat_one_owners.executeQuery();
    	JSONArray owners = new JSONArray();
    	JSONObject owner;
    	while(rezultati.next()){
    		owner = new JSONObject();
    		owner.put("id", rezultati.getInt("user.id"));
    		owner.put("name", rezultati.getString("user.name"));
    		owner.put("surname", rezultati.getString("surname"));
    		owner.put("oib", rezultati.getString("oib"));
    		owner.put("phone", rezultati.getString("phone"));
    		owner.put("email", rezultati.getString("email"));
    		owner.put("percentage", rezultati.getInt("percentage"));
    		owners.put(owner);
    	}
    	
    	data.put("owners", owners);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String edit(String session_id, String object_id, String name, String desc, String addr, String city, String country_id, String lat, String lng) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(object_id, name, addr, city, country_id, lat, lng)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_edit.setString(1, name);
    	stat_edit.setString(2, desc);
    	stat_edit.setString(3, addr);
    	stat_edit.setString(4, city);
    	stat_edit.setString(5, country_id);
    	stat_edit.setDouble(6, Double.parseDouble(lat));
    	stat_edit.setDouble(7, Double.parseDouble(lng));
    	stat_edit.setString(8, object_id);
    	stat_edit.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String add(String session_id, String name, String desc, String addr, String city, String country_id, String lat, String lng) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(name, addr, city, country_id, lat, lng)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_add.setString(1, name);
    	stat_add.setString(2, desc);
    	stat_add.setString(3, addr);
    	stat_add.setString(4, city);
    	stat_add.setString(5, country_id);
    	stat_add.setDouble(6, Double.parseDouble(lat));
    	stat_add.setDouble(7, Double.parseDouble(lng));
    	stat_add.executeUpdate();
    	
    	stat_add_object_id.setString(1, name);
    	stat_add_object_id.setString(2, desc);
    	stat_add_object_id.setString(3, addr);
    	stat_add_object_id.setString(4, city);
    	stat_add_object_id.setString(5, country_id);
    	stat_add_object_id.setDouble(6, Double.parseDouble(lat));
    	stat_add_object_id.setDouble(7, Double.parseDouble(lng));
    	ResultSet rezultati = stat_add_object_id.executeQuery();
    	rezultati.first();
    	int object_id = rezultati.getInt("id");
    	
    	stat_add_owner.setInt(1, id);
    	stat_add_owner.setInt(2, object_id);
    	stat_add_owner.setInt(3, 100);
    	stat_add_owner.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String editOwners(String session_id, String object_id, String owners) throws JSONException, SQLException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	JSONObject vlasnici = new JSONObject(owners);
    	Iterator<String> iterator = vlasnici.keys();
    	while(iterator.hasNext()){
    		String key = iterator.next();
    		stat_edit_owners.setInt(1, vlasnici.getInt(key));
    		stat_edit_owners.setString(2, key);
    		stat_edit_owners.setString(3, object_id);
    		stat_edit_owners.executeUpdate();
    	}
    	
    	data.put("success", 1);
    	main.put("data", data);
		
		return main.toString();
	}
	
	public String addOwner(String session_id, String object_id, String oib, String percentage) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_add_owner_user.setString(1, oib);
    	ResultSet rezultati = stat_add_owner_user.executeQuery();
    	int user_id;
    	if(rezultati.next()){
    		user_id = rezultati.getInt("user.id");
    		String type = rezultati.getString("user_type.name");
    		if(type.equals("Guest")){
    			return Security.prepareErrorJson(13,Security.ERROR13);
    		}
    		if(user_id==id){
    			return Security.prepareErrorJson(14,Security.ERROR14);
    		}
    	}
    	else{
    		return Security.prepareErrorJson(13,Security.ERROR13);
    	}
    	
    	stat_add_owner.setInt(1, user_id);
    	stat_add_owner.setString(2, object_id);
    	stat_add_owner.setString(3, percentage);
    	stat_add_owner.executeUpdate();
    	
    	stat_owner_update_current.setInt(1, -Integer.parseInt(percentage));
    	stat_owner_update_current.setInt(2, id);
    	stat_owner_update_current.setString(3, object_id);
    	stat_owner_update_current.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
		
		return main.toString();
	}
	
	public String deleteOwner(String session_id, String user_id, String object_id, String percentage) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);

    	stat_delete_owner.setString(1, user_id);
    	stat_delete_owner.setString(2, object_id);
    	stat_delete_owner.executeUpdate();
    	
    	stat_delete_user_for_percentage.setString(1, object_id);
    	ResultSet rezultati = stat_delete_user_for_percentage.executeQuery();
    	rezultati.next();
    	int user_id_za_dodavanje_postotka = rezultati.getInt("user_id");
    	
    	stat_owner_update_current.setInt(1, Integer.parseInt(percentage));
    	stat_owner_update_current.setInt(2, user_id_za_dodavanje_postotka);
    	stat_owner_update_current.setString(3, object_id);
    	stat_owner_update_current.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
		
		return main.toString();
	}
}
