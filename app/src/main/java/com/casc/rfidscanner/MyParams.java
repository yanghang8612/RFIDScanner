package com.casc.rfidscanner;

import java.util.HashMap;
import java.util.Map;

public class MyParams {

    private MyParams() {
    }

    public static final String SOFT_CODE = "A01";

    public static final String AppId = "10703509";
    public static final String AppKey = "208B855UXS46N1WBR5sNyGCb";
    public static final String AppSecret = "c50303647164111b552b168a8c6fa2c7";

    public static final int ADMIN_CARD_SCANNED_COUNT = 10; // 次数

    public static final int TID_START_INDEX = 0; // word
    public static final int TID_READ_LENGTH = 6; // word
    public static final int PC_START_INDEX = 1; // word
    public static final int EPC_START_INDEX = 2; // word

    public static final int EPC_HEADER_LENGTH = 4; // byte
    public static final int EPC_TYPE_INDEX = 4; // byte

    public static final int BODY_CODE_HEADER = 3; // char
    public static final int BODY_CODE_LENGTH = 8; // char
    public static final int EPC_BUCKET_LENGTH = 12; // byte
    public static final int EPC_DELIVERY_CARD_LENGTH = 12; // byte
    public static final int EPC_ADMIN_CARD_LENGTH = 12; // byte
    public static final int EPC_REFLUX_CARD_LENGTH = 12; // byte

    // 网络连接参数配置
    public static final int NET_CONNECT_TIMEOUT = 2; // s
    public static final int NET_RW_TIMEOUT = 2; // s
    public static final int CONFIG_UPDATE_INTERVAL = 5 * 1000; // ms
    public static final int INTERNET_STATUS_CHECK_INTERVAL = 990; // ms
    public static final int PLATFORM_STATUS_CHECK_INTERVAL = 3 * 1000; // ms
    // 运维人员配置
    public static final String S_LINK = "link"; // 工位
    public static final String S_SENSOR_SWITCH = "sensor_switch"; // 传感器检测开关
    public static final String S_RSSI_THRESHOLD = "rssi_threshold"; // 达标阈值
    public static final String S_MIN_REACH_TIMES = "min_reach_times"; // 最少达标次数
    public static final String S_POWER = "power"; // 发射功率
    public static final String S_Q_VALUE = "q_value"; // Q值
    public static final String S_TAG_LIFECYCLE = "tag_lifecycle"; // 标签生命周期
    public static final String S_DEVICE_ADDR = "device_cloud_addr"; // 设备运管云接口地址
    public static final String S_READER_ID = "reader_id"; // 读写器ID
    public static final String S_LONGITUDE = "longitude"; // 经度
    public static final String S_LATITUDE = "latitude"; // 纬度
    public static final String S_HEIGHT = "height"; // 高度
    public static final String S_API_JSON = "api_json";
    public static final String S_CONFIG_JSON = "config_json";
    public static final Map<String, String> CONFIG_DEFAULT_MAP = new HashMap<>();
    static {
        CONFIG_DEFAULT_MAP.put(S_LINK, "00");
        CONFIG_DEFAULT_MAP.put(S_SENSOR_SWITCH, "false");
        CONFIG_DEFAULT_MAP.put(S_RSSI_THRESHOLD, "-35dBm");
        CONFIG_DEFAULT_MAP.put(S_MIN_REACH_TIMES, "1");
        CONFIG_DEFAULT_MAP.put(S_POWER, "15dBm");
        CONFIG_DEFAULT_MAP.put(S_Q_VALUE, "0");
        CONFIG_DEFAULT_MAP.put(S_TAG_LIFECYCLE, "1Min");
        CONFIG_DEFAULT_MAP.put(S_DEVICE_ADDR, "http://59.252.101.154");
        CONFIG_DEFAULT_MAP.put(S_READER_ID, "4000000001");
        CONFIG_DEFAULT_MAP.put(S_LONGITUDE, "121.39");
        CONFIG_DEFAULT_MAP.put(S_LATITUDE, "37.52");
        CONFIG_DEFAULT_MAP.put(S_HEIGHT, "922.88");
        CONFIG_DEFAULT_MAP.put(S_API_JSON, "{}");
        CONFIG_DEFAULT_MAP.put(S_CONFIG_JSON, "{}");
    }

    public static final boolean ENABLE_BACKDOOR = true;
    public static final boolean PRINT_COMMAND = true;
    public static final boolean PRINT_JSON = true;
    public static final int STACK_WAIT_TIME = 3 * 1000;

}
