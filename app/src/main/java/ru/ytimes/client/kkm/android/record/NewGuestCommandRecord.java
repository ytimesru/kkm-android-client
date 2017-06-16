package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by root on 27.05.17.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class NewGuestCommandRecord extends AbstractCommandRecord {

    public String name;
    public String startTime;
    public String barcodeNum;

}
