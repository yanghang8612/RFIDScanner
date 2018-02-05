package com.casc.rfidscanner.backend;

/**
 * 上层逻辑接口
 */
public interface InstructionHandler {

    /**
     * 响应帧/通知帧处理
     *
     * @param ins 返回帧（通知帧或响应帧）
     */
    void deal(byte[] ins);
}
