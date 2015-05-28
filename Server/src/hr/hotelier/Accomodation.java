package hr.hotelier;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Accomodation {
	
	private Connection connect = null;
	private Security sec;
	
	private int BROJ_APP_PO_STRANICI = 9;
	private double POLUMJER_PRETRAGE = 0.031726; //odgovara duljini 2.5 km
	
	private int DATA_TYPE_IMAGE = 1;
	
	private PreparedStatement stat_guest_all;
	private PreparedStatement stat_pages_num;
	private PreparedStatement stat_guest_one;
	private PreparedStatement stat_guest_one_pictures;
	private PreparedStatement stat_guest_one_comments;
	private PreparedStatement stat_guest_one_rating;
	private PreparedStatement stat_guest_one_prices;
	private PreparedStatement stat_owner_all;
	private PreparedStatement stat_owner_one;
	private PreparedStatement stat_owner_one_images;
	private PreparedStatement stat_edit;
	private PreparedStatement stat_add_picture;
	private PreparedStatement stat_delete_picture;
	private PreparedStatement stat_add;
	private PreparedStatement stat_delete_prices;
	private PreparedStatement stat_add_price;
	
	public Accomodation(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		stat_guest_all = connect.prepareStatement("SELECT accommodation.id, accommodation.name, object.name, category, main_pic, acc_type.name, "
				+ "object.city, MIN(price) AS min_price, MAX(price) AS max_price, ROUND(AVG(rating),2) as avg_rating from accommodation JOIN acc_type ON acc_type_id=acc_type.id "
				+ "JOIN object ON object_id=object.id JOIN prices ON prices.acc_id=accommodation.id LEFT OUTER JOIN comments ON accommodation.id=comments.acc_id "
				+ "WHERE accommodation.name LIKE ? AND object.name LIKE ? AND category LIKE ? AND acc_type.id LIKE ? AND beach_distance<? "
				+ "AND has_sea_view LIKE ? AND has_air_condition LIKE ? "
				+ "AND has_sattv LIKE ? AND has_balcony LIKE ? AND has_breakfast LIKE ? AND accepts_pets LIKE ? "
				+ "AND lat BETWEEN ? AND ? AND `long` BETWEEN ? AND ? "
				+ "AND accommodation.id NOT IN (SELECT acc_id FROM reservation JOIN res_status ON res_status_id=res_status.id WHERE (name='Confirmed' OR name='Pending' OR name='Completed') AND ((date_from<=? AND date_until>=?) OR (date_from<=DATE_SUB(?,INTERVAL 1 DAY) AND date_until>=DATE_SUB(?,INTERVAL 1 DAY)) OR (date_from>? AND date_until<DATE_SUB(?,INTERVAL 1 DAY)))) " //kraj zeljenog boravka-1 jer se gledaju noæenja, a u bazi se kao zadnji dan sprema dan prije samog odlaska
				+ "AND ( ? OR accommodation.id IN (SELECT acc_id FROM prices "
				+ "WHERE acc_id IN (SELECT acc_id FROM prices WHERE date_from<=? AND date_until>=?) "
				+ "AND acc_id IN (SELECT acc_id FROM prices WHERE date_from<=DATE_SUB(?,INTERVAL 1 DAY) "
				+ "AND date_until>=DATE_SUB(?,INTERVAL 1 DAY)))) "  //ako se barem dio trazenog termina preklapa s cjenikom, apartman se prikazuje
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
		stat_guest_one_prices = connect.prepareStatement("SELECT date_from, DATE_ADD(date_until,INTERVAL 1 DAY) as date_until, price FROM prices WHERE acc_id=? AND YEAR(date_from)=YEAR(NOW()) ORDER BY date_from;");
		stat_owner_all = connect.prepareStatement("SELECT object.name, accommodation.id, accommodation.name FROM accommodation JOIN object ON accommodation.object_id=object.id "
				+ "JOIN owner ON object.id=owner.object_id WHERE user_id=? "
				+ "ORDER BY object.name, accommodation.name;");
		stat_owner_one = connect.prepareStatement("SELECT * FROM accommodation WHERE id=?;");
		stat_edit = connect.prepareStatement("UPDATE accommodation SET object_id=?, name=?, category=?, surface=?, has_sea_view=?, "
				+ "has_air_condition=?, has_sattv=?, has_balcony=?, has_breakfast=?, accepts_pets=?, beach_distance=?, "
				+ "main_pic=?, `desc`=?, acc_type_id=? WHERE id=?;");
		stat_owner_one_images = connect.prepareStatement("SELECT acc_data.id, value FROM acc_data JOIN data_type ON data_type_id=data_type.id "
				+ "WHERE data_type.name='Image' AND acc_id=?;");
		stat_add_picture = connect.prepareStatement("INSERT INTO acc_data(acc_id, value, data_type_id) VALUES(?, ?,"+DATA_TYPE_IMAGE+");");
		stat_delete_picture = connect.prepareStatement("DELETE FROM acc_data WHERE id=?;");
		stat_add = connect.prepareStatement("INSERT INTO accommodation(object_id, name, category, surface, has_sea_view, has_air_condition, has_sattv, has_balcony, has_breakfast, accepts_pets, "
				+ "beach_distance, main_pic, `desc`, acc_type_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
		stat_delete_prices = connect.prepareStatement("DELETE FROM prices WHERE acc_id=? AND YEAR(date_from)=YEAR(NOW());");
		stat_add_price = connect.prepareStatement("INSERT INTO prices(acc_id, date_from, date_until, price) VALUES (?, ?, DATE_SUB(?,INTERVAL 1 DAY), ?);");
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
		
		//ResultSet rezultati = stat_pages_num.executeQuery();
		//rezultati.first();
		//int count = rezultati.getInt("count");
		//data.put("pages", count/BROJ_APP_PO_STRANICI+1);
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
		stat_guest_all.setString(12, params.get("lat_min"));
		stat_guest_all.setString(13, params.get("lat_max"));
		stat_guest_all.setString(14, params.get("long_min"));
		stat_guest_all.setString(15, params.get("long_max"));
		stat_guest_all.setString(16, params.get("date_from"));
		stat_guest_all.setString(17, params.get("date_from"));
		stat_guest_all.setString(18, params.get("date_until"));
		stat_guest_all.setString(19, params.get("date_until"));
		stat_guest_all.setString(20, params.get("date_from"));
		stat_guest_all.setString(21, params.get("date_until"));
		if(!params.get("date_from").equals("%%") && !params.get("date_until").equals("%%")){
			stat_guest_all.setBoolean(22, false);
		}  //ako su datumi definirani, onda se pretrazuje da li su smijestaji u ponudi
		else{
			stat_guest_all.setBoolean(22, true);
		}
		stat_guest_all.setString(23, params.get("date_from"));
		stat_guest_all.setString(24, params.get("date_from"));
		stat_guest_all.setString(25, params.get("date_until"));
		stat_guest_all.setString(26, params.get("date_until"));
		stat_guest_all.setString(27, params.get("price_min"));
		stat_guest_all.setString(28, params.get("price_max"));
		stat_guest_all.setInt(29, nizi);
		stat_guest_all.setInt(30, visi);
		ResultSet rezultati = stat_guest_all.executeQuery();
		JSONArray acc = new JSONArray();
		JSONObject acc_one;
		int count = 0;
		while(rezultati.next()){
			count++;
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
		data.put("pages", count/BROJ_APP_PO_STRANICI+1);
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
    		data.put("lat", rezultati.getDouble("lat"));
    		data.put("long", rezultati.getDouble("long"));
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
    		
    		stat_guest_one_prices.setString(1, acc_id);
    		rezultati = stat_guest_one_prices.executeQuery();
    		JSONArray prices = new JSONArray();
    		JSONObject price;
    		while(rezultati.next()){
    			price = new JSONObject();
    			price.put("date_from",rezultati.getString("date_from"));
    			price.put("date_until",rezultati.getString("date_until"));
    			price.put("price",rezultati.getString("price"));
    			prices.put(price);
    		}
    		data.put("prices", prices);
    		
    		main.put("data", data);
    		odgovor=main.toString();
    	}else{
    		odgovor=Security.prepareErrorJson(6,Security.ERROR6);
    	}
    	rezultati.close();
    	return odgovor;
	}
	
	public String ownerAll(String session_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_owner_all.setInt(1, id);
    	ResultSet rezultati = stat_owner_all.executeQuery();
    	String trenutacni_objekt = "";
    	JSONArray trenutacni_smijestaji = new JSONArray();
    	JSONObject trenutacni_smijestaj;
    	while(rezultati.next()){
    		trenutacni_smijestaj = new JSONObject();
    		if(!trenutacni_objekt.equals(rezultati.getString("object.name"))){
    			if(!trenutacni_objekt.equals("")){
    				data.put(trenutacni_objekt, trenutacni_smijestaji);
    			}
    			trenutacni_objekt = rezultati.getString("object.name");
    			trenutacni_smijestaji = new JSONArray();
    		}
    		trenutacni_smijestaj.put("id", rezultati.getInt("accommodation.id"));
    		trenutacni_smijestaj.put("name", rezultati.getString("accommodation.name"));
    		trenutacni_smijestaji.put(trenutacni_smijestaj);
    	}
    	data.put(trenutacni_objekt, trenutacni_smijestaji);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String all(String session_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_owner_all.setInt(1, id);
    	ResultSet rezultati = stat_owner_all.executeQuery();
    	JSONObject accommodation;
    	while(rezultati.next()){
    		accommodation = new JSONObject();
    		accommodation.put("id", rezultati.getInt("accommodation.id"));
    		accommodation.put("name", rezultati.getString("accommodation.name"));
    		data.put(accommodation);
    	}
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String ownerOne(String session_id, String acc_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_owner_one.setString(1, acc_id);
    	ResultSet rezultati = stat_owner_one.executeQuery();
    	rezultati.first();
    	data.put("id", rezultati.getInt("id"));
    	data.put("object_id", rezultati.getInt("object_id"));
    	data.put("name", rezultati.getString("name"));
    	data.put("category", rezultati.getInt("category"));
    	data.put("surface", rezultati.getInt("surface"));
    	data.put("sea", rezultati.getBoolean("has_sea_view"));
    	data.put("air", rezultati.getBoolean("has_air_condition"));
    	data.put("sattv", rezultati.getBoolean("has_sattv"));
    	data.put("balcony", rezultati.getBoolean("has_balcony"));
    	data.put("breakfast", rezultati.getBoolean("has_breakfast"));
    	data.put("pets", rezultati.getBoolean("accepts_pets"));
    	data.put("beach_distance", rezultati.getInt("beach_distance"));
    	data.put("main_pic", rezultati.getString("main_pic"));
    	data.put("desc", rezultati.getString("desc"));
    	data.put("acc_type_id", rezultati.getInt("acc_type_id"));
    	
    	stat_owner_one_images.setString(1, acc_id);
    	rezultati = stat_owner_one_images.executeQuery();
    	JSONArray images = new JSONArray();
    	JSONObject image;
    	while(rezultati.next()){
    		image = new JSONObject();
    		image.put("id", rezultati.getInt("acc_data.id"));
    		image.put("value", rezultati.getString("value"));
    		images.put(image);
    	}
    	data.put("images", images);
    	
    	stat_guest_one_prices.setString(1, acc_id);
    	rezultati = stat_guest_one_prices.executeQuery();
    	JSONArray prices = new JSONArray();
    	JSONObject price;
    	while(rezultati.next()){
    		price = new JSONObject();
    		price.put("date_from", rezultati.getString("date_from"));
    		price.put("date_until", rezultati.getString("date_until"));
    		price.put("price", rezultati.getInt("price"));
    		prices.put(price);
    	}
    	data.put("prices", prices);
    	
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String edit(String session_id, String acc_id, String object_id, String name, String category, String surface, String sea, String air, 
			String sattv, String balcony, String breakfast, String pets, String beach_distance, String main_pic, String desc, String acc_type_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(object_id, name, category, sea, air, 
    			sattv, balcony, breakfast, pets, acc_type_id)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_edit.setString(1, object_id);
    	stat_edit.setString(2, name);
    	stat_edit.setString(3, category);
    	stat_edit.setString(4, surface);
    	stat_edit.setBoolean(5, Boolean.parseBoolean(sea));
    	stat_edit.setBoolean(6, Boolean.parseBoolean(air));
    	stat_edit.setBoolean(7, Boolean.parseBoolean(sattv));
    	stat_edit.setBoolean(8, Boolean.parseBoolean(balcony));
    	stat_edit.setBoolean(9, Boolean.parseBoolean(breakfast));
    	stat_edit.setBoolean(10, Boolean.parseBoolean(pets));
    	stat_edit.setString(11, beach_distance);
    	stat_edit.setString(12, main_pic);
    	stat_edit.setString(13, desc);
    	stat_edit.setString(14, acc_type_id);
    	stat_edit.setString(15, acc_id);
    	stat_edit.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String add(String session_id, String object_id, String name, String category, String surface, String sea, String air, 
			String sattv, String balcony, String breakfast, String pets, String beach_distance, String main_pic, String desc, String acc_type_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	if(!Security.areParamsOK(object_id, name, category, sea, air, 
    			sattv, balcony, breakfast, pets, acc_type_id)){
    		return Security.prepareErrorJson(12, Security.ERROR12);
    	}
    	
    	stat_add.setString(1, object_id);
    	stat_add.setString(2, name);
    	stat_add.setString(3, category);
    	stat_add.setString(4, surface);
    	stat_add.setBoolean(5, Boolean.parseBoolean(sea));
    	stat_add.setBoolean(6, Boolean.parseBoolean(air));
    	stat_add.setBoolean(7, Boolean.parseBoolean(sattv));
    	stat_add.setBoolean(8, Boolean.parseBoolean(balcony));
    	stat_add.setBoolean(9, Boolean.parseBoolean(breakfast));
    	stat_add.setBoolean(10, Boolean.parseBoolean(pets));
    	stat_add.setString(11, beach_distance);
    	stat_add.setString(12, main_pic);
    	stat_add.setString(13, desc);
    	stat_add.setString(14, acc_type_id);
    	stat_add.executeUpdate();
    
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String picturesSave(String session_id, String acc_id, String images) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	JSONArray slike = new JSONArray(images);
    	JSONObject slika;
    	for(int i=0;i<slike.length();i++){
    		slika = slike.getJSONObject(i);
    		if(slika.getInt("id") != -1){
    			continue;
    		}
    		stat_add_picture.setString(1, acc_id);
    		stat_add_picture.setString(2, slika.getString("value"));
    		stat_add_picture.executeUpdate();
    	}

    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String picturesDelete(String session_id, String image_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_delete_picture.setString(1, image_id);
    	stat_delete_picture.executeUpdate();
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String pictureUpload(String session_id, String base64_code) throws JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	base64_code = base64_code.replaceAll("\\ ", "+");
    	Base64.Decoder decoder = Base64.getDecoder();
    	byte[] picture = null;
		try {
			picture = decoder.decode(base64_code);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
    	Date t = new Date();
    	String file_name = null;
    	try {
    		file_name = "photos/"+Security.sha1(session_id+t.getTime())+".jpg";
    		OutputStream out = new FileOutputStream(Server.PATH+file_name);
			out.write(picture);
	    	out.close();
	    	BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
	    	img.createGraphics().drawImage(ImageIO.read(new File(Server.PATH+file_name)).getScaledInstance(400, 300, Image.SCALE_SMOOTH),0,0,null);
	    	ImageIO.write(img, "jpg", new File(Server.PATH+"small/"+file_name));
		} catch (Exception e) {
			e.printStackTrace();
		}
    	data.put("file", file_name);
    	main.put("data", data);
    	
		return main.toString();
	}
	
	public String pricesAdd(String session_id, String acc_id, String prices) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
		
		int id = sec.getSession(session_id);
    	if(id==-1){
    		return Security.prepareErrorJson(1,Security.ERROR1);
    	}
    	sec.updateSession(session_id);
    	
    	stat_delete_prices.setString(1, acc_id);
    	stat_delete_prices.executeUpdate();
    	
    	JSONArray cijene = new JSONArray(prices);
    	JSONObject cijena;
    	for(int i=0;i<cijene.length();i++){
    		cijena = cijene.getJSONObject(i);
    		if(!Security.areParamsOK(cijena.getString("date_from"), cijena.getString("date_until"), cijena.getString("price"))){
        		return Security.prepareErrorJson(12, Security.ERROR12);
        	}
    		stat_add_price.setString(1, acc_id);
    		stat_add_price.setString(2, cijena.getString("date_from"));
    		stat_add_price.setString(3, cijena.getString("date_until"));
    		stat_add_price.setString(4, cijena.getString("price"));
    		stat_add_price.executeUpdate();
    	}
    	
    	data.put("success", 1);
    	main.put("data", data);
    	
		return main.toString();
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
			if(!Security.areParamsOK(date_from))
				date_from = "%%";
		}catch(Exception e){
			date_from = "%%";
		}
		search_params.put("date_from", date_from);
		
		String date_until;
		try{
			date_until = search.getString("date_until");
			if(!Security.areParamsOK(date_until))
				date_until = "%%";
		}catch(Exception e){
			date_until = "%%";
		}
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
		
		String lat_min, lat_max;
		double lat;
		try{
			lat = Double.parseDouble(search.getString("lat"));
			lat_min = ""+(lat-POLUMJER_PRETRAGE);
			lat_max = ""+(lat+POLUMJER_PRETRAGE);
		}catch(Exception e){
			lat_min = "-90";
			lat_max = "90";
		}
		search_params.put("lat_min", lat_min);
		search_params.put("lat_max", lat_max);
		
		String long_min, long_max;
		double lng;
		try{
			lng = Double.parseDouble(search.getString("long"));
			long_min = ""+(lng-POLUMJER_PRETRAGE);
			long_max = ""+(lng+POLUMJER_PRETRAGE);
		}catch(Exception e){
			long_min = "-180";
			long_max = "180";
		}
		search_params.put("long_min", long_min);
		search_params.put("long_max", long_max);
		
		return search_params;
	}
	
	String convertBoolean(String a){
		if(a.equals("true"))
			return "1";
		return "";
	}
}
