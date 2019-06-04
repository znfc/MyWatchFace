package com.google.android.clockwork.settings;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.VisibleForTesting;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/** Shadow for BluetoothAdapter for BT classic peripheral connectability test */
@Implements(BluetoothDevice.class)
public class ShadowBluetoothDevice {
    private String mAddress;
    private int mType;

    @VisibleForTesting
    static BluetoothDevice createInstance(int type,  String address) {
        BluetoothDevice device = Shadow.newInstance(BluetoothDevice.class,
                new Class[]{String.class},
                new Object[]{address});
        ((ShadowBluetoothDevice) Shadow.extract(device)).setType(type);
        ((ShadowBluetoothDevice) Shadow.extract(device)).setAddress(address);
        return device;
    }

    private void setType(int type) {
        mType = type;
    }

    private void setAddress(String address) {
        mAddress = address;
    }

    @Implementation
    public void __constructor__(String address) {}

    @Implementation
    public String getAddress() {
        return mAddress;
    }

    @Implementation
    public int getType() {
        return mType;
    }

    @Implementation
    public int hashCode() {
        return mType;
    }

    @Implementation
    public boolean equals(Object o) {
        if (o instanceof BluetoothDevice) {
            return mAddress.equals(((BluetoothDevice) o).getAddress());
        }
        return true;
    }
}

