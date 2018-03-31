package jerome.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface IBluetoothView {
    public enum BondTypeEnum {
        NONE, CONNECTING, BOND,UNBOND, BOND_FAIL, HID_STATE_CONNECTED, HID_STATE_DISCONNECTED;
    }
    public enum SearchingTypeEnum{
        NONE, FOUND, SEARCHING, SEARCHING_FINISHED;
    }
    void notifyBondStatus(BondTypeEnum type, BluetoothDevice bluetoothDevice);

    void notifySearchingStatus(SearchingTypeEnum type, BluetoothDevice bluetoothDevice);

    void showConnectionDialog(BluetoothDevice bluetoothDevice, String str);

    void showUnpairDialog(BluetoothDevice bluetoothDevice);
}