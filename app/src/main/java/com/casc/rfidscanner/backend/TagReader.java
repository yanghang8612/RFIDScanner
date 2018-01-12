package com.casc.rfidscanner.backend;

import android.content.Context;

/**
 * UL6+通信模块接口
 */
public interface TagReader {

    /**
     * Constants that indicate the current connection state for inner class function use
     */
    int STATE_NONE = 0;       // we're doing nothing
    int STATE_LISTEN = 1;     // now listening for incoming connections
    int STATE_CONNECTING = 2; // now initiating an outgoing connection
    int STATE_CONNECTED = 3;  // now connected to a remote device

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

    /**
     * 终止连接
     */
    void stop();
}

