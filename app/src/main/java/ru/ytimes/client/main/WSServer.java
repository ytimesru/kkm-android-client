package ru.ytimes.client.main;

import android.content.Context;
import android.content.Intent;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

import ru.ytimes.client.records.ScreenInfoRecord;

/**
 * Created by andrey on 14.06.18.
 */

public class WSServer extends WebSocketServer {
    private Context context;
    private ScreenInfoRecord info;
    private ObjectMapper mapper = new ObjectMapper();


    public WSServer(Context context, int port) {
        super(new InetSocketAddress(port));
        this.context = context;
    }

    public void showMessage(String message) {
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        context.sendBroadcast(local);
    }

    public void setInfo(ScreenInfoRecord record) {
        info = record;
        sendToAll();
    }

    private void sendToAll() {
        try {
            broadcast(info != null ? mapper.writeValueAsString(info) : "clear");
        }
        catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        showMessage(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " подключился");
        sendToAll();
    }

    @Override
    public void onClose(WebSocket conn, int i, String s, boolean b) {
        showMessage(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " отключился");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        showMessage(conn.getRemoteSocketAddress().getAddress().getHostAddress() + ": " + message );
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        showMessage(e.getMessage());
    }

    @Override
    public void onStart() {
        showMessage("WS запущен");
    }



}
