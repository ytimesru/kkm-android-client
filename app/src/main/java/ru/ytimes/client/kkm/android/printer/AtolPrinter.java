package ru.ytimes.client.kkm.android.printer;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.ytimes.client.kkm.android.Utils;
import ru.ytimes.client.kkm.android.record.AbstractCommandRecord;
import ru.ytimes.client.kkm.android.record.CashIncomeRecord;
import ru.ytimes.client.kkm.android.record.GuestRecord;
import ru.ytimes.client.kkm.android.record.GuestType;
import ru.ytimes.client.kkm.android.record.ItemRecord;
import ru.ytimes.client.kkm.android.record.ModelInfoRecord;
import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.OFDChannel;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ReportCommandRecord;
import ru.ytimes.client.kkm.android.record.VAT;

/**
 * Created by andrey on 17.06.17.
 */

public class AtolPrinter implements Printer {
    private static final String TAG = "YTIMES";

    private Context context;
    private IFptr fptr = null;
    private String port;
    private String wifiIP;
    private Integer wifiPort;
    private int model;
    private VAT vat = VAT.NO;
    private OFDChannel ofdChannel = null;
    private Map<String, Integer> modelList = new HashMap<String, Integer>();

    public AtolPrinter(Context context) {
        this.context = context;

        modelList.put("ATOLAUTO", 500);
        modelList.put("ATOL11F", 67);
        modelList.put("ATOL15F", 78);
        modelList.put("ATOL20F", 81);
        modelList.put("ATOL22F", 63);
        modelList.put("ATOL25F", 57);
        modelList.put("ATOL30F", 61);
        modelList.put("ATOL50F", 80);
        modelList.put("ATOL55F", 62);
        modelList.put("ATOL90F", 72);
        modelList.put("ATOL91F", 82);
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setWifiIP(String wifiIP) {
        this.wifiIP = wifiIP;
    }

    public void setWifiPort(Integer wifiPort) {
        this.wifiPort = wifiPort;
    }

    public void setModel(String model) throws PrinterException {
        if (!modelList.containsKey(model)) {
            throw new PrinterException(0, "Модель не поддерживается в данной версии коммуникационного модуля");
        }

        this.model = modelList.get(model);
    }

    public void setVat(VAT vat) {
        this.vat = vat;
    }

    public void setOfdChannel(OFDChannel ofdChannel) {
        this.ofdChannel = ofdChannel;
    }

    public void stop() {
        if (fptr != null) {
            fptr.destroy();
        }
    }

    synchronized public boolean isConnected() throws PrinterException {
        doConnect();
        try {
            return true;
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public ModelInfoRecord getInfo() throws PrinterException {
        doConnect();
        try {
            fptr.setParam(IFptr.LIBFPTR_PARAM_DATA_TYPE, IFptr.LIBFPTR_DT_STATUS);
            if (fptr.queryData() < 0) {
                checkError(fptr);
            }

            ModelInfoRecord record = new ModelInfoRecord();
            record.serialNumber    = fptr.getParamString(IFptr.LIBFPTR_PARAM_SERIAL_NUMBER);
            record.modelName       = fptr.getParamString(IFptr.LIBFPTR_PARAM_MODEL_NAME);
            record.unitVersion     = fptr.getParamString(IFptr.LIBFPTR_PARAM_UNIT_VERSION);

            //ОФД
            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_REG_INFO);
            if (fptr.fnQueryData() < 0) {
                checkError(fptr);
            }

            record.ofdName = fptr.getParamString(1046);


            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_OFD_EXCHANGE_STATUS);
            if (fptr.fnQueryData() < 0) {
                checkError(fptr);
            }

            record.ofdUnsentCount    = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENTS_COUNT);
            Date unsentDateTime = fptr.getParamDateTime(IFptr.LIBFPTR_PARAM_DATE_TIME);
            if (unsentDateTime != null) {
                record.ofdUnsentDatetime = Utils.toDateString(unsentDateTime);
            }


            //ФФД
            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_FFD_VERSIONS);
            if (fptr.fnQueryData() < 0) {
                checkError(fptr);
            }

            long deviceFfdVersion    = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DEVICE_FFD_VERSION);
            record.deviceFfdVersion  = getFFDVersion(deviceFfdVersion);
            long fnFfdVersion        = fptr.getParamInt(IFptr.LIBFPTR_PARAM_FN_FFD_VERSION);
            record.fnFfdVersion      = getFFDVersion(fnFfdVersion);
            long ffdVersion          = fptr.getParamInt(IFptr.LIBFPTR_PARAM_FFD_VERSION);
            record.ffdVersion        = getFFDVersion(ffdVersion);

            //ФН
            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_FN_INFO);
            fptr.fnQueryData();

            record.fnSerial = fptr.getParamString(IFptr.LIBFPTR_PARAM_SERIAL_NUMBER);
            record.fnVersion = fptr.getParamString(IFptr.LIBFPTR_PARAM_FN_VERSION);

            //ФН Дата окончания
            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_VALIDITY);
            if (fptr.fnQueryData() < 0) {
                checkError(fptr);
            }

            Date dateTime = fptr.getParamDateTime(IFptr.LIBFPTR_PARAM_DATE_TIME);
            if (dateTime != null) {
                record.fnDate = Utils.toDateString(dateTime);
            }

            return record;
        }
        finally {
            doDisconnect();
        }
    }

    private String getFFDVersion(long version) {
        if (version == IFptr.LIBFPTR_FFD_1_0) {
            return "1.0";
        }
        if (version == IFptr.LIBFPTR_FFD_1_0_5) {
            return "1.05";
        }
        if (version == IFptr.LIBFPTR_FFD_1_1) {
            return "1.1";
        }
        return "неизвестная";
    }


    @Override
    public void connect(final Context application) throws ExecutionException, InterruptedException {
        final AsyncTask<Void, String, Void> task = new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    if (fptr != null) {
                        try {
                            stop();
                        }
                        catch (Throwable e) {
                            publishProgress(e.getMessage());
                        }
                    }

                    if (port == null || port.isEmpty()) {
                        throw new PrinterException(0, "Порт подключения не задан");
                    }

                    fptr = new Fptr(application);
                    fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_MODEL, String.valueOf(model));
                    if (port.equals("TCPIP")) {
                        publishProgress("Подключение через WiFi: " + wifiIP + ":" + wifiPort);
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_PORT, String.valueOf(IFptr.LIBFPTR_PORT_TCPIP));
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_IPADDRESS, wifiIP);
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_IPPORT, String.valueOf(wifiPort));
                    }
                    else if (port.equals("BLUETOOTH")) {
                        Set<BluetoothDevice> devices = Utils.getBluetoothDevices();
                        String macAddress = null;
                        String macName = null;
                        for(BluetoothDevice bt : devices) {
                            publishProgress("Поиск устройства: " + bt.getName());
                            if (bt.getName() != null && bt.getName().toLowerCase().contains("atol") || bt.getName().toLowerCase().contains("атол")) {
                                macAddress = bt.getAddress();
                                macName = bt.getName();
                                break;
                            }
                        }

                        if (macAddress == null) {
                            throw new PrinterException(0, "Устройство не найдено");
                        }
                        publishProgress("Подключение через Bluetooth: " + macName + ", " + macAddress);
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_PORT, String.valueOf(IFptr.LIBFPTR_PORT_BLUETOOTH));
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_MACADDRESS, macAddress);
                    } else if (port.equals("USBAUTO")) {
                        publishProgress("Подключение через порт USB (автоопределение)");
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_PORT, String.valueOf(IFptr.LIBFPTR_PORT_USB));
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_USB_DEVICE_PATH, "auto");
                    } else {
                        publishProgress("Подключение через порт: " + port);
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_PORT, String.valueOf(IFptr.LIBFPTR_PORT_USB));
                        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_USB_DEVICE_PATH, port);
                    }

                    if (ofdChannel != null) {
                        if (ofdChannel.equals(OFDChannel.PROTO)) {
                            publishProgress("ОФД средвами транспортного протокола (OFD PROTO 1)");
                            fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_OFD_CHANNEL, String.valueOf(IFptr.LIBFPTR_OFD_CHANNEL_PROTO));
                        } else if (ofdChannel.equals(OFDChannel.ASIS)) {
                            publishProgress("ОФД используя настройки ККМ (OFD NONE 2)");
                            fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_OFD_CHANNEL, String.valueOf(IFptr.LIBFPTR_OFD_CHANNEL_NONE));
                        } else {
                            throw new PrinterException(0, "Не поддерживаемое значение параметра связи с ОФД");
                        }
                    }

                    if (fptr.applySingleSettings() < 0) {
                        checkError(fptr);
                    }

                    publishProgress("Проверка связи");
                    doConnect();
                    try {
                        cancelCheck();
                        publishProgress("Подключено");
                    } finally {
                        doDisconnect();
                    }

                }
                catch (Exception e) {
                    publishProgress(e.getMessage(), "true");
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                if (values == null || values.length == 0) {
                    return;
                }
                showMessage(values[0]);
            }

        };
        task.execute().get();
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        context.sendBroadcast(local);
    }

    private void doConnect() throws PrinterException {
        //showMessage("Связь установлена");
        if (fptr.open() < 0) {
            checkError(fptr);
        }
    }

    private void doDisconnect() throws PrinterException {
        //showMessage("Отключились от устройства");
        if (fptr.close() < 0) {
            checkError(fptr);
        }
    }

    public void destroy() throws Throwable {
        stop();
    }

    synchronized public void reportX(ReportCommandRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_X);
            if (fptr.report() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public void reportZ(AbstractCommandRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_CLOSE_SHIFT);
            if (fptr.report() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public void startShift(ReportCommandRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            if (fptr.openShift() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public void cashIncome(CashIncomeRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            fptr.setParam(IFptr.LIBFPTR_PARAM_SUM, record.sum);
            if (fptr.cashIncome() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }


    synchronized public void copyLastDoc(AbstractCommandRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_LAST_DOCUMENT);
            if (fptr.report() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public void demoReport(AbstractCommandRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_KKT_DEMO);
            if (fptr.report() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public void ofdTestReport(AbstractCommandRecord record) throws PrinterException {
        doConnect();
        try {
            loginOperator(record);
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_OFD_TEST);
            if (fptr.report() < 0) {
                checkError(fptr);
            }
            if (!waitDocumentClosed()) {
                checkError(fptr);
            }
        }
        finally {
            doDisconnect();
        }
    }

    //выставление счета
    synchronized public void printPredCheck(PrintCheckCommandRecord record) throws PrinterException {
        doConnect();
        try {
            doPrintPredCheck(record);
        }
        finally {
            doDisconnect();
        }
    }

    private void doPrintPredCheck(PrintCheckCommandRecord record) throws PrinterException {
        checkRecord(record);
        if (fptr.beginNonfiscalDocument() < 0) {
            checkError(fptr);
        }

        printText("СЧЕТ (ПРЕДЧЕК)");
        printText("");
        printText("ПОЗИЦИИ ОПЛАТЫ", IFptr.LIBFPTR_ALIGNMENT_CENTER, IFptr.LIBFPTR_TW_WORDS);
        for(int i = 0; i < record.itemList.size(); i++) {
            ItemRecord r = record.itemList.get(i);
            printText((i + 1) + ". " + r.name, IFptr.LIBFPTR_ALIGNMENT_LEFT, IFptr.LIBFPTR_TW_WORDS);

            double total = r.price * r.quantity;
            printText(r.price + " x " + r.quantity + " = " + total, IFptr.LIBFPTR_ALIGNMENT_RIGHT, IFptr.LIBFPTR_TW_CHARS);

            if (r.discountSum != null && r.discountSum > 0) {
                printText("Скидка: " + r.discountSum, IFptr.LIBFPTR_ALIGNMENT_RIGHT, IFptr.LIBFPTR_TW_CHARS);
            }
            if (r.discountPercent != null && r.discountPercent > 0) {
                printText("Скидка: " + r.discountPercent + "%", IFptr.LIBFPTR_ALIGNMENT_RIGHT, IFptr.LIBFPTR_TW_CHARS);
            }
        }
        printBoldText("ИТОГО: " + record.moneySum, IFptr.LIBFPTR_ALIGNMENT_RIGHT, IFptr.LIBFPTR_TW_CHARS);

        if (GuestType.TIME.equals(record.type) && record.guestInfoList != null) {
            printText("");
            printText("РАССЧИТЫВАЕМЫЕ ГОСТИ", IFptr.LIBFPTR_ALIGNMENT_LEFT, IFptr.LIBFPTR_TW_CHARS);
            int i = 1;
            for(GuestRecord r: record.guestInfoList) {
                String name = r.name;
                if (r.card != null && !r.card.isEmpty()) {
                    name += " (" + r.card + ")";
                };
                printText(i + ". " + name, IFptr.LIBFPTR_ALIGNMENT_LEFT, IFptr.LIBFPTR_TW_CHARS);
                printText("время прихода: " + r.startTime, IFptr.LIBFPTR_ALIGNMENT_RIGHT, IFptr.LIBFPTR_TW_CHARS);
                printText("проведенное время: " + r.minutes + " мин.", IFptr.LIBFPTR_ALIGNMENT_RIGHT, IFptr.LIBFPTR_TW_CHARS);
                i++;
            }

            printText("");
            printText("");
        }

        if (GuestType.TOGO.equals(record.type)  && record.guestInfoList != null) {
            printText("");
            for(GuestRecord r: record.guestInfoList) {
                String name = r.name;
                if (r.phone != null && !r.phone.isEmpty()) {
                    name += ", " + r.phone;
                }
                printText(name, IFptr.LIBFPTR_ALIGNMENT_CENTER, IFptr.LIBFPTR_TW_CHARS);
                printText(r.message);
            }

            printText("");
            printText("");
        }

        if (record.additionalInfo != null) {
            printText("");
            for(String s: record.additionalInfo) {
                printText(s, IFptr.LIBFPTR_ALIGNMENT_CENTER, IFptr.LIBFPTR_TW_WORDS);
            }
            printText("");
        }

        if (fptr.endNonfiscalDocument() < 0) {
            checkError(fptr);
        }
    }

    synchronized public void printCheck(PrintCheckCommandRecord record) throws PrinterException {
        doConnect();
        try {
            doPrintCheck(record, IFptr.LIBFPTR_RT_SELL);
        }
        finally {
            doDisconnect();
        }
    }

    synchronized public void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException {
        doConnect();
        try {
            doPrintCheck(record, IFptr.LIBFPTR_RT_SELL_RETURN);
        }
        finally {
            doDisconnect();
        }
    }

    private void doPrintCheck(PrintCheckCommandRecord record, int checkType) throws PrinterException {
        checkRecord(record);
        cancelCheck();
        loginOperator(record);

        // Открываем чек продажи, попутно обработав превышение смены
        try {
            openCheck(record, checkType);
        } catch (PrinterException e) {
            // Проверка на превышение смены
            if (e.getCode() == IFptr.LIBFPTR_ERROR_SHIFT_EXPIRED) {
                reportZ(record);
                openCheck(record, checkType);
            } else {
                throw e;
            }
        }

        try {
            BigDecimal totalPrice = new BigDecimal(0.0);
            for (ItemRecord r : record.itemList) {
                BigDecimal price = new BigDecimal(r.price);
                BigDecimal discountPosition = new BigDecimal(0.0);
                if (r.discountSum != null) {
                    discountPosition = new BigDecimal(r.discountSum);
                } else if (r.discountPercent != null) {
                    if (r.discountPercent > 100) {
                        r.discountPercent = 100.0;
                    }
                    BigDecimal value = new BigDecimal(r.price).multiply(new BigDecimal(r.quantity));
                    discountPosition = value.multiply(new BigDecimal(r.discountPercent)).divide(new BigDecimal(100.0));
                }
                BigDecimal priceWithDiscount = price.subtract(discountPosition);

                Log.i (TAG, "Name: " + r.name + ", price=" + price + ", discount = " + discountPosition + ", priceWithDiscount = " + priceWithDiscount);
                registrationFZ54(r.name, priceWithDiscount.doubleValue(), r.quantity, r.vatValue);

                totalPrice = totalPrice.add(priceWithDiscount.multiply(new BigDecimal(r.quantity)));
            }

            if (record.creditSum != null && record.creditSum > 0) {
                payment(record.creditSum, IFptr.LIBFPTR_PT_ELECTRONICALLY);
            }

            if (record.moneySum != null && record.moneySum > 0) {
                payment(record.moneySum, IFptr.LIBFPTR_PT_CASH);
            }

            Log.i (TAG, "Total price = " + totalPrice);
            if (Boolean.TRUE.equals(record.dropPenny)) {
                double totalWithoutPenny = totalPrice.setScale(0, BigDecimal.ROUND_HALF_DOWN).doubleValue();
                fptr.setParam(IFptr.LIBFPTR_PARAM_SUM, totalWithoutPenny);
                if (fptr.receiptTotal() < 0) {
                    checkError(fptr);
                }
            }

            // Закрываем чек
            if (Boolean.TRUE.equals(record.testMode)) {
                cancelCheck();
            }
            else {
                if (fptr.closeReceipt() < 0) {
                    checkError(fptr);
                }
                if (!waitDocumentClosed()) {
                    cancelCheck();
                }
                continuePrint();
            }
        }
        catch (PrinterException e) {
            cancelCheck();
            throw e;
        }
    }

    private void checkRecord(PrintCheckCommandRecord record) throws PrinterException {
        if (record.itemList == null || record.itemList.isEmpty()) {
            throw new PrinterException(0, "Список оплаты пустой");
        }
        if (record.moneySum == null && record.creditSum == null) {
            throw new PrinterException(0, "Итоговое значение для оплаты не задано");
        }
        if (record.moneySum != null && record.moneySum == 0.0 &&
                record.creditSum != null && record.creditSum == 0.0) {
            throw new PrinterException(0, "Итоговое значение для оплаты не задано");
        }
        for(ItemRecord r: record.itemList) {
            if (r.name == null || r.name.trim().isEmpty()) {
                throw new PrinterException(0, "Не задано наименование позиции");
            }
            if (r.price == null) {
                throw new PrinterException(0, "Не задана цена позиции: " + r.name);
            }
            if (r.quantity == null) {
                throw new PrinterException(0, "Не задано количество позиции: " + r.name);
            }

            if (r.discountPercent != null && r.discountSum != null) {
                throw new PrinterException(0, "Нужно задать только один тип скидки - либо в процентах, либо в сумме. Позиция: " + r.name);
            }
        }
        if (Boolean.TRUE.equals(record.onlyElectronically)) {
            if ((record.phone == null || record.phone.trim().isEmpty()) &&
                (record.email == null || record.email.trim().isEmpty())) {
                throw new PrinterException(0, "Для электронных чеков обязателно задание телефона или email покупателя");
            }
        }
    }

    private int getVatNumber(VAT vatValue) throws PrinterException {
        if (vatValue == null) {
            return IFptr.LIBFPTR_TAX_NO;
        }
        if (vatValue.equals(VAT.NO)) {
            return IFptr.LIBFPTR_TAX_NO;
        }
        if (vatValue.equals(VAT.VAT0)) {
            return IFptr.LIBFPTR_TAX_VAT0;
        }
        if (vatValue.equals(VAT.VAT10)) {
            return IFptr.LIBFPTR_TAX_VAT10;
        }
        if (vatValue.equals(VAT.VAT18)) {
            return IFptr.LIBFPTR_TAX_VAT18;
        }
        if (vatValue.equals(VAT.VAT110)) {
            return IFptr.LIBFPTR_TAX_VAT110;
        }
        if (vatValue.equals(VAT.VAT118)) {
            return IFptr.LIBFPTR_TAX_VAT118;
        }
        throw new PrinterException(0, "Неизвестный тип налога: " + vatValue);
    }

    private void registrationFZ54(String name, double price, double quantity, VAT itemVat) throws PrinterException {
        fptr.setParam(IFptr.LIBFPTR_PARAM_COMMODITY_NAME, name);
        fptr.setParam(IFptr.LIBFPTR_PARAM_PRICE, price);
        fptr.setParam(IFptr.LIBFPTR_PARAM_QUANTITY, quantity);

        VAT vatValue = this.vat;
        if (itemVat != null) {
            vatValue = itemVat;
        }
        int vatNumber = getVatNumber(vatValue);

        fptr.setParam(IFptr.LIBFPTR_PARAM_TAX_TYPE, vatNumber);
        if (fptr.registration() < 0) {
            checkError(fptr);
        }
    }

    private void payment(double sum, int type) throws PrinterException {
        fptr.setParam(IFptr.LIBFPTR_PARAM_PAYMENT_TYPE, type);
        fptr.setParam(IFptr.LIBFPTR_PARAM_PAYMENT_SUM, sum);
        fptr.payment();
    }

    private void openCheck(PrintCheckCommandRecord record, int type) throws PrinterException {
        fptr.setParam(IFptr.LIBFPTR_PARAM_RECEIPT_TYPE, type);
        if (record.phone != null && !record.phone.isEmpty()) {
            fptr.setParam(1008, record.phone);
        }
        else if (record.email != null && !record.email.isEmpty()) {
            fptr.setParam(1008, record.email);
        }

        if (Boolean.TRUE.equals(record.onlyElectronically)) {
            fptr.setParam(IFptr.LIBFPTR_PARAM_RECEIPT_ELECTRONICALLY, true);
        }

        if (fptr.openReceipt() < 1) {
            checkError(fptr);
        }
    }

    private void checkError(IFptr fptr) throws PrinterException {
        checkError(fptr, true);
    }

    private void checkError(IFptr fptr, boolean log) throws PrinterException {
        int rc = fptr.errorCode();
        if (rc > 0) {
            if (log) {
                showMessage(fptr.errorDescription());
            }
            throw new PrinterException(rc, fptr.errorDescription());
        }
    }

    private boolean waitDocumentClosed() {
        int count = 0;
        while (fptr.checkDocumentClosed() < 0) {
            // Не удалось проверить состояние документа.
            // Вывести пользователю текст ошибки,
            // попросить устранить неполадку и повторить запрос
            showMessage(fptr.errorDescription());

            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                break;
            }
            count++;
            if (count > 20) {
                break;
            }
        }

        if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_CLOSED)) {
            return false;
        }
        return true;
    }

    private void continuePrint() {
        int count = 0;

        if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_PRINTED)) {
            while (fptr.continuePrint() < 0) {
                showMessage(fptr.errorDescription());

                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    break;
                }
                count++;
                if (count > 20) {
                    break;
                }
            }
        }
    }

    private void loginOperator(AbstractCommandRecord record) {
        if (record.userFIO == null || record.userFIO.isEmpty()) {
            return;
        }
        String fio = record.userFIO;
        if (record.userPosition != null && !record.userPosition.isEmpty()) {
            fio = record.userPosition + " " + fio;
        }
        fptr.setParam(1021, fio);
        if (record.userINN != null && !record.userINN.isEmpty()) {
            fptr.setParam(1203, record.userINN);
        }
        fptr.operatorLogin();
    }

    private void cancelCheck() throws PrinterException {
        // Отменяем чек, если уже открыт. Ошибки "Неверный режим" и "Чек уже закрыт"
        // не являются ошибками, если мы хотим просто отменить чек
        try {
            if (fptr.cancelReceipt() < 0)
                checkError(fptr, false);
        } catch (PrinterException e) {
            int rc = e.getCode();
            if (rc != IFptr.LIBFPTR_ERROR_DENIED_IN_CLOSED_RECEIPT) {
                throw e;
            }
        }
    }

    private void printText(String text) throws PrinterException {
        printText(text, IFptr.LIBFPTR_ALIGNMENT_CENTER, IFptr.LIBFPTR_TW_WORDS);
    }

    private void printText(String text, int alignment, int wrap) throws PrinterException {
        fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT, text);
        fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, alignment);
        fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT_WRAP, wrap);
        fptr.printText();
    }

    private void printBoldText(String text, int alignment, int wrap) throws PrinterException {
        fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT, text);
        fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, alignment);
        fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT_WRAP, wrap);
        fptr.setParam(IFptr.LIBFPTR_PARAM_FONT_DOUBLE_WIDTH, true);
        fptr.setParam(IFptr.LIBFPTR_PARAM_FONT_DOUBLE_HEIGHT, true);
        fptr.printText();
    }

}
