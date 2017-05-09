package com.xswxm.controller.network;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Air on 4/10/2017.
 * Post task, return an input stream
 */

public class PostTask extends AsyncTask<Object, Object, String> {
    public String deviceAddress;
    public String devicePostText;
    @Override
    protected String doInBackground(Object... params) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            URL url = new URL(deviceAddress);
            HttpURLConnection httpURLConnection = null;
            try {
                httpURLConnection = (HttpURLConnection)url.openConnection();
            } catch (Exception e) {
                return "";
            }
            httpURLConnection.setRequestMethod("HEAD");
            httpURLConnection.setConnectTimeout(3000); //set timeout to 3 seconds


            String urlParameters = devicePostText;
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("USER-AGENT", "Mozilla/5.0");
            httpURLConnection.setRequestProperty("ACCEPT-LANGUAGE", "en-US, en; 0.5");

            httpURLConnection.setDoOutput(true);
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());

            dataOutputStream.writeBytes(urlParameters);
            dataOutputStream.flush();
            dataOutputStream.close();

            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            return bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}