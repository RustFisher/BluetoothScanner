package com.rust.btscanner;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class stands for bluetooth device
 * Created by Rust on 2017/5/4.
 */
public final class BtDeviceItem implements Parcelable, Comparable {

    private String address;
    private String name;
    private int rssi;
    private BluetoothDevice bluetoothDevice;

    public BtDeviceItem(BluetoothDevice device, int rssi) {
        this.address = device.getAddress();
        this.name = device.getName();
        this.rssi = rssi;
        this.bluetoothDevice = device;
    }

    protected BtDeviceItem(Parcel in) {
        address = in.readString();
        name = in.readString();
        rssi = in.readInt();
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public static final Creator<BtDeviceItem> CREATOR = new Creator<BtDeviceItem>() {
        @Override
        public BtDeviceItem createFromParcel(Parcel in) {
            return new BtDeviceItem(in);
        }

        @Override
        public BtDeviceItem[] newArray(int size) {
            return new BtDeviceItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeInt(rssi);
        dest.writeParcelable(bluetoothDevice, flags);
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || !(obj instanceof BtDeviceItem)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        BtDeviceItem likeThis = (BtDeviceItem) obj;
        return ((likeThis.getName().equals(this.getName())) &&
                (likeThis.getAddress().equals(this.getAddress())));
    }

    @Override
    public String toString() {
        return name + ", " + address + " , rssi = " + rssi;
    }

    /**
     * larger is previous
     */
    @Override
    public int compareTo(Object o) {
        BtDeviceItem esObj = (BtDeviceItem) o;
        int rssiEs = esObj.getRssi();
        if (this.rssi > rssiEs) {
            return -1;
        } else if (this.rssi < rssiEs) {
            return 1;
        }
        return 0;
    }
}

