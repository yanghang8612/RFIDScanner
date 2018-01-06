package com.casc.rfidscanner.backend;

import android.content.Context;

/**
 * UL6+通信模块接口
 */
public interface TagReader {

    /**
     * 连接读写器
     *
     * @param context
     * @return
     * @throws Exception
     */
    boolean connectReader(Context context) throws Exception;

    /**
     * 初始化读写器参数
     *
     * @param context
     * @return
     * @throws Exception
     */
    boolean initReader(Context context) throws Exception;

    /**
     * 向读写器下发指令（仅发送指令，并不对指令进行校验）
     *
     * @param cmd
     */
    void sendCommand(byte[] cmd);

    /**
     * 检测读写器连接状态
     *
     * @return
     */
    boolean isConnected();
}

