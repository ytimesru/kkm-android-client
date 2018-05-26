package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by andrey on 30.05.17.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GuestRecord {

    public String name;
    public String card;
    public String phone;
    public String startTime;
    public Integer minutes;
    public String message;

}
