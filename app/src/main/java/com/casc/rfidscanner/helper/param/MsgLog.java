package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.utils.CommonUtils;

public class MsgLog {

    private String stage;

    private String time;

    private String reader_id;

    private String content;

    public MsgLog(String content) {
        this.stage = MyVars.config.getCompanySymbol() + "-" + LinkType.getType().comment;
        this.time = CommonUtils.convertDateTime(System.currentTimeMillis());
        this.reader_id = ConfigHelper.getString(MyParams.S_READER_ID);
        this.content = content;
    }
}
