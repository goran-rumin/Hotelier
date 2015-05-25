package hr.hotelier;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.before;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import spark.Filter;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;


public class Server {
	
	private static Connection connect = null;
	private static Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;
	
	public static String PATH = "D:/FER/Knjige/6. semestar/Završni rad/Spark public folder";
	
	
	public static void main(String[] args) throws SQLException {
		
		Spark.externalStaticFileLocation(PATH);
		
		Security.enableCORS("*", "*", "*");
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection("jdbc:mysql://localhost/hotels?" + "user=hotelier&password=d02f3cA9");
		    statement = connect.createStatement();
		} catch (SQLException e1) {
			System.out.println("SQL error");
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.out.println("No JDBC driver");
			e1.printStackTrace();
		}
		
		statement.execute("SET NAMES utf8;");
		statement.execute("SET CHARSET utf8;");
		//ResultSet rezultati = statement.executeQuery("SHOW VARIABLES LIKE 'character//_set//_%';");
		
		
		Security sec = new Security();
		User user = new User(connect, sec);
		Accomodation accm = new Accomodation(connect, sec);
		Country country = new Country(connect);
		AType atype = new AType(connect, sec);
		Comment comment = new Comment(connect, sec);
		Reservation reservation = new Reservation(connect, sec);
		Object object = new Object(connect, sec);
		
        get("/", (request, response) -> {
            return "Main site";
        });
        
        //USER dio
        
        get("/user/login", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/login", (request, response) -> {
        	String odgovor;
        	String username = request.queryParams("username");
        	String password = request.queryParams("password");   //stize kao sha1(password)
        	
        	odgovor = user.login(username, password);
        	
            return odgovor;
        });
       	
       	get("/user/logout", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/logout", (request, response) -> {
       		String odgovor;
       		String id = request.queryParams("session_id");
       		odgovor = user.logout(id);
            return odgovor;
        });
       	
       	get("/user/register", (request, response) -> {
       		response.redirect("/user/register/guest");
        	return response;
        });
       	
       	get("/user/register/guest", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/register/guest", (request, response) -> {
       		String odgovor;
       		String username = request.queryParams("username");
       		String password = request.queryParams("password");
       		String name = request.queryParams("name");
       		String surname = request.queryParams("surname");
       		String address = request.queryParams("address");
       		String city = request.queryParams("city");
       		String country_id = request.queryParams("country_id");
       		String phone = request.queryParams("phone");
       		String email = request.queryParams("email");
       		String date_birth = request.queryParams("date_birth");
       		odgovor = user.registerGuest(username, password, name, surname, address, city, country_id, phone, email, date_birth);
            return odgovor;
        });
       	
       	get("/user/register/owner", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/register/owner", (request, response) -> {
       		String odgovor;
       		String username = request.queryParams("username");
       		String password = request.queryParams("password");
       		String name = request.queryParams("name");
       		String surname = request.queryParams("surname");
       		String oib = request.queryParams("oib");
       		String address = request.queryParams("address");
       		String city = request.queryParams("city");
       		String country_id = request.queryParams("country_id");
       		String phone = request.queryParams("phone");
       		String email = request.queryParams("email");
       		String date_birth = request.queryParams("date_birth");
       		odgovor = user.registerOwner(username, password, name, surname, oib, address, city, country_id, phone, email, date_birth);
            return odgovor;
        });
       	
       	get("/user/edit", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/edit", (request, response) -> {
        	String odgovor="";
        	String SHA1key = request.queryParams("session_id");
        	String username = request.queryParams("username");
       		String password = request.queryParams("password");
       		String name = request.queryParams("name");
       		String surname = request.queryParams("surname");
       		String oib = request.queryParams("oib");
       		String address = request.queryParams("address");
       		String city = request.queryParams("city");
       		String country_id = request.queryParams("country_id");
       		String phone = request.queryParams("phone");
       		String email = request.queryParams("email");
       		String date_birth = request.queryParams("date_birth");
       		//System.out.println(""+username+" "+password+" "+name+" "+surname+" "+oib+" "+address+" "+city+" "+country_id+" "+phone+" "+email+" "+date_birth);
        	odgovor = user.edit(SHA1key, username, password, name, surname, oib, address, city, country_id, phone, email, date_birth);
            return odgovor;
        });
       	
        get("/user/data", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/data", (request, response) -> {
        	String odgovor="";
        	String SHA1key = request.queryParams("session_id");
        	odgovor = user.data(SHA1key);
            return odgovor;
        });
       	
       	get("/user/stats", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/user/stats", (request, response) -> {
        	String odgovor="";
        	String SHA1key = request.queryParams("session_id");
        	odgovor = user.stats(SHA1key);
            return odgovor;
        });
       	
       	//COUNTRY dio
       	get("/country/all", (request, response) -> {
        	return country.all();
        });
       	
       	post("/country/all", (request, response) -> {
        	return country.all();
        });
       	
       	//ACCOMODATION dio
       	get("/accommodation/all/guest", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/accommodation/all/guest", (request, response) -> {
        	String odgovor="";
        	String index = request.queryParams("index");
        	String search_params = request.queryParams("search_params");
        	odgovor = accm.guestAll(index, search_params);
            return odgovor;
        });
       	
       	get("/accommodation/one/guest", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/accommodation/one/guest", (request, response) -> {
        	String odgovor="";
        	String acc_id = request.queryParams("acc_id");
        	odgovor = accm.guestOne(acc_id);
            return odgovor;
        });
       	
       	//ACC_TYPE dio
       	get("/atype/all", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/atype/all", (request, response) -> {
            return atype.all();
        });
       	
      //COMMENT dio
       	get("/comment/all", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/comment/all", (request, response) -> {
       		String acc_id = request.queryParams("acc_id");
            return comment.all(acc_id);
        });
       	
       	get("/comment/add", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/comment/add", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String acc_id = request.queryParams("acc_id");
       		String text = request.queryParams("text");
       		String rating = request.queryParams("rating");
            return comment.add(session_id, acc_id, rating, text);
        });
       	
       	get("/comment/my", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/comment/my", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String acc_id = request.queryParams("acc_id");
            return comment.my(session_id, acc_id);
        });
       	
      //RESERVATION dio
       	get("/reservation/accommodation", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/reservation/accommodation", (request, response) -> {
       		String acc_id = request.queryParams("acc_id");
            return reservation.accommodation(acc_id);
        });
       	
       	get("/reservation/add/guest", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/reservation/add/guest", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String acc_id = request.queryParams("acc_id");
       		String date_from = request.queryParams("date_from");
       		String date_until = request.queryParams("date_until");
       		String ppl_adults = request.queryParams("ppl_adults");
       		String ppl_children = request.queryParams("ppl_children");
            return reservation.guestAdd(session_id, date_from, date_until, ppl_adults, ppl_children, acc_id);
        });
       	
       	get("/reservation/all/guest", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/reservation/all/guest", (request, response) -> {
       		String session_id = request.queryParams("session_id");
            return reservation.guestAll(session_id);
        });
       	
       	get("/reservation/delete/guest", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/reservation/delete/guest", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String res_id = request.queryParams("res_id");
            return reservation.guestDelete(session_id, res_id);
        });
       	
       	//OBJECT dio
       	get("/object/all", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/all", (request, response) -> {
       		String session_id = request.queryParams("session_id");
            return object.all(session_id);
        });
       	
       	get("/object/one", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/one", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String object_id = request.queryParams("object_id");
            return object.one(session_id, object_id);
        });
       	
       	get("/object/edit", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/edit", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String object_id = request.queryParams("object_id");
       		String name = request.queryParams("name");
       		String desc = request.queryParams("desc");
       		String addr = request.queryParams("addr");
       		String city = request.queryParams("city");
       		String country_id = request.queryParams("country_id");
       		String lat = request.queryParams("lat");
       		String lng = request.queryParams("long");
            return object.edit(session_id, object_id, name, desc, addr, city, country_id, lat, lng);
        });
       	
       	get("/object/add", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/add", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String name = request.queryParams("name");
       		String desc = request.queryParams("desc");
       		String addr = request.queryParams("addr");
       		String city = request.queryParams("city");
       		String country_id = request.queryParams("country_id");
       		String lat = request.queryParams("lat");
       		String lng = request.queryParams("long");
            return object.add(session_id, name, desc, addr, city, country_id, lat, lng);
        });
       	
       	get("/object/edit/owners", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/edit/owners", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String object_id = request.queryParams("object_id");
       		String owners = request.queryParams("owners");
            return object.editOwners(session_id, object_id, owners);
        });
       	
       	get("/object/add/owner", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/add/owner", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String object_id = request.queryParams("object_id");
       		String oib = request.queryParams("oib");
       		String percentage = request.queryParams("percentage");
            return object.addOwner(session_id, object_id, oib, percentage);
        });
       	
       	get("/object/delete/owner", (request, response) -> {
        	return Security.ERROR;
        });
        
       	post("/object/delete/owner", (request, response) -> {
       		String session_id = request.queryParams("session_id");
       		String object_id = request.queryParams("object_id");
       		String user_id = request.queryParams("user_id");
       		String percentage = request.queryParams("percentage");
            return object.deleteOwner(session_id, user_id, object_id, percentage);
        });
    }
	
	
	
	
	
	
}