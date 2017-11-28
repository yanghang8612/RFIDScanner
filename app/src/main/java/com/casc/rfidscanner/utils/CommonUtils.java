package com.casc.rfidscanner.utils;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static String getIPAddress() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    public static boolean validatePhone(String phoneNumber) {
        String regExp = "^[1][0-9]{10}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(phoneNumber);
        return m.matches();
    }

    public static boolean validateEMail(String email) {
        String regExp = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    public static boolean validateVerificationCode(String verificationCode) {
        String regExp = "^[0-9]{6}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(verificationCode);
        return m.matches();
    }

    public static boolean validateInvitationCode(String verificationCode) {
        String regExp = "^[0-9A-Z]{6}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(verificationCode);
        return m.matches();
    }

    public static boolean validatePassword(String password) {
        String regExp = "^.{6,}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(password);
        return m.matches();
    }

    public static boolean validateNumber(String number) {
        String regExp = "^\\d+(?:\\.\\d+)?$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(number);
        return m.matches();
    }

    public static boolean validateYear(String year) {
        String regExp = "^\\d{4}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(year);
        return m.matches();
    }

    public static boolean validateChineseName(String name) {
        String regExp = "^[\\u4e00-\\u9fa5]{2,}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(name);
        return m.matches();
    }

    public static String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 1000000));
    }

    public static String generateRandomString(int length) {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        String temp = str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(19, 23) + str.substring(24);
        return temp.substring(0, length);
    }

    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 80, output);
        if (needRecycle) {
            bmp.recycle();
        }
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(validateNumber("100"));
        System.out.println(validateNumber("100.0"));
        System.out.println(validateNumber(".1"));
        System.out.println(validateNumber("0.1"));
        System.out.println(validateNumber("1.1.1"));
        System.out.println(validateNumber("1."));
        System.out.println(validateNumber("1..1"));
    }
}
