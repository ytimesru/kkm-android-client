package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Created by andrey on 25.05.18.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ConfigRecord {

    public String verificationCode;
    public String model;
    public String port;
    public String wifiIP;
    public Integer wifiPort;
    public VAT vat;
    public OFDChannel ofd;
    public Map<String, String> params;


}
