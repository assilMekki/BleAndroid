package assil.com.bleconnect.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import assil.com.bleconnect.R;


public class MainActivity extends AppCompatActivity {

    TextView devicename,mac_adresse,temp,date;
    LinearLayout ll_data,ll_scan;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    /*private BluetoothGatt mBluetoothGatt;
    Boolean foundIt = false;*/
    Boolean bluetoothConnected = false;
/*
    UUID TIME_SERVICE_UUID = convertFromInteger(0x1805);
    UUID TIME_CHAR_UUID = convertFromInteger(0x2A2B);
    public static String TEMPERATURESERVICE_SERVICE_UUID = "e95d6100-251d-470a-a062-fa1922dfa9a8";
    public static String TEMPERATURE_CHARACTERISTIC_UUID = "00002a1f-0000-1000-8000-00805f9b34fb";*/
/*
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mHandler = new Handler();

        devicename = (TextView) findViewById(R.id.devicename);
        mac_adresse= (TextView) findViewById(R.id.mac_adresse);
        temp= (TextView) findViewById(R.id.temp);
        date= (TextView) findViewById(R.id.date);
        ll_data=(LinearLayout) findViewById(R.id.ll_data);
        ll_scan=(LinearLayout) findViewById(R.id.ll_scan);

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            startScanning();
        }


    }

    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
                if (result.getScanRecord().getManufacturerSpecificData().keyAt(0) == 48830) {

                    byte[] data=result.getScanRecord().getManufacturerSpecificData(48830);

                    devicename.setText("Device Name"+result.getDevice().getName());
                    mac_adresse.setText("Device Adresse"+result.getDevice().getAddress());

                    if (data.length==6) {
                        try {
                            byte[] tempbytes = {00,00,data[0], data[1]};
                            int tempInt = ByteBuffer.wrap(tempbytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                            if (tempInt>0){
                                temp.setText("tempurature : "+(tempInt/0.1)+"");

                            }

                        }catch (Exception  ex){
                            temp.setText("tempurature : not available");

                        }
                        try {
                            byte[] timebytes = {data[2], data[3], data[4], data[5]};
                            int time = ByteBuffer.wrap(timebytes).order(ByteOrder.LITTLE_ENDIAN).getInt();


                            if (time>0) {
                                date.setText("Time : " + getDate(time + ""));
                            }
                        }catch (Exception ex){
                            date.setText("Time : not available");

                        }


                    }

                    ll_data.setVisibility(View.VISIBLE);
                    ll_scan.setVisibility(View.GONE);




                    // I did this when I though that I would be reading from a service
                    // mBluetoothGatt = result.getDevice().connectGatt(MainActivity.this, false, mGattCallback);
                }


        }

    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        btScanner.startScan(leScanCallback);


    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
           /* if (newState == BluetoothProfile.STATE_CONNECTED) {
                //device connected
                mBluetoothGatt.discoverServices();
                bluetoothConnected = true;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothConnected = false;

            }*/
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
              /*  for (BluetoothGattService service : gatt.getServices()) {

                    if (service.getUuid().toString().equals(TEMPERATURESERVICE_SERVICE_UUID)) {

                        for (BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {

                            if (gattCharacteristic.getUuid().toString().equals(TEMPERATURE_CHARACTERISTIC_UUID)) {

                                gatt.readCharacteristic(gattCharacteristic);
                                readCharacteristic(gatt, gattCharacteristic);
                            }

                        }

                    }

                    if (service.getUuid().toString().equals(TIME_SERVICE_UUID.toString())) {
                        for (BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {
                            if (gattCharacteristic.getUuid().equals(TIME_CHAR_UUID)) {
                                gatt.readCharacteristic(gattCharacteristic);
                                readCharacteristic(gatt, gattCharacteristic);
                            }
                        }

                    }
                }*/
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
/*
                if (characteristic.getUuid().toString().equals(TEMPERATURE_CHARACTERISTIC_UUID)) {
                    int valueTemp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
                    String s="";
                }

                if (characteristic.getUuid().equals(TIME_CHAR_UUID)) {
                    String valueTIME = characteristic.getStringValue( 0);
                    String s="";

                }*/
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }
    };
/*
    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }*/

    /*private void readCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic gattCharacteristic) {
        if (bluetoothConnected) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gatt.readCharacteristic(gattCharacteristic);
                    readCharacteristic(gatt, gattCharacteristic);
                }
            }, 10000);
        }
    }*/

    public String getDate(String dateSt){
        long unixSeconds = Long.parseLong(dateSt);
        Date date = new Date(unixSeconds);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

}
