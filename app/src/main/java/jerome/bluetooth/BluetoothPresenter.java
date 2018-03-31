package jerome.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class BluetoothPresenter {

    private static final String TAG = BluetoothPresenter.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private final BroadcastReceiver bluetoothConnectActivityReceiver;
    private Context mContext;
    private IBluetoothView mIBluetoothView;
    private BluetoothDevice mHidBluetoothDevice;
    private class BluetoothBroadcastReceiver extends BroadcastReceiver {
        BluetoothBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtility.d(TAG, "onReceive", "action:" + action);
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");

            if (action.equals(Constant.BLUETOOTH_ACTION_FOUND)) {
                if (mIBluetoothView != null) {
                    mIBluetoothView.notifySearchingStatus(IBluetoothView.SearchingTypeEnum.FOUND, device);
                }
            } else if (action.equals(Constant.BLUETOOTH_ACTION_PARING_REQUEST)) {
                try {
                    abortBroadcast();
                    mIBluetoothView.showConnectionDialog(device, getBluetoothKey(intent) + "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (!action.equals(Constant.BLUETOOTH_ACTION_PAIRING_CANCEL) &&
                        action.equals(Constant.BLUETOOTH_ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
                int oldState = intent.getIntExtra("android.bluetooth.device.extra.PREVIOUS_BOND_STATE", Integer.MIN_VALUE);
                LogUtility.d(TAG, "onReceive", "oldState:" + oldState + ", bondState:" + bondState);
                if (oldState == 11 && bondState == 12) {
                    mIBluetoothView.notifyBondStatus(IBluetoothView.BondTypeEnum.BOND, device);
                } else if (oldState == 12 && bondState == 10) {
                    mIBluetoothView.notifyBondStatus(IBluetoothView.BondTypeEnum.UNBOND, device);
                } else if (oldState == 11 && bondState == 10) {
                    mIBluetoothView.notifyBondStatus(IBluetoothView.BondTypeEnum.BOND_FAIL, device);
                }
            } else if (action.equals(Constant.BLUETOOTH_ACTION_BOND_DISCOVERY_STARTED)) {
                mIBluetoothView.notifySearchingStatus(IBluetoothView.SearchingTypeEnum.SEARCHING, null);
            } else if (action.equals(Constant.BLUETOOTH_ACTION_BOND_DISCOVERY_FINISHED)) {
                mIBluetoothView.notifySearchingStatus(IBluetoothView.SearchingTypeEnum.SEARCHING_FINISHED, null);
            } else if (action.equals(Constant.BLUETOOTH_ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,0);
                LogUtility.d(TAG, "onReceive", "CONNECTION_STATE_CHANGED state:" + state);
                if(state == BluetoothProfile.STATE_CONNECTED){
                    mIBluetoothView.notifyBondStatus(IBluetoothView.BondTypeEnum.HID_STATE_CONNECTED, device);
                } else if(state == BluetoothProfile.STATE_DISCONNECTED){
                    mIBluetoothView.notifyBondStatus(IBluetoothView.BondTypeEnum.HID_STATE_DISCONNECTED, device);
                }
            }
        }
    }
    private BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener(){

        @Override
        public void onServiceConnected(int profile, BluetoothProfile bluetoothProfile) {
            LogUtility.d(TAG, "onServiceConnected", "profile:" + profile+", mHidBluetoothDevice:"+mHidBluetoothDevice);
            if (mHidBluetoothDevice == null)
                return;
            if (bluetoothProfile instanceof BluetoothHealth)
                return;

            try {
                LogUtility.d(TAG, "onServiceConnected", "BluetoothInputDevice");
                Method method = bluetoothProfile.getClass().getMethod("connect", new Class[]{BluetoothDevice.class});
                method.setAccessible(true);
                method.invoke(bluetoothProfile, mHidBluetoothDevice);

            } catch (Exception ex){
                LogUtility.d(TAG, "onServiceConnected", "ex:" + ex.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            LogUtility.d(TAG, "onServiceDisconnected", "profile:"+profile);
        }
    };
    private static class SingletonHolder {
        private static final BluetoothPresenter sSingleton = new BluetoothPresenter();

        private SingletonHolder() {
        }
    }

    public static BluetoothPresenter getInstance() {
        return SingletonHolder.sSingleton;
    }

    private BluetoothPresenter() {
        this.mIBluetoothView = null;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = null;
        this.bluetoothConnectActivityReceiver = new BluetoothBroadcastReceiver();
    }

    public BluetoothPresenter init(IBluetoothView bluetoothView, Context context) {
        this.mIBluetoothView = bluetoothView;
        this.mContext = context;
        return this;
    }

    public boolean onStart() {
        LogUtility.d(TAG, "onStart", "");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_PARING_REQUEST);
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_FOUND);
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_BOND_DISCOVERY_FINISHED);
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_BOND_DISCOVERY_STARTED);
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_PAIRING_CANCEL);
        intentFilter.addAction(Constant.BLUETOOTH_ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.setPriority(1000);
        this.mContext.registerReceiver(this.bluetoothConnectActivityReceiver, intentFilter);
        return true;
    }

    public Set<BluetoothDevice> getBondDevices(){
        if (mBluetoothAdapter == null)
            return null;
        return mBluetoothAdapter.getBondedDevices();
    }
    public boolean enable() {
        LogUtility.d(TAG, "enable", "");
        return mBluetoothAdapter.enable();
    }

    public boolean disable() {
        LogUtility.d(TAG, "disable", "");
        return mBluetoothAdapter.disable();
    }

    public boolean isEnabled() {
        LogUtility.d(TAG, "isEnabled", "");
        return mBluetoothAdapter.isEnabled();
    }

    public boolean startScanning() {
        LogUtility.d(TAG, "startScanning", "");
        this.mBluetoothAdapter.startDiscovery();
        return true;
    }
    public boolean stopScanning() {
        LogUtility.d(TAG, "startScanning", "");
        this.mBluetoothAdapter.cancelDiscovery();
        return true;
    }

    public boolean onStop() {
        try {
            LogUtility.d(TAG, "onStop", "");
            this.mContext.unregisterReceiver(this.bluetoothConnectActivityReceiver);
        } catch (IllegalArgumentException ex) {
            ex.toString();
        }
        return true;
    }

    public boolean connect(BluetoothDevice device) {
        LogUtility.d(TAG, "connect", "device:"+device);
        if (device == null)
            return false;
        boolean connectStatus = false;
        try {
            connectStatus = ((Boolean) device.getClass().getMethod("createBond", new Class[0]).invoke(device, new Object[0])).booleanValue();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
        }
        return connectStatus;
    }

    public boolean pair(BluetoothDevice device, String key) {
        LogUtility.d(TAG, "pair", "device:"+device+", key:"+key);
        try {
            setPairingConfirmation(device.getClass(), device, true);
            setPin(device, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    public boolean connectHidDevice(BluetoothDevice hidDevice, int profile){
        LogUtility.d(TAG, "connectHidDevice", "hidDevice:"+hidDevice+", profile:"+profile);
        if (mBluetoothAdapter == null )
            return false;
        mHidBluetoothDevice = hidDevice;

        return mBluetoothAdapter.getProfileProxy(mContext, serviceListener, profile);
    }
    public boolean unPair(BluetoothDevice device) {
        LogUtility.d(TAG, "unPair", "device:"+device);
        if (device == null)
            return false;
        boolean result = false;
        try {
            Method method = device.getClass().getDeclaredMethod("removeBond", new Class[0]);
            method.setAccessible(true);
            result = ((Boolean) method.invoke(device, new Object[0])).booleanValue();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
        }
        return result;
    }

    private void setPairingConfirmation(Class<?> btClass, BluetoothDevice device, boolean isConfirm) throws Exception {
        LogUtility.d(TAG, "setPairingConfirmation", "btClass:"+btClass+", device:"+device+", isConfirm:"+isConfirm);
        btClass.getDeclaredMethod("setPairingConfirmation", new Class[]{Boolean.TYPE}).invoke(device, new Object[]{Boolean.valueOf(isConfirm)});
    }

    private int getBluetoothKey(Intent intent) {
        if (intent == null)
            return 0;
        LogUtility.d(TAG, "getBluetoothKey", "intent:"+intent);

        int type = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
        LogUtility.d(TAG, "getBluetoothKey", "type:"+type);
        if (type == 2 || type == 4 || type == 5) {
            return intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
        }
        return Integer.MIN_VALUE;
    }

    private boolean setPin(BluetoothDevice btDevice, String str) throws Exception {
        try {
            LogUtility.d(TAG, "setPin", "returnValue=" + ((Boolean) btDevice.getClass().getDeclaredMethod("setPin", new Class[]{byte[].class}).invoke(btDevice, new Object[]{str.getBytes()})));
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return true;
    }

    public boolean cancelBondProcess(BluetoothDevice device) {
        LogUtility.d(TAG, "cancelBondProcess", "device=" +device);
        if (device == null)
            return false;
        return cancelBondProcess(device.getClass(), device);
    }

    private boolean cancelBondProcess(Class btClass, BluetoothDevice device) {
        LogUtility.d(TAG, "cancelBondProcess", "btClass=" +btClass+", device:"+device);
        boolean status = false;
        try {
            status = ((Boolean) btClass.getMethod("cancelBondProcess", new Class[0]).invoke(device, new Object[0])).booleanValue();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
        }
        return status;
    }
}