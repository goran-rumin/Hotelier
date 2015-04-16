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
	
	
	public static void main(String[] args) throws SQLException {
		
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
		
		Security sec = new Security();
		User user = new User(connect, sec);
		Country country = new Country(connect);
		
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
       	
       	//COUNTRY dio
       	get("/country/all", (request, response) -> {
        	return country.all();
        });
       	
       	post("/country/all", (request, response) -> {
        	return country.all();
        });
       	
    }
	
	
	
	
	
	
}