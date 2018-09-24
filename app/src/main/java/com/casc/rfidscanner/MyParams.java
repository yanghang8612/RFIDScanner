package com.casc.rfidscanner;

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
    public static final int ADMIN_CARD_SCANNED_COUNT = 10; // 次数
    public static final int BILL_NO_OPERATION_CHECK_INTERVAL = 5 * 60 * 1000; // ms

    public static final int TID_START_INDEX = 0; // word
    public static final int TID_READ_LENGTH = 6; // word
    public static final int TID_LENGTH = 12; // byte
    public static final int PC_START_INDEX = 1; // word
    public static final int PC_LENGTH = 1; // word
    public static final int USER_MEMORY_START_INDEX = 0; // word
    public static final int USER_MEMORY_LENGTH = 12; // word
    public static final int EPC_HEADER_LENGTH = 4; // byte
    public static final int EPC_TYPE_INDEX = 4; // byte

    public static final int EPC_START_INDEX = 2; // word
    public static final int EPC_BUCKET_LENGTH = 12; // byte
    public static final int EPC_DELIVERY_CARD_LENGTH = 12; // byte
    public static final int EPC_ADMIN_CARD_LENGTH = 12; // byte
    public static final int EPC_REFLUX_CARD_LENGTH = 12; // byte

    public static final int BODY_CODE_HEADER_LENGTH = 3;
    public static final int BODY_CODE_CONTENT_LENGTH = 5;
    public static final int BODY_CODE_LENGTH = 8;

    /**
     * Setting Parameters
     */
    // APP参数配置
    public static final int HEARTBEAT_TIMEOUT = 2000; // ms
    public static final int NET_CONNECT_TIMEOUT = 5; // s
    public static final int NET_RW_TIMEOUT = 5; // s
    public static final int CONFIG_UPDATE_INTERVAL = 60; // s
    public static final int INTERNET_STATUS_CHECK_INTERVAL = 990; // ms
    public static final int PLATFORM_STATUS_CHECK_INTERVAL = 5000; // ms
    // 运维人员配置
    public static final String S_LINK = "link"; // 工位
    public static final String S_SENSOR_SWITCH = "sensor_switch"; // 传感器检测开关
    public static final String S_RSSI_THRESHOLD = "rssi_threshold"; // 发射功率
    public static final String S_MIN_REACH_TIMES = "min_reach_times"; // 发射功率
    public static final String S_POWER = "power"; // 发射功率
    public static final String S_Q_VALUE = "q_value"; // Q值
    public static final String S_REST = "rest"; // 占空时间
    public static final String S_INTERVAL = "interval"; // 轮询指令发送间隔
    public static final String S_TIME = "time"; // 单次任务轮询指令发送次数
    public static final String S_TAG_LIFECYCLE = "tag_lifecycle"; // 标签生命周期
    public static final String S_BLANK_INTERVAL = "blank_interval"; // 空白期间隔
    public static final String S_DISCOVERY_INTERVAL = "discovery_interval"; // 最小时间间隔
    public static final String S_MAIN_PLATFORM_ADDR = "main_platform_addr"; // 主平台软件地址
    public static final String S_MONITOR_APP_ADDR = "standby_platform_addr"; // 监控APP地址
    public static final String S_READER_ID = "reader_id"; // 读写器ID
    public static final String S_READER_MAC = "reader_mac"; // 读写器蓝牙MAC地址
    public static final String S_LONGITUDE = "longitude"; // 经度
    public static final String S_LATITUDE = "latitude"; // 纬度
    public static final String S_HEIGHT = "height"; // 高度
    public static final String S_COUNTER_HISTORY = "counter_history";
    public static final String S_DRIVER_HISTORY = "driver_history";
    public static final String S_LINE_NAME = "line_name";
    // 平台软件外部接口，统一使用一个json字符串存储
    public static final String S_API_JSON = "api_json";
    public static final Map<String, String> CONFIG_DEFAULT_MAP = new HashMap<>();
    static {
        CONFIG_DEFAULT_MAP.put(S_LINK, "00");
        CONFIG_DEFAULT_MAP.put(S_SENSOR_SWITCH, "false");
        CONFIG_DEFAULT_MAP.put(S_RSSI_THRESHOLD, "-30dBm");
        CONFIG_DEFAULT_MAP.put(S_MIN_REACH_TIMES, "1");
        CONFIG_DEFAULT_MAP.put(S_POWER, "15dBm");
        CONFIG_DEFAULT_MAP.put(S_Q_VALUE, "0");
        CONFIG_DEFAULT_MAP.put(S_REST, "20ms");
        CONFIG_DEFAULT_MAP.put(S_INTERVAL, "7ms");
        CONFIG_DEFAULT_MAP.put(S_TIME, "20");
        CONFIG_DEFAULT_MAP.put(S_TAG_LIFECYCLE, "5Min");
        CONFIG_DEFAULT_MAP.put(S_BLANK_INTERVAL, "5");
        CONFIG_DEFAULT_MAP.put(S_DISCOVERY_INTERVAL, "2Sec");
        CONFIG_DEFAULT_MAP.put(S_MAIN_PLATFORM_ADDR, "http://59.252.100.114");
        CONFIG_DEFAULT_MAP.put(S_MONITOR_APP_ADDR, "http://192.168.1.8:8888");
        CONFIG_DEFAULT_MAP.put(S_READER_ID, "100000000000000000000001");
        CONFIG_DEFAULT_MAP.put(S_READER_MAC, "00:00:00:00:00:00");
        CONFIG_DEFAULT_MAP.put(S_LONGITUDE, "121.39");
        CONFIG_DEFAULT_MAP.put(S_LATITUDE, "37.52");
        CONFIG_DEFAULT_MAP.put(S_HEIGHT, "922.88");
        CONFIG_DEFAULT_MAP.put(S_API_JSON, "{}");
        CONFIG_DEFAULT_MAP.put(S_COUNTER_HISTORY, "");
        CONFIG_DEFAULT_MAP.put(S_DRIVER_HISTORY, "");
        CONFIG_DEFAULT_MAP.put(S_LINE_NAME, "");
    }

    /**
     * Parameters for testing
     */
//    public static final int DELAY = 2 * 60 * 60;
    public static final int DELAY = 0;
    public static final boolean ENABLE_BACKDOOR = true;
    public static final boolean PRINT_COMMAND = true;
    public static final boolean PRINT_JSON = true;

    /**
     * EPC各种类型
     */
    public enum EPCType {
        NONE((byte) 0xFF, "(UNKOWN)"),
        BUCKET((byte) 0x00, "(已注册桶)"),
        BUCKET_SCRAPED((byte) 0x04, "(报废桶)"),
        CARD_DELIVERY((byte) 0x01, "(出库专用卡)"),
        CARD_ADMIN((byte) 0x02, "(运维专用卡)"),
        CARD_REFLUX((byte) 0x03, "(运维专用卡)");

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
