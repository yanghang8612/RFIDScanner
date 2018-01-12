package com.casc.rfidscanner.backend.impl;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import com.casc.rfidscanner.GlobalParams;
import com.casc.rfidscanner.backend.InstructionDeal;
import com.casc.rfidscanner.backend.TagReader;

import java.io.IOException;
import java.sql.Wrapper;

/**
 *
 */
public class TagReaderImpl implements TagReader {
    private static final String TAG = TagReaderImpl.class.getSimpleName();

    private TagReader tagReader;

    private GlobalParams.LinkType link; // 工位 软件所在的工作环节

    public TagReaderImpl(ContextWrapper context, GlobalParams.LinkType link, InstructionDeal instructionDeal) {
        this.link = link;

        if (link.getReaderConnectionType().equals(GlobalParams.ReaderConnectionType.BT)) {
            tagReader = new BTReaderImpl(context, instructionDeal);
        } else {
            tagReader = new USBReaderImpl(context,instructionDeal);
        }
    }

    public GlobalParams.LinkType getLink() {
        return link;
    }

    public void setLink(GlobalParams.LinkType link) {
        this.link = link;
    }

    @Override
    public boolean connectReader(Context context) throws Exception {
        tagReader.connectReader(context);
        return false;
    }

    @Override
    public boolean initReader(Context context) {
        boolean result = false;
        try {
            result = tagReader.initReader(context);
        } catch (Exception e) {
            Log.e(TAG, "initReader failed!", e);
        }
        return result;

    }

    @Override
    public void sendCommand(byte[] cmd) {
        tagReader.sendCommand(cmd);
    }

    @Override
    public boolean isConnected() {
        return tagReader.isConnected();
    }

    @Override
    public void stop() {
        tagReader.stop();
    }

}
