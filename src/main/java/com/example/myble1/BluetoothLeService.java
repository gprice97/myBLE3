package com.example.myble1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import android.os.Bundle;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager myBluetoothManager;
    private BluetoothAdapter myBluetoothAdapter;
    private String myBluetoothDeviceAddress;
    private BluetoothGatt myBluetoothGatt;
    private int myConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    //TODO : Create SampleGattAttribute Class or Just Store here, but later :)
    /*public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);*/

    //This Next Block implements callback methods for GATT events the app cares about
    //For example when a connection changes and services are discovered!

    private final BluetoothGattCallback myGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                myConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                //Attempts to discover Services after successful connection
                Log.i(TAG, "Attempting to Start Service Discovery:" +
                        myBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                myConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered Method Received:" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate2(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        //Calls the broadcastUpdate2 method
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate2(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    //This broadcastUpdate method displays which the app is currently trying to do (Connecting, Connected, Disconnected)
    private void broadcastUpdate(final String action) {
        final Intent myIntent = new Intent(action);
        sendBroadcast(myIntent);
    }

    //This broadcastUpdate sends over data from a characteristic
    private void broadcastUpdate2(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent myIntent2 = new Intent(action);
        //TODO : Here I should add a Switch Statement or If-Else chain to determine which measurement is being read
        // Here is what the Google Source Code did
       /* if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                //Flag up
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                //Flag Down
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {*/
        // For all other profiles, writes the data formatted in HEX.
        // Pulled in the Bytes from the selected characteristic
        final byte[] data = characteristic.getValue();
        //If we actually have data at all
        if (data != null && data.length > 0) {
            //Creates a constant version of Stringbuilder Class myStringBuilder
            final StringBuilder myStringBuilder = new StringBuilder(data.length);
            //Creates a constant version of Stringbuilder Class myStringBuilder
            for (byte byteChar : data)
                myStringBuilder.append(String.format("%02X ", byteChar));
            //Puts the String of bytes from the data variable into a new string variable EXTRA_DATA
            myIntent2.putExtra(EXTRA_DATA, new String(data) + "\n" + myStringBuilder.toString());
        }
        sendBroadcast(myIntent2);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder myBinder = new LocalBinder();

    //TODO: initialize method used within DeviceControlActivity
    public boolean initialize() {
        if (myBluetoothManager == null) {
            myBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (myBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        myBluetoothAdapter = myBluetoothManager.getAdapter();
        if (myBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }
    //TODO: Used in DeviceControlActivity
    public boolean connect(final String address){
        if (myBluetoothAdapter == null || address == null){
            Log.w(TAG,"Bluetooth Adapter not initialized OR unspecified address.");
            return false;
        }
        if (myBluetoothDeviceAddress != null && address.equals(myBluetoothDeviceAddress)
                && myBluetoothGatt != null){
            Log.d(TAG, "Trying to use an existing myBluetoothGatt for connection.");
            if (myBluetoothGatt.connect()){
                myConnectionState = STATE_CONNECTING;
                return true;
            } else{
                return false;
            }
        }
        final BluetoothDevice myDevice = myBluetoothAdapter.getRemoteDevice(address);
        if (myDevice == null){
            Log.w(TAG, "Device Not Found, Unable to Connect.");
            return false;
        }
        myBluetoothGatt = myDevice.connectGatt(this,false,myGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        myBluetoothDeviceAddress = address;
        myConnectionState = STATE_CONNECTING;
        return true;
    }

    //TODO: Used in DeviceControlActivity
    public void disconnect() {
        if (myBluetoothAdapter == null || myBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        myBluetoothGatt.disconnect();
    }

    public void close(){
        if (myBluetoothGatt == null) {
            return;
        }
        myBluetoothGatt.close();
        myBluetoothGatt = null;
    }

    //TODO: Used in DeviceControlActivity
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (myBluetoothAdapter == null || myBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        myBluetoothGatt.readCharacteristic(characteristic);
    }

    //TODO: Used in DeviceControlActivity
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (myBluetoothAdapter == null || myBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        myBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        //TODO: This is specific to Heart Rate Measurement.
        /*if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            myBluetoothGatt.writeDescriptor(descriptor);
        }*/
    }
    public List<BluetoothGattService> getSupportedGattServices() {
        if (myBluetoothGatt == null) return null;

        return myBluetoothGatt.getServices();
    }
}

