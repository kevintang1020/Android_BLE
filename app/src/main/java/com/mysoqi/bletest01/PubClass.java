package com.mysoqi.bletest01;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * general Public Class, use for all project
 */
public class PubClass {
    public static final String TAG = "myLOG";

    public PubClass(Context context) {
        super();
    }

    /**
     * 螢幕 顯示指定文字
     */
    public static void xxDump(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    /**
     * Log message, tag name => myTAG
     *
     * @param strMsg
     */
    public static void xxLog(String strMsg) {
        Log.v(TAG, strMsg);
    }
}