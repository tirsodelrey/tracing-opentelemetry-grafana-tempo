package bigdata.zylk.net.tracing.pojo;

public class SensorRecord {
    private String key;
    private String value;

    public SensorRecord() {}  //

    public SensorRecord(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Getters y setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

