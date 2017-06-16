package ru.ytimes.client.kkm.android;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(KKMServer.class);

    private Printer printer;

    private String code;

    private ObjectMapper mapper = new ObjectMapper();

    public KKMServer(int port, String code) {
        super(new InetSocketAddress(port));
        this.code = code;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected");
    }

    public void onClose(WebSocket conn, int i, String s, boolean b) {
        logger.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " disconnected");
    }

    public void onMessage(WebSocket conn, String message) {
        logger.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + ": " + message );

        ActionRecord action = parseMessage(conn, message, ActionRecord.class);
        if (action == null) {
            return;
        }
        try {
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

            Result result = new Result();
            result.success = true;
            try {
                conn.send(mapper.writeValueAsString(result));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        catch (PrinterException e) {
            processException(conn, e);
        }
    }

    private void checkCode(String code) throws PrinterException {
        if (code == null || code.trim().isEmpty() || !code.equals(this.code)) {
            throw new PrinterException("Неизвестная команда. Проверьте настройки системы");
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
        logger.error(e.getMessage(), e);
        if( conn != null ) {
            processException(conn, e);
        }
    }

    public void onStart() {
        logger.info("KKM server started");
    }

}
