package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by root on 27.05.17.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ActionRecord {

    public String code;
    public String action;
    public String data;

}
