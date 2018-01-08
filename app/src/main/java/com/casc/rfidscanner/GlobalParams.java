package com.casc.rfidscanner;

public class GlobalParams {

    private GlobalParams() {
    }

    /**
     * BTServiceHelper
     */
    // Message types sent from the BTServiceHelper Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BTServiceHelper Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    /**
     * Setting Parameters
     */
    // 运维人员配置
    public static final String S_LINK = "link"; // 工位
    public static final String S_TAG_LIFECYCLE = "tag_lifecycle"; // 标签生命周期
    public static final String S_BLANK_INTERVAL = "blank_interval"; // 空白期间隔
    public static final String S_MAIN_PLATFORM_ADDR = "main_platform_addr"; // 主平台软件地址
    public static final String S_STANDBY_PLATFORM_ADDR = "standby_platform_addr"; // 备用平台软件地址
    public static final String S_READER_ID = "reader_id"; // 读写器ID
    public static final String S_READER_POWER = "reader_power"; // 读写器发射功率
    public static final String S_READER_QVALUE = "reader_qvalue"; // 读写器Q值
    public static final String S_READER_MAC = "reader_mac"; // 读写器蓝牙MAC地址
    public static final String S_LONGITUDE = "longitude"; // 经度
    public static final String S_LATITUDE = "latitude"; // 纬度
    public static final String S_HEIGHT = "height"; // 高度
    // 平台软件外部接口
    public static final String S_BUCKET_SPECINFO = "bucketspecinfo"; // 桶规格
    public static final String S_BUCKET_TYPE_INFO = "buckettypeinfo"; // 桶类型
    public static final String S_WATER_BRAND_INFO = "waterbrandinfo"; // 水品牌
    public static final String S_WATER_SPEC_INFO = "waterspecinfo"; // 水规格
    public static final String S_BUCKET_PRODUCER_INFO = "bucketproducerinfo"; // 桶生产方
    public static final String S_BUCKET_OWNER_INFO = "bucketownerinfo"; // 桶所有方
    public static final String S_BUCKET_USER_INFO = "bucketuserinfo"; // 桶合法使用方
    public static final String S_DEALER_INFO = "dealerinfo"; // 经销商
    public static final String S_DRIVER_INFO = "driverinfo"; // 司机

    /**
     * 读写器连接方式
     */
    public enum ReaderConnectionType {
        BT, USB, BOTH
    }

    /**
     * 工位
     */
    public enum LinkType {
        R0("桶注册", ReaderConnectionType.USB),
        R1("桶报废", ReaderConnectionType.USB),
        R2("水企空桶回流", ReaderConnectionType.BT),
        R3("桶筛选", ReaderConnectionType.USB),
        R4("成品注册", ReaderConnectionType.USB),
        R5("打垛", ReaderConnectionType.BOTH),
        R6("水企成品出库", ReaderConnectionType.BT),
        R10("经销商成品入库", ReaderConnectionType.USB),
        R11("经销商成品出库", ReaderConnectionType.USB),
        R12("经销商空桶入库", ReaderConnectionType.USB),
        R13("经销商空桶出库", ReaderConnectionType.USB);

        //        private String index;
        private String vaule;
        private ReaderConnectionType readerConnectionType;

        LinkType(String value, ReaderConnectionType readerConnectionType) {
//            this.index = index;
            this.vaule = value;
            this.readerConnectionType = readerConnectionType;
        }

        public String getVaule() {
            return vaule;
        }

        public ReaderConnectionType getReaderConnectionType() {
            return readerConnectionType;
        }
    }

}
