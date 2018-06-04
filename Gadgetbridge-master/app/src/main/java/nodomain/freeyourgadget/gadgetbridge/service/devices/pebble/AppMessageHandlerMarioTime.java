/*  Copyright (C) 2016-2018 Andreas Shimokawa

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

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;

class AppMessageHandlerMarioTime extends AppMessageHandler {

    private static final int KEY_WEATHER_ICON_ID = 10;
    private static final int KEY_WEATHER_TEMPERATURE = 11;

    AppMessageHandlerMarioTime(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        return new GBDeviceEvent[]{sendBytes};
    }
}
