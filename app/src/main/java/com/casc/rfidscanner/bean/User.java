package com.casc.rfidscanner.bean;

import android.content.ContentValues;
import android.database.Cursor;

public class User {
    private Integer id; // INTEGER in SQLite, '_id', primary key autoincrement
    private String name; // TEXT in SQLite
    private String pwd; // TEXT in SQLite
    private String type; // 用户权限, TEXT in SQLite

    public User(Integer id, String name, String pwd, String type) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
        this.type = type;
    }

    public User(String name, String pwd, String type) {
        id = -1;
        this.name = name;
        this.pwd = pwd;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!id.equals(user.id)) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        if (pwd != null ? !pwd.equals(user.pwd) : user.pwd != null) return false;
        return type != null ? type.equals(user.type) : user.type == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (pwd != null ? pwd.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public ContentValues transfor2ContentValues() {
        ContentValues values = new ContentValues();
        if (id != null && id >= 0) values.put("_id", this.getId());
        values.put("name", this.getName());
        values.put("pwd", this.getPwd());
        values.put("type", this.getType());
        return values;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public static User transforByCursor(Cursor c) {
        int id = c.getInt(c.getColumnIndex("_id")); // SQLite feature
        String name = c.getString(c.getColumnIndex("name"));
        String pwd = c.getString(c.getColumnIndex("pwd"));
        String type = c.getString(c.getColumnIndex("type"));

        return new User(id, name, pwd, type);
    }
}
