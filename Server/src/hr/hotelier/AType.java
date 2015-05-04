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
	
	public AType(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		
		stat_type_all = connect.prepareStatement("SELECT id, name FROM acc_type;");
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
}
