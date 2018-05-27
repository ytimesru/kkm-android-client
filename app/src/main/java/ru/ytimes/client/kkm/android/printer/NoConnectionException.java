package ru.ytimes.client.kkm.android.printer;

/**
 * Created by andrey on 27.05.18.
 */

public class NoConnectionException extends PrinterException {

    public NoConnectionException() {
        super(0, "Нет соединения с кассовым аппаратом");
    }

}
