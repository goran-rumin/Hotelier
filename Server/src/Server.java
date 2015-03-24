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
import spark.template.freemarker.FreeMarkerEngine;


import freemarker.template.Configuration;

public class Server {
	
	private static Connection connect = null;
	private static Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;
	
	private static HashMap<String,Integer> sessions;
	private static HashMap<String,Date> lease;
	private static String ERROR = "No parameters";
	private static String ERROR1 = "Unknown user ID";
	private static String ERROR2 = "Unknown user credentials";
	public static void main(String[] args) {
		
		
		enableCORS("*", "*", "*");
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
		sessions = new HashMap<String,Integer>();
		lease = new HashMap<String,Date>();
		
        get("/", (request, response) -> {
            return "Main site";
        });
        
        //USER dio
        
        get("/user/login", (request, response) -> {
        	return ERROR;
        });
        
       	post("/user/login", (request, response) -> {
        	String odgovor="";
        	JSONObject main = new JSONObject();
			JSONObject data = new JSONObject();
        	String username = request.queryParams("username");
        	String password = request.queryParams("password");   //stize kao sha1(password)
        	if(username==null || password==null){
        		odgovor = prepare_error_json(2, ERROR2);
        		return odgovor;
        	}
        	PreparedStatement statement = connect.prepareStatement("SELECT id, flag FROM user WHERE username=? AND password=?;");
        	statement.setString(1, username);
        	statement.setString(2, password);
        	ResultSet rezultati = statement.executeQuery();
        	if(rezultati.first()){
        		int id = rezultati.getInt("id");
        		int flag = rezultati.getInt("flag");
        		Date t = new Date();
        		String session_id = sha1(""+id+t.getTime());
        		sessions.put(session_id,id);
        		lease.put(session_id, new Date());
        		data.put("id", session_id);
        		data.put("flag", flag);
        		main.put("data", data);
        		odgovor=main.toString();
        	}else{
        		odgovor=prepare_error_json(2,ERROR2);
        	}
        	rezultati.close();
            return odgovor;
        });
       	
       	get("/user/logout", (request, response) -> {
        	return ERROR;
        });
        
       	post("/user/logout", (request, response) -> {
        	String odgovor="";
        	JSONObject main = new JSONObject();
			JSONObject data = new JSONObject();
        	String id = request.queryParams("id");
        	if(sessions.containsKey(id)){
        		sessions.remove(id);
        		data.put("success", 1);
        		main.put("data", data);
        		odgovor = main.toString();
        	}
        	else{
        		odgovor=prepare_error_json(1,ERROR1);
        	}
            return odgovor;
        });
       	
        get("/user/data", (request, response) -> {
        	return ERROR;
        });
       	post("/user/data", (request, response) -> {
        	String odgovor="";
        	JSONObject main = new JSONObject();
			JSONObject data = new JSONObject();
        	String user_id = request.queryParams("user_id");
        	int id = -1;
        	try{
        		id = Integer.parseInt(user_id);
        	}catch(Exception e){
        		odgovor=prepare_error_json(1,ERROR1);
        	}
        	if(id!=-1){
        		PreparedStatement statement = connect.prepareStatement("SELECT * FROM user WHERE id=?;");
        		statement.setInt(1, id);
        		ResultSet rezultati = statement.executeQuery();
        		if(rezultati.first()){
        			int flag = rezultati.getInt("flag");
        			String username = rezultati.getString("username");
        			String name = rezultati.getString("name");
        			String surname = rezultati.getString("surname");
        			String address = rezultati.getString("address");
        			String city = rezultati.getString("city");
        			String country = rezultati.getString("country");
        			String phone = rezultati.getString("phone");
        			String email = rezultati.getString("email");
        			if(flag==1){
        				String oib = rezultati.getString("oib");
        				String object_desc = rezultati.getString("object_desc");
        				data.put("oib", oib);
        				if(object_desc==null)
        					object_desc="";
        				data.put("object_desc", object_desc);
        			}
        			
        			
        			data.put("username", username);
        			data.put("name", name);
        			data.put("surname", surname);
        			data.put("address", address);
        			data.put("city", city);
        			data.put("country", country);
        			data.put("phone", phone);
        			data.put("email", email);
        			main.put("data", data);
        			odgovor=main.toString();
        		}
        		else{
        			odgovor=prepare_error_json(1,ERROR1);
        		}
        		rezultati.close();
        	}
            return odgovor;
        });
       	
       	Timer timer = new Timer();
       	TimerTask zadatak = new TimerTask() {
       		@Override
       		public void run() {
       			for(Entry<String, Date> entry : lease.entrySet()) {
       			    String key = entry.getKey();
       			    Date value = entry.getValue();
       			    Date sada = new Date();
       			    if(value.getTime()<(sada.getTime()-30*60*1000)){
       			    	lease.remove(key);
       			    	sessions.remove(key);
       			    }
       			}
       		}
       	};
       	timer.scheduleAtFixedRate(zadatak, (long) 30*60*1000, (long) 30*60*1000);
    }
	
	static String prepare_error_json(int id, String description){
		JSONObject main = new JSONObject();
		JSONObject error = new JSONObject();
		try {
			error.put("id", id);
			error.put("description", description);
			main.put("error", error);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return main.toString();
	}
	
	private static void enableCORS(final String origin, final String methods, final String headers) {
	    before((request, response) -> {
	    	response.header("Access-Control-Allow-Origin", origin);
	    	response.header("Access-Control-Request-Method", methods);
	    	response.header("Access-Control-Allow-Headers", headers);
	    });
	}
	
	static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}