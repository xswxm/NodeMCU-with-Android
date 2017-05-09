package com.xswxm.controller;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.xswxm.controller.network.Network;

import static android.R.color.holo_blue_light;
import static com.xswxm.controller.global.Variables.deviceList;
import static com.xswxm.controller.global.Variables.refreshItem;
import static com.xswxm.controller.global.Variables.swipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorScheme(holo_blue_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                refreshItem.setEnabled(false);
                new Network(MainActivity.this).ScanNetwork();
            }
        });
        deviceList = (LinearLayout) findViewById(R.id.deviceList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        refreshItem = (MenuItem) menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_refresh:
                swipeRefreshLayout.setRefreshing(true);
                item.setEnabled(false);
                new Network(this).ScanNetwork();
                return true;
            case R.id.action_add_device:
                Intent intent = new Intent(this, GuideActivity.class);
                startActivity(intent);
                return  true;
/*            case R.id.action_settings:
                return true;*/
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Update device's title (Display Name) if it is available
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                Bundle bundle = data.getExtras();
                String title = bundle.getString("title");
                int deviceID = bundle.getInt("deviceID");
                Device device = (Device) deviceList.findViewById(deviceID);
                device.setText(title);
                break;
            default:
                break;
        }
    }

    /*
     * Add support for screen rotations.
     * There are some problems in this guideActivity which needs further developments
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}