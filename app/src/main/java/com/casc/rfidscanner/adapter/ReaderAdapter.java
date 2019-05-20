package com.casc.rfidscanner.adapter;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class ReaderAdapter extends BaseQuickAdapter<BluetoothDevice, BaseViewHolder> {

    public ReaderAdapter(@Nullable List<BluetoothDevice> data) {
        super(R.layout.item_reader, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, BluetoothDevice item) {
//        if (item.getAddress().equals(SpHelper.getParam(MyParams.S_READER_MAC)))
//            helper.setBackgroundRes(R.id.ll_reader_content, R.drawable.bg_reader_selected);
        helper.setText(R.id.tv_reader_name, item.getName())
                .setText(R.id.tv_reader_mac, item.getAddress())
                .setGone(R.id.tv_reader_bonded, item.getBondState() == BluetoothDevice.BOND_BONDED)
                .setGone(R.id.tv_reader_bonding, item.getBondState() == BluetoothDevice.BOND_BONDING)
                .setGone(R.id.ll_reader_bond, item.getBondState() == BluetoothDevice.BOND_NONE)
                .addOnClickListener(R.id.btn_reader_bond);
    }
}
