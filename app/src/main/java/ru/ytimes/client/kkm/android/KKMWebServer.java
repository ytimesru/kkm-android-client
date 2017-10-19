package ru.ytimes.client.kkm.android;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import ru.ytimes.client.kkm.android.printer.Printer;
import ru.ytimes.client.kkm.android.printer.PrinterException;
import ru.ytimes.client.kkm.android.record.ActionRecord;
import ru.ytimes.client.kkm.android.record.CashIncomeRecord;
import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;
import ru.ytimes.client.kkm.android.record.Result;

/**
 * Created by andrey on 26.09.17.
 */

public class KKMWebServer extends NanoHTTPD {
    private static final String TAG = "YTIMES";

    private Printer printer;
    private Context context;
    private String code;
    private ObjectMapper mapper = new ObjectMapper();

    public KKMWebServer(int port, SSLServerSocketFactory sslFactory, String code, Context context) throws Exception {
        super(port);
        makeSecure(sslFactory, null);
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

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        NanoHTTPD.Response response = null;
        if (method.equals(Method.POST)) {
            final HashMap<String, String> map = new HashMap<String, String>();
            try {
                session.parseBody(map);
                final String json = map.get("postData");

                Log.i(TAG, "received: " + json);

                Result res = new Result();
                res.success = true;
                try {
                    processAction(json);
                } catch (Exception e) {
                    showMessage("Error: " + e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                    res.success = false;
                    res.errorMessage = e.getMessage();
                    res.errorClass = e.getClass().getSimpleName();
                }
                response = newFixedLengthResponse(mapper.writeValueAsString(res));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                showMessage("Error: " + e.getMessage());
                response = newFixedLengthResponse("{success: false, errorMessage: \"" + e.getMessage() + "\"}");
            }
        }
        else {
            response = newFixedLengthResponse("");
        }
        response = addCORSHeaders(response);
        return response;
    }

    private Response addCORSHeaders(Response resp) {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Headers", "origin,accept,content-type");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        resp.addHeader("Access-Control-Max-Age", "" + (42 * 60 * 60));
        return resp;
    }

    private void processAction(String json) throws PrinterException, IOException {
        ActionRecord action = parseMessage(json, ActionRecord.class);
        if (action == null) {
            throw new IllegalArgumentException("error parse ActionRecord");
        }
        showMessage("Обработка действия: " + action.action);
        if ("newGuest".equals(action.action)) {
            NewGuestCommandRecord record = parseMessage(action.data, NewGuestCommandRecord.class);
            checkCode(record.code);
            printer.printNewGuest(record);
        }
        else if ("printCheck".equals(action.action)) {
            PrintCheckCommandRecord record = parseMessage(action.data, PrintCheckCommandRecord.class);
            checkCode(record.code);
            printer.printCheck(record);
        }
        else if ("printReturnCheck".equals(action.action)) {
            PrintCheckCommandRecord record = parseMessage(action.data, PrintCheckCommandRecord.class);
            checkCode(record.code);
            printer.printReturnCheck(record);
        }
        else if ("printPredCheck".equals(action.action)) {
            PrintCheckCommandRecord record = parseMessage(action.data, PrintCheckCommandRecord.class);
            checkCode(record.code);
            printer.printPredCheck(record);
        }
        else if ("reportX".equals(action.action)) {
            ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
            checkCode(record.code);
            printer.reportX();
        }
        else if ("reportZ".equals(action.action)) {
            ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
            checkCode(record.code);
            printer.reportZ();
        }
        else if ("openSession".equals(action.action)) {
            ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
            checkCode(record.code);
            printer.startShift();
        }
        else if ("cashIncome".equals(action.action)) {
            CashIncomeRecord record = parseMessage(action.data, CashIncomeRecord.class);
            checkCode(record.code);
            printer.cashIncome(record.sum);
        }
        else {
            throw new IllegalArgumentException("Неизвестная команда: " + action.action + ". Вероятно требуется обновить " +
                    "модуль для связи с кассой до последней версии");
        }
        showMessage("Обработано действие: " + action.action);
    }

    private void checkCode(String code) throws PrinterException {
        if (code == null || code.trim().isEmpty() || !code.equals(this.code)) {
            throw new PrinterException(0, "Неизвестная команда. Проверьте настройки системы");
        }
    }

    private <T> T parseMessage(String message, Class<T> tClass) throws IOException {
        return mapper.readValue(message, tClass);
    }

}
