package ru.ytimes.client.kkm.android.printer;

import android.app.Application;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.TextView;

import com.atol.drivers.fptr.Fptr;
import com.atol.drivers.fptr.IFptr;

import ru.ytimes.client.kkm.android.record.NewGuestCommandRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;

/**
 * Created by andrey on 17.06.17.
 */

public class AtolPrinter implements Printer {

    private IFptr fptr = null;
    private TextView statusView;

    public AtolPrinter(Application application) {
        try{
            fptr = new Fptr();
            fptr.create(application);
        } catch (NullPointerException ex){
            fptr = null;
        }
    }

    public String getDefaultSettings() {
        return fptr.get_DeviceSettings();
    }

    public void connect(final Application application, final String settings, final TextView statusView) {
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
                setStatus(values[0], values[1]);
            }

            protected void setStatus(String status, String isError) {
                statusView.setText(status);
                if ("true".equals(isError)) {
                    statusView.setTextColor(Color.parseColor("#ffcc00"));
                }
                else {
                    statusView.setTextColor(Color.parseColor("#ff6699"));
                }
            }

        };
        task.execute();
    }

    protected void checkError(IFptr fptr) {
        int rc = fptr.get_ResultCode();
        if (rc < 0) {
            String rd = fptr.get_ResultDescription(), bpd = null;
            if (rc == -6) {
                bpd = fptr.get_BadParamDescription();
            }
            if (bpd != null) {
                throw new IllegalStateException(String.format("[%d] %s (%s)", rc, rd, bpd));
            } else {
                throw new IllegalStateException(String.format("[%d] %s", rc, rd));
            }
        }
    }

    @Override
    public void reportZ() throws PrinterException {
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
    public void reportX() throws PrinterException {
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
    public void printCheck(PrintCheckCommandRecord record) throws PrinterException {

    }

    @Override
    public void printNewGuest(NewGuestCommandRecord record) throws PrinterException {

    }

}
