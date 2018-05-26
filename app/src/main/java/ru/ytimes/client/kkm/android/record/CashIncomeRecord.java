package ru.ytimes.client.kkm.android.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by andrey on 19.10.17.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CashIncomeRecord extends AbstractCommandRecord {

    public Integer sum;

}

