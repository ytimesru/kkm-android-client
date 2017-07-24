package ru.ytimes.client.kkm.android.printer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.atol.drivers.fptr.Fptr;
import com.atol.drivers.fptr.IFptr;

import ru.ytimes.client.kkm.android.record.GuestRecord;
import ru.ytimes.client.kkm.android.record.GuestType;
import ru.ytimes.client.kkm.android.record.ItemRecord;
import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 17.06.17.
 */

public class AtolPrinter implements Printer {
    private static final String TAG = "YTIMES";

    private IFptr fptr = null;
    private Context context;
    private String lastSettings;

    public AtolPrinter(Context context) {
        this.context = context;
    }

    public String getDefaultSettings(Context context) {
        try{
            fptr = new Fptr();
            fptr.create(context);
        } catch (NullPointerException ex){
            fptr = null;
        }
        return fptr.get_DeviceSettings();
    }

    public void stop() {
        if (fptr != null) {
            fptr.destroy();
        }
    }

    public void reconnect() {
        if (lastSettings != null) {
            connect(context, lastSettings);
        }
    }

    public void connect(final Context application, final String settings) {
        lastSettings = settings;
        final AsyncTask<Void, String, Void> task = new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                fptr = new Fptr();
                try {
                    fptr.create(application);
                    publishProgress("Загрузка настроек", "");
                    if (fptr.put_DeviceSettings(settings) < 0) {
                        checkError(fptr);
                    }
                    publishProgress("Установка соединения...", "");
                    if (fptr.put_DeviceEnabled(true) < 0) {
                        checkError(fptr);
                    }
                    publishProgress("OK", "");
                    publishProgress("Проверка связи...", "");
                    if (fptr.GetStatus() < 0) {
                        checkError(fptr);
                    }
                    publishProgress("Связь установлена", "");
                } catch (Exception e) {
                    publishProgress(e.toString(), "true");
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
        task.execute();
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        context.sendBroadcast(local);
    }

    protected void checkError(IFptr fptr) throws PrinterException {
        checkError(fptr, true);
    }

    protected void checkError(IFptr fptr, boolean throwError) throws PrinterException {
        int rc = fptr.get_ResultCode();
        if (rc < 0) {
            String rd = fptr.get_ResultDescription(), bpd = null;
            if (rc == -6) {
                bpd = fptr.get_BadParamDescription();
            }
            if (throwError) {
                if (bpd != null) {
                    throw new PrinterException(rc, String.format("[%d] %s (%s)", rc, rd, bpd));
                } else {
                    throw new PrinterException(rc, String.format("[%d] %s", rc, rd));
                }
            }
        }
    }

    @Override
    synchronized public void reportZ() throws PrinterException {
        if (fptr.put_DeviceSingleSetting(IFptr.SETTING_USERPASSWORD, 30) < 0) {
            checkError(fptr);
        }
        if (fptr.ApplySingleSettings() < 0) {
            checkError(fptr);
        }
        if (fptr.put_Mode(IFptr.MODE_REPORT_CLEAR) < 0) {
            checkError(fptr);
        }
        if (fptr.SetMode() < 0) {
            checkError(fptr);
        }
        if (fptr.put_ReportType(IFptr.REPORT_Z) < 0) {
            checkError(fptr);
        }
        if (fptr.Report() < 0) {
            checkError(fptr);
        }
    }

    @Override
    synchronized public void reportX() throws PrinterException {
        if (fptr.put_DeviceSingleSetting(IFptr.SETTING_USERPASSWORD, 30) < 0)
            checkError(fptr);
        if (fptr.ApplySingleSettings() < 0)
            checkError(fptr);
        if (fptr.put_Mode(IFptr.MODE_REPORT_NO_CLEAR) < 0)
            checkError(fptr);
        if (fptr.SetMode() < 0)
            checkError(fptr);
        if (fptr.put_ReportType(IFptr.REPORT_X) < 0)
            checkError(fptr);
        if (fptr.Report() < 0)
            checkError(fptr);
    }

    @Override
    synchronized public void printCheck(PrintCheckCommandRecord record) throws PrinterException {
        checkRecord(record);

        if (fptr.put_DeviceSingleSetting(IFptr.SETTING_USERPASSWORD, 1) < 0)
            checkError(fptr);
        if (fptr.ApplySingleSettings() < 0)
            checkError(fptr);

        cancelCheck();

        if (record.userFIO != null && !record.userPosition.isEmpty()) {
            String fio = record.userFIO;
            if (record.userPosition != null && !record.userPosition.isEmpty()) {
                fio = record.userPosition + " " + fio;
            }
            setUserFIO(fio);
        }

        // Открываем чек продажи, попутно обработав превышение смены
        try {
            openCheck(IFptr.CHEQUE_TYPE_SELL);
        } catch (PrinterException e) {
            // Проверка на превышение смены
            if (fptr.get_ResultCode() == -3822) {
                reportZ();
                openCheck(IFptr.CHEQUE_TYPE_SELL);
            } else {
                throw e;
            }
        }


        for(ItemRecord r: record.itemList) {
            int discountType = IFptr.DISCOUNT_SUMM;
            double discountSum = r.discountSum != null ? r.discountSum : 0.0;

            if (r.discountPercent != null) {
                discountType = IFptr.DISCOUNT_PERCENT;
                discountSum = r.discountPercent;
            }

            int tax = r.taxNumber != null ? r.taxNumber : 1;

            registrationFZ54(r.name, r.price, r.quantity, discountType, discountSum, tax);
        }

        // Скидка на чек
        //discount(1, IFptr.DISCOUNT_PERCENT, IFptr.DESTINATION_CHECK);

        if (record.creditSum != null && record.creditSum > 0) {
            payment(record.creditSum, 1);   //1 по карте
        }

        if (record.moneySum != null && record.moneySum > 0) {
            payment(record.moneySum, 0);
        }

        if (record.phone != null && !record.phone.isEmpty()) {
            sendCheck(record.phone);
        } else if (record.email != null && !record.email.isEmpty()) {
            sendCheck(record.email);
        }

        // Закрываем чек
        closeCheck(0);
    }

    @Override
    synchronized public void printReturnCheck(PrintCheckCommandRecord record) throws PrinterException {
        checkRecord(record);

        if (fptr.put_DeviceSingleSetting(IFptr.SETTING_USERPASSWORD, 1) < 0)
            checkError(fptr);
        if (fptr.ApplySingleSettings() < 0)
            checkError(fptr);

        cancelCheck();

        if (record.userFIO != null && !record.userPosition.isEmpty()) {
            String fio = record.userFIO;
            if (record.userPosition != null && !record.userPosition.isEmpty()) {
                fio = record.userPosition + " " + fio;
            }
            setUserFIO(fio);
        }

        // Открываем чек продажи, попутно обработав превышение смены
        try {
            openCheck(IFptr.CHEQUE_TYPE_RETURN);
        } catch (PrinterException e) {
            // Проверка на превышение смены
            if (fptr.get_ResultCode() == -3822) {
                reportZ();
                openCheck(IFptr.CHEQUE_TYPE_RETURN);
            } else {
                throw e;
            }
        }


        for(ItemRecord r: record.itemList) {
            int discountType = IFptr.DISCOUNT_SUMM;
            double discountSum = r.discountSum != null ? r.discountSum : 0.0;

            if (r.discountPercent != null) {
                discountType = IFptr.DISCOUNT_PERCENT;
                discountSum = r.discountPercent;
            }

            int tax = r.taxNumber != null ? r.taxNumber : 1;

            registrationFZ54(r.name, r.price, r.quantity, discountType, discountSum, tax);
        }

        if (record.creditSum != null && record.creditSum > 0) {
            payment(record.creditSum, 1);   //1 по карте
        }

        if (record.moneySum != null && record.moneySum > 0) {
            payment(record.moneySum, 0);
        }

        // Закрываем чек
        closeCheck(0);
    }

    private void sendCheck(String address) throws PrinterException {
        if (fptr.put_FiscalPropertyNumber(1008) < 0) {
            checkError(fptr);
        }
        if (fptr.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING) < 0) {
            checkError(fptr);
        }
        if (fptr.put_FiscalPropertyValue(address) < 0) {
            checkError(fptr);
        }
        if (fptr.WriteFiscalProperty() < 0) {
            checkError(fptr);
        }
    }

    private void setUserFIO(String fio) throws PrinterException {
        if (fptr.put_FiscalPropertyNumber(1021) < 0) {
            checkError(fptr);
        }
        if (fptr.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING) < 0) {
            checkError(fptr);
        }
        if (fptr.put_FiscalPropertyValue(fio) < 0) {
            checkError(fptr);
        }
        if (fptr.WriteFiscalProperty() < 0) {
            checkError(fptr);
        }
    }

    //выставление счета
    synchronized public void printPredCheck(PrintCheckCommandRecord record) throws PrinterException {
        checkRecord(record);

        printText("СЧЕТ");
        printText("");
        printText("ПОЗИЦИИ ОПЛАТЫ", IFptr.ALIGNMENT_LEFT, IFptr.WRAP_LINE);
        for(int i = 0; i < record.itemList.size(); i++) {
            ItemRecord r = record.itemList.get(i);
            printText((i + 1) + ". " + r.name, IFptr.ALIGNMENT_LEFT, IFptr.WRAP_WORD);

            double total = r.price * r.quantity;
            printText(r.price + " x " + r.quantity + " = " + total, IFptr.ALIGNMENT_RIGHT, IFptr.WRAP_LINE);

            if (r.discountSum != null && r.discountSum > 0) {
                printText("Скидка: " + r.discountSum, IFptr.ALIGNMENT_RIGHT, IFptr.WRAP_LINE);
            }
            if (r.discountPercent != null && r.discountPercent > 0) {
                printText("Скидка: " + r.discountPercent + "%", IFptr.ALIGNMENT_RIGHT, IFptr.WRAP_LINE);
            }
        }
        printText("ИТОГО: " + record.moneySum, IFptr.ALIGNMENT_RIGHT, IFptr.WRAP_LINE);

        if (GuestType.TIME.equals(record.type) && record.guestInfoList != null) {
            printText("РАССЧИТЫВАЕМЫЕ ГОСТИ", IFptr.ALIGNMENT_LEFT, IFptr.WRAP_LINE);
            int i = 1;
            for(GuestRecord r: record.guestInfoList) {
                String name = r.name;
                if (r.card != null && !r.card.isEmpty()) {
                    name += " (" + r.card + ")";
                };
                printText(i + ". " + name, IFptr.ALIGNMENT_LEFT, IFptr.WRAP_LINE);
                printText("время прихода: " + r.startTime, IFptr.ALIGNMENT_RIGHT, IFptr.WRAP_LINE);
                printText("проведенное время: " + r.minutes + " мин.", IFptr.ALIGNMENT_RIGHT, IFptr.WRAP_LINE);
                i++;
            }

            printText("");
            printText("");
        }

        if (GuestType.TOGO.equals(record.type)  && record.guestInfoList != null) {
            for(GuestRecord r: record.guestInfoList) {
                String name = r.name;
                if (r.phone != null && !r.phone.isEmpty()) {
                    name += ", " + r.phone;
                }
                printText(name, IFptr.ALIGNMENT_CENTER, IFptr.WRAP_LINE);
                printText(r.message);
            }

            printText("");
            printText("");
        }

        printHeader();
    }

    private void cancelCheck() throws PrinterException {
        // Отменяем чек, если уже открыт. Ошибки "Неверный режим" и "Чек уже закрыт"
        // не являются ошибками, если мы хотим просто отменить чек
        try {
            if (fptr.CancelCheck() < 0)
                checkError(fptr, false);
        } catch (PrinterException e) {
            int rc = fptr.get_ResultCode();
            if (rc != -16 && rc != -3801)
                throw e;
        }
    }

    @Override
    synchronized public void printNewGuest(NewGuestCommandRecord record) throws PrinterException {
        printText(record.name);
        printText(record.startTime);
        if (record.barcodeNum != null && !record.barcodeNum.trim().isEmpty()) {
            printBarcode(IFptr.BARCODE_TYPE_CODE39, record.barcodeNum, 100);
        }
        printText("");
        printText("");

        printHeader();
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
    }

    private void registrationFZ54(String name, double price, double quantity, int discountType,
                                  double discount, int taxNumber) throws PrinterException {
        if (fptr.put_DiscountType(discountType) < 0)
            checkError(fptr);
        if (fptr.put_Summ(discount) < 0)
            checkError(fptr);
        if (fptr.put_TaxNumber(taxNumber) < 0)
            checkError(fptr);
        if (fptr.put_Quantity(quantity) < 0)
            checkError(fptr);
        if (fptr.put_Price(price) < 0)
            checkError(fptr);
        if (fptr.put_TextWrap(IFptr.WRAP_WORD) < 0)
            checkError(fptr);
        if (fptr.put_Name(name) < 0)
            checkError(fptr);
        if (fptr.Registration() < 0)
            checkError(fptr);
    }

    private void payment(double sum, int type) throws PrinterException {
        if (fptr.put_Summ(sum) < 0)
            checkError(fptr);
        if (fptr.put_TypeClose(type) < 0)
            checkError(fptr);
        if (fptr.Payment() < 0)
            checkError(fptr);
        System.out.println(String.format("Remainder: %.2f, Change: %.2f", fptr.get_Remainder(), fptr.get_Change()));
    }

    private void printText(String text, int alignment, int wrap) throws PrinterException {
        if (fptr.put_Caption(text) < 0)
            checkError(fptr);
        if (fptr.put_TextWrap(wrap) < 0)
            checkError(fptr);
        if (fptr.put_Alignment(alignment) < 0)
            checkError(fptr);
        if (fptr.PrintString() < 0)
            checkError(fptr);
    }

    private void printText(String text) throws PrinterException {
        printText(text, IFptr.ALIGNMENT_CENTER, IFptr.WRAP_LINE);
    }

    private void openCheck(int type) throws PrinterException {
        if (fptr.put_Mode(IFptr.MODE_REGISTRATION) < 0)
            checkError(fptr);
        if (fptr.SetMode() < 0)
            checkError(fptr);
        if (fptr.put_CheckType(type) < 0)
            checkError(fptr);
        if (fptr.OpenCheck() < 0)
            checkError(fptr);
    }

    private void closeCheck(int typeClose) throws PrinterException {
        if (fptr.put_TypeClose(typeClose) < 0)
            checkError(fptr);
        if (fptr.CloseCheck() < 0)
            checkError(fptr);
    }

    private void printFooter() throws PrinterException {
        if (fptr.put_Mode(IFptr.MODE_REPORT_NO_CLEAR) < 0)
            checkError(fptr);
        if (fptr.SetMode() < 0)
            checkError(fptr);
        if (fptr.PrintFooter() < 0)
            checkError(fptr);
    }

    private void printHeader() throws PrinterException {
        if (fptr.PrintHeader() < 0)
            checkError(fptr);
    }

    private void printBarcode(int type, String barcode, double scale) throws PrinterException {
        if (fptr.put_Alignment(IFptr.ALIGNMENT_CENTER) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeType(type) < 0)
            checkError(fptr);
        if (fptr.put_Barcode(barcode) < 0)
            checkError(fptr);
        if (fptr.put_Height(0) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeVersion(0) < 0)
            checkError(fptr);
        if (fptr.put_BarcodePrintType(IFptr.BARCODE_PRINTTYPE_AUTO) < 0)
            checkError(fptr);
        if (fptr.put_PrintBarcodeText(false) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeControlCode(true) < 0)
            checkError(fptr);
        if (fptr.put_Scale(scale) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeCorrection(0) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeColumns(3) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeRows(1) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeProportions(50) < 0)
            checkError(fptr);
        if (fptr.put_BarcodeUseProportions(true) < 0)
            checkError(fptr);
        if (fptr.put_BarcodePackingMode(IFptr.BARCODE_PDF417_PACKING_MODE_DEFAULT) < 0)
            checkError(fptr);
        if (fptr.put_BarcodePixelProportions(300) < 0)
            checkError(fptr);
        if (fptr.PrintBarcode() < 0)
            checkError(fptr);
    }


}
