package com.casc.rfidscanner.message;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.utils.CommonUtils;

public class PollingResultMessage {

    public boolean isRead;

    public int rssi;

    public byte[] epc;

    public PollingResultMessage() {
        this.isRead = false;
    }

    public PollingResultMessage(String epc) {
        this(CommonUtils.hexToBytes(epc));
    }

    public PollingResultMessage(byte[] epc) {
        this((byte) 0, epc);
    }

    public PollingResultMessage(byte rssi, byte[] epc) {
        this.isRead = true;
        this.rssi = (int) rssi + ConfigHelper.getInt(MyParams.S_POWER);
        this.epc = epc;
    }

}
