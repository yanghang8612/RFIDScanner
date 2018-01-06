package com.casc.rfidscanner.backend;

/**
 * 上层逻辑接口
 */
public interface InstructionDeal {
    /**
     * 响应帧/通知帧处理
     *
     * @param ins
     */
    void callback(byte[] ins);

    /**
     * 连接建立时触发
     */
    void onConnectionStart();

    /**
     * 连接中断时触发
     */
    void onConnectionLost();
}
