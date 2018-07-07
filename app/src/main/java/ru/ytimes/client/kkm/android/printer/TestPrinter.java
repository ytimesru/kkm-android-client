package ru.ytimes.client.kkm.android.printer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.ytimes.client.kkm.android.record.AbstractCommandRecord;
import ru.ytimes.client.kkm.android.record.CashChangeRecord;
import ru.ytimes.client.kkm.android.record.ModelInfoRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;

/**
 * Created by andrey on 28.09.17.
 */

public class TestPrinter implements Printer {
    private static final String TAG = "YTIMES";

    private Context context;

    public TestPrinter(Context context) {
        this.context = context;
        showMessage("Тестовый принтер");
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        context.sendBroadcast(local);
    }

    @Override
    public void connect(Context application) {
        showMessage("test connect");
    }

    @Override
    public boolean isConnected() throws PrinterException {
        return true;
    }

    @Override
    public void stop() throws PrinterException {
        showMessage("stop");
    }

    @Override
    public ModelInfoRecord getInfo() throws PrinterException {
        return null;
    }

    @Override
    public void reportZ(AbstractCommandRecord record) throws PrinterException {
        showMessage("test: reportZ");
    }

    @Override
    public void reportX(ReportCommandRecord record) throws PrinterException {
        showMessage("test: reportX");
    }

    @Override
    public void startShift(ReportCommandRecord record) throws PrinterException {
        showMessage("test: start shift");
    }

    @Override
    public void cashIncome(CashChangeRecord record) throws PrinterException {
        showMessage("test: cash");
    }

    @Override
    public void cashOutcome(CashChangeRecord record) throws PrinterException {
        showMessage("test: outcome cash");
    }

    @Override
    public void copyLastDoc(AbstractCommandRecord record) throws PrinterException {

    }

    @Override
    public void demoReport(AbstractCommandRecord record) throws PrinterException {

    }

    @Override
    public void ofdTestReport(AbstractCommandRecord record) throws PrinterException {

    }

    @Override
    public void printCheck(PrintCheckCommandRecord record) throws PrinterException {

    }

    @Override
    public void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException {

    }

    @Override
    public void printPredCheck(PrintCheckCommandRecord record) throws PrinterException {

    }

}
