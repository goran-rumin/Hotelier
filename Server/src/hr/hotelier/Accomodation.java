package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Accomodation {
	
	private Connection connect = null;
	private Security sec;
	
	private int BROJ_APP_PO_STRANICI = 9;
	
	private PreparedStatement stat_guest_all;
	private PreparedStatement stat_pages_num;
	
	public Accomodation(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		stat_guest_all = connect.prepareStatement("SELECT accommodation.id, accommodation.name, category, main_pic, acc_type.name, "
				+ "object_city, MIN(price) AS min_price, MAX(price) AS max_price from accommodation JOIN acc_type ON acc_type_id=acc_type.id "
				+ "JOIN object ON object_id=object.id JOIN prices ON prices.acc_id=accommodation.id "
				+ "GROUP BY accommodation.id LIMIT ?,?;");
		stat_pages_num = connect.prepareStatement("SELECT COUNT(id) AS count FROM accommodation;");
	}
	
	public String guestAll(String index) throws SQLException, JSONException{
		int nizi,visi;
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		visi = Integer.parseInt(index);
		nizi = (visi-1)*BROJ_APP_PO_STRANICI;
		visi*=BROJ_APP_PO_STRANICI;
		
		ResultSet rezultati = stat_pages_num.executeQuery();
		rezultati.first();
		int count = rezultati.getInt("count");
		data.put("pages", count/BROJ_APP_PO_STRANICI+1);
		
		stat_guest_all.setInt(1, nizi);
		stat_guest_all.setInt(2, visi);
		rezultati = stat_guest_all.executeQuery();
		JSONArray acc = new JSONArray();
		JSONObject acc_one;
		while(rezultati.next()){
			acc_one = new JSONObject();
			acc_one.put("acc_id", rezultati.getInt("accommodation.id"));
			acc_one.put("name", rezultati.getString("accommodation.name"));
			acc_one.put("category", rezultati.getString("category"));
			acc_one.put("image", rezultati.getString("main_pic"));
			acc_one.put("acc_type_name", rezultati.getString("acc_type.name"));
			acc_one.put("city", rezultati.getString("object_city"));
			acc_one.put("min_price", rezultati.getString("min_price"));
			acc_one.put("max_price", rezultati.getString("max_price"));
			acc.put(acc_one);
		}
		data.put("acc", acc);
		main.put("data", data);
		return main.toString();
	}
	
}
