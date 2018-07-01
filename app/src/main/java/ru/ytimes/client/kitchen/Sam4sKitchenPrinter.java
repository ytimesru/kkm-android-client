package ru.ytimes.client.kitchen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import ru.ytimes.client.kkm.android.printer.PrinterException;
import ru.ytimes.client.kkm.android.record.ItemRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.main.Utils;
import ru.ytimes.client.utils.Sam4sBuilder;
import ru.ytimes.client.utils.StringUtils;

/**
 * Created by andrey on 22.06.18.
 */

public class Sam4sKitchenPrinter implements KitchenPrinter {
    private String host;
    private int port;
    private Integer number;

    public Sam4sKitchenPrinter(String host, int port, Integer number) {
        this.host = host;
        this.port = port;
        this.number = number;
    }

    @Override
    public void print(PrintCheckCommandRecord record) throws PrinterException {
        int i = 0;
        if (record.itemList != null) {
            for(ItemRecord itemRecord: record.itemList) {
                if (itemRecord.kitchenNum != null) {
                    i++;
                }
            }
        }
        if (i == 0) {
            return;
        }

        try {
            Sam4sBuilder builder = new Sam4sBuilder(host, port);

            builder.addTextLang(1);
            builder.addTextFont(Sam4sBuilder.FONT_A);
            builder.addTextSize(1, 1);
            builder.addTextStyle(false, false, false, Sam4sBuilder.COLOR_1);

            builder.addTextSize(1, 2);
            builder.addTextAlign(0);
            builder.add2Col("ЗАКАЗ", "#" + (record.checkNum != null ? record.checkNum : ""));
            builder.addTextSize(1, 1);

            builder.addTextAlign(2);
            builder.addText(Utils.toDateTimeString(new Date()));

            builder.addTextAlign(0);
            builder.add2Col("КАССИР", record.userFIO);
            builder.addText(" ");

            builder.addDelim();
            builder.addText(" ");

            for(ItemRecord itemRecord: record.itemList) {
                if (itemRecord.kitchenNum == null) {
                    continue;
                }
                if (number != null && number != itemRecord.kitchenNum) {
                    continue;
                }

                builder.addTextAlign(0);
                builder.addPosition(itemRecord.name, itemRecord.quantity, '.');
            }

            builder.addTextAlign(0);
            builder.addText(" ");
            builder.addDelim();
            builder.addText(" ");

            builder.addCut(Sam4sBuilder.CUT_FEED);
            builder.sendData();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException(0, e.getMessage());
        }
    }


}

