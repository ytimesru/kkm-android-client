package ru.ytimes.client.main;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import ru.ytimes.client.kkm.android.printer.AtolPrinter;
import ru.ytimes.client.kkm.android.printer.Printer;
import ru.ytimes.client.kkm.android.printer.PrinterException;
import ru.ytimes.client.kkm.android.printer.TestPrinter;
import ru.ytimes.client.kkm.android.record.ActionRecord;
import ru.ytimes.client.kkm.android.record.CashIncomeRecord;
import ru.ytimes.client.kkm.android.record.ConfigRecord;
import ru.ytimes.client.kkm.android.record.OFDChannel;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;
import ru.ytimes.client.kkm.android.record.Result;
import ru.ytimes.client.kkm.android.record.StatusRecord;
import ru.ytimes.client.kkm.android.record.VAT;

/**
 * Created by andrey on 26.09.17.
 */

public class WebServer extends NanoHTTPD {
    private static final String TAG = "YTIMES";
    private static String version = "2.0.2.android";

    private Printer printer;
    private Context context;
    private String verificationCode = "87fays87f";
    private ObjectMapper mapper = new ObjectMapper();

    public WebServer(int port, SSLServerSocketFactory sslFactory, Context context) throws Exception {
        super(port);
        makeSecure(sslFactory, null);
        this.context = context;
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
                    res.res = processAction(json);
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

    private Object processAction(String json) throws PrinterException, IOException, InterruptedException, ExecutionException {
        ActionRecord action = parseMessage(json, ActionRecord.class);
        if (action == null) {
            throw new IllegalArgumentException("error parse ActionRecord");
        }
        //checkCode(action.code);
        showMessage("Обработка действия: " + action.action);

        if ("config".equals(action.action)) {
            ConfigRecord record = parseMessage(action.data, ConfigRecord.class);
            applyConfig(record);
        }
        else if ("status".equals(action.action)) {
            StatusRecord record = new StatusRecord();
            record.config = getConfig();
            record.version = version;
            if (printer != null) {
                try {
                    record.isConnected = printer.isConnected();
                    if (Boolean.TRUE.equals(record.isConnected)) {
                        record.info = printer.getInfo();
                    }
                }
                catch (Exception e) {
                    record.lastError = e.getMessage();
                    Log.e(TAG, e.getMessage(), e);
                    showMessage("Error: " + e.getMessage());

                }
            }
            return record;
        }
        else {
            if (printer == null) {
                throw new IllegalArgumentException("Не настроен принтер чеков. Проверьте настройки системы в разделе Оборудование");
            }
            else {
                if (!printer.isConnected()) {
                    initPrinter();
                }

                if ("printCheck".equals(action.action)) {
                    PrintCheckCommandRecord record = parseMessage(action.data, PrintCheckCommandRecord.class);
                    printer.printCheck(record);
                }
                else if ("printReturnCheck".equals(action.action)) {
                    PrintCheckCommandRecord record = parseMessage(action.data, PrintCheckCommandRecord.class);
                    printer.printReturnCheck(record);
                }
                else if ("printPredCheck".equals(action.action)) {
                    PrintCheckCommandRecord record = parseMessage(action.data, PrintCheckCommandRecord.class);
                    printer.printPredCheck(record);
                }
                else if ("reportX".equals(action.action)) {
                    ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
                    printer.reportX(record);
                }
                else if ("reportZ".equals(action.action)) {
                    ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
                    printer.reportZ(record);
                }
                else if ("copyLastDoc".equals(action.action)) {
                    ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
                    printer.copyLastDoc(record);
                }
                else if ("ofdTest".equals(action.action)) {
                    ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
                    printer.ofdTestReport(record);
                }
                else if ("openSession".equals(action.action)) {
                    ReportCommandRecord record = parseMessage(action.data, ReportCommandRecord.class);
                    printer.startShift(record);
                }
                else if ("cashIncome".equals(action.action)) {
                    CashIncomeRecord record = parseMessage(action.data, CashIncomeRecord.class);
                    printer.cashIncome(record);
                }
                else {
                    throw new IllegalArgumentException("Неизвестная команда: " + action.action + ". Вероятно требуется обновить " +
                            "модуль для связи с кассой до последней версии");
                }
                showMessage("Обработано действие: " + action.action);
            }
        }
        return null;
    }

    private ConfigRecord getConfig() {
        ConfigRecord record = new ConfigRecord();
        record.params = new HashMap<String, String>();
        record.verificationCode = this.verificationCode;

        SharedPreferences preferences = context.getSharedPreferences("kkm", Context.MODE_PRIVATE);
        Map<String, ?> preferencesAll = preferences.getAll();
        for(String keys: preferencesAll.keySet()) {
            String value = (String)preferencesAll.get(keys);
            if (keys.equals("verificationCode")) {
                record.verificationCode = value != null ? value : this.verificationCode;
            }
            else if (keys.equals("model")) {
                record.model = value;
            }
            else if (keys.equals("port")) {
                record.port = value;
            }
            else if (keys.equals("wifiIP")) {
                record.wifiIP = value;
            }
            else if (keys.equals("wifiPort")) {
                if (value != null) {
                    try {
                        record.wifiPort = Integer.parseInt(value);
                    }
                    catch (NumberFormatException e) {}
                }
                else {
                    record.wifiPort = 5555;
                }
            }
            else if (keys.equals("vat")) {
                record.vat = value != null ? VAT.valueOf(value) : VAT.NO;
            }
            else if (keys.equals("ofd")) {
                record.ofd = value != null ? OFDChannel.valueOf(value) : OFDChannel.PROTO;
            }
            else {
                record.params.put(keys, value);
            }
        }

        return record;
    }

    private void applyConfig(ConfigRecord record) throws PrinterException {
        try {
            SharedPreferences preferences = context.getSharedPreferences("kkm", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("model", "ATOL1111");
            editor.putString("verificationCode", record.verificationCode);
            editor.putString("model", record.model);
            editor.putString("port", record.port);
            editor.putString("wifiIP", record.wifiIP);
            editor.putString("wifiPort", record.wifiPort != null ? record.wifiPort + "" : null);
            editor.putString("vat", record.vat != null ? record.vat.name() : VAT.NO.name());
            editor.putString("ofd", record.ofd != null ? record.ofd.name() : OFDChannel.PROTO.name());
            if (record.params != null && record.params.size() > 0) {
                for (String keys : record.params.keySet()) {
                    editor.putString(keys, record.params.get(keys));
                }
            }

            editor.commit();
        }
        finally {
            initPrinter();
        }
    }

    private void checkCode(String code) throws PrinterException {
        if (code == null || code.trim().isEmpty() || !code.equals(this.verificationCode)) {
            throw new PrinterException(0, "Неизвестная команда. Проверьте настройки системы");
        }
    }

    private <T> T parseMessage(String message, Class<T> tClass) throws IOException {
        return mapper.readValue(message, tClass);
    }

    synchronized public void initPrinter() throws PrinterException {
        ConfigRecord config = getConfig();
        if (config.model == null || config.model.isEmpty()) {
            showMessage("Фискальный регистратор не подключен");
            return;
        }
        showMessage("Подключаем фискальный регистратор");
        stopPrinter();


        if ("TEST".equals(config.model)) {
            printer = new TestPrinter(context);
        }
        else if (config.model.startsWith("ATOL")) {
            AtolPrinter atolPrinter = new AtolPrinter(context);
            atolPrinter.setModel(config.model);
            atolPrinter.setPort(config.port);
            atolPrinter.setWifiIP(config.wifiIP);
            atolPrinter.setWifiPort(config.wifiPort);
            atolPrinter.setVat(config.vat != null ? config.vat : VAT.NO);
            atolPrinter.setOfdChannel(config.ofd != null ? config.ofd : OFDChannel.PROTO);

            printer = atolPrinter;
            try {
                printer.connect(context);
            }
            catch (Exception e) {
                throw new PrinterException(0, e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        stopPrinter();
        super.stop();
    }

    private void stopPrinter() {
        if (printer != null) {
            try {
                printer.stop();
            }
            catch (Throwable e) {}
        }
    }

}
