package com.casc.rfidscanner.helper.param;

public class MsgTask {

    private String taskid;

    private long time;

    public MsgTask(String taskID) {
        this.taskid = taskID;
        this.time = System.currentTimeMillis();
    }
}
