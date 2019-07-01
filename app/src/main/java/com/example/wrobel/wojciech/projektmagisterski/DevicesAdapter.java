package com.example.wrobel.wojciech.projektmagisterski;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DevicesAdapter extends ArrayAdapter {
    private Context mContext;
    private List<BTDevice> devicesList = new ArrayList<>();

    public DevicesAdapter(@NonNull Context context, ArrayList<BTDevice> list) {
        super(context, 0 , list);
        mContext = context;
        devicesList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.single_device, parent, false);

        BTDevice currentDevice = devicesList.get(position);
        TextView name = (TextView) listItem.findViewById(R.id.textView_name);
        name.setText(currentDevice.getName());

        TextView address = (TextView) listItem.findViewById(R.id.textView_address);
        address.setText(currentDevice.getAddress());

        return listItem;
    }
}
