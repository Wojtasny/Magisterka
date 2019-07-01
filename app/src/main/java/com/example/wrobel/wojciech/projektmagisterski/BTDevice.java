package com.example.wrobel.wojciech.projektmagisterski;

// POJO for Bluetooth devices
public class BTDevice {

    // Name of the device
    private String mName;

    // Mac address of the device
    private String mAddress;

    public BTDevice(String name, String address) {
        if(name != null) {
            this.mName = name;
        } else {
            this.mName = "Not defined";
        }
        this.mAddress = address;
    }


    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }
}