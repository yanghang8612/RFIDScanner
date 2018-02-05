package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.fragment.CardFragment;
import com.casc.rfidscanner.fragment.R0Fragment;
import com.casc.rfidscanner.fragment.R1Fragment;
import com.casc.rfidscanner.fragment.R2Fragment;
import com.casc.rfidscanner.fragment.R3Fragment;
import com.casc.rfidscanner.fragment.R4Fragment;
import com.casc.rfidscanner.fragment.R6Fragment;
import com.casc.rfidscanner.helper.ConfigHelper;

public enum LinkType {
    R0("00", R0Fragment.class, "桶注册"),
    R1("01", R1Fragment.class, "桶报废"),
    R2("02", R2Fragment.class, "空桶回流"),
    R3("03", R3Fragment.class, "桶筛选"),
    R4("04", R4Fragment.class, "成品注册"),
    R6("06", R6Fragment.class, "成品出库"),
    Card("Card", CardFragment.class, "专用卡注册");

    public final String link;
    public final String power;
    public final String qValue;
    public final String sendInterval;
    public final Class fragmentClass;
    public final String comment;

    LinkType(String link, Class fragmentClass, String comment) {
        this.link = link;
        this.power = link + "_power";
        this.qValue = link + "_q_value";
        this.sendInterval = link + "_send_interval";
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

    public static int getPower() {
        return Integer.valueOf(ConfigHelper.getParam(getType().power).replace("dBm", ""));
    }

    public static int getSendInterval() {
        return Integer.valueOf(ConfigHelper.getParam(getType().sendInterval).replace("ms", ""));
    }
}
