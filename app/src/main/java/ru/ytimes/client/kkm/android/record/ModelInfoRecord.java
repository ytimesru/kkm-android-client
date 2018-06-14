package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by andrey on 26.05.18.
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class ModelInfoRecord {

    public String serialNumber;
    public String modelName;
    public String unitVersion;

    public String deviceFfdVersion;
    public String fnFfdVersion;
    public String ffdVersion;

    public String ofdName;
    public Long ofdUnsentCount;
    public String ofdUnsentDatetime;

    public String fnSerial;
    public String fnVersion;
    public String fnDate;

}
