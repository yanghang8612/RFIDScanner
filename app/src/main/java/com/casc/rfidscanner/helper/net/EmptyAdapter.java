package com.casc.rfidscanner.helper.net;

import com.casc.rfidscanner.helper.net.param.Reply;

public class EmptyAdapter extends NetAdapter {

    @Override
    public void onSuccess(Reply reply) {}

    @Override
    public void onFail(String msg) {}
}
