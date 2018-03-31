package jerome.bluetooth;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.widget.AbsListView.CHOICE_MODE_SINGLE;

public class BluetoothActivity extends Activity implements OnFocusChangeListener, IBluetoothView {
    private final String TAG = BluetoothActivity.class.getSimpleName();
    private BluetoothPresenter mBluetoothPresenter = null;
    private ListViewAdapter mDevicePairedListViewAdapter = null;
    private ListViewAdapter mDeviceUnpairedListViewAdapter = null;
    private ListView mDeviceUnpairedListView;
    private ListView mDevicePairedListView;
    private int mNextFocusListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtility.d(TAG, "onCreateView", "");
        mBluetoothPresenter = BluetoothPresenter.getInstance().init(this, this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.fragment_bluetooth, null);
        view.findViewById(R.id.textview_bluetooth_devices_unpair).setOnFocusChangeListener(this);
        view.findViewById(R.id.linearlayout_bluetooth_switch).setOnFocusChangeListener(this);
        view.findViewById(R.id.linearlayout_bluetooth_switch).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_DPAD_LEFT == keyCode && getVisibleBluetoothAdpaterUI()) {
                    setVisibleBluetoothAdapterUI(false);
                    mBluetoothPresenter.disable();
                    return true;
                } else if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode && !getVisibleBluetoothAdpaterUI()) {
                    setVisibleBluetoothAdapterUI(true);
                    mBluetoothPresenter.enable();
                    mDeviceUnpairedListViewAdapter.clearData();
                    mDevicePairedListViewAdapter.clearData();
                    mDeviceUnpairedListViewAdapter.notifyDataSetChanged();
                    mDevicePairedListViewAdapter.notifyDataSetChanged();
                }
                return false;
            }
        });

        view.findViewById(R.id.linearlayout_bluetooth_devices).setOnFocusChangeListener(this);
        view.findViewById(R.id.linearlayout_bluetooth_devices).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothPresenter.startScanning();
                Set<BluetoothDevice> deviceSet = mBluetoothPresenter.getBondDevices();
                if (deviceSet != null) {
                    Iterator<BluetoothDevice> deviceIterator = deviceSet.iterator();
                    while (deviceIterator.hasNext()) {
                        notifyDeviceBond(deviceIterator.next());
                    }
                }
            }
        });
        setContentView(view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtility.d(TAG, "onViewCreated", "");
        mDeviceUnpairedListView = (ListView) findViewById(R.id.listview_bluetooth_devices_unpair);
        mDeviceUnpairedListView.setChoiceMode(CHOICE_MODE_SINGLE);
        mDeviceUnpairedListView.setItemsCanFocus(true);
        mDeviceUnpairedListView.setClickable(true);
        mDeviceUnpairedListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LogUtility.d(TAG, "onItemSelected", "mDeviceUnpairedListView position:"+position);
                drawListItem(mDeviceUnpairedListViewAdapter, view, R.id.relativeLayout_bluetooth_unpair_listview, R.id.listview_bluetooth_devices_unpair);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                LogUtility.d(TAG, "onNothingSelected", "mDeviceUnpairedListView");
                mDeviceUnpairedListView.setSelection(0);
            }
        });
        mDeviceUnpairedListView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                LogUtility.d(TAG, "onFocusChange", "mDeviceUnpairedListView position:"+mDeviceUnpairedListView.getSelectedItemPosition()+",hasFocus:"+hasFocus);
                if (mDeviceUnpairedListView.getSelectedItemPosition() == 0) {
                    enforceListViewUnselected(mDeviceUnpairedListView);
                }
                cleanListViewDrawItem(mDevicePairedListViewAdapter);
                mNextFocusListView = R.id.listview_bluetooth_devices_paired;
            }
        });

        mDevicePairedListView = (ListView) findViewById(R.id.listview_bluetooth_devices_paired);
        mDevicePairedListView.setChoiceMode(CHOICE_MODE_SINGLE);
        mDevicePairedListView.setItemsCanFocus(true);
        mDevicePairedListView.setClickable(true);
        mDevicePairedListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LogUtility.d(TAG, "onItemSelected", "mDevicePairedListView position:"+position);
                drawListItem(mDevicePairedListViewAdapter, view, R.id.relativeLayout_bluetooth_paired_listview, R.id.listview_bluetooth_devices_paired);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                LogUtility.d(TAG, "onNothingSelected", "mDevicePairedListView");
                mDevicePairedListView.setSelection(0);
            }
        });
        mDevicePairedListView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                LogUtility.d(TAG, "onFocusChange", "mDevicePairedListView position:"+mDevicePairedListView.getSelectedItemPosition()+",hasFocus:"+hasFocus);
                if (mDevicePairedListView.getSelectedItemPosition() == 0) {
                    enforceListViewUnselected(mDevicePairedListView);
                }
                cleanListViewDrawItem(mDeviceUnpairedListViewAdapter);
                mNextFocusListView = R.id.listview_bluetooth_devices_unpair;
            }
        });
        mDeviceUnpairedListViewAdapter = new ListViewAdapter(R.layout.adapter_bluetooth_unpair, R.id.relativeLayout_bluetooth_unpair_listview);
        mDevicePairedListViewAdapter = new ListViewAdapter(R.layout.adapter_bluetooth_paired, R.id.relativeLayout_bluetooth_paired_listview);
        mDevicePairedListView.setAdapter(mDevicePairedListViewAdapter);
        mDeviceUnpairedListView.setAdapter(mDeviceUnpairedListViewAdapter);
        Set<BluetoothDevice> deviceSet = mBluetoothPresenter.getBondDevices();
        if (deviceSet != null) {
            Iterator<BluetoothDevice> deviceIterator = deviceSet.iterator();
            while (deviceIterator.hasNext()){
                notifyDeviceBond(deviceIterator.next());
            }
        }
        askPermissions();
    }
    public void onResume() {
        super.onResume();
        LogUtility.d(TAG, "onResume", "");
        mBluetoothPresenter.onStart();
        if (mBluetoothPresenter.isEnabled()) {
            mBluetoothPresenter.startScanning();
        }
        setVisibleBluetoothAdapterUI(mBluetoothPresenter.isEnabled());
    }

    public void onPause() {
        LogUtility.d(TAG, "onPause", "");
        super.onPause();
        mBluetoothPresenter.onStop();
    }
    private void setVisibleBluetoothAdapterUI(boolean visible) {
        LogUtility.d(TAG, "setVisibleBluetoothAdapterUI", "visible:"+visible);
        int flag = visible ? View.VISIBLE : View.GONE;
        int textRes = visible ? R.string.bluetooth_switch_turnon_textview : R.string.bluetooth_switch_turnoff_textview;
        findViewById(R.id.textview_bluetooth_devices_paired).setVisibility(flag);
        findViewById(R.id.listview_bluetooth_devices_paired).setVisibility(flag);
        findViewById(R.id.textview_bluetooth_devices_unpair).setVisibility(flag);
        findViewById(R.id.listview_bluetooth_devices_unpair).setVisibility(flag);
        ((TextView) findViewById(R.id.textView_switch_control)).setText(textRes);
    }

    private void askPermissions() {
        LogUtility.d(TAG, "askPermissions", "");
        Dexter.withActivity(this).
                withPermissions("android.permission.BLUETOOTH",
                        "android.permission.BLUETOOTH_ADMIN",
                        "android.permission.ACCESS_COARSE_LOCATION",
                        "android.permission.ACCESS_FINE_LOCATION").
                withListener(new BluetoothPermission()).check();
    }

    private boolean getVisibleBluetoothAdpaterUI() {
        LogUtility.d(TAG, "getVisibleBluetoothAdpaterUI", "");
        return findViewById(R.id.textview_bluetooth_devices_paired).getVisibility() == View.VISIBLE;
    }

    public int getEnterViewId() {
        LogUtility.d(TAG, "getEnterViewId", "");
        return R.id.linearlayout_bluetooth_switch;
    }
    private void drawListItem(ListViewAdapter currentListViewAdapter, View view, int layoutID, int listViewID){
        LogUtility.d(TAG, "drawListItem", "");
        View focusView = getCurrentFocus();
        if (focusView.getParent() instanceof View) {
            if (((View) focusView.getParent()).getId() != listViewID) {
                return;
            }
        }
        View currentView = view.findViewById(layoutID);
        View previousView = null;
        if (currentListViewAdapter!= null && currentListViewAdapter.getPreviousListItemView() != null)
            previousView = currentListViewAdapter.getPreviousListItemView();
        if (currentView == previousView)
            return;
        drawSelectedView(currentView, true);
        drawSelectedView(currentListViewAdapter.getPreviousListItemView(), false);
        currentListViewAdapter.setPreviousListItemView(currentView);
    }
    private void enforceListViewUnselected(View v) {
        LogUtility.d(TAG, "enforceListViewUnselected", "");
        try {
            Class<ListView> absListViewClass = (Class<ListView>) Class.forName("android.widget.AdapterView");
            Field[] attributes = absListViewClass.getDeclaredFields();
            for (Field f : attributes) {
                if ("mOldSelectedPosition".equals(f.getName())) {
                    f.setAccessible(true);
                    f.set(v, -1);
                }
                else if("mNextSelectedPosition".equals(f.getName())) {
                    f.setAccessible(true);
                    f.set(v, -1);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void drawSelectedView(View view, boolean focus) {
        LogUtility.d(TAG, "drawSelectedView", "focus:"+focus);
        if (view == null)
            return;
        RectangleMargin margin = new RectangleMargin();
        margin.setFocus(focus);
        setMargin(view, margin.getLeft(), margin.getTop(), margin.getRight(), margin.getBottom());
        setBackground(view, margin.getDrawable());
        int fontColorResID = focus ? R.color.bluetooth_textview_focus_font_color : R.color.bluetooth_textview_unfocus_font_color;
        int fontSizeResID = focus ? R.dimen.bluetooth_second_focus_textview_size_40px : R.dimen.bluetooth_second_unfocus_textview_size_39px;
        changeTextViewStyle((TextView)view.findViewById(R.id.textview_bluetooth_main), fontColorResID, fontSizeResID);
    }
    public void onFocusChange(View v, boolean hasFocus) {
        LogUtility.d(TAG, "onFocusChange", "hasFocus:" + hasFocus);
        int fontColorID = hasFocus ? R.color.bluetooth_textview_focus_font_color : R.color.bluetooth_textview_unfocus_font_color;
        int fontSize = hasFocus ? R.dimen.bluetooth_second_focus_textview_size_40px : R.dimen.bluetooth_second_unfocus_textview_size_39px;
        int linearLayoutID = v.getId();

        switch (v.getId()) {
            case R.id.linearlayout_bluetooth_switch: {
                changeTextViewStyle(R.id.textView_bluetooth_switch, fontColorID, fontSize);
                changeTextViewStyle(R.id.textView_switch_control, fontColorID, fontSize);
                break;
            }
            case R.id.linearlayout_bluetooth_devices:{
                changeTextViewStyle(R.id.textView_bluetooth_devices, fontColorID, fontSize);
                break;
            }
            case R.id.textview_bluetooth_devices_unpair:{
                findViewById(mNextFocusListView).requestFocus();
                return;
            }
        }
        cleanListViewDrawItem(mDevicePairedListViewAdapter);
        cleanListViewDrawItem(mDeviceUnpairedListViewAdapter);
        RectangleMargin margin = new RectangleMargin();
        margin.setFocus(hasFocus);
        View view = findViewById(linearLayoutID);
        setMargin(view, margin.getLeft(), margin.getTop(), margin.getRight(), margin.getBottom());
        setBackground(view, margin.getDrawable());
    }
    private void changeTextViewStyle(int textViewID, int colorResID, int fontSizeResID){
        LogUtility.d(TAG, "changeTextViewStyle", "textViewID");
        TextView textView = findViewById(textViewID);
        if (textView == null)
            return;
        textView.setTextColor(getResources().getColor(colorResID));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(fontSizeResID));
    }
    private void changeTextViewStyle(TextView textView, int colorResID, int fontSizeResID){
        LogUtility.d(TAG, "changeTextViewStyle", "textView");
        if (textView == null)
            return;
        textView.setTextColor(getResources().getColor(colorResID));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(fontSizeResID));
    }

    private void setMargin(View view, int left, int top, int right, int bottom) {
        LogUtility.d(TAG, "setMargin", "left:" + left + ", top:" + top + ",right:" + right + ", bottom:" + bottom);
        if (view == null)
            return;
        ViewGroup.LayoutParams test =view.getLayoutParams();
        if (view != null && (view.getLayoutParams() instanceof MarginLayoutParams)) {
            MarginLayoutParams marginParams = (MarginLayoutParams) view.getLayoutParams();
            marginParams.setMargins(left, top, right, bottom);
            view.setLayoutParams(marginParams);
        }
    }

    private void setBackground(View view, Drawable backgroudColor) {
        LogUtility.d(TAG, "setBackground", "");
        if (view == null)
            return;
        if (backgroudColor != null) {
            view.setBackground(backgroudColor);
        } else {
            view.setBackgroundResource(0);
        }
    }
    private void onDeviceFound(BluetoothDevice bluetoothDevice) {
        LogUtility.d(TAG, "onDeviceFound", "");
        if (bluetoothDevice == null)
            return;
        int bondState = bluetoothDevice.getBondState();
        LogUtility.d(TAG, "onDeviceFound", "name:"+bluetoothDevice.getName()+" ,bondState:"+bondState);
        if (bondState == BOND_NONE) {
            mDeviceUnpairedListViewAdapter.addData(bluetoothDevice);
            mDeviceUnpairedListViewAdapter.notifyDataSetChanged();
            if (bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL) {
                mBluetoothPresenter.stopScanning();
                mBluetoothPresenter.connect(bluetoothDevice);
            }
        } else if (bondState == BOND_BONDED) {
            mDevicePairedListViewAdapter.addData(bluetoothDevice);
            mDevicePairedListViewAdapter.notifyDataSetChanged();
        } else if (bondState != BOND_BONDING) {
        }
    }
    private void notifyDeviceUnbond(BluetoothDevice bluetoothDevice) {
        LogUtility.d(TAG, "notifyDeviceUnbond", "");
        if (bluetoothDevice == null)
            return;
        mDevicePairedListViewAdapter.removeData(bluetoothDevice);
        mDevicePairedListViewAdapter.notifyDataSetChanged();
        mDeviceUnpairedListViewAdapter.addData(bluetoothDevice);
        mDeviceUnpairedListViewAdapter.notifyDataSetChanged();
    }

    private void notifyDeviceBond(BluetoothDevice bluetoothDevice) {
        LogUtility.d(TAG, "notifyDeviceBond", "");
        if (bluetoothDevice == null)
            return;
        LogUtility.d(TAG, "notifyDeviceBond", "majorDeviceClass:"+bluetoothDevice.getBluetoothClass().getMajorDeviceClass());
        if (bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL) {
            int INPUT_DEVICE = 4;
            mBluetoothPresenter.connectHidDevice(bluetoothDevice, INPUT_DEVICE);
        }
        else if (bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
            mBluetoothPresenter.connectHidDevice(bluetoothDevice, BluetoothProfile.HEADSET);
        }

        mDevicePairedListViewAdapter.addData(bluetoothDevice);
        mDevicePairedListViewAdapter.notifyDataSetChanged();
        mDeviceUnpairedListViewAdapter.removeData(bluetoothDevice);
        mDeviceUnpairedListViewAdapter.notifyDataSetChanged();
    }
    private void cleanListViewDrawItem(ListViewAdapter listViewAdapter){
        if (listViewAdapter != null) {
            drawSelectedView(listViewAdapter.getPreviousListItemView(), false);
            listViewAdapter.resetPreviousListItemView();
        }
    }

    @Override
    public void notifyBondStatus(BondTypeEnum type, BluetoothDevice bluetoothDevice) {
        LogUtility.d(TAG, "notifyBondStatus", "notifyType:" + type);
        if (bluetoothDevice == null)
            return;
        if (type == BondTypeEnum.BOND) {
            notifyDeviceBond(bluetoothDevice);
        } else if (type == BondTypeEnum.UNBOND) {
            notifyDeviceUnbond(bluetoothDevice);
        } else if (type == BondTypeEnum.BOND_FAIL) {
            showDialog(bluetoothDevice, 1, "");
        } else if (type == BondTypeEnum.HID_STATE_CONNECTED) {
            //todo
        } else if (type == BondTypeEnum.HID_STATE_DISCONNECTED) {
            //todo
        }
    }

    @Override
    public void notifySearchingStatus(SearchingTypeEnum type, BluetoothDevice bluetoothDevice) {
        LogUtility.d(TAG, "notifySearchingStatus", "type:" + type);
        if (type == SearchingTypeEnum.FOUND) {
            onDeviceFound(bluetoothDevice);
        } else if (type == SearchingTypeEnum.SEARCHING_FINISHED) {
            //todo
        }
    }

    public void showConnectionDialog(BluetoothDevice bluetoothDevice, String key) {
        LogUtility.d(TAG, "showConnectionDialog", "key:" + key);
        if (bluetoothDevice == null)
            return;
        showDialog(bluetoothDevice, 0, key);
    }

    public void showUnpairDialog(BluetoothDevice bluetoothDevice) {
        LogUtility.d(TAG, "showUnpairDialog", "");
        if (bluetoothDevice == null)
            return;
        showDialog(bluetoothDevice, 2, "");
    }

    public void showDialog(final BluetoothDevice bluetoothDevice, final int dialogType, final String key) {
        LogUtility.d(TAG, "showDialog", "bluetoothDevice:" + bluetoothDevice + ",dialogType:" + dialogType + ". key:" + key);
        int messageID = 0;
        int leftButtonMessageResID = 0;
        int rightButtonMessageID = 0;
        if (dialogType == 0) {
            messageID = R.string.bluetooth_pair_confirm_message;
            leftButtonMessageResID = R.string.bluetooth_pair_button;
            rightButtonMessageID = R.string.bluetooth_cancel_button;
        } else if (dialogType == 1) {
            messageID = R.string.bluetooth_connection_fail_message;
            leftButtonMessageResID = R.string.bluetooth_ok_button;
        } else if (dialogType == 2) {
            messageID = R.string.bluetooth_unpair_confirm_message;
            leftButtonMessageResID = R.string.bluetooth_ok_button;
            rightButtonMessageID = R.string.bluetooth_cancel_button;
        }
        final Dialog connectionDialog = new Dialog(this, R.style.Translucent_NoTitle);
        connectionDialog.requestWindowFeature(1);
        connectionDialog.setCancelable(false);
        connectionDialog.setContentView(R.layout.dialog_bluetooth_connection);
        Button rightButton = (Button) connectionDialog.findViewById(R.id.button_bluetooth_dialog_cancel);
        final Button leftButton = (Button) connectionDialog.findViewById(R.id.button_bluetooth_dialog_pair);
        String deviceName = bluetoothDevice.getName();
        String message = String.format(getResources().getString(messageID), new Object[]{deviceName});
        ((TextView) connectionDialog.findViewById(R.id.textview_bluetooth_dialog_message)).setText(message);
        if (dialogType == 0) {
            Spannable wordTwo = new SpannableString("\n"+key);
            wordTwo.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.bluetooth_dialog_textview_focus_font_color_ffffff)), 0, wordTwo.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView) connectionDialog.findViewById(R.id.textview_bluetooth_dialog_message)).append(wordTwo);
        }
        leftButton.setText(leftButtonMessageResID);

        if (dialogType == 1) {
            rightButton.setVisibility(View.GONE);
        } else {
            rightButton.setText(rightButtonMessageID);
        }
        leftButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (dialogType == 0) {
                    mBluetoothPresenter.pair(bluetoothDevice, key);
                } else if (dialogType == 2) {
                    mBluetoothPresenter.unPair(bluetoothDevice);
                }
                connectionDialog.dismiss();
            }
        });
        leftButton.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Button button =(Button)v;
                if (hasFocus) {
                    button.setTextColor(getColor(R.color.bluetooth_dialog_textview_focus_font_color_ffffff));
                } else {
                    button.setTextColor(getColor(R.color.bluetooth_dialog_textview_unfocus_font_color_f0f5fa_035));
                }
            }
        });
        rightButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (dialogType == 0) {
                    mBluetoothPresenter.cancelBondProcess(bluetoothDevice);
                }
                connectionDialog.dismiss();
            }
        });
        rightButton.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Button button =(Button)v;
                if (hasFocus) {
                    button.setTextColor(getColor(R.color.bluetooth_dialog_textview_focus_font_color_ffffff));
                } else {
                    button.setTextColor(getColor(R.color.bluetooth_dialog_textview_unfocus_font_color_f0f5fa_035));
                }
            }
        });
        connectionDialog.show();
    }


    private class ListViewAdapter extends BaseAdapter {
        ArrayList<BluetoothDevice> mData = new ArrayList();
        int mLayoutID = 0;
        int mRelativeLayoutID = 0;
        View mPreviousListItemView = null;
        class Holder {
            ImageView imageView;
            TextView mainTextView;
            Holder() {
            }
        }

        public ListViewAdapter(int layoutID, int relativeLayoutID) {
            mLayoutID = layoutID;
            mRelativeLayoutID = relativeLayoutID;
        }

        public void clearData() {
            mData.clear();
        }

        public void setData(ArrayList<BluetoothDevice> data) {
            mData = data;
        }

        public void addData(BluetoothDevice data) {
            int size = mData.size();
            for (int index = 0; index < size; index++) {
                if (mData.get(index).getAddress().equalsIgnoreCase(data.getAddress()))
                    return;
            }
            mData.add(data);

        }

        public void removeData(BluetoothDevice data) {
            mData.remove(data);
        }

        public int getCount() {
            return mData.size();
        }

        public Object getItem(int position) {
            return mData.get(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LogUtility.d(TAG, "getView", "position:" + position);
            Holder holder;
            View view = convertView;
            BluetoothDevice bluetoothDevice =  mData.get(position);
            if (view == null) {
                view = LayoutInflater.from(BluetoothActivity.this).inflate(mLayoutID, parent, false);
                view.setFocusable(true);
                view.setClickable(true);
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LogUtility.d(TAG, "onClick", "position:" + v.getId() + ", mData size:" + mData.size());
                        BluetoothDevice bluetoothDevice =  mData.get(v.getId());
                        if (bluetoothDevice.getBondState() == 10) {
                            mBluetoothPresenter.connect((BluetoothDevice) mData.get(v.getId()));
                        } else if (bluetoothDevice.getBondState() == 12) {
                            showUnpairDialog((BluetoothDevice) mData.get(v.getId()));
                        }
                    }
                });
                holder = new Holder();
                holder.imageView = (ImageView) view.findViewById(R.id.imageview_bluetooth);
                holder.mainTextView = (TextView) view.findViewById(R.id.textview_bluetooth_main);
                view.setTag(holder);
            } else {
                holder = (Holder) view.getTag();
            }
            view.setId(position);
            String name = bluetoothDevice.getName();
            if (name == null || name.length() == 0)
                name = bluetoothDevice.getAddress();
            holder.mainTextView.setText(name);
            holder.imageView.setBackgroundResource(getDeviceTypeImage(bluetoothDevice));
            return view;
        }
        public void setPreviousListItemView(View view){
            mPreviousListItemView = view;
        }
        public View getPreviousListItemView() {
            return mPreviousListItemView;
        }

        public void resetPreviousListItemView() {
            mPreviousListItemView = null;
        }
        private int getDeviceTypeImage( BluetoothDevice bluetoothDevice){
            int deviceType = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
            if (deviceType == BluetoothClass.Device.Major.WEARABLE){
                return R.drawable.bluetooth_device_type_wearable_icon;
            } else if (deviceType == BluetoothClass.Device.Major.COMPUTER) {
                return R.drawable.bluetooth_device_type_computer_icon;
            } else if (deviceType == BluetoothClass.Device.Major.PHONE) {
                return R.drawable.bluetooth_device_type_phone_icon;
            } else if (deviceType == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                return R.drawable.bluetooth_device_type_tv_icon;
            } else {
                return R.drawable.bluetooth_device_type_bluetooth_icon;
            }
        }
    }

    private class RectangleMargin {
        private int bottom;
        private Drawable drawable;
        private int left;
        private int right;
        private int top;
        private int mLeftMargin;
        private int mTopMargin;
        private int mBottomMargin;
        private int mRightMargin;

        public RectangleMargin() {
            mLeftMargin = (int)getResources().getDimension(R.dimen.bluetooth_all_layout_margin_left_27px);
            mBottomMargin = (int)getResources().getDimension(R.dimen.bluetooth_all_layout_margin_bottom_11px);
            mTopMargin = (int)getResources().getDimension(R.dimen.bluetooth_all_layout_margin_top_11px);
            mRightMargin = (int)getResources().getDimension(R.dimen.bluetooth_all_layout_margin_right_20px);
            left = 0;
            right = 0;
            top = 0;
            bottom = 0;
            drawable = ContextCompat.getDrawable(BluetoothActivity.this, R.drawable.bluetooth_linearlayout_focus_border);
        }

        public void setFocus(boolean focus) {
            LogUtility.d(TAG, "setFocus", "focus:" + focus);
            left = focus ? 0 : mLeftMargin;
            top = focus ? (int)getResources().getDimension(R.dimen.bluetooth_all_layout_margin_top_11px) : mTopMargin;
            right = focus ? 0 : mRightMargin;
            bottom = focus ? (int)getResources().getDimension(R.dimen.bluetooth_all_layout_margin_bottom_11px) : mBottomMargin;

            if (focus) {
                drawable = ContextCompat.getDrawable(BluetoothActivity.this, R.drawable.bluetooth_linearlayout_focus_border);
            } else {
                drawable = ContextCompat.getDrawable(BluetoothActivity.this, R.drawable.bluetooth_linearlayout_unfocus_border);
            }
        }

        public int getLeft() {
            return left;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }

        public int getTop() {
            return top;
        }

        public Drawable getDrawable() {
            return drawable;
        }
    }

    class BluetoothPermission implements MultiplePermissionsListener {
        BluetoothPermission() {
        }

        public void onPermissionsChecked(MultiplePermissionsReport report) {
            LogUtility.d(TAG, "onPermissionsChecked", "report:" + report);
            if (!report.areAllPermissionsGranted()) {
                askPermissions();
            }
        }

        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken token) {
            LogUtility.d(TAG, "onPermissionRationaleShouldBeShown", "list:" + list+", token:"+token);
            token.continuePermissionRequest();
        }
    }
}