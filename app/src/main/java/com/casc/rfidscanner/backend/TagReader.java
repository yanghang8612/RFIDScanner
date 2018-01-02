package com.casc.rfidscanner.backend;

/**
 * UL6+通信模块接口
 */

public interface TagReader {

    /**
     * 初始化读写器
     *
     * @return
     */
    boolean initReader();

    /**
     * 向读写器下发指令，返回对应的返回帧，同步阻塞
     *
     * @param cmd
     * @return
     */
    byte[] sendCommand(byte[] cmd);

    /**
     * 检测读写器连接状态
     *
     * @return
     */
    boolean isConnected();
}

