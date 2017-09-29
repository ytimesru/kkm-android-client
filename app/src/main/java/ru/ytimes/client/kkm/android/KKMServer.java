package ru.ytimes.client.kkm.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

import ru.ytimes.client.kkm.android.printer.Printer;
import ru.ytimes.client.kkm.android.printer.PrinterException;
import ru.ytimes.client.kkm.android.record.ActionRecord;
import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;
import ru.ytimes.client.kkm.android.record.Result;

/**
 * Created by andrey on 27.05.17.
 */
public class KKMServer extends WebSocketServer {
    private static final String TAG = "YTIMES";

    private Printer printer;
    private Context context;
    private String code;

    private ObjectMapper mapper = new ObjectMapper();

    public KKMServer(int port, String code, Context context) {
        super(new InetSocketAddress(port));
        this.code = code;
        this.context = context;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public void showMessage(String message) {
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        context.sendBroadcast(local);
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String addr = "unknown";
        if (conn.getRemoteSocketAddress() != null && conn.getRemoteSocketAddress().getAddress() != null) {
            addr = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        }
        Log.i(TAG, addr + " connected");
        showMessage("Подключился: " + addr);
    }

    public void onClose(WebSocket conn, int i, String s, boolean b) {
        String addr = "unknown";
        if (conn.getRemoteSocketAddress() != null && conn.getRemoteSocketAddress().getAddress() != null) {
            addr = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        }
        Log.i(TAG, addr + " disconnected");
        showMessage("Отключился: " + addr);
    }

    public void onMessage(WebSocket conn, String message) {
        String addr = "unknown";
        if (conn.getRemoteSocketAddress() != null && conn.getRemoteSocketAddress().getAddress() != null) {
            addr = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        }

        Log.i(TAG, addr + ": " + message );
        showMessage("Получено сообщение от: " + addr);

        ActionRecord action = parseMessage(conn, message, ActionRecord.class);
        if (action == null) {
            return;
        }
        try {
            showMessage("Обработка действия: " + addr + ", " + action.action);
            if ("newGuest".equals(action.action)) {
                NewGuestCommandRecord record = parseMessage(conn, action.data, NewGuestCommandRecord.class);
                checkCode(record.code);
                printer.printNewGuest(record);
            }
            else if ("printCheck".equals(action.action)) {
                PrintCheckCommandRecord record = parseMessage(conn, action.data, PrintCheckCommandRecord.class);
                checkCode(record.code);
                printer.printCheck(record);
            }
            else if ("printReturnCheck".equals(action.action)) {
                PrintCheckCommandRecord record = parseMessage(conn, action.data, PrintCheckCommandRecord.class);
                checkCode(record.code);
                printer.printReturnCheck(record);
            }
            else if ("printPredCheck".equals(action.action)) {
                PrintCheckCommandRecord record = parseMessage(conn, action.data, PrintCheckCommandRecord.class);
                checkCode(record.code);
                printer.printPredCheck(record);
            }
            else if ("reportX".equals(action.action)) {
                ReportCommandRecord record = parseMessage(conn, action.data, ReportCommandRecord.class);
                checkCode(record.code);
                printer.reportX();
            }
            else if ("reportZ".equals(action.action)) {
                ReportCommandRecord record = parseMessage(conn, action.data, ReportCommandRecord.class);
                checkCode(record.code);
                printer.reportZ();
            }
            else {
                sendError(conn, "kkm server", "Неизвестная команда: " + action.action);
            }
            showMessage("Обработано действие: " + addr + ", " + action.action);
            Result result = new Result();
            result.success = true;

            try {
                conn.send(mapper.writeValueAsString(result));
            }
            catch (Exception ex) {
                ex.printStackTrace();
                showMessage("Нет связи до: " + addr + ", " + ex.getMessage());
            }
        }
        catch (PrinterException e) {
            processException(conn, e);
        }
    }

    private boolean isDeviseNotAvailable(PrinterException e) {
        return e.getCode() == -11 ||    //нет связи
                e.getCode() == -1 ||
                e.getCode() == -3;  //порт не доступен
    }

    private void checkCode(String code) throws PrinterException {
        if (code == null || code.trim().isEmpty() || !code.equals(this.code)) {
            throw new PrinterException(0, "Неизвестная команда. Проверьте настройки системы");
        }
    }

    private <T> T parseMessage(WebSocket conn, String message, Class<T> tClass) {
        try {
            return mapper.readValue(message, tClass);
        }
        catch (Exception e) {
            processException(conn, e);
            return null;
        }
    }

    private void processException(WebSocket conn, Exception e) {
        sendError(conn, e.getClass().getSimpleName(), e.getMessage());
    }

    private void sendError(WebSocket conn, String errorClass, String message) {
        showMessage("Ошибка: " + message);
        Result result = new Result();
        result.success = false;
        result.errorClass = errorClass;
        result.errorMessage = message;
        try {
            conn.send(mapper.writeValueAsString(result));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onError(WebSocket conn, Exception e) {
        Log.e(TAG, e.getMessage(), e);
        if( conn != null ) {
            processException(conn, e);
        }
    }

    public void onStart() {
        Log.i(TAG, "KKM server started");
    }

}
