package ru.ytimes.client.kkm.android.printer;

import android.util.Log;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 16.06.17.
 */

public class LogPrinter implements Printer {
    private static final String TAG = "LogPrinter";

    public void reportZ() throws PrinterException {
        Log.i(TAG, "do z report");
    }

    public void reportX() throws PrinterException {
        Log.i(TAG, "do x report");
    }

    public void printCheck(PrintCheckCommandRecord record) throws PrinterException {
        Log.i(TAG, "do print check");
    }

    public void printNewGuest(NewGuestCommandRecord record) throws PrinterException {
        Log.i(TAG, "print new guest");
    }

}
