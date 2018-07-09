package ru.ytimes.client.kkm.android.record;

public class ErrorRecord {

    private String type;
    private String message;

    public ErrorRecord() {
    }

    public ErrorRecord(String message) {
        this.message = message;
    }

    public ErrorRecord(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
