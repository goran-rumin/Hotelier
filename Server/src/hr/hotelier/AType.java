package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AType {
	
	private Connection connect = null;
	private Security sec;
	
	private PreparedStatement stat_type_all;
	private PreparedStatement stat_type_add;
	
	public AType(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		
		stat_type_all = connect.prepareStatement("SELECT id, name FROM acc_type;");
		stat_type_add = connect.prepareStatement("INSERT INTO acc_type(name, ppl_num_min, ppl_num_max) VALUES (?, ?, ?);");
	}

	public String all() throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		ResultSet rezultati = stat_type_all.executeQuery();
		JSONObject type;
		while(rezultati.next()){
			type = new JSONObject();
			type.put("id", rezultati.getInt("id"));
			type.put("name", rezultati.getString("name"));
			data.put(type);
		}
		main.put("data", data);
		return main.toString();
	}
	
	public String add(String session_id, String name, String min, String max) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(name, min, max)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_type_add.setString(1, name);
    	stat_type_add.setString(2, min);
    	stat_type_add.setString(3, max);
    	stat_type_add.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
}
