package com.casc.rfidscanner.backend;

/**
 * UL6+通信模块接口
 */
public interface TagReader {

    /**
     * Constants that indicate the current connection state for inner class function use
     */
    int STATE_NONE = 0;       // we're doing nothing
    int STATE_CONNECTING = 1;       // we're doing nothing
    int STATE_CONNECTED = 2;  // now connected to a remote device

    /**
     * 初始化读写器参数
     *
     */
    void initReader();

    /**
     * 设置读写器返回帧的解析Handler
     *
     */
    void setHandler(InstructionHandler handler);

    /**
     * 向读写器下发指令（仅发送指令，并不对指令进行校验）
     *
     * @param cmd 指令byte数组
     */
    void sendCommand(byte[] cmd);

    /**
     * 向读写器下发指定次数的指令（仅发送指令，并不对指令进行校验）
     *
     * @param cmd 指令byte数组
     * @param times 指令执行次数
     */
    void sendCommand(byte[] cmd, int times);

    /**
     * 检测读写器连接状态
     *
     * @return true：读写器连接正常；false：读写器连接异常
     */
    boolean isConnected();

    /**
     * 开始读写器工作
     *
     */
    void start();

    /**
     * 恢复读写器工作
     *
     */
    void resume();

    /**
     * 暂停读写器工作
     *
     */
    void pause();

    /**
     * 停止读写器工作
     *
     */
    void stop();
}

