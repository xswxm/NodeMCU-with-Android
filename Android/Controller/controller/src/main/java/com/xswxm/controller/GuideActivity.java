package com.xswxm.controller;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.xswxm.controller.animation.RefreshAnimation;
import com.xswxm.controller.network.PostTask;
import com.xswxm.controller.utils.SpUtils;

import static com.xswxm.controller.global.Variables.wifiSSID;
import static com.xswxm.controller.global.Variables.wifiPSK;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class GuideActivity extends Activity implements View.OnClickListener {

    private ViewPager viewPager;
    private Button scanQRBtn;

    private Spinner existedSSIDSpinner;
    private ImageView refreshExistedSSIDBtn;
    private Button selectWiFiBtn;
    private Button connectWiFiBtn;
    private TextView wifiStatusTextView;
    private Button configBtn;
    private Spinner ssidSpinner;
    private boolean isLocked = false;
    private boolean isQRScanned = false;
    private boolean isExistedWiFiSelected = false;
    private boolean isWiFiConnected = false;
    private boolean isConfigured = false;
    private WifiManager wifiManager;
    private ImageView refreshSSIDBtn;
    private TextView notifyTxv;
    private boolean first_config;

    //Load guiding pages
    private static final int[] pages = { R.layout.activity_guide_poweron,
            R.layout.activity_guide_scanqr, R.layout.activity_guide_connectwifi, R.layout.activity_guide_configure};

    //Current location
    private int currentIndex;

    /*
     * Load guiding pages and assign events.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Justify if it is the first-time configuration or configuration for fixing devices after installations.
        //and then load different pages to the activity based on users' preference.

        Intent intent = getIntent();
        first_config = intent.getBooleanExtra("com.xswxm.controller.first_config", true);
        if (first_config) {
            pages[1] = R.layout.activity_guide_scanqr;
        } else {
            pages[1] = R.layout.activity_guide_select_exists;
        }

        //Load activity_guide.
        setContentView(R.layout.activity_guide);

        Context context = this;
        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        List<View> views = new ArrayList<View>();

        //Initialize guiding pages
        for (int i = 0; i < pages.length; i++) {
            View view = LayoutInflater.from(this).inflate(pages[i], null);
            switch (i) {
                //for test purpose, i set the current page to page 3.
                case 0:
                    break;
                case 1:
                    if (first_config) {
                        scanQRBtn = (Button) view.findViewById(R.id.btn_scanQR);
                        scanQRBtn.setTag("scanQR");
                        scanQRBtn.setOnClickListener(this);
                    } else {
                        refreshExistedSSIDBtn = (ImageView) view.findViewById(R.id.img_ref_existed_ssid);
                        refreshExistedSSIDBtn.setTag("refreshExistedSSID");
                        refreshExistedSSIDBtn.setOnClickListener(this);
                        existedSSIDSpinner = (Spinner) view.findViewById(R.id.spinner_existed_ssid);
                        existedSSIDSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                String selectSSID = existedSSIDSpinner.getSelectedItem().toString();
                                if (selectSSID.isEmpty()) {
                                    isExistedWiFiSelected = false;
                                    isLocked = true;
                                } else {
                                    wifiSSID = selectSSID;
                                    wifiPSK = SpUtils.getString(GuideActivity.this, wifiSSID);
                                    Log.e("Selected SSID", selectSSID);
                                    isExistedWiFiSelected = true;
                                    isLocked = false;
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                isExistedWiFiSelected = false;
                                isLocked = true;
                            }
                        });
                    }
                    break;
                case 2:
                    connectWiFiBtn = (Button) view.findViewById(R.id.btn_connectWiFi);
                    connectWiFiBtn.setTag("connectWiFi");
                    connectWiFiBtn.setOnClickListener(this);
                    wifiStatusTextView = (TextView) view.findViewById(R.id.text_wifiStatus);
                    break;
                case 3:
                    ssidSpinner = (Spinner) view.findViewById(R.id.spinner_ssid);
                    configBtn = (Button) view.findViewById(R.id.btn_config);
                    configBtn.setTag("config");
                    configBtn.setOnClickListener(this);
                    refreshSSIDBtn = (ImageView) view.findViewById(R.id.img_ref_ssid);
                    refreshSSIDBtn.setTag("refreshSSID");
                    refreshSSIDBtn.setOnClickListener(this);
                    notifyTxv = (TextView) view.findViewById(R.id.txv_config_notify);
                    break;
            }

            views.add(view);

        }

        viewPager = (ViewPager) findViewById(R.id.viewPager_guide);
        //Initialize adapter
        GuideViewPagerAdapter adapter = new GuideViewPagerAdapter(views);
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(new PageChangeListener());
        viewPager.setOnTouchListener(new TouchListener());
    }

    /*
     * Disable guiding slides if users did not finish a task, e.g.: scanning a QR code, connecting to a WiFi AP
     */
    private class TouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return isLocked;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
     * Add support for screen rotations.
     * There are some problems in this guideActivity which needs further developments
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void setCurView(int position) {
        if (position < 0 || position >= pages.length) {
            return;
        }
        viewPager.setCurrentItem(position);
    }

    /*
     * Execute tasks based on tag we assigned to different buttons.
     * scanQR: scan a QR code, which contains the wifi ssid and psk.
     * connectWiFi: force to connect the wifi we just scanned.
     * config: send configurations to device.
     * allDone: it means users have added a device successfully, and they can go back to the main app safely.
     */
    @Override
    public void onClick(View v) {
        switch (v.getTag().toString()) {
            case "scanQR":
                ScanQRCode();
                return;
            case "refreshExistedSSID":
                RefreshAnimation.showRefreshAnimation(refreshExistedSSIDBtn, this);
                wifiManager.startScan();
                registerReceiver(wifiScanReceiverForSelection, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                break;
            case "connectWiFi":
                ForceConnectWiFi();
                return;
            case "config":
                // In the case, wifiSSID and wifiPSK will be reused as the SSID and PSK for the Home WiFi
                EditText deviceUnameEdt = (EditText) findViewById(R.id.edt_uname);
                EditText wifipskEdt = (EditText) findViewById(R.id.edt_psk);
                EditText wifiTitleEdt = (EditText) findViewById(R.id.edt_title);
                String wifiSSID  = ssidSpinner.getSelectedItem().toString();
                String wifiUnmae = deviceUnameEdt.getText().toString();
                String wifiPSK = wifipskEdt.getText().toString();
                String deviceTitle = wifiTitleEdt.getText().toString();
                if (wifiSSID.isEmpty() || wifiPSK.isEmpty() || deviceTitle.isEmpty()) {
                    Toast.makeText(this, getString(R.string.notify_config_error), Toast.LENGTH_SHORT).show();
                } else {
                    //Modify the post message
                    String devicePostText = "ssid=" + wifiSSID +
                            "&psk=" + wifiPSK +
                            "&title=" + deviceTitle;
                    if (!wifiUnmae.isEmpty()) {
                        devicePostText += "&uname=" + wifiUnmae;
                    }
                    ConfigDevice configDevice = new ConfigDevice();
                    configDevice.deviceAddress = "http://192.168.4.1/configure";
                    configDevice.devicePostText = devicePostText;
                    configDevice.execute();
                    //new ConfigDevice().execute();
                    configBtn.setText(getString(R.string.configuring));
                    configBtn.setEnabled(false);
                    isLocked = true;
                }
                return;
            case "refreshSSID":
                RefreshAnimation.showRefreshAnimation(refreshSSIDBtn, this);
                wifiManager.startScan();
                registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                break;
            case "allDone":
                finish();
                return;
            default:
                int position = (Integer) v.getTag();
                setCurView(position);
                break;
        }
    }

    /*
     * Refresh the SSID list once we received new wifi scan results for selecting wifi records
     */
    private final BroadcastReceiver wifiScanReceiverForSelection = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RefreshAnimation.hideRefreshAnimation(refreshExistedSSIDBtn);
            RefreshExistedSSIDSpinner();
            context.unregisterReceiver(wifiScanReceiverForSelection);
        }
    };

    /*
     * Refresh the SSID list once we received new wifi scan results
     */
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RefreshAnimation.hideRefreshAnimation(refreshSSIDBtn);
            RefreshSSIDSpinner();
            context.unregisterReceiver(wifiScanReceiver);
        }
    };

    /*
     * Scan a QR code.
     * setCameraId(0) -> use the major camera
     */
    private void ScanQRCode() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(GuideActivity.this);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        intentIntegrator.setPrompt(getString(R.string.qr_device_scanning));
        intentIntegrator.setCameraId(0);
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.initiateScan();

    }

    /*
     * Refresh existed SSID
     * Check the ssid in the current network match with the saved network configurations in the wifi Manager
     * Added the result to the spinner/list if the record exists.
     */
    private void RefreshExistedSSIDSpinner() {
        List<String> ssidList = new ArrayList<String>();
        // Here, thisActivity is the current activity
        for (ScanResult scanResult : getScanResults(2)) {
            Log.e("SSID list child", scanResult.SSID);
            ssidList.add(scanResult.SSID);
        }

        if (ssidList.size() > 0) {
            //Keep the ssid start with "ESP" and remove others
            //it is important to scan the ssidList from its botton to top because we have to remove items by using item id.
            for (int i = ssidList.size() - 1; i >= 0; i--) {
                if (ssidList.get(i).length() != 10) {
                    Log.e("RemoveSSID",  ssidList.get(i));
                    ssidList.remove(i);
                } else {
                    if (!ssidList.get(i).substring(0,3).equals("ESP")) {
                        Log.e("RemoveSSID",  ssidList.get(i));
                        ssidList.remove(i);
                    }
                }
            }

            //check existence
            if (ssidList.size() > 0) {
                boolean remove;
                for (int i = ssidList.size() - 1; i >= 0; i--) {
                    //Check existence, set a boolean value 'remove' as the tag to remove the item
                    if (SpUtils.getString(this, ssidList.get(i)).isEmpty()) {
                        Log.e("RemoveSSID",  ssidList.get(i));
                        ssidList.remove(i);
                    }
                }
            }

            //Because the ssidList must contain at least one item to let users to select, add an empty item if the the ssidList is empty.
            if (ssidList.size() == 0) {
                ssidList.add("");
                //Notify users did not find any available wifi records in the wifi Manager.
                Toast.makeText(this, "Do not find any resonable wifi records, try to scan again!", Toast.LENGTH_SHORT).show();
            }
        }
        Log.e("SSID List",  ssidList.toString());
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        existedSSIDSpinner.setAdapter(adapter);
    }

    /*
     * Override this method to read the QR code we scanned if users are doing the first-time configuration.
     * Enable users to go to next step if the QR code contains the information we want.
     * otherwise, ask users to scan one again manually.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("onActivityResult", Integer.toString(resultCode));
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() != null) {
                try {
                    Log.e("QR", intentResult.getContents());
                    String[] message = intentResult.getContents().split("\n");
                    if (message[0].substring(0, 4).equals("ESP_") && message[0].length() == 10 && message[1].length() == 32) {
                        wifiSSID = message[0];
                        wifiPSK = message[1];
                        isQRScanned = true;
                        isLocked = false;
                        scanQRBtn.setText(getString(R.string.btn_qr_device_scanned));
                        scanQRBtn.setEnabled(false);
                        // Store the secret after a successful read
                        SpUtils.putString(this, wifiSSID, wifiPSK);
                        Log.e("SpUtils", "secret from QR Code is stored successfully!");
                        Toast.makeText(this, getString(R.string.qr_device_scan_successfully), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.qr_device_scan_failed), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {
                    Toast.makeText(this, getString(R.string.qr_device_scan_failed), Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, getString(R.string.qr_device_scan_cancelled), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int position) {

        }

        @Override
        public void onPageScrolled(int position, float arg1, int arg2) {

        }

        /*
         * EIdentify different tasks and execute them when different pages are selected.
         */
        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 1:
                    if (first_config) {
                        if (!isQRScanned) {
                            isLocked = true;
                            ScanQRCode();    //Scan QR code to import WiFi SSID and Password.
                        }
                    } else {
                        if (!isExistedWiFiSelected) {
                            isLocked = true;
                            RefreshExistedSSIDSpinner();
                        }
                    }
                    break;
                case 2:
                    if (!isWiFiConnected) {
                        isLocked = true;
                        ForceConnectWiFi();    //Force to connect to the access point provided by the device..
                    }
                case 3:
                    notifyTxv.setText(String.format(getString(R.string.notice_config), wifiSSID));
                    RefreshSSIDSpinner();    //Refresh the WiFi SSID list for choosing one as the home wifi.
                    if (!isConfigured) {
                        isLocked = true;
                    }
            }
        }

    }

    /*
     * Get the ssid of APs around users and add them to the ssidSpinner,
     * which allows users to choose the network they want the device to connect to
     * rather than type the ssid manually.
     */
    private void RefreshSSIDSpinner() {
        List<String> ssidList = new ArrayList<String>();
        // Here, thisActivity is the current activity
        for (ScanResult scanResult : getScanResults(1)) {
            Log.e("SSID list child", scanResult.SSID);
            ssidList.add(scanResult.SSID);
        }
        //remove empty items
        //for some unknown reasons, the ssidSpinner contains some empty items.
        // and we have to remove these items
        if (ssidList.size() > 0) {
            for (int i = ssidList.size() - 1; i >= 0; i--) {
                if (ssidList.get(i).isEmpty()) {
                    ssidList.remove(i);
                }
            }
        }
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ssidSpinner.setAdapter(adapter);
    }

    /*
     * force to connect to a WiFi Access Point provided by the device.
     * This method will keep trying to connect the WiFi since the embedded device (ESP8266)
     * used in this case is not quite stable.
     */
    private void ForceConnectWiFi() {
        //Enable WiFi
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        //Add wifi configuration (SSID and Password) to the WiFiConfiguration
        List<WifiConfiguration> list;
        //Update list
        list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration wifiConfiguration : list ) {
            Log.e("WifiConfiguration",  wifiConfiguration.SSID);
            if(wifiConfiguration.SSID != null && wifiConfiguration.SSID.equals("\"" + wifiSSID + "\"")) {
                wifiManager.removeNetwork(wifiConfiguration.networkId);
                Log.e("RemoveWifiConfiguration",  wifiConfiguration.SSID);
            }
        }
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + wifiSSID + "\"";
        conf.preSharedKey = "\""+ wifiPSK +"\"";
        wifiManager.addNetwork(conf);
        Log.e("AddWifiConfiguration", wifiSSID);

        //Scan WiFi
        connectWiFiBtn.setEnabled(false);

        //registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(wifiStateChangedReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

    }

    /*
     * Check the network state once the state is changed.
     * This method is used to ask to connect to device.
     * This is because the embedded device is not quite stable, and the signal could drops
     * and our Android may lost connections or connect to other APs.
     * Therefore, we should justify the cases and let the Android connect our device.
     */
    private final BroadcastReceiver wifiStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String currentSSID = wifiManager.getConnectionInfo().getSSID();
            NetworkInfo.DetailedState currentState = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
            int currentIP =  wifiInfo.getIpAddress();
            Log.e("WiFi State", currentSSID + " " + currentState.name() + " " + Integer.toString(currentIP));
            //Check if the current network SSID is the one we want
            if (currentSSID.equals("\"" + wifiSSID + "\"")) {
                switch (currentState) {
                    case AUTHENTICATING:
                        wifiStatusTextView.setText(getString(R.string.wifi_authenticating));
                        break;
                    case CONNECTING:
                        wifiStatusTextView.setText(getString(R.string.wifi_connecting));
                        break;
                    case SCANNING:
                        wifiStatusTextView.setText(getString(R.string.wifi_scanning));
                        break;
                    case CONNECTED:
                        wifiStatusTextView.setText(getString(R.string.wifi_connected));
                        WiFiConnectedAction(context);
                        break;
                    case OBTAINING_IPADDR:
                        if (currentIP != 0) {
                            wifiStatusTextView.setText(getString(R.string.wifi_connected));
                            WiFiConnectedAction(context);
                        } else {
                            wifiStatusTextView.setText(getString(R.string.wifi_obtaining_ipaddr));
                        }
                        break;
                    case DISCONNECTED:
                        if (autoDisconnect) {
                            wifiStatusTextView.setText(getString(R.string.wifi_disconnected));
                            autoDisconnect = false;
                        } else {
                            wifiStatusTextView.setText(getString(R.string.wifi_disconnected));
                            wifiManager.startScan();
                            ConnectWiFi(context, intent);
                        }
                        break;
                    default:
                        //get scan results and connect
                        wifiManager.startScan();
                        ConnectWiFi(context, intent);
                        break;
                }
            } else {
                //get scan results and connect
                ConnectWiFi(context, intent);
            }
        }
    };

    /*
     * Get the configuration (device's ssid and psk) and connect to the device
     */
    private boolean autoDisconnect = false;
    private void ConnectWiFi(Context context, Intent intent) {
        for (ScanResult scanResult : getScanResults(3)) {
            if (scanResult.SSID.equals(wifiSSID)) {
                for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks()) {
                    if (wifiConfiguration.SSID.equals("\"" + wifiSSID + "\"")) {
                        autoDisconnect = true;
                        wifiManager.disconnect();
                        wifiManager.enableNetwork(wifiConfiguration.networkId, true);
                        wifiManager.reconnect();
                        Log.e("Connecting", wifiSSID);
                        return;
                    }
                }
            }
        }
        Toast.makeText(GuideActivity.this, getString(R.string.notify_device_not_found), Toast.LENGTH_SHORT).show();
        wifiStatusTextView.setText(getString(R.string.wifi_connection_failed));
        WiFiDisconnectedAction(context);
    }

    /*
     * This method is used to return a scan results of the wifi around users.
     * Because this action need ACCESS_COARSE_LOCATION permission, we separate it from other methods.
     * requestCode is used to justify different tasks.
     */
    private List<ScanResult> getScanResults(int requestCode) {
        if (ContextCompat.checkSelfPermission(GuideActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GuideActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
            return Collections.<ScanResult>emptyList();
        } else {
            return wifiManager.getScanResults();
        }
    }

    /*
     * Execute different tasks based on the requestCode if users agree with the permission,
     * otherwise, this activity will be finished.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case 1:
                    RefreshSSIDSpinner();
                    break;
                case 2:
                    RefreshExistedSSIDSpinner();
                    break;
                case 3:
                    ForceConnectWiFi();
                    break;
            }
        } else {
            finish();;
        }
    }

    /*
     * Enable users to go to next stop once the device is connected.
     */
    private void WiFiConnectedAction(Context context) {
        context.unregisterReceiver(wifiStateChangedReceiver);
        isWiFiConnected = true;
        isLocked = false;
        connectWiFiBtn.setText(getString(R.string.btn_wifi_connected));
        connectWiFiBtn.setEnabled(false);
    }

    /*
     * Ask users to connect to the device again if the previous connection is not successful.
     */
    private void WiFiDisconnectedAction(Context context) {
        context.unregisterReceiver(wifiStateChangedReceiver);
        connectWiFiBtn.setEnabled(true);
    }

    /*
     * Send the configuration based on a HTTP Post request.
     */
    private class ConfigDevice extends PostTask {
        @Override
        protected String doInBackground(Object... params) {
            return super.doInBackground(params);
        }

        @Override
        protected void onPostExecute(String responseMsg) {
            super.onPostExecute(responseMsg);
            String toastMsg;
            Log.e("Received from ESP", responseMsg);
            if (!responseMsg.isEmpty()) {
                toastMsg = getString(R.string.notify_config_done);
                configBtn.setText(getString(R.string.btn_configure));
                //for test purpose, the following lines are commented
                //configBtn.setText(getString(R.string.btn_configured));
                //configBtn.setTag("allDone");
            } else {
                toastMsg = getString(R.string.notify_config_failed);
                configBtn.setText(getString(R.string.btn_configure));
            }
            isConfigured = true;
            isLocked = true;
            configBtn.setEnabled(true);
            Toast.makeText(GuideActivity.this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

}
