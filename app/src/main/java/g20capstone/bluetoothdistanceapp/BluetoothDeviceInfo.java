package g20capstone.bluetoothdistanceapp;

import android.bluetooth.BluetoothDevice;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Drew on 10/12/2016.
 */

public class BluetoothDeviceInfo {
    private static HashMap<BluetoothDevice, BluetoothDeviceInfo> deviceMap = new HashMap<>();
    public static ArrayList<BluetoothDeviceInfo> deviceList = new ArrayList<>();

    private BluetoothDevice device;
    private Deque<Integer> rssis = new ArrayDeque<>();
    private byte[] scanRecord;

    public static BluetoothDeviceInfo getInfo(BluetoothDevice device) {
        if (deviceMap.containsKey(device)) {
            return deviceMap.get(device);
        }
        BluetoothDeviceInfo bdi = new BluetoothDeviceInfo(device);
        return bdi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    private BluetoothDeviceInfo(BluetoothDevice device) {
        this.device = device;
        deviceMap.put(device, this);
        deviceList.add(this);
    }

    public void addRssi(int rssi) {
        rssis.addLast(rssi);

        if (rssis.size() > 10) {
            rssis.removeFirst();
        }
    }

    public int getRssiAverage() {
        int sum = 0;
        for (Integer i : rssis) {
            sum += i;
        }
        return sum / rssis.size();
    }

    public String toString() {
        String name = device.getName();
        if (name == null) {
            //Code from http://stackoverflow.com/questions/26290640/android-bluetoothdevice-getname-return-null
            BleAdvertisedData badata = BleUtil.parseAdertisedData(scanRecord);
            name = badata.getName();
        }
        return name + " " + device.getAddress() + ", RSSI: " + rssis.peekLast() + ", Average: " + getRssiAverage();
    }

    public void setScanRecord(byte[] scanRecord) {
        this.scanRecord = scanRecord;
    }
}

//Code below from http://stackoverflow.com/questions/26290640/android-bluetoothdevice-getname-return-null
class BleUtil {
    private final static String TAG=BleUtil.class.getSimpleName();
    public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();
        String name = null;
        if( advertisedData == null ){
            return new BleAdvertisedData(uuids, name);
        }

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length-1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return new BleAdvertisedData(uuids, name);
    }
}


class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}
