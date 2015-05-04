package hr.hotelier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Comment {
	
	private Connection connect = null;
	private Security sec;
	
	private PreparedStatement stat_comment_all;
	private PreparedStatement stat_comment_add;
	private PreparedStatement stat_comment_my;
	
	public Comment(Connection connection, Security security) throws SQLException{
		connect = connection;
		sec = security;
		stat_comment_all = connect.prepareStatement("SELECT text, rating, time, name, surname FROM comments JOIN user ON comments.user_id=user.id WHERE acc_id=? ORDER BY time DESC LIMIT 500;");
		stat_comment_add = connect.prepareStatement("INSERT INTO comments VALUES (?,?,?,?,?);");
		stat_comment_my = connect.prepareStatement("SELECT text, rating, time FROM comments WHERE user_id=? AND acc_id=?;");
	}
	
	public String all(String acc_id) throws SQLException, JSONException{
		JSONObject main = new JSONObject();
		JSONArray data = new JSONArray();
		stat_comment_all.setString(1, acc_id);
		ResultSet rezultati = stat_comment_all.executeQuery();
		JSONObject comment;
		while(rezultati.next()){
    		comment = new JSONObject();
    		comment.put("name",rezultati.getString("name")+" "+rezultati.getString("surname"));
    		comment.put("text",rezultati.getString("text"));
    		comment.put("rating",rezultati.getInt("rating"));
   			comment.put("time",rezultati.getString("time"));
   			data.put(comment);
   		}
		main.put("data", data);
		return main.toString();
	}
	
	public String add(String session_id, String acc_id, String rating, String text) throws SQLException, JSONException{
		String odgovor="";
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
    	
		int id = sec.getSession(session_id);
    	if(id==-1){
    		odgovor=Security.prepareErrorJson(1,Security.ERROR1);
    		return odgovor;
    	}
    	sec.updateSession(session_id);
    	if(Integer.parseInt(rating)>5 || Integer.parseInt(rating)<1 || text.length()>1000){
    		odgovor=Security.prepareErrorJson(7,Security.ERROR7);
    		return odgovor;
    	}
    	Date now = new Date();
		String datum = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
		stat_comment_add.setInt(1, id);
		stat_comment_add.setString(2, acc_id);
		stat_comment_add.setString(3, text);
		stat_comment_add.setString(4, rating);
		stat_comment_add.setString(5, datum);
		stat_comment_add.executeUpdate();
		data.put("success", 1);
		main.put("data", data);
		return main.toString();
	}
	
	public String my(String session_id, String acc_id) throws SQLException, JSONException{
		String odgovor="";
		JSONObject main = new JSONObject();
		JSONObject data = new JSONObject();
    	
		int id = sec.getSession(session_id);
    	if(id==-1){
    		odgovor=Security.prepareErrorJson(1,Security.ERROR1);
    		return odgovor;
    	}
    	sec.updateSession(session_id);
		stat_comment_my.setInt(1, id);
		stat_comment_my.setString(2, acc_id);
		ResultSet rezultati = stat_comment_my.executeQuery();
		if(rezultati.first()){
			data.put("text",rezultati.getString("text"));
    		data.put("rating",rezultati.getInt("rating"));
   			data.put("time",rezultati.getString("time"));
		}
		main.put("data", data);
		return main.toString();
	}
}
