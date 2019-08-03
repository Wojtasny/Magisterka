package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.SpacesOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;

public class ConnectThread extends Thread {

    private static final String TAG = "ConnectThread";
    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;
    private BluetoothAdapter mAdapter;
    private Handler mHandler;
    
    public ConnectThread(BluetoothDevice device, Handler handler) {
        BluetoothSocket tmp = null;
        mDevice = device;
        // Get BluetoothSocket for a connection with the BluetoothDevice
        try {
            tmp = mDevice.createRfcommSocketToServiceRecord(BluetoothIO.MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        BTProperties.getInstance().setBTSocket(tmp);
//        mSocket = tmp;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
//        ParcelUuid uuids[] = mSocket.getRemoteDevice().getUuids();
//        for (ParcelUuid uuid : uuids) {
//            Log.d(TAG, "ParcelUuids: " + uuid.getUuid().toString());
//        }
    }
    public void run() {

        Log.d(TAG, "run: Begin ConnectThread");
        setName("ConnectThread");
        // it's better to cancel discovery as it can slow down connection
        mAdapter.cancelDiscovery();

        mSocket = BTProperties.getInstance().getBTSocket();
        try{
            if(mSocket != null) {
                mAdapter.cancelDiscovery();
                mSocket.connect();
                Thread.sleep(1000);
            }
        } catch (IOException connectException) {
            try{
                mSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            connectionFailed();
            return;
        } catch (Exception e) {
            Log.e(TAG, "run: ", e);
        }
        boolean isSockedConnected = mSocket.isConnected();
        if (isSockedConnected) {
            try {
                final Thread initOBDThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                    try {
                        new ObdResetCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
                        Thread.sleep(10);
                        new EchoOffCommand().run(mSocket.getInputStream(),mSocket.getOutputStream());
                        Thread.sleep(20);
                        new LineFeedOffCommand().run(mSocket.getInputStream(),mSocket.getOutputStream());
                        Thread.sleep(20);
                        new SpacesOffCommand().run(mSocket.getInputStream(),mSocket.getOutputStream());
                        Thread.sleep(20);
                        new SpacesOffCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
                        Thread.sleep(20);
                        new TimeoutCommand(125).run(mSocket.getInputStream(), mSocket.getOutputStream());
                        //  updateNotification(getString(R.string.searching_protocol));
                        Thread.sleep(20);
                        new SelectProtocolCommand(ObdProtocols.AUTO).run(mSocket.getInputStream(), mSocket.getOutputStream());
                        Thread.sleep(20);
                        new EchoOffCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
                        //  updateNotification(getString(R.string.searching_supported_sensor));
                        Thread.sleep(20);
                    } catch (Exception e) {
                        Log.e(TAG, "exception in init thread", e);
                    }
                    }
                });
                initOBDThread.start();
                initOBDThread.join(10000);
            }
            catch (Exception e ){
                Log.e(TAG, "initializeOBDThread: ", e);
            }
        }

//        synchronized (this) {
//            MainActivity.mBluetoothIO.mConnectThread = null;
//        }
//        MainActivity.mBluetoothSocket = mSocket;
//
//        MainActivity.mBluetoothIO.connected(mSocket, mDevice);
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
//        bundle.putString(MainActivity.TOAST, "Not able to connect to a bluetooth device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

//        this.start();
    }

    public void cancel() {
        if(mSocket != null) {
            try{
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
}
