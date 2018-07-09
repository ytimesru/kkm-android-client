package ru.ytimes.client.kkm.android.record;

import java.util.ArrayList;
import java.util.List;

/**
 * @User andrey on 28.05.16.
 */
public class ServerResult<T> {

    private boolean success;
    private Long count;
    private List<T> rows = new ArrayList<T>();
    private List<ErrorRecord> errors = new ArrayList<ErrorRecord>();

    public ServerResult() {
    }

    public ServerResult(boolean isSuccess) {
        success = isSuccess;
    }

    public ServerResult(T row) {
        success = true;
        rows.add(row);
    }

    public ServerResult(Exception e) {
        success = false;
        errors.add(new ErrorRecord(e.getClass().getSimpleName(), e.getMessage()));
    }

    public ServerResult(List<T> rows) {
        success = true;
        this.rows.addAll(rows);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public List<ErrorRecord> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorRecord> errors) {
        this.errors = errors;
    }

    public void addError(String errorMessage) {
        addError(new ErrorRecord(errorMessage));
    }

    public void addError(ErrorRecord error) {
        success = false;
        errors.add(error);
    }

    public Long getCount() {
        if (count == null && rows != null) {
            return 1L*rows.size();
        }
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
