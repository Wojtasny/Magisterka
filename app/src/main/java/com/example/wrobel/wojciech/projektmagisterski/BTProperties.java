package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothSocket;

import java.util.HashMap;

public class BTProperties {
    private static BTProperties mInstance = null;

    private String mBTName;
    private String mBTAddress;
    private BluetoothSocket mBTSocket;

    private BTProperties(){}

    public void setBTDevice(String name, String address){
        mBTName = name;
        mBTAddress = address;
    }

    public String getBTName() {
        return mBTName;
    }

    public String getBTAddress() {
        return mBTAddress;
    }

    public static synchronized BTProperties getInstance() {
        if(mInstance == null) {
            mInstance = new BTProperties();
        }
        return mInstance;
    }

    synchronized public BluetoothSocket getBTSocket() {
        return mBTSocket;
    }

    public void setBTSocket(BluetoothSocket BTSocket) {
        mBTSocket = BTSocket;
    }
}
