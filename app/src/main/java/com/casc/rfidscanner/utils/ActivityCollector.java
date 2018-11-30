package com.casc.rfidscanner.utils;

import android.app.Activity;

import com.casc.rfidscanner.activity.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {

    private static List<BaseActivity> activities = new ArrayList<>();

    private ActivityCollector(){}

    public static Activity getTopActivity() {
        return activities.get(activities.size() - 1);
    }

    public static void addActivity(BaseActivity activity) {
        activities.add(activity);
    }

    public static void removeActivity(BaseActivity activity) {
        activities.remove(activity);
    }

    public static void removeActivityAndFinish(BaseActivity activity) {
        if (activity != null && !activity.isFinishing()) {
            activities.remove(activity);
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
}
