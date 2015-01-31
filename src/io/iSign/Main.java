package io.iSign;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class Main {

	   public static void main(String[] args) throws Exception {
	        String apiToken = ""; // Enter access token here
	        String host = "https://api-sandbox.isign.io/";

	        System.out.println("iSign.io API Java example ");
	        
	        HttpResponse prepareResponse = prepare(host, apiToken);
	        
	        JsonReader jsonReader = Json.createReader(new StringReader(EntityUtils.toString(prepareResponse.getEntity(),  "UTF-8")));
	        JsonObject prepareJson = jsonReader.readObject();
	        
	        
	        System.out.println("Phone will receive control code: "+prepareJson.getString("controlCode"));
	        System.out.println("Prepare responded with token: "+prepareJson.getString("token"));
	        
	        if((prepareJson.getString("status")).equals("ok")) {
	        	 status(host, apiToken, prepareJson.getString("token"));
	        } else {
	        	System.out.println("Responded with error:" + prepareJson.getString("message") );
	        }
	    }

	   public static void status(String host, String apiToken, String token) throws Exception {
		  
		   System.out.println("Requesting signed file status:");
	       HttpClient client = HttpClientBuilder.create().build();
		   HttpGet statusMethod = new HttpGet(host + "/mobile/sign/status/"+token+".json?access_token="+apiToken);
    
	       	for (int i=0; i<60; i = i+5) {
		        HttpResponse statusResponse = client.execute(statusMethod);
				HttpEntity entity = statusResponse.getEntity();
				String statusString = EntityUtils.toString(entity,  "UTF-8");
		        JsonReader jsonReader = Json.createReader(new StringReader(statusString));
		        JsonObject statusJson = jsonReader.readObject();
	
		        System.out.println(statusJson.getString("status"));
		        if ((statusJson.getString("status")).equals("ok")) {
		        	JsonObject file = statusJson.getJsonObject("file");
		        	try {
		        		FileUtils.writeByteArrayToFile(new File("test_signed.pdf"), DatatypeConverter.parseBase64Binary(file.getString("content")));
		        		
		        	} catch (IOException ex) {
		        		System.out.println(ex.toString());
		        	}
		        	System.out.println("Signed. Please open ./test_signed.pdf !\n");
			        break;
		        } else if ((statusJson.getString("status")).equals("error")) {
		        	System.out.println("Singing failed with message: "+statusJson.getString("message"));
		        }
		        try {
	        	  Thread.sleep(5000);
	        	} catch (InterruptedException ie) {
	        	    //Handle exception
		        	}
	       	}
	   }
	   
	   public static HttpResponse prepare(String host, String apiToken) throws Exception {
		   byte[] fileData = loadFile("test.pdf");

	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("type","pdf"));
	        nameValuePairs.add(new BasicNameValuePair("phone","+37060000007"));
	        nameValuePairs.add(new BasicNameValuePair("code","51001091072"));
	        nameValuePairs.add(new BasicNameValuePair("language","EN"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[contact]","Seventh Testnumber"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[reason]","Agreement"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[location]","Vilnius"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][name]","test.pdf"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][content]",new String(DatatypeConverter.printBase64Binary(fileData))));
	        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][digest]",toSHA1(fileData)));

	        HttpClient client = HttpClientBuilder.create().build();
	        HttpPost method = new HttpPost(host + "mobile/sign.json?access_token=" + apiToken);
	        method.setEntity(new UrlEncodedFormEntity(nameValuePairs));   
	     
	        method.addHeader("content-type", "application/x-www-form-urlencoded");
	        HttpResponse response = client.execute(method);
	        
	        return response;
	   }

	    private static String byteArrayToHexString(byte[] b) {
	        String result = "";
	        for (int i = 0; i < b.length; i++) {
	            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	        }
	        return result;
	    }

	    private static String toSHA1(byte[] convertme) throws NoSuchAlgorithmException {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        return byteArrayToHexString(md.digest(convertme));
	    }

	    private static byte[] loadFile(String name) throws IOException {
	        InputStream in = new FileInputStream(name);
	        return IOUtils.toByteArray(in);
	    }
}
