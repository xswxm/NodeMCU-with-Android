package com.xswxm.controller.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.xswxm.controller.R;
import com.xswxm.controller.animation.RefreshAnimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Air on 4/17/2017.
 */

public class ConfigureDevice extends AppCompatActivity {

    private Button configBtn;
    private Spinner ssidSpinner;
    private WifiManager wifiManager;
    private ImageView refreshSSIDBtn;
    private CheckBox modifyHomeWiFi;
    private int deviceID;
    private String ipAddr;
    private String title;
    private EditText deviceUnameEdt;
    private EditText wifipskEdt;
    private EditText deviceTitleEdt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        // Deal with parameters received from the previous activity
        Intent intent = getIntent();
        wifiManager= (WifiManager)this.getSystemService(Context.WIFI_SERVICE);;
        deviceID = intent.getIntExtra("com.xswxm.controller.deviceID", 10);
        ipAddr = intent.getStringExtra("com.xswxm.controller.ipAddr");
        title = intent.getStringExtra("com.xswxm.controller.title");
        Log.e("Intent", deviceID + ", " + ipAddr + ", " + title);

        deviceUnameEdt = (EditText) findViewById(R.id.edt_uname);
        wifipskEdt = (EditText) findViewById(R.id.edt_psk);
        deviceTitleEdt = (EditText) findViewById(R.id.edt_title);
        deviceTitleEdt.setText(title);

        ssidSpinner = (Spinner) findViewById(R.id.spinner_ssid);
        configBtn = (Button) findViewById(R.id.btn_config);
        configBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wifiSSID  = ssidSpinner.getSelectedItem().toString();
                String wifiUnmae = deviceUnameEdt.getText().toString();
                String wifiPSK = wifipskEdt.getText().toString();
                String deviceTitle = deviceTitleEdt.getText().toString();
                String devicePostText;
                if (modifyHomeWiFi.isChecked()) {
                    if (wifiSSID.isEmpty() || wifiPSK.isEmpty() || deviceTitle.isEmpty()) {
                        Toast.makeText(ConfigureDevice.this, getString(R.string.notify_config_error), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    devicePostText = "ssid=" + wifiSSID +
                            "&psk=" + wifiPSK +
                            "&title=" + deviceTitle;
                    if (!wifiUnmae.isEmpty()) {
                        devicePostText += "&uname=" + wifiUnmae;
                    }
                } else {
                    if (deviceTitle.isEmpty()) {
                        Toast.makeText(ConfigureDevice.this, getString(R.string.notify_config_error_title), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    devicePostText = "title=" + deviceTitle;
                }
                ConfigDevice configDevice = new ConfigDevice();
                configDevice.deviceAddress = "http://" + ipAddr + "/configure";
                configDevice.devicePostText = devicePostText;
                configDevice.execute();
                configBtn.setText(getString(R.string.configuring));
                configBtn.setEnabled(false);
            }
        });
        refreshSSIDBtn = (ImageView) findViewById(R.id.img_ref_ssid);
        refreshSSIDBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RefreshAnimation.showRefreshAnimation(refreshSSIDBtn, ConfigureDevice.this);
                wifiManager.startScan();
                registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            }
        });
        RefreshSSIDSpinner();

        modifyHomeWiFi = (CheckBox) findViewById(R.id.chkBox_HomeWiFi);
        modifyHomeWiFi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ssidSpinner.setEnabled(true);
                    refreshSSIDBtn.setEnabled(true);
                    deviceUnameEdt.setEnabled(true);
                    wifipskEdt.setEnabled(true);
                } else {
                    ssidSpinner.setEnabled(false);
                    refreshSSIDBtn.setEnabled(false);
                    deviceUnameEdt.setEnabled(false);
                    wifipskEdt.setEnabled(false);
                }
            }
        });
        modifyHomeWiFi.setChecked(false);
    }

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
            //ssidSp.addView(childView);
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
     * This method is used to return a scan results of the wifi around users.
     * Because this action need ACCESS_COARSE_LOCATION permission, we separate it from other methods.
     * requestCode is used to justify different tasks.
     */
    private List<ScanResult> getScanResults(int requestCode) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
            return Collections.<ScanResult>emptyList();
        } else {
            return wifiManager.getScanResults();
        }
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
                title = responseMsg;
                toastMsg = getString(R.string.notify_config_done);
                //for test purpose, the following lines are commented
                //configBtn.setText(getString(R.string.btn_config_done));
                //configBtn.setTag("allDone");
            } else {
                toastMsg = getString(R.string.notify_config_failed);
            }
            configBtn.setText(getString(R.string.btn_configure));
            configBtn.setEnabled(true);
            Toast.makeText(ConfigureDevice.this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Return changed values to the main activity.
     * deviceID: help to locate the right device
     * title: set title (Display Name) to the right device.
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("title", title);
        intent.putExtra("deviceID", deviceID);
        setResult(RESULT_OK, intent);
        finish();
    }
}