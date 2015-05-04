package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

public class Country {
	
	private Connection connect = null;
	
	private PreparedStatement stat_all;
	
	public Country(Connection connection) throws SQLException{
		connect = connection;
		stat_all = connect.prepareStatement("SELECT * FROM country;");
	}
	
	
	public String all() throws Exception{
		String name;
		int id;
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		JSONObject country;
		ResultSet rezultati = stat_all.executeQuery();
		while(rezultati.next()){
			id = rezultati.getInt("id");
			name = rezultati.getString("name");
			country = new JSONObject();
			country.put("id", id);
			country.put("name", name);
			data.put(country);
		}
		main.put("data", data);
		return main.toString();
	}
}
