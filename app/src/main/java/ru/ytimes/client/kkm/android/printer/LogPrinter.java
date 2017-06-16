package ru.ytimes.client.kkm.android.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 16.06.17.
 */

public class LogPrinter implements Printer {
    private static final Logger logger = LoggerFactory.getLogger(LogPrinter.class);

    public void reportZ() throws PrinterException {
        logger.info("do z report");
    }

    public void reportX() throws PrinterException {
        logger.info("do x report");
    }

    public void printCheck(PrintCheckCommandRecord record) throws PrinterException {
        logger.info("do print check");
    }

    public void printNewGuest(NewGuestCommandRecord record) throws PrinterException {
        logger.info("print new guest");
    }

}
