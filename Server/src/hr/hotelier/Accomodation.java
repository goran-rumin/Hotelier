package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Accomodation {
	
	private Connection connect = null;
	private Security sec;
	
	private int BROJ_APP_PO_STRANICI = 9;
	
	private PreparedStatement stat_guest_all;
	private PreparedStatement stat_pages_num;
	private PreparedStatement stat_guest_one;
	private PreparedStatement stat_guest_one_pictures;
	private PreparedStatement stat_guest_one_comments;
	private PreparedStatement stat_guest_one_rating;
	
	public Accomodation(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		stat_guest_all = connect.prepareStatement("SELECT accommodation.id, accommodation.name, object.name, category, main_pic, acc_type.name, "
				+ "object.city, MIN(price) AS min_price, MAX(price) AS max_price, ROUND(AVG(rating),2) as avg_rating from accommodation JOIN acc_type ON acc_type_id=acc_type.id "
				+ "JOIN object ON object_id=object.id JOIN prices ON prices.acc_id=accommodation.id LEFT OUTER JOIN comments ON accommodation.id=comments.acc_id "
				+ "WHERE accommodation.name LIKE ? AND object.name LIKE ? AND category LIKE ? AND acc_type.id LIKE ? AND beach_distance<? "
				+ "AND has_sea_view LIKE ? AND has_air_condition LIKE ? "
				+ "AND has_sattv LIKE ? AND has_balcony LIKE ? AND has_breakfast LIKE ? AND accepts_pets LIKE ? "
				+ "GROUP BY accommodation.id HAVING min_price>? AND max_price<? ORDER BY avg_rating DESC, min_price LIMIT ?,?;");
		stat_pages_num = connect.prepareStatement("SELECT COUNT(id) AS count FROM accommodation;");
		stat_guest_one = connect.prepareStatement("SELECT accommodation.id, accommodation.name, accommodation.desc, main_pic, object.name, object.desc, object.addr, object.city, "
				+ "country.name, lat, `long`, user.name, user.surname, user.phone, user.email, category, surface, has_sea_view, has_air_condition, "
				+ "has_sattv, has_balcony, has_breakfast, accepts_pets, beach_distance, acc_type.name, ppl_num_min, ppl_num_max "
				+ "FROM accommodation JOIN object ON accommodation.object_id=object.id "
				+ "JOIN acc_type ON acc_type_id=acc_type.id "
				+ "JOIN country ON object.country_id=country.id "
				+ "JOIN owner ON owner.object_id=object.id "
				+ "JOIN user ON owner.user_id=user.id "
				+ "WHERE user.id=(SELECT user_id FROM owner WHERE object.id=object_id ORDER BY percentage DESC, user_id LIMIT 1) "
				+ "AND accommodation.id=?;");
		stat_guest_one_pictures = connect.prepareStatement("SELECT value FROM acc_data JOIN data_type ON data_type_id=data_type.id WHERE name='Image' AND acc_id=?;");
		stat_guest_one_comments = connect.prepareStatement("SELECT text, rating, time, name, surname FROM comments JOIN user ON comments.user_id=user.id WHERE acc_id=? ORDER BY time DESC LIMIT 20;");
		stat_guest_one_rating = connect.prepareStatement("SELECT ROUND(AVG(rating),2) AS rating FROM comments WHERE acc_id=?;");
	}
	
	public String guestAll(String index, String search_params) throws SQLException, JSONException{
		int nizi,visi;
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
	
		HashMap<String,String> params = prepareSearchParams(search_params);
		System.out.println(""+params);
		visi = Integer.parseInt(index);
		nizi = (visi-1)*BROJ_APP_PO_STRANICI;
		visi*=BROJ_APP_PO_STRANICI;
		
		ResultSet rezultati = stat_pages_num.executeQuery();
		rezultati.first();
		int count = rezultati.getInt("count");
		data.put("pages", count/BROJ_APP_PO_STRANICI+1);
		stat_guest_all.setString(1, params.get("acc_name"));
		stat_guest_all.setString(2, params.get("obj_name"));
		stat_guest_all.setString(3, params.get("category"));
		stat_guest_all.setString(4, params.get("atype"));
		stat_guest_all.setString(5, params.get("beach_distance"));
		stat_guest_all.setString(6, params.get("sea"));
		stat_guest_all.setString(7, params.get("air"));
		stat_guest_all.setString(8, params.get("sattv"));
		stat_guest_all.setString(9, params.get("balcony"));
		stat_guest_all.setString(10, params.get("breakfast"));
		stat_guest_all.setString(11, params.get("pets"));
		stat_guest_all.setString(12, params.get("price_min"));
		stat_guest_all.setString(13, params.get("price_max"));
		stat_guest_all.setInt(14, nizi);
		stat_guest_all.setInt(15, visi);
		rezultati = stat_guest_all.executeQuery();
		JSONArray acc = new JSONArray();
		JSONObject acc_one;
		while(rezultati.next()){
			acc_one = new JSONObject();
			acc_one.put("acc_id", rezultati.getInt("accommodation.id"));
			acc_one.put("name", rezultati.getString("accommodation.name"));
			acc_one.put("object_name", rezultati.getString("object.name"));
			acc_one.put("category", rezultati.getString("category"));
			acc_one.put("image", rezultati.getString("main_pic"));
			acc_one.put("acc_type_name", rezultati.getString("acc_type.name"));
			acc_one.put("city", rezultati.getString("object.city"));
			acc_one.put("min_price", rezultati.getString("min_price"));
			acc_one.put("max_price", rezultati.getString("max_price"));
			acc_one.put("rating", rezultati.getString("avg_rating")==null ? "Not rated" : rezultati.getString("avg_rating"));
			acc.put(acc_one);
		}
		data.put("acc", acc);
		main.put("data", data);
		return main.toString();
	}
	
	String guestOne(String acc_id) throws SQLException, JSONException{
		String odgovor;
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		if(!Security.areParamsOK(acc_id)){
    		odgovor = Security.prepareErrorJson(6, Security.ERROR6);
    		return odgovor;
    	}
		stat_guest_one.setString(1, acc_id);
    	ResultSet rezultati = stat_guest_one.executeQuery();
    	String slika;
    	
    	if(rezultati.first()){
    		data.put("acc_id", rezultati.getString("accommodation.id"));
    		data.put("acc_name", rezultati.getString("accommodation.name"));
    		data.put("acc_desc", rezultati.getString("accommodation.desc"));
    		data.put("obj_name", rezultati.getString("object.name"));
    		data.put("obj_desc", rezultati.getString("object.desc"));
    		data.put("addr", rezultati.getString("object.addr"));
    		data.put("city", rezultati.getString("object.city"));
    		data.put("country", rezultati.getString("country.name"));
    		data.put("lat", rezultati.getString("lat"));
    		data.put("long", rezultati.getString("long"));
    		data.put("owner_name", rezultati.getString("user.name")+" "+rezultati.getString("user.surname"));
    		data.put("owner_phone", rezultati.getString("user.phone"));
    		data.put("owner_email", rezultati.getString("user.email"));
    		data.put("category", rezultati.getString("category"));
    		data.put("surface", rezultati.getString("surface"));
    		data.put("has_sea_view", rezultati.getInt("has_sea_view"));
    		data.put("has_air_condition", rezultati.getInt("has_air_condition"));
    		data.put("has_sattv", rezultati.getInt("has_sattv"));
    		data.put("has_balcony", rezultati.getInt("has_balcony"));
    		data.put("has_breakfast", rezultati.getInt("has_breakfast"));
    		data.put("accepts_pets", rezultati.getInt("accepts_pets"));
    		data.put("beach_distance", rezultati.getString("beach_distance"));
    		data.put("acc_type", rezultati.getString("acc_type.name"));
    		data.put("ppl_num_min", rezultati.getString("ppl_num_min"));
    		data.put("ppl_num_max", rezultati.getString("ppl_num_max"));
    		slika = rezultati.getString("main_pic");
    		
    		stat_guest_one_rating.setString(1, acc_id);
    		rezultati = stat_guest_one_rating.executeQuery();
    		if(rezultati.first()){
    			String rating = rezultati.getString("rating");
    			if(rating!=null)
    				data.put("rating", rating);
    			else
    				data.put("rating", "Not rated");
    		}
    		
    		stat_guest_one_pictures.setString(1, acc_id);
    		rezultati = stat_guest_one_pictures.executeQuery();
    		JSONArray images = new JSONArray();
    		images.put(slika);
    		while(rezultati.next()){
    			images.put(rezultati.getString("value"));
    		}
    		data.put("images", images);
    		
    		stat_guest_one_comments.setString(1, acc_id);
    		rezultati = stat_guest_one_comments.executeQuery();
    		JSONArray comments = new JSONArray();
    		JSONObject comment;
    		while(rezultati.next()){
    			comment = new JSONObject();
    			comment.put("name",rezultati.getString("name")+" "+rezultati.getString("surname"));
    			comment.put("text",rezultati.getString("text"));
    			comment.put("rating",rezultati.getInt("rating"));
    			comment.put("time",rezultati.getString("time"));
    			comments.put(comment);
    		}
    		data.put("comments", comments);
    		
    		main.put("data", data);
    		odgovor=main.toString();
    	}else{
    		odgovor=Security.prepareErrorJson(6,Security.ERROR6);
    	}
    	rezultati.close();
    	return odgovor;
	}
	
	HashMap<String,String> prepareSearchParams(String params) throws JSONException{
		HashMap<String,String> search_params = new HashMap<String,String>();
		JSONObject search = new JSONObject(params);
		String acc_name;
		try{
			acc_name = search.getString("name");
		}catch(Exception e){
			acc_name = "";
		}
		acc_name = "%"+acc_name+"%";
		search_params.put("acc_name", acc_name);
		
		String obj_name;
		try{
			obj_name = search.getString("obj_name");
		}catch(Exception e){
			obj_name = "";
		}
		obj_name = "%"+obj_name+"%";
		search_params.put("obj_name", obj_name);
		
		String category;
		try{
			category = search.getString("category");
		}catch(Exception e){
			category = "";
		}
		category = "%"+category+"%";
		search_params.put("category", category);
		
		String atype;
		try{
			atype = search.getString("atype");
			if(atype.equals("0"))
				atype="";
		}catch(Exception e){
			atype = "";
		}
		atype = "%"+atype+"%";
		search_params.put("atype", atype);
		
		String price_min;
		try{
			price_min = search.getString("price_min");
			if(!Security.areParamsOK(price_min))
				price_min = "-1";
		}catch(Exception e){
			price_min = "-1";
		}
		search_params.put("price_min", price_min);
		
		String price_max;
		try{
			price_max = search.getString("price_max");
			if(!Security.areParamsOK(price_max))
				price_max = "100000";
		}catch(Exception e){
			price_max = "100000";
		}
		search_params.put("price_max", price_max);
		
		String date_from;
		try{
			date_from = search.getString("date_from");
		}catch(Exception e){
			date_from = "";
		}
		date_from = "%"+date_from+"%";
		search_params.put("date_from", date_from);
		
		String date_until;
		try{
			date_until = search.getString("date_until");
		}catch(Exception e){
			date_until = "";
		}
		date_until = "%"+date_until+"%";
		search_params.put("date_until", date_until);
		
		String beach_distance;
		try{
			beach_distance = search.getString("beach_distance");
			if(!Security.areParamsOK(beach_distance))
				beach_distance = "100000";
		}catch(Exception e){
			beach_distance = "100000";
		}
		search_params.put("beach_distance", beach_distance);
		
		String sea;
		try{
			sea = search.getString("sea");
			sea = convertBoolean(sea);
		}catch(Exception e){
			sea = "";
		}
		sea = "%"+sea+"%";
		search_params.put("sea", sea);
		
		String air;
		try{
			air = search.getString("air");
			air = convertBoolean(air);
		}catch(Exception e){
			air = "";
		}
		air = "%"+air+"%";
		search_params.put("air", air);
		
		String sattv;
		try{
			sattv = search.getString("sattv");
			sattv = convertBoolean(sattv);
		}catch(Exception e){
			sattv = "";
		}
		sattv = "%"+sattv+"%";
		search_params.put("sattv", sattv);
		
		String balcony;
		try{
			balcony = search.getString("balcony");
			balcony = convertBoolean(balcony);
		}catch(Exception e){
			balcony = "";
		}
		balcony = "%"+balcony+"%";
		search_params.put("balcony", balcony);
		
		String breakfast;
		try{
			breakfast = search.getString("breakfast");
			breakfast = convertBoolean(breakfast);
		}catch(Exception e){
			breakfast = "";
		}
		breakfast = "%"+breakfast+"%";
		search_params.put("breakfast", breakfast);
		
		String pets;
		try{
			pets = search.getString("pets");
			pets = convertBoolean(pets);
		}catch(Exception e){
			pets = "";
		}
		pets = "%"+pets+"%";
		search_params.put("pets", pets);
		
		return search_params;
	}
	
	String convertBoolean(String a){
		if(a.equals("true"))
			return "1";
		return "";
	}
}
