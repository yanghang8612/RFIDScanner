package com.casc.rfidscanner.bean;

public class User {
    private Integer id; // INTEGER in SQLite, '_id', primary key autoincrement
    private String name; // TEXT in SQLite
    private String pwd; // TEXT in SQLite
    private String limit; // 用户权限, TEXT in SQLite

    public User(Integer id, String name, String pwd, String limit) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
        this.limit = limit;
    }

    public User(String name, String pwd, String limit) {
        this.name = name;
        this.pwd = pwd;
        this.limit = limit;
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

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!id.equals(user.id)) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        if (pwd != null ? !pwd.equals(user.pwd) : user.pwd != null) return false;
        return limit != null ? limit.equals(user.limit) : user.limit == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (pwd != null ? pwd.hashCode() : 0);
        result = 31 * result + (limit != null ? limit.hashCode() : 0);
        return result;
    }
}
