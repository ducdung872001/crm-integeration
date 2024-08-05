package com.tntexfinance.crm.integration.reborn;

import java.util.HashMap;
import java.util.Map;

public abstract class ExcelRow {
    private Integer row;
    private Map<String, String> keyValue = new HashMap<>();

    public void addValue(String key, String value) {
        this.keyValue.put(key, value);
    }

    private Map<String, String> getKeyValue() {
        return keyValue;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }
}