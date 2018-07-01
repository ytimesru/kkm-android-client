package ru.ytimes.client.kitchen;

import java.util.List;

import ru.ytimes.client.kkm.android.printer.PrinterException;
import ru.ytimes.client.kkm.android.record.GuestRecord;
import ru.ytimes.client.kkm.android.record.ItemRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 22.06.18.
 */

public interface KitchenPrinter {


    void print(PrintCheckCommandRecord record) throws PrinterException;


}
