package com.casc.rfidscanner.utils;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {

    private static List<Activity> activities = new ArrayList<>();

    private ActivityCollector(){}

    public static Activity getTopActivity() {
        return activities.isEmpty() ? null : activities.get(activities.size() - 1);
    }

    public static void addActivity(Activity activity) {
        if (activities == null) return;
        activities.add(activity);
    }

    public static void removeActivity(Activity activity) {
        if (activity == null) {
            return;
        }
        activities.remove(activity);
    }

    public static void removeActivityAndFinish(Activity activity) {
        if (activity == null) {
            return;
        }
        if (activities.remove(activity) && !activity.isFinishing()) {
            activity.finish();
        }
    }

    public static void finishAll() {
        try {
            for (Activity activity : activities) {
                if (!activity.isFinishing()) {
                    activity.finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static void backToLogin() {
        for (int i = activities.size() - 1; i > 0; i--) {
            if (!activities.get(i).isFinishing()) {
                activities.get(i).finish();
            }
        }
//        SharedPreferences defaultPref = activities.get(0).getSharedPreferences("default", Activity.MODE_PRIVATE);
//        SharedPreferences.Editor editor = defaultPref.edit();
//        editor.remove("phoneNumber");
//        editor.remove("password");
//        editor.apply();
//        LoginActivity.actionStart(activities.get(0));
//        activities.get(0).shutdown();
    }
}
