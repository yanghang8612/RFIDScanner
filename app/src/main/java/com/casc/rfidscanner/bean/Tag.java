package com.casc.rfidscanner.bean;

import android.content.ContentValues;

public class Tag {

    private Integer id; // INTEGER in SQLite, '_id', primary key autoincrement
    private String tid; // TEXT in SQLite
    private String rfid; // TEXT in SQLite
    private String link; // 所属环节, TEXT in SQLite
    private Double longitude; // 经度, REAL in SQLite
    private Double latitude; // 纬度, REAL in SQLite
    private String timestamp; // 时间戳, TEXT in SQLite (不支持Date、Time等类型)

    public Tag(String tid, String rfid, String link, Double longitude, Double latitude, String timestamp) {
        id = -1;
        this.tid = tid;
        this.rfid = rfid;
        this.link = link;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
    }

    public Tag(Integer id, String tid, String rfid, String link, Double longitude, Double latitude, String timestamp) {
        this.id = id;
        this.tid = tid;
        this.rfid = rfid;
        this.link = link;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;

        if (!id.equals(tag.id)) return false;
        if (tid != null ? !tid.equals(tag.tid) : tag.tid != null) return false;
        if (rfid != null ? !rfid.equals(tag.rfid) : tag.rfid != null) return false;
        if (link != null ? !link.equals(tag.link) : tag.link != null) return false;
        if (longitude != null ? !longitude.equals(tag.longitude) : tag.longitude != null)
            return false;
        if (latitude != null ? !latitude.equals(tag.latitude) : tag.latitude != null) return false;
        return timestamp != null ? timestamp.equals(tag.timestamp) : tag.timestamp == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (tid != null ? tid.hashCode() : 0);
        result = 31 * result + (rfid != null ? rfid.hashCode() : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    public ContentValues transfor2ContentValues() {
        ContentValues values = new ContentValues();
        if (id >= 0) values.put("_id", this.getId());
        values.put("tid", this.getTid());
        values.put("rfid", this.getRfid());
        values.put("link", this.getLink());
        values.put("longitude", this.getLongitude());
        values.put("latitude", this.getLatitude());
        values.put("timestamp", this.getTimestamp());
        return values;
    }
}
