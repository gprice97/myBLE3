package com.example.myble1;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.os.Bundle;

public class DataDisplay extends AppCompatActivity {
    public final static String TAG = DataDisplay.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private TextView myConnectionState;
    private TextView myDataField;
    private String myDeviceName;
    private String myDeviceAddress;
    private BluetoothLeService myBluetoothLeService;
    private boolean myConnected = false;
    private BluetoothGattCharacteristic myNotifyCharacteristic;


    // Code to manage Service lifecycle.
    private final ServiceConnection myServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            myBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!myBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            myBluetoothLeService.connect(myDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            myBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver myGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
                myConnected = true;
                updateConnectionState(R.string.menu_connect);
                invalidateOptionsMenu();
            } else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                myConnected = false;
                updateConnectionState(R.string.menu_disconnect);
                invalidateOptionsMenu();
            } /*else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                displayGattServices(myBluetoothLeService.getSupportedGattServices());
            }*/
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        final Intent myIntent = getIntent();
        myDeviceName = myIntent.getStringExtra(EXTRAS_DEVICE_NAME);
        myDeviceAddress = myIntent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //Sets up UI references
       // ((TextView) findViewById(R.id.device_address)).setText(myDeviceAddress);
        myDataField =  findViewById(R.id.data_value);


//        getActionBar().setTitle(myDeviceName);
      //  getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, myServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(myGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (myBluetoothLeService != null) {
            final boolean result = myBluetoothLeService.connect(myDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(myGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(myServiceConnection);
        myBluetoothLeService = null;
    }

    //TODO: Make sure to add in menu connect and disconnect to UI

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if(myConnected){
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }
        else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                myBluetoothLeService.connect(myDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                myBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myConnectionState.setText(resourceId);
            }
        });
    }



    private void displayData(String data) {
        if (data != null) {
            myDataField.setText(data);
        }
    }

   /* private void displayGattServices(List<BluetoothGattService> gattServices){
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        myGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available Characteristics.
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            charas.add(gattCharacteristic);
            HashMap<String, String> currentCharaData = new HashMap<String, String>();
            uuid = gattCharacteristic.getUuid().toString();
            currentCharaData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
            currentCharaData.put(LIST_UUID, uuid);
            gattCharacteristicGroupData.add(currentCharaData);
        }
        mGattCharacteristics.add(charas);
        gattCharacteristicData.add(gattCharacteristicGroupData);
    }*/

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        myIntentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        myIntentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        myIntentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return myIntentFilter;
    }
}