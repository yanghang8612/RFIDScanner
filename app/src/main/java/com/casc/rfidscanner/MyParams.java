package com.casc.rfidscanner;

import com.casc.rfidscanner.bean.LinkType;

import java.util.HashMap;
import java.util.Map;

public class MyParams {

    private MyParams() {
    }

    /**
     * AppKey for speech
     */
    public static final String AppId = "10703509";
    public static final String AppKey = "208B855UXS46N1WBR5sNyGCb";
    public static final String AppSecret = "c50303647164111b552b168a8c6fa2c7";

    /**
     * Global Parameters
     */
    public static final String API_VERSION = "1.4";
    public static final int SELECT_MAX_TRY_COUNT = 1; // 次数
    public static final int READ_TID_MAX_TRY_COUNT = 3; // 次数
    public static final int READ_TID_MAX_WAIT_TIME = 3000; // ms
    public static final int SINGLE_CART_MIN_SCANNED_COUNT = 10; // 次数
    public static final int ADMIN_CARD_SCANNED_COUNT = 5; // 次数
    public static final int BILL_NO_OPERATION_CHECK_INTERVAL = 5 * 60 * 1000; // ms
    public static final int PRODUCT_LIST_MAX_COUNT = 1000;

    public static final int TID_START_INDEX = 0; // word
    public static final int TID_LENGTH = 6; // word
    public static final int EPC_TYPE_INDEX = 5; // byte
    public static final int EPC_BUCKET_LENGTH = 16; // byte
    public static final String BUCKET_PC_CONTENT = "4000";
    public static final int EPC_DELIVERY_CARD_LENGTH = 30; // byte
    public static final String DELIVERY_PC_CONTENT = "7800";
    public static final int EPC_ADMIN_CARD_LENGTH = 12; // byte
    public static final String ADMIN_PC_CONTENT = "3000";
    public static final int EPC_REFLUX_CARD_LENGTH = 12; // byte
    public static final String REFLUX_PC_CONTENT = "3000";


    /**
     * Setting Parameters
     */
    // APP参数配置
    public static final int CONFIG_UPDATE_INTERVAL = 3; // s
    public static final int CHECK_STATUS_INTERVAL = 990; // ms
    // 运维人员配置
    public static final String S_LINK = "link"; // 工位
    public static final String S_TAG_LIFECYCLE = "tag_lifecycle"; // 标签生命周期
    public static final String S_BLANK_INTERVAL = "blank_interval"; // 空白期间隔
    public static final String S_MAIN_PLATFORM_ADDR = "main_platform_addr"; // 主平台软件地址
    public static final String S_STANDBY_PLATFORM_ADDR = "standby_platform_addr"; // 备用平台软件地址
    public static final String S_READER_ID = "reader_id"; // 读写器ID
    public static final String S_READER_MAC = "reader_mac"; // 读写器蓝牙MAC地址
    public static final String S_LONGITUDE = "longitude"; // 经度
    public static final String S_LATITUDE = "latitude"; // 纬度
    public static final String S_HEIGHT = "height"; // 高度
    // 平台软件外部接口，统一使用一个json字符串存储
    public static final String S_API_JSON = "api_json";
    public static final Map<String, String> CONFIG_DEFAULT_MAP = new HashMap<>();
    static {
        CONFIG_DEFAULT_MAP.put(S_API_JSON, "{}");CONFIG_DEFAULT_MAP.put(S_LINK, "00");
        CONFIG_DEFAULT_MAP.put(S_TAG_LIFECYCLE, "5Min");CONFIG_DEFAULT_MAP.put(S_BLANK_INTERVAL, "5");
        CONFIG_DEFAULT_MAP.put(S_MAIN_PLATFORM_ADDR, "http://106.37.201.142");
        CONFIG_DEFAULT_MAP.put(S_STANDBY_PLATFORM_ADDR, "http://106.37.201.142:8888");
        CONFIG_DEFAULT_MAP.put(S_READER_ID, "100000000000000000000001");
        CONFIG_DEFAULT_MAP.put(LinkType.R0.power, "15dBm");CONFIG_DEFAULT_MAP.put(LinkType.R0.qValue, "0");CONFIG_DEFAULT_MAP.put(LinkType.R0.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.R1.power, "15dBm");CONFIG_DEFAULT_MAP.put(LinkType.R1.qValue, "0");CONFIG_DEFAULT_MAP.put(LinkType.R1.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.R2.power, "26dBm");CONFIG_DEFAULT_MAP.put(LinkType.R2.qValue, "8");CONFIG_DEFAULT_MAP.put(LinkType.R2.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.R3.power, "15dBm");CONFIG_DEFAULT_MAP.put(LinkType.R3.qValue, "0");CONFIG_DEFAULT_MAP.put(LinkType.R3.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.R4.power, "15dBm");CONFIG_DEFAULT_MAP.put(LinkType.R4.qValue, "0");CONFIG_DEFAULT_MAP.put(LinkType.R4.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.R6.power, "26dBm");CONFIG_DEFAULT_MAP.put(LinkType.R6.qValue, "8");CONFIG_DEFAULT_MAP.put(LinkType.R6.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.RN.power, "15dBm");CONFIG_DEFAULT_MAP.put(LinkType.RN.qValue, "0");CONFIG_DEFAULT_MAP.put(LinkType.RN.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(LinkType.Card.power, "15dBm");CONFIG_DEFAULT_MAP.put(LinkType.Card.qValue, "0");CONFIG_DEFAULT_MAP.put(LinkType.Card.sendInterval, "50ms");
        CONFIG_DEFAULT_MAP.put(S_READER_MAC, "00:00:00:00:00:00");CONFIG_DEFAULT_MAP.put(S_LONGITUDE, "121.39");
        CONFIG_DEFAULT_MAP.put(S_LATITUDE, "37.52");CONFIG_DEFAULT_MAP.put(S_HEIGHT, "922.88");
    }

    /**
     * Parameters for testing
     */
    public static final String TEST_SERVER_ADDR = "http://192.168.1.11:8080/";

    /**
     * EPC各种类型
     */
    public enum EPCType {
        NONE((byte) 0xFF, ""),
        BUCKET((byte) 0x00, "(已注册桶)"),
        CARD_DELIVERY((byte) 0x01, "(出库专用卡)"),
        CARD_ADMIN((byte) 0x02, "(运维专用卡)"),
        CARD_REFLUX((byte) 0x03, "(回流专用卡)");

        private byte code;
        private String comment;

        EPCType(byte code, String comment) {
            this.code = code;
            this.comment = comment;
        }

        public byte getCode() {
            return code;
        }

        public String getComment() {
            return comment;
        }
    }
}
