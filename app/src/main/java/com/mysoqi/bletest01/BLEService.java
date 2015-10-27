package com.mysoqi.bletest01;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 2015/08/14, 本 class 使用 Hanlder 傳輸資料
 * 本裝置藍牙4.0 BLE BluetoothLeService
 * 使用模組為: HC-08
 * <p/>
 * 讀寫主 Service
 * UUID :0000ffe0-0000-1000-8000-00805f9b34fb
 * chart :0000ffe1-0000-1000-8000-00805f9b34fb
 */
public class BLEService {
    /**
     * remote 藍牙硬體規格
     */
    public final static UUID BTUID_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public final static UUID BTUID_CHART = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    // BT statu 代碼(int), 本 class 回傳 parent 辨識用
    public final static int BLEPROFILE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public final static int BLEPROFILE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    /**
     * BT GATT 有新的 service chanel 可使用
     */
    public final static int GATT_SERVICES_DISCOVERED = 1001;
    ;
    /**
     * BT GATT 有資料傳輸
     */
    public final static int GATT_DATA_AVAILABLE = 1002;
    /**
     * BT 測試寫入成功
     */
    public final static int GATT_TESTING_READY = 1003;
    /**
     * BT 測試寫入失敗
     */
    public final static int GATT_TESTING_FAILURE = 1004;
    /**
     * 藍牙 BLE 必填標準參數，關閉或打開通知(Notify)的UUID, 藍牙規格固定值
     */
    public static String D_STR_NOTIFY = "00002902-0000-1000-8000-00805f9b34fb";
    public static UUID UUID_NOTIFY = UUID.fromString(D_STR_NOTIFY);
    /**
     * 測試 service chanel, 不傳送測試碼
     */
    public boolean isOnlyDetcedService = false;
    /**
     * 僅測試試藍牙是否連線，其他程序不做
     */
    public boolean isOnlyDetcedBTProc = false;
    // 藍牙設定相關
    public BluetoothGatt mBluetoothGatt;
    private boolean isDebug = false;
    /**
     * BT 目前位於 'BluetoothGattCallback' 的狀態設定
     */
    private String strGattStatu = "";
    /**
     * 本 class 相關 property
     */
    private Context mContext;
    private String strMsg;
    private Handler mHandler;
    /**
     * property , BLE 資料傳輸時的 Callback<BR>
     * Various callback methods defined by the BLE API.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * BT BLE 連線狀態改變
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            strGattStatu = "onConnectionStateChange";

            // Bluetooth Profiles 已經連線
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                if (isDebug)
                    PubClass.xxLog("BLE: Profile CONNECTED");

                // 回傳 mHandler, BLEPROFILE_CONNECTED
                mHandler.obtainMessage(BLEPROFILE_CONNECTED).sendToTarget();

                // 尋找可用的 Service chanel
                if (isOnlyDetcedBTProc != true) {
                    if (isDebug)
                        PubClass.xxLog("BLE: start discoverServices...");

                    mBluetoothGatt.discoverServices();
                }
            }
            // Bluetooth Profiles 斷線
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                if (isDebug)
                    PubClass.xxLog("BLE: profile DISCONNECTED");

                // 回傳 mHandler, BLEPROFILE_DISCONNECTED
                mHandler.obtainMessage(BLEPROFILE_DISCONNECTED).sendToTarget();
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // TODO 取得目前 BLE 各項的 UUID
            //showGattService();

            if (isDebug) {
                PubClass.xxLog("BLE: do onServicesDiscovered");
            }

            strGattStatu = "onServicesDiscovered";

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (isDebug) {
                    PubClass.xxLog("BLE: onServicesDiscovered Success!");
                }

                // TODO 指定某個 SERVICE UUID 測試連接
                BluetoothGattCharacteristic mChart = mBluetoothGatt.getService(
                        BTUID_SERVICE).getCharacteristic(BTUID_CHART);
                setCharacteristicNotification(mChart, true);

                return;
            }

            // 其他非 'GATT_SUCCESS' 情況
            if (isDebug) {
                PubClass.xxLog("BLE: onServicesDiscovered failure, statu_code: " + String.valueOf(status));
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {

            strGattStatu = "onCharacteristicRead";

            if (isDebug) {
                PubClass.xxLog("BLE: do onCharacteristicRead");
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                PubClass.xxLog("BLE: onCharacteristicRead Success!");

                mHandler.obtainMessage(GATT_DATA_AVAILABLE,
                        BTStremByteData(characteristic)).sendToTarget();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            strGattStatu = "onCharacteristicChanged";

            if (isDebug) {
                PubClass.xxLog("BLE: onCharacteristicChanged");
            }

            mHandler.obtainMessage(GATT_DATA_AVAILABLE,
                    BTStremByteData(characteristic)).sendToTarget();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {

            strGattStatu = "onDescriptorWrite";

            if (isDebug) {
                PubClass.xxLog("BLE: onDescriptorWrite");
            }

            if (isDebug) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    PubClass.xxLog("BLE: onDescriptorWrite Success!");
                } else {
                    PubClass.xxLog("BLE: onDescriptorWrite Failure, statu_code: " + String.valueOf(status));
                }
            }
        }
    };
    private BluetoothDevice mBTDev;

    /**
     * 藍牙4.0 BLE BluetoothLeService
     *
     * @param context
     */
    public BLEService(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    /**
     * 開始連接藍牙裝置進行通訊
     * <p/>
     *
     * @param device : 已經連線的 BT device
     */
    public void StartBTService(BluetoothDevice device) {
        strGattStatu = "";
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
    }

    /**
     * 重寫 'activeCharacteristicNotification'
     * <p/>
     * 啟動 CharacteristicNotification, 啟動藍牙裝置主動發送訊息<BR>
     * 先讀取 'readCharacteristic', 再執行 descriptor 寫入<br>
     * 啟動代碼 'ENABLE_NOTIFICATION_VALUE'
     */
    public void activeCharacteristicNotification(UUID uid, byte[] mValue,
                                                 String strDescriptor) {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(
                BTUID_SERVICE).getCharacteristic(uid);
        boolean bolRS = false;

        if (isDebug) {
            PubClass.xxLog("BLE: do activeCharacteristicNotification");
            PubClass.xxLog("SERV: " + BTUID_SERVICE);
            PubClass.xxLog("CHAR: " + uid);
        }

        try {
            UUID mUUID_Descriptor = UUID.fromString(strDescriptor);
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(mUUID_Descriptor);
            mBluetoothGatt.readDescriptor(descriptor);
            descriptor.setValue(mValue);
            mBluetoothGatt.writeDescriptor(descriptor);

            bolRS = mBluetoothGatt.readCharacteristic(characteristic);

            if (isDebug) {
                PubClass.xxLog("BLE: do activeCharacteristicNotification " + String.valueOf(bolRS));
                PubClass.xxLog("SERV: " + characteristic.getService().getUuid());
                PubClass.xxLog("CHAR: " + uid);
                PubClass.xxLog("DESP: " + mUUID_Descriptor);
            }

        } catch (Exception e) {
            if (isDebug) {
                PubClass.xxLog("BLE: activeCharacteristicNotification Exception err" + e.toString());
                PubClass.xxLog("errstring:" + e.toString());
            }
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic
                .getDescriptor(UUID_NOTIFY);

        boolean bolRS0 = descriptor
                .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        boolean bolRS1 = mBluetoothGatt.writeDescriptor(descriptor);

        if (isDebug) {
            PubClass.xxLog("BLE: do setCharacteristicNotification");
            PubClass.xxLog("SERV: " + characteristic.getService().getUuid());
            PubClass.xxLog("CHAR: " + characteristic.getUuid());
            PubClass.xxLog("BLE: set descriptor val => " + String.valueOf(bolRS0));
            PubClass.xxLog("BLE: write descriptor val => " + String.valueOf(bolRS1));
        }

        // 測試寫入是否成功
        if (bolRS1) {
            mHandler.obtainMessage(GATT_TESTING_READY).sendToTarget();
        } else {
            mHandler.obtainMessage(GATT_TESTING_FAILURE).sendToTarget();
        }

        // 藍牙設備指定的 Servece UUID 是否可以進行資料傳輸，由此判定
        if (bolRS1) {
            // 回傳 mHandler
            if (strGattStatu.equalsIgnoreCase("onServicesDiscovered")) {
                if (isDebug) {
                    PubClass.xxLog("BLE: setCharacteristicNotification success!");
                    PubClass.xxLog("SERV: " + characteristic.getService().getUuid());
                    PubClass.xxLog("CHAR: " + characteristic.getUuid());
                }

                mHandler.obtainMessage(GATT_SERVICES_DISCOVERED).sendToTarget();
            }
        }
    }

    /**
     * !! TODO NOT USE !!
     * 主動讀取BT資料, 需要傳送必要的參數資料<BR>
     * 本體重計傳送資料如下:<BR>
     * 10 01 00 1E AF => 數據類型(固定)10, 用户：01, 性别：00, 年龄：2D(45), 身高：AD(173)
     */
    public void ReadBTData(int gender, int age, int height) {
        byte[] byteVal = {(byte) 0x10, (byte) 0x01, (byte) gender, (byte) age,
                (byte) height};
        // byte[] byteVal = { (byte) 0x10, (byte) 0x01, (byte) 0x00, (byte)
        // 0x2D, (byte) 0xAD };

        BluetoothGattCharacteristic mChart = mBluetoothGatt.getService(
                BTUID_SERVICE).getCharacteristic(BTUID_CHART);

        // 設定 value 後，必須再執行 'write'
        mChart.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mChart);

        // 直接主動讀取資料
        BluetoothGattCharacteristic mChart1 = mBluetoothGatt.getService(
                BTUID_SERVICE).getCharacteristic(BTUID_CHART);
        mBluetoothGatt.readCharacteristic(mChart1);
    }

    /**
     * 手機 BT 傳指令給 remote BT<BR>
     * 傳送資料如 'A' (bit)
     */
    public void sendBTData(char mCode) {
        byte[] byteVal = {(byte) mCode};
        BluetoothGattCharacteristic mChart = mBluetoothGatt.getService(
                BTUID_SERVICE).getCharacteristic(BTUID_CHART);

        // 設定 value 後，必須再執行 'write'
        /*
        String strVal = "";
        for(byte mVal : byteVal) {
            strVal += String.valueOf(mVal);
        }
        PubClass.xxLog("send val :" + strVal);
        */

        mChart.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mChart);

        // 直接主動讀取資料
        BluetoothGattCharacteristic mChart1 = mBluetoothGatt.getService(
                BTUID_SERVICE).getCharacteristic(BTUID_CHART);
        mBluetoothGatt.readCharacteristic(mChart1);
    }

    /**
     * BT模組回傳資料, 回傳 byte[] 格式
     *
     * @param characteristic
     * @return
     */
    private byte[] BTStremByteData(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            return data;
        }

        return null;
    }

    /**
     * 此處為接收 BT Stream 資料，轉成 Integer List Array 回傳
     *
     * @param characteristic : 藍牙傳輸的 stream 資料
     */
    private ArrayList<Integer> BTStremToListInt1(
            BluetoothGattCharacteristic characteristic) {

        // 讀取 Byte 資料, 轉換為 String
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {

            if (isDebug) {
                final StringBuilder stringBuilder = new StringBuilder(
                        data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                PubClass.xxLog("HEX: " + stringBuilder.toString());
            }

            // 轉換好的資料設定到 intent extra string data
            ArrayList<Integer> listData = new ArrayList<Integer>();
            String strHEX = "";

            for (byte byteChar : data) {
                strHEX = String.format("%02X", byteChar);
                listData.add(Integer.parseInt(strHEX, 16));
            }

            return listData;
        }

        return null;
    }

    /**
     * Close BT connect
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 測試用<P>
     * 自訂 Class, 取得目前 BLE 各項的 UUID <BR>
     * Service, Characteristic, Descriptor
     */
    private void showGattService() {
        PubClass.xxLog("Start showGattService ... ");

        List<BluetoothGattService> lsitBTGattServ = mBluetoothGatt.getServices();

        for (BluetoothGattService mSrv : lsitBTGattServ) {
            PubClass.xxLog("UUID :" + mSrv.getUuid());

            List<BluetoothGattCharacteristic> listChart = mSrv
                    .getCharacteristics();

            for (BluetoothGattCharacteristic mChart : listChart) {
                PubClass.xxLog("chart :" + mChart.getUuid());

                List<BluetoothGattDescriptor> listDesp = mChart
                        .getDescriptors();

                for (BluetoothGattDescriptor mDesp : listDesp) {
                    PubClass.xxLog("desp_uid: " + mDesp.getUuid());
                    PubClass.xxLog("desp_permis: " + mDesp.getPermissions());
                    PubClass.xxLog("desp_char: " + mDesp.getCharacteristic());

                    // 取得 val, type = byte
                    try {
                        String strVal = "";
                        for (byte byteVal : mDesp.getValue()) {
                            strVal += String.valueOf(byteVal);
                        }

                        PubClass.xxLog("desp_val: " + strVal);

                    } catch (Exception ignore) {
                    }
                }

            }
        }
    }

}