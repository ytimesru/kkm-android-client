package ru.ytimes.client.kkm.android.printer;

import android.app.Application;
import android.widget.TextView;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 13.06.17.
 */

public interface Printer {

    void connect(final Application application, final String settings, final TextView statusView);

    void reportZ() throws PrinterException;

    void reportX() throws PrinterException;

    void printCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printNewGuest(NewGuestCommandRecord record) throws PrinterException ;

}
