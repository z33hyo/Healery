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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;

class DatalogSession {
    private static final Logger LOG = LoggerFactory.getLogger(DatalogSession.class);

    final byte id;
    final int tag;
    final UUID uuid;
    final byte itemType;
    final short itemSize;
    final int timestamp;
    String taginfo = "(unknown)";

    DatalogSession(byte id, UUID uuid, int timestamp, int tag, byte itemType, short itemSize) {
        this.id = id;
        this.tag = tag;
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.itemType = itemType;
        this.itemSize = itemSize;
    }

    GBDeviceEvent[] handleMessage(ByteBuffer buf, int length) {
        return new GBDeviceEvent[]{null};
    }

    String getTaginfo() {
        return taginfo;
    }
}