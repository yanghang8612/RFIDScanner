package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

public class MsgLog {

    public static MsgLog info(String content) {
        return new MsgLog(0, content);
    }

    public static MsgLog warn(String content) {
        return new MsgLog(1, content);
    }

    public static MsgLog error(String content) {
        return new MsgLog(2, content);
    }

    @SerializedName("reader_id")
    private String readerID;

    @SerializedName("log_time")
    private long time;

    @SerializedName("log_level")
    private int level;

    @SerializedName("log_content")
    private String content;

    public MsgLog(int level, String content) {
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.time = System.currentTimeMillis() / 1000;
        this.level = level;
        this.content = content;
    }

    public MsgLog append(String content) {
        this.content += content;
        return this;
    }
}
