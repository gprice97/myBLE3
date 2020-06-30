package com.example.myble1;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);
    }
}