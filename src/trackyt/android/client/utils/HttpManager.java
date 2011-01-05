package trackyt.android.client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import trackyt.android.client.models.AuthResponse;
import trackyt.android.client.models.Credentials;
import trackyt.android.client.models.Task;
import android.util.Log;

public class HttpManager {
	
	private static HttpManager httpManager = new HttpManager(); 
	
	private HttpClient httpClient;
	private HttpResponse httpResponse;
	private HttpEntity httpEntity;
	private List<NameValuePair> params;
	
	private HttpManager() {
		if (MyConfig.DEBUG) Log.d("Dev", "HttpManager created");
	}

	public static HttpManager getInstance() {
		return httpManager;
	}

	/* Login */
	public AuthResponse login(Credentials credentials) {
		params = new ArrayList<NameValuePair>();
		URI uri = urlComposer(MyConfig.POST_AUTH_URL);
		HttpPost httpPost = new HttpPost(uri);
		
		params.add(new BasicNameValuePair("email", credentials.getEmail()));
		params.add(new BasicNameValuePair("password", credentials.getPassword()));
		
		try {
			UrlEncodedFormEntity ent = new UrlEncodedFormEntity(params,HTTP.UTF_8);
			httpPost.setEntity(ent);
			if (MyConfig.DEBUG) Log.d("Dev", "Entity has been set with email and password from credentials object");
			if (MyConfig.DEBUG) Log.d("Dev", "httpPost's entity is " + httpPost.getEntity().toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	    
		JSONObject receivedJSON = postRequest(httpPost);
		Converter converter = new Converter();
		return converter.convertToAuthResponse(receivedJSON);
	}
	
	/* Get tasks */
	public ArrayList<Task> getTasks(AuthResponse auth) {
		Converter converter = new Converter();
		

		URI uri = urlComposer(MyConfig.GET_TASKS_URL, auth.getToken());
		HttpGet httpGet = new HttpGet(uri);
		
		JSONObject receivedJSON = getRequest(httpGet);
		return converter.convertJsonInTasks(receivedJSON);
	}
	
	private JSONObject getRequest(HttpUriRequest requestType) {
		httpClient = new DefaultHttpClient();
		if (MyConfig.DEBUG) Log.d("Dev", "HttpManager's getRequest() invoked");
		try {
			httpResponse = httpClient.execute(requestType); 

			if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
				httpEntity = httpResponse.getEntity();
			
				if (httpEntity != null) {
					InputStream instream = httpEntity.getContent(); 
					String convertedString = convertStreamToString(instream);
					return convertToJSON(convertedString);
				} else return null;
				
			} else return null;
		} catch (ClientProtocolException e) {
			if (MyConfig.DEBUG) Log.d("Dev", "ClientProtocolException in getRequest()");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			if (MyConfig.DEBUG) Log.d("Dev", "IOException in getRequst()");
			e.printStackTrace();
			return null;
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	private JSONObject postRequest(HttpUriRequest requestType) {
		try {
			httpClient = new DefaultHttpClient();
			httpResponse = httpClient.execute(requestType);
			if (MyConfig.DEBUG) Log.d("Dev", "Response from the Server, Status Line: " + httpResponse.getStatusLine().toString());
			
			if (httpResponse != null) {
				httpEntity = httpResponse.getEntity();
				if (MyConfig.DEBUG) Log.d("Dev", "Entity from the response obtained");
			
				if (httpEntity != null) {
					InputStream instream = httpEntity.getContent(); 
					String convertedString = convertStreamToString(instream);
					return convertToJSON(convertedString);
				} else return null;
				
			} else return null;
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (MyConfig.DEBUG) Log.d("Dev", "httpClient.shutdown()");
			httpClient.getConnectionManager().shutdown();
		}
		return null;
	}
	
	/*Reads data from InputStream and put it in String*/
	private String convertStreamToString(InputStream instream) {
		if (MyConfig.DEBUG) Log.d("Dev", "convertStreamToString() invoked");
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(instream));	
		StringBuilder stringBuilder = new StringBuilder();
		String readLine;
        try {
        	while ((readLine = buffReader.readLine()) != null) {
				stringBuilder.append(readLine + "\n");
				if (MyConfig.DEBUG) Log.d("Dev", "Read response " + readLine);
			}
        } catch (IOException e) {
        	if (MyConfig.DEBUG) Log.d("Dev", "converStreamToSting() unsuccessfull");
            e.printStackTrace();
        } finally {
            try {
                instream.close();
            } catch (IOException e) {
            	if (MyConfig.DEBUG) Log.d("Dev", "Stream closure was unsuccessfull");
                e.printStackTrace();
                return null;
            }
        }
        return stringBuilder.toString();
	}
	
	/*Converts String to JSON*/
	private JSONObject convertToJSON(String string) {
		if (MyConfig.DEBUG) Log.d("Dev", "convertToJSON() invoked");
		if (string.equals("")) {
			if (MyConfig.DEBUG) Log.d("Dev", "Sting is null, nothing to be converted");
			return null;
		}
		
		try {
			JSONObject json = new JSONObject(string);
			return json;
		} catch (JSONException e) {
			if (MyConfig.DEBUG) Log.d("Dev", "Converting String to JSON was't successfull"); 
			e.printStackTrace();
			return null;
		}
	}
	
    private URI urlComposer(String apiUri) {
    	URI uri = null;
    	try {
			uri = URIUtils.createURI(null, MyConfig.WEB_SERVER, -1, apiUri, null, null);
			if (MyConfig.DEBUG) Log.d("Dev", "Constructed url " + uri);
			return uri;	
		} catch (URISyntaxException e) {
			if (MyConfig.DEBUG) Log.d("Dev", "urlComposer was unable to construct a URL"); 
			e.printStackTrace();
		}
    	return null;
    }
    
    private URI urlComposer(String apiUri, String token) {
    	URI uri = null;
    	try {
    		String tmp = apiUri.toString();
    		String[] array = tmp.split("<token>");
    		tmp = array[0] + token + array[1];
    		
    		uri = URIUtils.createURI(null, MyConfig.WEB_SERVER, -1, tmp, null, null);
			if (MyConfig.DEBUG) Log.d("Dev", "Constructed url " + uri);
			return uri;	
		} catch (URISyntaxException e) {
			if (MyConfig.DEBUG) Log.d("Dev", "urlComposer was unable to construct a URL"); 
			e.printStackTrace();
		}
    	return null;
    }
    
    
    
    private void setParams(String key, String value) {
    	params.add(new BasicNameValuePair(key, value));
    }
	
}