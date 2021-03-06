package ru.ytimes.client.kkm.android.printer;

import android.content.Context;

import java.util.concurrent.ExecutionException;

import ru.ytimes.client.kkm.android.record.AbstractCommandRecord;
import ru.ytimes.client.kkm.android.record.CashChangeRecord;
import ru.ytimes.client.kkm.android.record.ModelInfoRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;

/**
 * Created by andrey on 13.06.17.
 */

public interface Printer {

    void connect(Context application) throws ExecutionException, InterruptedException, PrinterException;

    boolean isConnected() throws PrinterException;

    void stop() throws PrinterException;

    ModelInfoRecord getInfo() throws PrinterException;

    void reportZ(AbstractCommandRecord record) throws PrinterException;

    void reportX(ReportCommandRecord record) throws PrinterException;

    void startShift(ReportCommandRecord record) throws PrinterException;

    void cashIncome(CashChangeRecord record) throws PrinterException;

    void cashOutcome(CashChangeRecord record) throws PrinterException;

    void copyLastDoc(AbstractCommandRecord record) throws PrinterException;

    void demoReport(AbstractCommandRecord record) throws PrinterException;

    void ofdTestReport(AbstractCommandRecord record) throws PrinterException;

    void printCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException;

    void printPredCheck(PrintCheckCommandRecord record) throws PrinterException;

}
