package com.example.myble1;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import android.os.Bundle;


public class DeviceScanner extends ListActivity {
    private LeDeviceListAdapter myLeDeviceListAdapter;
    private BluetoothAdapter myBluetoothAdapter;
    private boolean myScanning;
    private Handler myHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    //Scanning will stop after 10 seconds
    private static final long SCAN_PERIOD = 10000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       myHandler = new Handler();

       //The following lines check to determine if BLE is supported on your device
        //Then you can selectively disable BLE-related features
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this,"BLE isn't supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Initializes a Bluetooth Adapter. For API 18 and above get a reference to
        //BlueoothAdapter through BluetoothManager
        final BluetoothManager myBluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = myBluetoothManager.getAdapter();

        //Checks if Bluetooth is supported on the device
        if(myBluetoothAdapter == null){
            Toast.makeText(this,"Bluetooth not supported.",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if(!myScanning){
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }else{
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_scan:
                myLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        myLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(myLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //User chose not to enable Bluetooth
        if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED){
            finish();
            return;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onPause(){
        super.onPause();
        scanLeDevice(false);
        myLeDeviceListAdapter.clear();
    }
    @Override
    protected void onListItemClick(ListView l,View v, int position, long id){
        final BluetoothDevice device = myLeDeviceListAdapter.getDevice(position);
        if(device == null) return;
        final Intent myIntent = new Intent(this,DataDisplay.class);
        myIntent.putExtra(DataDisplay.EXTRAS_DEVICE_NAME,device.getName());
        myIntent.putExtra(DataDisplay.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (myScanning) {
            myBluetoothAdapter.stopLeScan(myLeScanCallback);
            myScanning = false;
        }
        startActivity(myIntent);
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    myScanning = false;
                    myBluetoothAdapter.stopLeScan(myLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            myScanning = true;
            myBluetoothAdapter.startLeScan(myLeScanCallback);
        } else {
            myScanning = false;
            myBluetoothAdapter.stopLeScan(myLeScanCallback);
        }
        invalidateOptionsMenu();
    }
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> myLeDevices;
        private LayoutInflater myInflator;

        public LeDeviceListAdapter() {
            super();
            myLeDevices = new ArrayList<BluetoothDevice>();
            myInflator = DeviceScanner.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!myLeDevices.contains(device)) {
                myLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return myLeDevices.get(position);
        }

        public void clear() {
            myLeDevices.clear();
        }

        @Override
        public int getCount() {
            return myLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return myLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = myInflator.inflate(R.layout.activity_device_scanner, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = myLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback myLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myLeDeviceListAdapter.addDevice(device);
                            myLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}