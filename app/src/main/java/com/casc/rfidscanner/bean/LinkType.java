package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.fragment.CardFragment;
import com.casc.rfidscanner.fragment.R0Fragment;
import com.casc.rfidscanner.fragment.R1Fragment;
import com.casc.rfidscanner.fragment.R2Fragment;
import com.casc.rfidscanner.fragment.R2MonitorFragment;
import com.casc.rfidscanner.fragment.R3Fragment;
import com.casc.rfidscanner.fragment.R4Fragment;
import com.casc.rfidscanner.fragment.R6Fragment;
import com.casc.rfidscanner.fragment.R6MonitorFragment;
import com.casc.rfidscanner.fragment.RNFragment;
import com.casc.rfidscanner.helper.ConfigHelper;

public enum LinkType {
    R0("00", true, R0Fragment.class, "桶注册"),
    R1("01", true, R1Fragment.class, "桶报废"),
    R2("02", true, R2Fragment.class, "空桶回流"),
    M2("M2", false, R2MonitorFragment.class, "回流监控"),
    R3("03", true, R3Fragment.class, "桶筛选"),
    R4("04", true, R4Fragment.class, "成品注册"),
    R6("06", true, R6Fragment.class, "成品出库"),
    M6("M6", false, R6MonitorFragment.class, "出库监控"),
    RN("0N", true, RNFragment.class, "经销商库存管理"),
    Card("Card", true, CardFragment.class, "专用卡注册");

    public final String link;
    public final boolean isNeedReader;
    public final Class fragmentClass;
    public final String comment;

    LinkType(String link, boolean isNeedReader, Class fragmentClass, String comment) {
        this.link = link;
        this.isNeedReader = isNeedReader;
        this.fragmentClass = fragmentClass;
        this.comment = comment;
    }

    public static LinkType getType() {
        return getType(ConfigHelper.getParam(MyParams.S_LINK));
    }

    public static LinkType getType(String link) {
        for (LinkType type : LinkType.values()) {
            if (link.equals(type.link))
                return type;
        }
        throw new IllegalArgumentException("No matched link type");
    }

    public static LinkType getTypeByComment(String comment) {
        for (LinkType type : LinkType.values()) {
            if (comment.equals(type.comment))
                return type;
        }
        throw new IllegalArgumentException("No matched link type by comment");
    }
}
