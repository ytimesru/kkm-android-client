package ru.ytimes.client.kkm.android.printer;

import android.content.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import ru.ytimes.client.kkm.android.record.AbstractCommandRecord;
import ru.ytimes.client.kkm.android.record.CashIncomeRecord;
import ru.ytimes.client.kkm.android.record.ItemRecord;
import ru.ytimes.client.kkm.android.record.ModelInfoRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;
import ru.ytimes.client.main.Utils;
import ru.ytimes.client.utils.Sam4sBuilder;

/**
 * Created by andrey on 26.06.18.
 */

public class POSPrinter implements Printer {

    private String host = "192.168.0.253";
    private int port = 6001;

    public POSPrinter(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect(Context application) throws ExecutionException, InterruptedException, PrinterException {

    }

    @Override
    public boolean isConnected() throws PrinterException {
        return true;
    }

    @Override
    public void stop() throws PrinterException {

    }

    @Override
    public ModelInfoRecord getInfo() throws PrinterException {
        return null;
    }

    @Override
    public void reportZ(AbstractCommandRecord record) throws PrinterException {
        throw new IllegalStateException("Недоступно для данного принтера");
    }

    @Override
    public void reportX(ReportCommandRecord record) throws PrinterException {
        throw new IllegalStateException("Недоступно для данного принтера");
    }

    @Override
    public void startShift(ReportCommandRecord record) throws PrinterException {
        throw new IllegalStateException("Недоступно для данного принтера");
    }

    @Override
    public void cashIncome(CashIncomeRecord record) throws PrinterException {
        throw new IllegalStateException("Недоступно для данного принтера");
    }

    @Override
    public void copyLastDoc(AbstractCommandRecord record) throws PrinterException {
        throw new IllegalStateException("Недоступно для данного принтера");
    }

    @Override
    public void demoReport(AbstractCommandRecord record) throws PrinterException {
        PrintCheckCommandRecord record2 = new PrintCheckCommandRecord();
        record2.userFIO = "Фамилия И.О.";
        record2.itemList = new ArrayList<>();
        ItemRecord itemRecord = new ItemRecord();
        itemRecord.name = "Позиция 1";
        itemRecord.price = 33.0;
        itemRecord.quantity = 2.0;
        record2.itemList.add(itemRecord);
        record2.moneySum = 100.0;
        doPrint("ТЕСТ ПОДКЛЮЧЕНИЯ ПРИНТЕРА", record2);
    }

    @Override
    public void ofdTestReport(AbstractCommandRecord record) throws PrinterException {
        throw new IllegalStateException("Недоступно для данного принтера");
    }

    @Override
    public void printCheck(PrintCheckCommandRecord record) throws PrinterException {
        doPrint("ПРОДАЖА", record);
    }

    @Override
    public void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException {
        doPrint("ВОЗВРАТ", record);
    }

    @Override
    public void printPredCheck(PrintCheckCommandRecord record) throws PrinterException {
        doPrint("ПРЕДЧЕК", record);
    }

    private void doPrint(String title, PrintCheckCommandRecord record) throws PrinterException {
        try {
            Sam4sBuilder builder = new Sam4sBuilder(host, port);

            builder.addTextLang(1);
            builder.addTextFont(Sam4sBuilder.FONT_A);
            builder.addTextSize(1, 1);
            builder.addTextStyle(false, false, false, Sam4sBuilder.COLOR_1);

            builder.addTextSize(1, 2);
            builder.addTextAlign(0);
            builder.add2Col(title, "#" + (record.checkNum != null ? record.checkNum : ""));
            builder.addTextSize(1, 1);

            builder.addTextAlign(2);
            builder.addText(Utils.toDateTimeString(new Date()));

            builder.addTextAlign(0);
            builder.add2Col("КАССИР", record.userFIO);
            builder.addText(" ");

            builder.addDelim();
            builder.addText(" ");

            BigDecimal totalSum = new BigDecimal(0);
            for(ItemRecord itemRecord: record.itemList) {
                BigDecimal total = new BigDecimal(itemRecord.price).multiply(new BigDecimal(itemRecord.quantity));
                totalSum = totalSum.add(total);

                builder.addTextAlign(0);
                builder.addText(itemRecord.name);

                String price = "   " + itemRecord.price + " x " + itemRecord.quantity;

                builder.addTextAlign(2);
                builder.addPosition(price, total.doubleValue());
            }

            builder.addTextAlign(0);
            builder.addText(" ");
            builder.addDelim();
            builder.addText(" ");

            builder.addTextSize(1, 2);
            builder.addTextAlign(0);
            builder.addPosition("ИТОГО К ОПЛАТЕ:", totalSum.doubleValue(), ' ');

            builder.addTextSize(1, 1);
            builder.addTextAlign(0);
            builder.addText("Оплата");

            if (record.creditSum != null && record.creditSum > 0) {
                builder.addTextAlign(0);
                builder.addPosition("   Банк. карта", record.creditSum);
            }
            if (record.moneySum != null && record.moneySum > 0) {
                builder.addTextAlign(0);
                builder.addPosition("   Наличные", record.moneySum);

                if (record.creditSum != null && record.creditSum > 0) {
                    totalSum = totalSum.subtract(new BigDecimal(record.creditSum));
                }
                totalSum = new BigDecimal(record.moneySum).subtract(totalSum);
                if (totalSum.doubleValue() >= 0) {
                    builder.addTextAlign(0);
                    builder.addPosition("Сдача", totalSum.doubleValue());
                }
            }

            builder.addCut(Sam4sBuilder.CUT_FEED);
            builder.sendData();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException(0, e.getMessage());
        }
    }

}
