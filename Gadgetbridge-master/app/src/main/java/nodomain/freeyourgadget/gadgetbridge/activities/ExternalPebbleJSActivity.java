/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo, Uwe Hermann

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;

public class ExternalPebbleJSActivity extends AbstractGBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPebbleJSActivity.class);

    private Uri confUri;

    public static final String START_BG_WEBVIEW = "start_webview";
    public static final String SHOW_CONFIG = "configure";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        boolean showConfig = false;

        UUID currentUUID = null;
        GBDevice currentDevice = null;

        if (extras == null) {
            confUri = getIntent().getData();
            if(confUri.getScheme().equals("gadgetbridge")) {
                try {
                    currentUUID = UUID.fromString(confUri.getHost());
                } catch (IllegalArgumentException e) {
                    LOG.error("UUID in incoming configuration is not a valid UUID: " +confUri.toString());
                }

                //first check if we are still connected to a pebble
                DeviceManager deviceManager = ((GBApplication) getApplication()).getDeviceManager();
                List<GBDevice> deviceList = deviceManager.getDevices();
                for (GBDevice device : deviceList) {
                    if (device.getState() == GBDevice.State.INITIALIZED) {
                        if (device.getType().equals(DeviceType.PEBBLE)) {
                            currentDevice = device;
                            break;
                        } else {
                            LOG.error("attempting to load pebble configuration but a different device type is connected!!!");
                            finish();
                            return;
                        }
                    }
                }
                if (currentDevice == null) {
                    //then try to reconnect to last connected device
                    String btDeviceAddress = GBApplication.getPrefs().getPreferences().getString("last_device_address", null);
                    if (btDeviceAddress != null) {
                        GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                        if(!candidate.isConnected() && candidate.getType() == DeviceType.PEBBLE){
                            Intent intent = new Intent(this, DeviceCommunicationService.class)
                                    .setAction(ACTION_CONNECT)
                                    .putExtra(GBDevice.EXTRA_DEVICE, currentDevice);
                            this.startService(intent);
                            currentDevice = candidate;
                        }
                    }
                }

                showConfig = true; //we are getting incoming configuration data
            }
        } else {
            currentDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
            currentUUID = (UUID) extras.getSerializable(DeviceService.EXTRA_APP_UUID);

            if (extras.getBoolean(START_BG_WEBVIEW, false)) {
                return;
            }
            showConfig = extras.getBoolean(SHOW_CONFIG, false);
        }
    }



    @Override
    protected void onNewIntent(Intent incoming) {
        incoming.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        super.onNewIntent(incoming);
        confUri = incoming.getData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ActivityJSInterface {

        @JavascriptInterface
        public void closeActivity() {
            NavUtils.navigateUpFromSameTask(ExternalPebbleJSActivity.this);
        }
    }
}
