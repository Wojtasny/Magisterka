package com.example.wrobel.wojciech.projektmagisterski;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends Activity {

    private static final String TAG = "DeviceListActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    BluetoothAdapter mBluetoothAdapter;
    private ListView mListViewPaired;
    private ListView mListViewUnpaired;
    private DevicesAdapter mAdapterPaired;
    private DevicesAdapter mAdapterUnPaired;
    ArrayList<BTDevice> unPairedDevicesList = new ArrayList<>();

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
        mAdapterPaired.clear();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performDiscovery();
            }
        });


        mListViewPaired = (ListView) findViewById(R.id.paired_devices);

        ArrayList<BTDevice> pairedDevicesList = new ArrayList<>();

        mListViewUnpaired = (ListView) findViewById(R.id.new_devices);



        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if(pairedDevices.size() > 0){
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesList.add(new BTDevice(device.getName(), device.getAddress()));
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesList.add(new BTDevice(noDevices, ""));
        }
        mAdapterPaired = new DevicesAdapter(this, pairedDevicesList);
        mListViewPaired.setAdapter(mAdapterPaired);
        mListViewPaired.setOnItemClickListener(mDeviceClickListener);
    }

    private void performDiscovery() {
        Log.d(TAG, "performDiscovery: ");
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }



    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mBluetoothAdapter.cancelDiscovery();

            TextView nameTV = (TextView)view.findViewById(R.id.textView_name);
            String name = nameTV.getText().toString();
            TextView addressTV = (TextView)view.findViewById(R.id.textView_address);
            String address = addressTV.getText().toString();


            BTProperties.getInstance().setBTDevice(name, address);
            setResult(Activity.RESULT_OK);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, device.getName() + "\n" + device.getAddress());
                    unPairedDevicesList.add(new BTDevice(device.getName(), device.getAddress()));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (unPairedDevicesList.size() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    unPairedDevicesList.add(new BTDevice(noDevices, ""));
                }
            }

            mAdapterUnPaired= new DevicesAdapter(DeviceListActivity.this, unPairedDevicesList);
            mListViewUnpaired.setAdapter(mAdapterUnPaired);
            mListViewUnpaired.setOnItemClickListener(mDeviceClickListener);
        }
    };
}
