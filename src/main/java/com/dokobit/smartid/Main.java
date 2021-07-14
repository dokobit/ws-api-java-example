package com.dokobit.smartid;

import com.dokobit.util.Util;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static Log log = LogFactory.getLog(Main.class);

    public static final String API_TOKEN = "";
    public static final String HOST = "https://developers.dokobit.com/";

    public static void main(String[] args) throws Exception {
        log.info("Developers.dokobit.com WS API Smart-ID Java example ");

        if (API_TOKEN.equals("")) {
            log.info("Please set API_TOKEN at Main:33" );
            System.exit(1);
        }

        HttpResponse prepareResponse = prepare(HOST, API_TOKEN);

        JsonReader jsonReader = Json.createReader(new StringReader(EntityUtils.toString(prepareResponse.getEntity(), CharEncoding.UTF_8)));
        JsonObject prepareJson = jsonReader.readObject();


        log.info("Phone will receive control code: " + prepareJson.getString("control_code"));
        log.info("Prepare responded with token: " + prepareJson.getString("token"));

        if ((prepareJson.getString("status")).equals("ok")) {
            status(HOST, API_TOKEN, prepareJson.getString("token"));
        } else {
            log.info("Responded with error:" + prepareJson.getString("message"));
        }
    }

    public static void status(String host, String apiToken, String token) throws Exception {
        log.info("Requesting signed file status:");
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet statusMethod = new HttpGet(host + "smartid/sign/status/" + token + ".json?access_token=" + apiToken);

        for (int i = 0; i < 60; i = i + 5) {
            HttpResponse statusResponse = client.execute(statusMethod);
            HttpEntity entity = statusResponse.getEntity();
            String statusString = EntityUtils.toString(entity, CharEncoding.UTF_8);
            JsonReader jsonReader = Json.createReader(new StringReader(statusString));
            JsonObject statusJson = jsonReader.readObject();

            log.info(statusJson.getString("status"));
            if ((statusJson.getString("status")).equals("ok")) {
                JsonObject file = statusJson.getJsonObject("file");
                try {
                    FileUtils.writeByteArrayToFile(new File("test_signed.pdf"), DatatypeConverter.parseBase64Binary(file.getString("content")));

                } catch (IOException ex) {
                    log.info(ex.toString());
                }
                log.info("Signed. Please open ./test_signed.pdf !\n");
                break;
            } else if ((statusJson.getString("status")).equals("error")) {
                log.info("Singing failed with message: " + statusJson.getString("message"));
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                //Handle exception
            }
        }
    }

    public static HttpResponse prepare(String host, String apiToken) throws Exception {
        byte[] fileData = Util.loadFile("test.pdf");

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("type", "pdf"));
        nameValuePairs.add(new BasicNameValuePair("code", "30303039914"));
        nameValuePairs.add(new BasicNameValuePair("country", "EE"));
        nameValuePairs.add(new BasicNameValuePair("pdf[contact]", "Seventh Testnumber"));
        nameValuePairs.add(new BasicNameValuePair("pdf[reason]", "Agreement"));
        nameValuePairs.add(new BasicNameValuePair("pdf[location]", "Tallinn"));
        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][name]", "test.pdf"));
        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][content]", DatatypeConverter.printBase64Binary(fileData)));
        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][digest]", Util.toSHA256(fileData)));

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost method = new HttpPost(host + "smartid/sign.json?access_token=" + apiToken);
        method.setEntity(new UrlEncodedFormEntity(nameValuePairs, CharEncoding.UTF_8));

        method.addHeader("content-type", "application/x-www-form-urlencoded");
        HttpResponse response = client.execute(method);

        return response;
    }


}
