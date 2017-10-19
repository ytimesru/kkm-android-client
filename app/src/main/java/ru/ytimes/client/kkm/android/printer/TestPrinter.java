package ru.ytimes.client.kkm.android.printer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 28.09.17.
 */

public class TestPrinter implements Printer {
    private static final String TAG = "YTIMES";

    private Context context;

    public TestPrinter(Context context) {
        this.context = context;
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        context.sendBroadcast(local);
    }

    @Override
    public void connect(Context application, String settings) {
        showMessage("test connect");
    }

    @Override
    public void stop() {
        showMessage("test stop");
    }

    @Override
    public void reportZ() throws PrinterException {
        showMessage("test report z");
    }

    @Override
    public void reportX() throws PrinterException {
        showMessage("test report x");
    }

    @Override
    public void printCheck(PrintCheckCommandRecord record) throws PrinterException {
        showMessage("test print check");
    }

    @Override
    public void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException {
        showMessage("test return check");
    }

    @Override
    public void printPredCheck(PrintCheckCommandRecord record) throws PrinterException {
        showMessage("test pred check");
    }

    @Override
    public void printNewGuest(NewGuestCommandRecord record) throws PrinterException {
        showMessage("test new guest");
    }

    @Override
    public void cashIncome(Integer summ) throws PrinterException {
        showMessage("cash income");
    }

    @Override
    public void startShift() throws PrinterException {
        showMessage("start shift");
    }

}
