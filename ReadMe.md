# Android Bluetooth scanner

Use BtScanner to search bluetooth device.

```java
BtScanner mScanner = new BtScanner(3000); // or use defult constructor BtScanner()

mScanner.addListener(new BtScanner.Listener() {
            @Override
            public void onDeviceListUpdated(ArrayList<BtDeviceItem> list) {
                // do something..
            }

            @Override
            public void onScanning(boolean scan) {
                // scan status
            }
        });

mScanner.startScan(); // start scan
mScanner.stopScan();  // stop scan
```

After using scanner, clear listener. Call this in `Activity.onDestroy()` or somewhere else.
```java
mScanner.clearListener();
```

Use different API by Android SDK version 
```java
    if (laterThanLL()) {
        bleScanner.startScan(bleScanCallback);
        Log.d(TAG, "bleScanner startScan");
    } else {
        btAdapter.startLeScan(defBleScanCallback);
        Log.d(TAG, "btAdapter startLeScan");
    }

    private static boolean laterThanLL() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
```

