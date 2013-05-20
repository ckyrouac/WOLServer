package com.varma.android.aws.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URLDecoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.varma.android.aws.R;
import com.varma.android.aws.app.AppLog;
import com.varma.android.aws.constants.Constants;
import com.varma.android.aws.ui.AWSMessageActivity;
import com.varma.android.aws.utility.Utility;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class WakeOnLanCommandHandler implements HttpRequestHandler{

    private Context context = null;

    public WakeOnLanCommandHandler(Context context, String ipAddress, String macAddress){
        this.context = context;
    }

    @Override
    public void handle(
            HttpRequest request,
            HttpResponse response,
            HttpContext httpContext) throws HttpException, IOException {

        String uriString = request.getRequestLine().getUri();
        Uri uri = Uri.parse(uriString);
        String ipAddress = URLDecoder.decode(uri.getQueryParameter("ipAddress"));
        String macAddress = URLDecoder.decode(uri.getQueryParameter("macAddress"));

        try {
            byte[] macBytes = getMacBytes(macAddress);
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            //TODO: validate IP address
            InetAddress address = InetAddress.getByName(ipAddress);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 9);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            sendResponse(R.raw.wolsend, response);

        } catch (Exception e) {
            //TODO: better error
            sendResponse(R.raw.notfound, response);
        }
    }

    private void sendResponse(final int fileNameId, HttpResponse response) {
        HttpEntity entity = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outstream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
                String resp = Utility.openHTMLString(context, fileNameId);

                writer.write(resp);
                writer.flush();
            }
        });

        response.setHeader("Content-Type", "text/html");
        response.setEntity(entity);
    }

    /**
     * This code is based off http://www.jibble.org/wake-on-lan/WakeOnLan.java
     *
     * @param macStr
     * @return
     * @throws IllegalArgumentException
     */
    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }
}