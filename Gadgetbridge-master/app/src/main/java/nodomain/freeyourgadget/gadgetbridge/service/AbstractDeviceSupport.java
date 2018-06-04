/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.FindPhoneActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsHost;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

// TODO: support option for a single reminder notification when notifications could not be delivered?
// conditions: app was running and received notifications, but device was not connected.
// maybe need to check for "unread notifications" on device for that.

/**
 * Abstract implementation of DeviceSupport with some implementations for
 * common functionality. Still transport independent.
 */
public abstract class AbstractDeviceSupport implements DeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);
    private static final int NOTIFICATION_ID_SCREENSHOT = 8000;

    protected GBDevice gbDevice;
    private BluetoothAdapter btAdapter;
    private Context context;
    private boolean autoReconnect;

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        this.gbDevice = gbDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    /**
     * Default implementation just calls #connect()
     */
    @Override
    public boolean connectFirstTime() {
        return connect();
    }

    @Override
    public boolean isConnected() {
        return gbDevice.isConnected();
    }

    /**
     * Returns true if the device is not only connected, but also
     * initialized.
     *
     * @see GBDevice#isInitialized()
     */
    protected boolean isInitialized() {
        return gbDevice.isInitialized();
    }

    @Override
    public void setAutoReconnect(boolean enable) {
        autoReconnect = enable;
    }

    @Override
    public boolean getAutoReconnect() {
        return autoReconnect;
    }

    @Override
    public GBDevice getDevice() {
        return gbDevice;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof GBDeviceEventVersionInfo) {
            handleGBDeviceEvent((GBDeviceEventVersionInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventAppInfo) {
            handleGBDeviceEvent((GBDeviceEventAppInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventSleepMonitorResult) {
            handleGBDeviceEvent((GBDeviceEventSleepMonitorResult) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventNotificationControl) {
            handleGBDeviceEvent((GBDeviceEventNotificationControl) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventBatteryInfo) {
            handleGBDeviceEvent((GBDeviceEventBatteryInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventFindPhone) {
            handleGBDeviceEvent((GBDeviceEventFindPhone) deviceEvent);
        }
    }

    private void handleGBDeviceEvent(GBDeviceEventFindPhone deviceEvent) {
        Context context = getContext();
        LOG.info("Got GBDeviceEventFindPhone");
        switch (deviceEvent.event) {
            case START:
                Intent startIntent = new Intent(getContext(), FindPhoneActivity.class);
                context.startActivity(startIntent);
                break;
            case STOP:
                Intent intent = new Intent(FindPhoneActivity.ACTION_FOUND);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                break;
            default:
                LOG.warn("unknown GBDeviceEventFindPhone");
        }
    }

    protected void handleGBDeviceEvent(GBDeviceEventVersionInfo infoEvent) {
        Context context = getContext();
        LOG.info("Got event for VERSION_INFO");
        if (gbDevice == null) {
            return;
        }
        gbDevice.setFirmwareVersion(infoEvent.fwVersion);
        gbDevice.setModel(infoEvent.hwVersion);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    private void handleGBDeviceEvent(GBDeviceEventAppInfo appInfoEvent) {
        Context context = getContext();
        LOG.info("Got event for APP_INFO");

        Intent appInfoIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
        int appCount = appInfoEvent.apps.length;
        appInfoIntent.putExtra("app_count", appCount);
        for (Integer i = 0; i < appCount; i++) {
            appInfoIntent.putExtra("app_name" + i.toString(), appInfoEvent.apps[i].getName());
            appInfoIntent.putExtra("app_creator" + i.toString(), appInfoEvent.apps[i].getCreator());
            appInfoIntent.putExtra("app_uuid" + i.toString(), appInfoEvent.apps[i].getUUID().toString());
            appInfoIntent.putExtra("app_type" + i.toString(), appInfoEvent.apps[i].getType().ordinal());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
    }

    private void handleGBDeviceEvent(GBDeviceEventSleepMonitorResult sleepMonitorResult) {
        Context context = getContext();
        LOG.info("Got event for SLEEP_MONIOR_RES");
        Intent sleepMonitorIntent = new Intent(ChartsHost.REFRESH);
        sleepMonitorIntent.putExtra("smartalarm_from", sleepMonitorResult.smartalarm_from);
        sleepMonitorIntent.putExtra("smartalarm_to", sleepMonitorResult.smartalarm_to);
        sleepMonitorIntent.putExtra("recording_base_timestamp", sleepMonitorResult.recording_base_timestamp);
        sleepMonitorIntent.putExtra("alarm_gone_off", sleepMonitorResult.alarm_gone_off);

        LocalBroadcastManager.getInstance(context).sendBroadcast(sleepMonitorIntent);
    }


    private void handleGBDeviceEvent(GBDeviceEventNotificationControl deviceEvent) {
        Context context = getContext();
        LOG.info("Got NOTIFICATION CONTROL device event");
        String action = null;
        switch (deviceEvent.event) {
            case DISMISS:
                action = NotificationListener.ACTION_DISMISS;
                break;
            case DISMISS_ALL:
                action = NotificationListener.ACTION_DISMISS_ALL;
                break;
            case OPEN:
                action = NotificationListener.ACTION_OPEN;
                break;
            case MUTE:
                action = NotificationListener.ACTION_MUTE;
                break;
            case REPLY:
                if (deviceEvent.phoneNumber == null) {
                    deviceEvent.phoneNumber = (String) GBApplication.getIDSenderLookup().lookup(deviceEvent.handle);
                }
                if (deviceEvent.phoneNumber != null) {
                    LOG.info("Got notification reply for SMS from " + deviceEvent.phoneNumber + " : " + deviceEvent.reply);
                    SmsManager.getDefault().sendTextMessage(deviceEvent.phoneNumber, null, deviceEvent.reply, null, null);
                } else {
                    LOG.info("Got notification reply for notification id " + deviceEvent.handle + " : " + deviceEvent.reply);
                    action = NotificationListener.ACTION_REPLY;
                }
                break;
        }
        if (action != null) {
            Intent notificationListenerIntent = new Intent(action);
            notificationListenerIntent.putExtra("handle", deviceEvent.handle);
            if (deviceEvent.reply != null) {
                Prefs prefs = GBApplication.getPrefs();
                String suffix = prefs.getString("canned_reply_suffix", null);
                if (suffix != null && !Objects.equals(suffix, "")) {
                    deviceEvent.reply += suffix;
                }
                notificationListenerIntent.putExtra("reply", deviceEvent.reply);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(notificationListenerIntent);
        }
    }

    protected void handleGBDeviceEvent(GBDeviceEventBatteryInfo deviceEvent) {
        Context context = getContext();
        LOG.info("Got BATTERY_INFO device event");
        gbDevice.setBatteryLevel(deviceEvent.level);
        gbDevice.setBatteryState(deviceEvent.state);

        //show the notification if the battery level is below threshold and only if not connected to charger
        if (deviceEvent.level <= gbDevice.getBatteryThresholdPercent() &&
                (BatteryState.BATTERY_LOW.equals(deviceEvent.state) ||
                        BatteryState.BATTERY_NORMAL.equals(deviceEvent.state))
                ) {
            GB.updateBatteryNotification(context.getString(R.string.notif_battery_low_percent, gbDevice.getName(), String.valueOf(deviceEvent.level)),
                    deviceEvent.extendedInfoAvailable() ?
                            context.getString(R.string.notif_battery_low_percent, gbDevice.getName(), String.valueOf(deviceEvent.level)) + "\n" +
                                    context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(deviceEvent.lastChargeTime.getTime())) +
                                    context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(deviceEvent.numCharges))
                            : ""
                    , context);
        } else {
            GB.removeBatteryNotification(context);
        }

        gbDevice.sendDeviceUpdateIntent(context);
    }

    public void handleGBDeviceEvent(GBDeviceEventDisplayMessage message) {
        GB.log(message.message, message.severity, null);

        Intent messageIntent = new Intent(GB.ACTION_DISPLAY_MESSAGE);
        messageIntent.putExtra(GB.DISPLAY_MESSAGE_MESSAGE, message.message);
        messageIntent.putExtra(GB.DISPLAY_MESSAGE_DURATION, message.duration);
        messageIntent.putExtra(GB.DISPLAY_MESSAGE_SEVERITY, message.severity);

        LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);
    }
}
