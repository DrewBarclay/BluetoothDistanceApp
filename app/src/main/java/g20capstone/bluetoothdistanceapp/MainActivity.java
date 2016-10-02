package g20capstone.bluetoothdistanceapp;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    protected BluetoothAdapter bluetoothAdapter;
    protected final int REQUEST_ENABLE_BT = 1;
    protected ArrayList<String> foundDevices = new ArrayList<String>();
    protected BroadcastReceiver deviceFoundReceiver;
    protected ArrayAdapter<String> devicesAdapter;
    protected ListView devicesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanForBluetoothDevices();
            }
        });

        //Link array to device list in the UI
        devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, foundDevices);
        devicesListView = (ListView) findViewById(R.id.devicesList);
        devicesListView.setAdapter(devicesAdapter);
        foundDevices.add("Bluetooth devices found go here when you push the button on the bottom right.");

        //Bluetooth setting up
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth, quit???
        }

        //Make sure bluetooth is on
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //Make sure this device is discoverable
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); //forever
        startActivity(discoverableIntent);

        //Register hook for found devices
        deviceFoundReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                foundDevices.add(device.getName() + " " + device.getAddress());
                devicesAdapter.notifyDataSetChanged();
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceFoundReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(deviceFoundReceiver);
    }

    protected void scanForBluetoothDevices() {
        foundDevices.clear();
        devicesAdapter.notifyDataSetChanged();
        bluetoothAdapter.cancelDiscovery();
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                //Bluetooth is now on
            } else {
                //Bluetooth was not enabled, we should quit or re-ask for Bluetooth
                //Not sure what to put here yet :)
            }
        }
    }
}
