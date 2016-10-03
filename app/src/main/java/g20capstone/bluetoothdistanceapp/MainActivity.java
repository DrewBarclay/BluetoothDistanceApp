package g20capstone.bluetoothdistanceapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    protected BluetoothAdapter bluetoothAdapter;
    protected final int REQUEST_ENABLE_BT = 1;
    protected ArrayList<BluetoothDevice> foundDevices = new ArrayList<>();
    protected BroadcastReceiver deviceFoundReceiver;
    protected ArrayAdapter<BluetoothDevice> devicesAdapter;
    protected ListView devicesListView;
    protected UUID bluetoothUUID = UUID.fromString("23275a50-891c-11e6-bdf4-0800200c9a66");

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
        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foundDevices);
        devicesListView = (ListView) findViewById(R.id.devicesList);
        devicesListView.setAdapter(devicesAdapter);
        devicesListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                ConnectThread clientThread = new ConnectThread(bluetoothAdapter, device, bluetoothUUID);
                clientThread.setPriority(Thread.MAX_PRIORITY);
                clientThread.start();
            }
        });

        //Bluetooth setting up
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth, quit???
            throw new UnsupportedOperationException();
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
                foundDevices.add(device);
                devicesAdapter.notifyDataSetChanged();
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceFoundReceiver, filter); // Don't forget to unregister during onDestroy

        //Set up server for requests
        AcceptThread serverThread = new AcceptThread(bluetoothAdapter, bluetoothUUID);
        serverThread.setPriority(Thread.MAX_PRIORITY);
        serverThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            if (resultCode != RESULT_OK) {
                //Bluetooth was not enabled, we should quit or re-ask for Bluetooth
                //Not sure what to put here yet :)
                throw new UnsupportedOperationException();
            }
        }
    }
}

class AcceptThread extends Thread {
    private BluetoothServerSocket serverSocket;
    protected BluetoothAdapter bluetoothAdapter;

    public AcceptThread(BluetoothAdapter bluetoothAdapter, UUID uuid) {
        this.bluetoothAdapter = bluetoothAdapter;
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothDistanceApp", uuid);
        } catch (IOException e) { throw new UnsupportedOperationException(); }
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        bluetoothAdapter.cancelDiscovery();

        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
                try {
                    serverSocket.close();
                } catch (IOException e) {

                }
                break;
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        byte[] buffer = new byte[1];
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            while(true) {
                //Server is 'dumb' and just echoes a single byte response back.
                int bytes = is.read(buffer);
                os.write(buffer);
                os.flush(); //Flush to ensure speediest sending
            }
        } catch (IOException e) {

        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException e) { }
    }
}

class ConnectThread extends Thread {
    protected BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice device;

    public ConnectThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device, UUID uuid) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        this.device = device;
        this.bluetoothAdapter = bluetoothAdapter;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { }
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        bluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            socket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                socket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection
        manageConnectedSocket(socket);
        try {
            socket.close();
        } catch (IOException e) {

        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        byte[] buffer = new byte[1];
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            //Measure response times in an infinite loop
            while(true) {
                os.write(buffer);
                long startTime = System.nanoTime();
                os.flush();
                int bytes = is.read(buffer);
                long endTime = System.nanoTime();
                System.out.println("Ping complete. Nanoseconds: " + (endTime - startTime));
            }
        } catch (IOException e) {

        }
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) { }
    }
}

