package ru.ytimes.client.kkm.android.printer;

import android.content.Context;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 13.06.17.
 */

public interface Printer {

    void connect(final Context application, final String settings);

    void reportZ() throws PrinterException;

    void reportX() throws PrinterException;

    void printCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printPredCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printNewGuest(NewGuestCommandRecord record) throws PrinterException ;

}
