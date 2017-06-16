package ru.ytimes.client.kkm.android.printer;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 13.06.17.
 */

public interface Printer {

    void reportZ() throws PrinterException;

    void reportX() throws PrinterException;

    void printCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printNewGuest(NewGuestCommandRecord record) throws PrinterException ;

}
