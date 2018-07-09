package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Created by andrey on 25.05.18.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ConfigRecord {

    public String verificationCode;
    public String model = "ATOLAUTO";
    public String port = "BLUETOOTH";
    public String wifiIP;
    public Integer wifiPort = 5555;
    public VAT vat = VAT.NO;
    public OFDChannel ofd = OFDChannel.PROTO;
    public Map<String, String> params;


    //Kitchen
    public String kitchenPrinterModel = "NONE";
    public String kitchenPrinterIP = "192.168.0.253";
    public Integer kitchenPrinterPort = 6001;
    public Integer kitchenPrinterNumber = null;


    //for remote print check
    public String accountExternalId;
    public String accountExternalBaseUrl;

}
