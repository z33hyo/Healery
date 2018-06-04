/*  Copyright (C) 2016-2018 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerSquare extends AppMessageHandler {
    private int CfgKeyCelsiusTemperature;
    private int CfgKeyConditions;
    private int CfgKeyWeatherMode;
    private int CfgKeyUseCelsius;
    private int CfgKeyWeatherLocation;

    AppMessageHandlerSquare(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            CfgKeyCelsiusTemperature = appKeys.getInt("CfgKeyCelsiusTemperature");
            CfgKeyConditions = appKeys.getInt("CfgKeyConditions");
            CfgKeyWeatherMode = appKeys.getInt("CfgKeyWeatherMode");
            CfgKeyUseCelsius = appKeys.getInt("CfgKeyUseCelsius");
            CfgKeyWeatherLocation = appKeys.getInt("CfgKeyWeatherLocation");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the Square watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        return new GBDeviceEvent[]{sendBytes};
    }
}
