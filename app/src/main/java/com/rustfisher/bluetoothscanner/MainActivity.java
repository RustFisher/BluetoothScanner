package com.rustfisher.bluetoothscanner;

import android.bluetooth.le.ScanSettings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.rustfisher.btscanner.BtDeviceItem;
import com.rustfisher.btscanner.BtScanner;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "rustApp";
    BtScanner mScanner = new BtScanner(3000);
    TextView mTv;
    Button mScanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTv = (TextView) findViewById(R.id.textView);
        mScanBtn = (Button) findViewById(R.id.searchBtn);
        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScanner.isScanning()) {
                    mScanner.stopScan();
                } else {
                    mScanner.startScan();
                }

            }
        });

        initScanner();
    }

    private void initScanner() {
        mScanner.setLoadBondDevice(false);
        mScanner.setScanPeriod(50000);
        mScanner.setNotifyInterval(500);
        if (BtScanner.sdkLLOrLater()) {
//            mScanner.addScanFilter(new ScanFilter.Builder()
//                    .setDeviceName("XXX")
//                    .setDeviceAddress("12:34:56:78:90:EA")
//                    .build());

            mScanner.setScanSettings(new ScanSettings.Builder()
                    .setReportDelay(100)
                    .build());
        }

        mScanner.addListener(new BtScanner.Listener() {
            @Override
            public void onDeviceListUpdated(ArrayList<BtDeviceItem> list) {
                mTv.setText("");
                for (BtDeviceItem item : list) {
                    mTv.append(item.toString());
                    mTv.append("\n");
                }
                Log.d(TAG, "update: " + list);
            }

            @Override
            public void onScanning(boolean scan) {
                mScanBtn.setText(scan ? "Scanning" : "Scan");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanner.stopScan();
        mScanner.clearListener();
    }
}
