package com.casc.rfidscanner.backend;

/**
 * 上层逻辑接口
 */
public interface InsHandler {

    /**
     * 传感器信号
     *
     * @param isHigh true表示传感器IO高电平，false则表示低电平
     */
    void sensorSignal(boolean isHigh);

    /**
     * 响应帧/通知帧处理
     *
     * @param ins 返回帧（通知帧或响应帧）
     */
    void dealIns(byte[] ins);
}
