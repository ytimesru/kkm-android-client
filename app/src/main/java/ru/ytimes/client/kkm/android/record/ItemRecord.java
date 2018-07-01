package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by root on 27.05.17.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ItemRecord {

    public String name;
    public Double price;
    public Double quantity;

    //задавать что-то одно из этого
    public Double discountSum;
    public Double discountPercent;

    public VAT vatValue;

    public Integer kitchenNum;
}
