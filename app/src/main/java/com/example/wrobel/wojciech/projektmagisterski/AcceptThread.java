package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

import static com.example.wrobel.wojciech.projektmagisterski.BluetoothIO.STATE_CONNECTED;
import static com.example.wrobel.wojciech.projektmagisterski.BluetoothIO.STATE_CONNECTING;
import static com.example.wrobel.wojciech.projektmagisterski.BluetoothIO.STATE_LISTEN;
import static com.example.wrobel.wojciech.projektmagisterski.BluetoothIO.STATE_NONE;

public class AcceptThread extends Thread {
    private final BluetoothServerSocket mServerSocket;
    private static final String TAG = "AcceptThread";


    public AcceptThread(){
        BluetoothServerSocket tmp = null;

        // Create new listening server socket
        try {
            tmp = MainActivity.mBluetoothIO.mAdapter.listenUsingRfcommWithServiceRecord(TAG, BluetoothIO.MY_UUID);
        } catch (IOException e) {
            Log.d(TAG, "AcceptThread failed with " + e);
        }
        mServerSocket = tmp;
    }

    public void run() {
        Log.d(TAG, "BEGIN mAcceptedThread " + this);
        setName("AcceptThread");

        BluetoothSocket socket;

        while (MainActivity.mBluetoothIO.getState() != STATE_CONNECTED) {
            try {
                // blocking call, will only return on successful connection or exception
                socket = mServerSocket.accept();
            } catch (IOException e ){
                Log.d(TAG, "AcceptThread failed with: " + e);
                break;
            }
            if (socket != null) {
                switch (MainActivity.mBluetoothIO.getState()) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        MainActivity.mBluetoothIO.connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        // Either not ready or already connected. Terminate new socket.
                        try {
                            socket.close();
                        } catch (IOException e){
                            Log.d(TAG, "AcceptThread closing unwanted socket failed with: " + e);
                            break;
                        }
                }
            }
        }
        Log.d(TAG, "END AcceptThread");
    }
    public void cancel() {
        Log.d(TAG, "cancel: " + this);
        try{
            mServerSocket.close();
        } catch (IOException e) {
            Log.d(TAG, "cancel: server socket close failed" + e);
        }
    }
}
