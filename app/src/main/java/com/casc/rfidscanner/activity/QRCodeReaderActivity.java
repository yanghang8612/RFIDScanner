package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Vibrator;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import butterknife.ButterKnife;

public class QRCodeReaderActivity extends BaseActivity implements QRCodeReaderView.OnQRCodeReadListener {

    private static final String TAG = QRCodeReaderActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        if (!(ActivityCollector.getTopActivity() instanceof QRCodeReaderActivity)) {
            Intent intent = new Intent(context, QRCodeReaderActivity.class);
            context.startActivity(intent);
        }
    }

    // 系统震动辅助类
    protected Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_scan);
        ButterKnife.bind(this);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        mVibrator.vibrate(50);
        String[] result = text.split("=");
        if (result.length == 2 && result[1].length() == MyParams.BODY_CODE_LENGTH
                && result[1].startsWith(MyVars.config.getHeader())) {

        }
    }
}
