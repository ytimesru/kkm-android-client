package ru.ytimes.client.records;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

import ru.ytimes.client.kkm.android.record.GuestRecord;
import ru.ytimes.client.kkm.android.record.ItemRecord;

/**
 * Created by andrey on 14.06.18.
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class ScreenInfoRecord {

    public String type;
    public List<GuestRecord> clientList;
    public List<ItemRecord> itemList;

}
