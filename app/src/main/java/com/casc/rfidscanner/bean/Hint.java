package com.casc.rfidscanner.bean;

public class Hint {

    // 产生时间
    private long time;

    // 提示信息
    private String content;

    public Hint(String content) {
        this.time = System.currentTimeMillis();
        this.content = content;
    }

    public long getTime() {

        return time;
    }

    public String getContent() {
        return content;
    }
}
