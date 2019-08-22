package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.SpacesOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.util.UUID;


public class BluetoothIO {
    //todo 8. Indicate that connection attempt failed and notify UI/ connection lost

    private static final String TAG = "BluetoothIO";
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Constants that indicate the current connection state
    protected static final int STATE_NONE = 0;       // we're doing nothing
    protected static final int STATE_LISTEN = 1;     // now listening for incoming connections
    protected static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    protected static final int STATE_CONNECTED = 3;  // now connected to a remote device

    protected final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private int mState;
    protected ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;
    private BluetoothSocket mBluetoothSocket;

    private BluetoothIO(){
        mAdapter = null;
        mHandler = null;
        mBluetoothSocket = BTProperties.getInstance().getBTSocket();
    }


    /**
     * Constructor. Prepares a new Bluetooth session
     * @param context The UI Activity Context
     * @param handler to send messages back to the UI Activity
     */
    public BluetoothIO(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the connection
     * @param state int defining current connection
     */
    private synchronized void setState(int state){
        Log.d(TAG, "setState: " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return current connection state
     */
    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        clearThreads();

//        setState(STATE_LISTEN);

        if(mAcceptThread == null) {
//            mAcceptThread = new AcceptThread(); bo nullpointer z nieprzerobionego codu z innego maina
//            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect: " + device.getAddress());

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            clearThreads();
        }
        setState(STATE_CONNECTING);

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread((BluetoothDevice) device, mHandler);
        mConnectThread.start();
        try {
            mConnectThread.join(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Set state
        setState(STATE_CONNECTED);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connected(BTProperties.getInstance().getBTSocket(), device);
    }

    public void clearThreads() {
        if(mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread managing connection
        if(mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }
    protected void connectionLost(){
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        BluetoothIO.this.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
//        clearThreads();
//        if(mAcceptThread != null) {
//            mAcceptThread.cancel();
//        }
        // Cancel any thread managing connection
        if(mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start ConnectedThread
        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString("device_name", device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);

//        initializeOBDlib(socket);

//        getSampleInfo(socket);
    }

    protected void getSampleInfo(BluetoothSocket socket) {
        RPMCommand mRpmCpmmand = new RPMCommand();
        Log.d(TAG, "getSampleInfo: ");
        if (!Thread.currentThread().isInterrupted()) {
            try {
                mRpmCpmmand.run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                Log.e(TAG, "getSampleInfo: ", e);
            }
            Log.d(TAG, "RPM: " + mRpmCpmmand.getFormattedResult());
        }
    }

    protected void initializeOBDlib() {

        if (mBluetoothSocket.isConnected()) {
            try {
                final Thread newThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new ObdResetCommand().run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            Log.d(TAG, "After first command");
                            Thread.sleep(100);
                            new EchoOffCommand().run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            Thread.sleep(200);
                            new LineFeedOffCommand().run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            Thread.sleep(200);
                            new SpacesOffCommand().run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            Thread.sleep(200);
                            new SpacesOffCommand().run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            Thread.sleep(200);
                            new TimeoutCommand(125).run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            //  updateNotification(getString(R.string.searching_protocol));
                            Thread.sleep(200);
                            new SelectProtocolCommand(ObdProtocols.AUTO).run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            Thread.sleep(200);
                            new EchoOffCommand().run(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                            //  updateNotification(getString(R.string.searching_supported_sensor));
                            Thread.sleep(200);
                        } catch (Exception e) {
                            Log.e(TAG, "exception in init thread", e);
                        }
                    }
                });
                newThread.start();
                newThread.join(15000);
            }
            catch (Exception e ){
                Log.e(TAG, "initializeOBDlib: outer", e);
            }
        }
    }
}
