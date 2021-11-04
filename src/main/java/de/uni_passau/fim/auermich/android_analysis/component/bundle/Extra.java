package de.uni_passau.fim.auermich.android_analysis.component.bundle;

public class Extra {

    private final String key;
    private String valueType;

    public Extra(String key, String valueType) {
        this.key = key;
        this.valueType = valueType;
    }

    public String getKey() {
        return key;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String newType) {
        valueType = newType;
    }
}
