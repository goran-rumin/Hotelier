package hr.hotelier;

import static spark.Spark.before;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

public class Security {

	private HashMap<String,Integer> sessions;
	private HashMap<String,Date> lease;
	
	static String ERROR = "No parameters";
	static String ERROR1 = "Unknown user ID";
	static String ERROR2 = "Unknown user credentials";
	static String ERROR3 = "Not enough data to make registration";
	static String ERROR4 = "User with this username exists";
	static String ERROR5 = "Data edit for current data not possible";
	static String ERROR6 = "Unknown accommodation ID";
	static String ERROR7 = "Unknown comment data";
	
	public Security(){
		sessions = new HashMap<String,Integer>();
		lease = new HashMap<String,Date>();
		timer.scheduleAtFixedRate(zadatak, (long) 30*60*1000, (long) 30*60*1000);
	}
	
	/**
	 * Funkcija za stvaranje sesije
	 * @param user_id - integer id za korisnike
	 * @return SHA1 hash korisnickog id-a i trenutacnog vremena
	 */
	public String createSession(int user_id){
		Date t = new Date();
		String session_id="";
		try {
			session_id = sha1(""+user_id+t.getTime());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		sessions.put(session_id,user_id);
		lease.put(session_id, new Date());
		return session_id;
	}
	
	/**
	 * 
	 * @param SHA1key - SHA1 kljuc korisnika
	 * @return true ako postoji sesija koja se moze odlogirati, false ako ne
	 */
	public boolean deleteSession(String SHA1key){
		if(sessions.containsKey(SHA1key)){
    		sessions.remove(SHA1key);
    		lease.remove(SHA1key);
    		return true;
		}
		return false;
	}
	
	/**
	 * Funkcija koja produzuje trajanje kljuca
	 * @param SHA1key - kljuc korisnika
	 * @return da li je sesija uspijesno azurirana
	 */
	public boolean updateSession(String SHA1key){
		if(sessions.containsKey(SHA1key)){
    		lease.put(SHA1key,new Date());
    		return true;
		}
		return false;
	}
	
	public int getSession(String SHA1key){
		if(sessions.containsKey(SHA1key)){
    		return sessions.get(SHA1key);
		}
		return -1;
	}
	
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
	
	static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
	
	static void enableCORS(final String origin, final String methods, final String headers) {
	    before((request, response) -> {
	    	response.header("Access-Control-Allow-Origin", origin);
	    	response.header("Access-Control-Request-Method", methods);
	    	response.header("Access-Control-Allow-Headers", headers);
	    });
	}
	
	/**
	 * 
	 * @param id identifikator greske
	 * @param description opis greske
	 * @return JSON oblik greske
	 */
	static String prepareErrorJson(int id, String description){
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
	
	/**
	 * 
	 * @param params lista parametara iz POST zahtjeva
	 * @return da li svi parametri zadovoljavaju minimalne uvjete za daljnju obradu
	 */
	static boolean areParamsOK(String... params){
		for(int i=0;i<params.length;i++){
			if(params[i]==null || params[i].equals(""))
				return false;
		}
		return true;
	}
}
