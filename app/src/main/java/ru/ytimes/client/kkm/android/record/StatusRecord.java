package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class StatusRecord {

    public String version;
    public Boolean isConnected = false;
    public String lastError;
    public ModelInfoRecord info;
    public ConfigRecord config;

}
