package com.xswxm.controller.network;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.xswxm.controller.Device;
import com.xswxm.controller.R;
import com.xswxm.controller.animation.RefreshAnimation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.xswxm.controller.global.Variables.deviceList;
import static com.xswxm.controller.global.Variables.refreshItem;
import static com.xswxm.controller.global.Variables.swipeRefreshLayout;

/**
 * Created by Air on 4/16/2017.
 */

public class Network {
    private Context myContext;
    private final Handler handler;
    // Cool pool size
    private static final int CORE_POOL_SIZE = 1;
    // Maximum pool size
    private static final int MAXIMUM_POOL_SIZE = 254;
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
            2000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(
            CORE_POOL_SIZE));
    private Process process;
    private Runtime runtime = Runtime.getRuntime();
    // The number of threads executed
    private int threadExecuted = 0;

    public Network(Context context){
        myContext = context;
        handler = new Handler(myContext.getMainLooper());
    }

    private void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    private Integer getIPAddrInt() {
        WifiManager wifiManager = (WifiManager) myContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getIpAddress();
    }

    private String getIPAddr(int ipAddrInt) {
        return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ipAddrInt & 0xff), (ipAddrInt >> 8 & 0xff), (ipAddrInt >> 16 & 0xff), (ipAddrInt >> 24 & 0xff));
    }

    private String getIPAddrPrefix(String ipAddress) {
        if (!ipAddress.equals("")) {
            return ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
        }
        return null;
    }

    /*
     * Scan the whole network and get the connected devices' IP address
     * Referred from http://blog.csdn.net/crazy_zihao/article/details/50523719
     */
    public void ScanNetwork() {
        int ipAddrInt = getIPAddrInt();
        if (ipAddrInt == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myContext, myContext.getString(R.string.scan_network_no_wifi), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    refreshItem.setEnabled(true);
                }
            });
            return;
        }
        String ipAddr = getIPAddr(ipAddrInt);
        String ipAddrPrefix = getIPAddrPrefix(ipAddr);
        deviceList.removeAllViews();

        for (int ipAddrSuffixInt = 1; ipAddrSuffixInt <= 255; ipAddrSuffixInt++) {
            final String finalIPAddr = ipAddrPrefix + Integer.toString(ipAddrSuffixInt);
            if (!finalIPAddr.equals(ipAddr)) {
                final int finalDeviceID = 2000 + ipAddrSuffixInt;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            process = runtime.exec("/system/bin/ping -c 1 " + finalIPAddr);
                            if (process.waitFor() == 0) {
                                Log.e("Scanned IP ", finalIPAddr);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Add new scanned IP to the device list
                                        Device device = new Device(myContext);
                                        device.setId(finalDeviceID);
                                        device.id = finalDeviceID;
                                        device.ipAddr = finalIPAddr;
                                        device.setText(finalIPAddr);
                                        device.setPadding(0,16,0,16);
                                        device.setDeviceEnabled(false);
                                        deviceList.addView(device);
                                        // Check if the IP Address is an available device
                                        // Assign events if it is, otherwise remove this IP Address from the device list
                                        CheckDevice checkDevice = new CheckDevice();
                                        checkDevice.deviceID = finalDeviceID;
                                        checkDevice.device = device;
                                        checkDevice.deviceAddress = "http://"+ finalIPAddr +"/getDevice";
                                        checkDevice.devicePostText = "";
                                        checkDevice.execute(finalIPAddr);

                                    }
                                });
                            }
                        } catch (InterruptedException | IOException ignore) {} finally {
                            process.destroy();
                        }
                        threadExecuted += 1;
                        // Stop refreshing once it scanned the whole network
                        if (threadExecuted == 254) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    swipeRefreshLayout.setRefreshing(false);
                                    refreshItem.setEnabled(true);
                                }
                            });
                            threadExecuted = 0;
                            threadPoolExecutor.shutdownNow();
                        }
                    }
                };
                threadPoolExecutor.execute(runnable);
            }
        }
    }

    /*
     * Send a post request to every exist ip address and check if it is the device we want,
     * otherwise, remove the ip address from the list.
     */
    private class CheckDevice extends PostTask {
        private int deviceID;
        private Device device;
        @Override
        protected void onPostExecute(String deviceTitle) {
            if (deviceTitle.isEmpty()) {
                View device = deviceList.findViewById(deviceID);
                deviceList.removeView(device);
                Log.e("HTTP", "removeDevice succeed!");
            } else {
                String[] str = deviceTitle.split("\u0000");
                Log.e("onPostExecute", Arrays.toString(str));
                device.setText(str[0]);
                if (str[1].equals("1")) {
                    device.setChecked(false);
                } else {
                    device.setChecked(true);
                }
                device.setDeviceEnabled(true);
            }
        }
    }
}
