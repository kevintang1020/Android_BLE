package com.mysoqi.bletest01;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 主程式
 */
public class MainHome extends AppCompatActivity {
    private String D_DEVNAME = "HTEBT401";

    // common property
    private Context mContext;
    private PubClass pubClass;

    // layout field
    private TextView tvMsg0, tvMsg1;

    // BT 相關
    private BLEService mBLEService; // BLE 主要的 Service
    private BluetoothDevice mBTDev = null; // 藍牙設備的 object
    private BluetoothAdapter mBTAdpt = null; // 行動裝置的 BT adpt
    private boolean isScanningDev = false; // BLE 程序是否正在搜尋藍牙設備中
    /**
     * BLE, 搜尋指定藍牙設備
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new LeScanCallback() {
        /**
         * 此 method 會一直 scan device, 需要背景執行並且需要控制時間結束該程序
         */
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {

            // 藍牙設備比對, 比對已儲存設備的 addr
            if (device.getName().equalsIgnoreCase(D_DEVNAME)) {
                // 找到設備
                mBTDev = device;
                isScanningDev = false;
            }

        }
    };
    /**
     * property Handler, 藍牙體重計回傳資料接收 handler
     */
    private final Handler mHandlerBTActivity = new Handler() {
        // 接收傳來的識別標記再做相關處理
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // BLE profile 已連結
                case BLEService.BLEPROFILE_CONNECTED:
                    //PubClass.xxLog("handler: BLEPROFILE_CONNECTED");
                    break;

                // BLE profile 中斷, 直接跳離
                case BLEService.BLEPROFILE_DISCONNECTED:
                    //PubClass.xxLog("handler: BLEPROFILE_DISCONNECTED");
                    tvMsg0.setText("conn lost ...");
                    StopBTConn();

                    break;

                // GATT server 有某個 service chanel 連接
                case BLEService.GATT_SERVICES_DISCOVERED:
                    //PubClass.xxLog("handler: GATT_SERVICES_DISCOVERED");
                    break;

                // BT service chanel 找到並執行讀寫測試成功後，表示BT設備就緒可以開始傳輸資料
                case BLEService.GATT_TESTING_READY:
                    //PubClass.xxLog("handler: GATT_TESTING_READY");
                    break;

                // GATT server 的某個 services 有資料傳輸
                case BLEService.GATT_DATA_AVAILABLE:
                    analyBTData((byte[]) msg.obj);

                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set property value
        mContext = this;
        pubClass = new PubClass(mContext);

        setContentView(R.layout.activity_main_home);
        tvMsg0 = (TextView) this.findViewById(R.id.tvMsg0);
        tvMsg1 = (TextView) this.findViewById(R.id.tvMsg1);

        // 行動裝置的 BT adpt 檢查支援 BLE 是否正確
        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_BLUETOOTH_LE)) {
                BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBTAdpt = bluetoothManager.getAdapter();
            }
        } catch (Exception e) {
            PubClass.xxLog(e.toString());
        }

        mBLEService = new BLEService(mContext, mHandlerBTActivity);

        setPageBtn();
    }

    /**
     * page button
     */
    private void setPageBtn() {
        // 連線藍牙裝置
        Button btnConn = (Button) this.findViewById(R.id.btnConn);
        btnConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 檢查行動裝置的 BT adpt
                if (mBTAdpt == null) {
                    PubClass.xxDump(mContext, "BTAdpt = null !!");

                    return;
                }

                // 開始搜尋藍牙裝置
                new AsyncBTScan().execute();
            }
        });

        // 斷開連線
        Button btnDisconn = (Button) this.findViewById(R.id.btnDisconn);
        btnDisconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StopBTConn();
                mBTAdpt.disable();
            }
        });


        // 發送指令 'A', 'B', 'C'
        Button btnA = (Button) this.findViewById(R.id.btnA);
        btnA.setOnClickListener(new lstnBtnCode('A'));

        Button btnB = (Button) this.findViewById(R.id.btnB);
        btnB.setOnClickListener(new lstnBtnCode('B'));

        Button btnC = (Button) this.findViewById(R.id.btnC);
        btnC.setOnClickListener(new lstnBtnCode('C'));
    }

    /**
     * 解析接收的藍牙資料, 執行相關程序<P>
     * Remote BT 回傳 LED 狀態, val 型態為 byte[]<BR>
     * '0', '1' 目前 array 只有一筆資料
     */
    private void analyBTData(byte[] byteData) {
        if (byteData == null) {
            return;
        }

        String strStatu = (byteData[0] > '0') ? "LED ON" : "LED OFF";
        tvMsg1.setText(strStatu);
    }

    /**
     * 行動裝置藍牙相關功能與程序關閉
     */
    public void StopBTConn() {
        mBTDev = null;
        mBTAdpt.stopLeScan(mLeScanCallback);
        mBLEService.close();
    }

    /**
     * 點取發送指令的 button
     */
    private class lstnBtnCode implements View.OnClickListener {
        private char mCode;

        public lstnBtnCode(char code) {
            mCode = code;
        }

        @Override
        public void onClick(View v) {
            mBLEService.sendBTData(mCode);
        }
    }

    /**
     * Async class, 背景執行藍牙設備連接與初始
     */
    private class AsyncBTScan extends AsyncTask<Void, Void, Void> {
        private int secLeScan = 8; // LeScanCallback 產生 BTdev 等待秒數

        @Override
        protected void onPreExecute() {
            isScanningDev = true;
            tvMsg0.setText("AsyncBTScan Start ...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            startScan();

            return null;
        }

        /**
         * scan BT device
         */
        private void startScan() {
            mBTAdpt.startLeScan(mLeScanCallback);

            try {
                int countSecond = 0;
                while (isScanningDev && countSecond < secLeScan) {
                    countSecond++;
                    Thread.sleep(1000);
                }
            } catch (Exception ignore) {
            }

            isScanningDev = false;
            mBTAdpt.stopLeScan(mLeScanCallback);
        }

        @Override
        protected void onPostExecute(Void result) {
            String strMsg = "AsyncBTScan Stop !\n";

            // 若找到藍牙設備，執行後續初始程序
            if (mBTDev != null) {
                strMsg += "device : " + mBTDev.getName() + ", addr : " + mBTDev.getAddress();

                // 產生 BLEService 實體
                mBLEService.StartBTService(mBTDev);
            } else {
                strMsg = "BT not conn . . .";
            }

            tvMsg0.setText(strMsg);
        }
    }

}