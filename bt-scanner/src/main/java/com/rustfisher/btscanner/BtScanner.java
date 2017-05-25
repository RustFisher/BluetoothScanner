package com.rustfisher.btscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Search Bluetooth device
 */
public final class BtScanner {

    private static final String TAG = "rustApp";

    private long scanPeriod = 14000L;     // default scan period - start scan to stop scan
    private long notifyInterval = 2000L;  // default notify interval
    private boolean loadBondDevice = true;// load bonded device to result list

    public interface Listener {
        void onDeviceListUpdated(ArrayList<BtDeviceItem> list);

        void onScanning(boolean scan);
    }

    private BluetoothAdapter btAdapter;
    private Handler handler;
    private ArrayList<BtDeviceItem> deviceList;
    private List<Listener> listeners;
    private NotifyScanListThread notifyScanListThread;
    private BluetoothLeScanner bleScanner; // For LOLLIPOP or newer
    private ScanCallback bleScanCallback;  // For LOLLIPOP or newer

    private boolean scanning;

    public BtScanner() {
        initBtUtils();
    }

    /**
     * @param period millisecond
     */
    public BtScanner(long period) {
        initBtUtils();
        setScanPeriod(period);
    }

    private void initBtUtils() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        listeners = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
        if (laterThanLL()) {
            bleScanner = btAdapter.getBluetoothLeScanner();
            initBleScanCallback();
        }
    }

    public long getScanPeriod() {
        return scanPeriod;
    }

    public void setScanPeriod(long scanPeriod) {
        if (scanPeriod < 100) {
            scanPeriod = 100;
        } else if (scanPeriod > 180000) {
            scanPeriod = 180000;
        }
        this.scanPeriod = scanPeriod;
    }

    public long getNotifyInterval() {
        return notifyInterval;
    }

    public void setNotifyInterval(long interval) {
        if (interval < 100) {
            interval = 100;
        } else if (interval > 5000) {
            interval = 5000;
        }
        this.notifyInterval = interval;
    }

    public boolean isLoadBondDevice() {
        return loadBondDevice;
    }

    public void setLoadBondDevice(boolean loadBondDevice) {
        this.loadBondDevice = loadBondDevice;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void clearListener() {
        listeners.clear();
    }

    public boolean isScanning() {
        return scanning;
    }

    private void updatePairedDevice() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                deviceList.add(new BtDeviceItem(device, 0));
            }
            notifyDeviceListChanged(deviceList);
        }
    }

    private Runnable stopScanLeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                stopNotifyThread();
                scanning = false;
                notifyScanStatus(false);
                if (laterThanLL()) {
                    bleScanner.stopScan(bleScanCallback);
                } else {
                    btAdapter.stopLeScan(defBleScanCallback);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    private void restartNotifyThread() {
        stopNotifyThread();
        notifyScanListThread = new NotifyScanListThread(this);
        notifyScanListThread.start();
    }

    private void stopNotifyThread() {
        if (null != notifyScanListThread) {
            notifyScanListThread.interrupt();
            notifyScanListThread = null;
        }
    }

    public void startScan() {
        if (!btIsOn()) {
            Log.e(TAG, "start scan fail, bt is not On.  Bt state = " + btAdapter.getState());
            return;
        }
        deviceList.clear();
        handler.removeCallbacks(stopScanLeRunnable);
        if (loadBondDevice) {
            updatePairedDevice();
        }
        handler.postDelayed(stopScanLeRunnable, scanPeriod);
        scanning = true;
        if (laterThanLL()) {
            bleScanner.startScan(bleScanCallback);
            Log.d(TAG, "bleScanner startScan");
        } else {
            btAdapter.startLeScan(defBleScanCallback);
            Log.d(TAG, "btAdapter startLeScan");
        }
        notifyScanStatus(scanning);
        restartNotifyThread();
    }

    public void stopScan() {
        if (!btIsOn()) {
            Log.e(TAG, "stop scan fail, bt is not On.  Bt state = " + btAdapter.getState());
            return;
        }
        stopNotifyThread();
        handler.removeCallbacks(null);
        scanning = false;
        notifyScanStatus(false);
        if (laterThanLL()) {
            bleScanner.stopScan(bleScanCallback);
        } else {
            btAdapter.stopLeScan(defBleScanCallback);
        }
    }

    private void initBleScanCallback() {
        if (laterThanLL()) {
            bleScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (laterThanLL()) { // to avoid error
                        String name = result.getDevice().getName();
                        if (TextUtils.isEmpty(name)) {
                            return;
                        }
                        BtDeviceItem item = new BtDeviceItem(result.getDevice(), result.getRssi());
                        int index = deviceList.indexOf(item);
                        if (index <= -1) {
                            deviceList.add(item);
                        } else {
                            deviceList.get(index).setRssi(result.getRssi());
                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    notifyScanStatus(false);
                }
            };
        }
    }


    /**
     * default scan callback
     */
    private BluetoothAdapter.LeScanCallback defBleScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
            if (TextUtils.isEmpty(name)) {
                return;
            }
            BtDeviceItem item = new BtDeviceItem(device, rssi);
            int index = deviceList.indexOf(item);
            if (index <= -1) {
                deviceList.add(item);
            } else {
                deviceList.get(index).setRssi(rssi);
            }
        }
    };

    private void notifyDeviceListChanged(ArrayList<BtDeviceItem> deviceList) {
        final ArrayList<Listener> listenerList = new ArrayList<>(listeners);
        final ArrayList<BtDeviceItem> newESList = new ArrayList<>(deviceList);
        Collections.sort(newESList);
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Listener l : listenerList) {
                    l.onDeviceListUpdated(newESList);
                }
            }
        });
    }

    private void notifyScanStatus(boolean scanning) {
        final ArrayList<Listener> list = new ArrayList<>(listeners);
        final boolean finalScanning = scanning;
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Listener l : list) {
                    l.onScanning(finalScanning);
                }
            }
        });
    }


    /**
     * Tell listener
     */
    private class NotifyScanListThread extends Thread {
        BtScanner mmScanner;

        NotifyScanListThread(BtScanner s) {
            this.mmScanner = s;
        }

        @Override
        public void run() {
            super.run();
            while (mmScanner.isScanning() && !isInterrupted()) {
                try {
                    Thread.sleep(notifyInterval);
                    mmScanner.notifyDeviceListChanged(mmScanner.deviceList);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private boolean btIsOn() {
        return btAdapter.isEnabled();
    }

    private static boolean laterThanLL() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
