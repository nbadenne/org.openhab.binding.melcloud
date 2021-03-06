/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.melcloud.internal.handler;

import static org.openhab.binding.melcloud.internal.MelCloudBindingConstants.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.melcloud.internal.Connection;
import org.openhab.binding.melcloud.internal.json.DeviceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MelCloudDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author LucaCalcaterra - Initial contribution
 */

public class MelCloudDeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MelCloudDeviceHandler.class);
    // @Nullable
    // private MelCloudConfiguration config;
    private MelCloudBridgeHandler bridgeHandler;
    private DeviceStatus deviceStatus = new DeviceStatus();
    DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;

    public MelCloudDeviceHandler(Thing thing) {
        super(thing);
    }

    public MelCloudBridgeHandler getBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof MelCloudBridgeHandler) {
                this.bridgeHandler = (MelCloudBridgeHandler) handler;
            } else {
                return null;
            }
        }
        return this.bridgeHandler;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handled command {}", command);

        if (command instanceof RefreshType) {
            // only for refresh. unused for now... must be optimized
            logger.debug("refresh type");
        } else {
            Integer effectiveFlags = 0;
            // DeviceStatus cmdtoSend = new DeviceStatus();
            DeviceStatus cmdtoSend = this.deviceStatus;
            if (CHANNEL_POWER.equals(channelUID.getId())) {
                cmdtoSend.setPower((OnOffType) command == OnOffType.ON ? true : false);
                effectiveFlags += 1;
            }
            if (CHANNEL_OPERATION_MODE.equals(channelUID.getId())) {
                if (cmdtoSend.getPower().equals(false)) {
                    cmdtoSend.setPower(true);
                    effectiveFlags += 1;
                }
                cmdtoSend.setOperationMode(((DecimalType) command).intValue());
                effectiveFlags += 2;
            }
            if (CHANNEL_SET_TEMPERATURE.equals(channelUID.getId())) {
                cmdtoSend.setSetTemperature(((QuantityType<Temperature>) command).doubleValue());
                effectiveFlags += 4;
            }
            if (CHANNEL_SET_FAN_SPEED.equals(channelUID.getId())) {
                cmdtoSend.setSetFanSpeed(((DecimalType) command).intValue());
                effectiveFlags += 8;
            }
            if (CHANNEL_VANE_HORIZONTAL.equals(channelUID.getId())) {
                cmdtoSend.setVaneHorizontal(((DecimalType) command).intValue());
                effectiveFlags += 256;
            }
            if (CHANNEL_VANE_VERTICAL.equals(channelUID.getId())) {
                cmdtoSend.setVaneVertical(((DecimalType) command).intValue());
                effectiveFlags += 16;
            }
            logger.debug("devicestatus prepared for send command");
            cmdtoSend.setDeviceID(Integer.parseInt(thing.getProperties().get("deviceID")));
            cmdtoSend.setEffectiveFlags(effectiveFlags);
            // sending command

            MelCloudBridgeHandler bridgeHandler = this.bridgeHandler;
            if (bridgeHandler != null) {
                Connection connection = bridgeHandler.getConnectionHandler();
                if (connection != null) {
                    connection.sendCommand(cmdtoSend);
                    for (Channel channel : getChannels()) {
                        updateChannels(channel.getUID().getId(), this.deviceStatus);
                    }
                }
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing {} handler.", getThing().getThingTypeUID());

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
            return;
        }

        if (getBridgeHandler() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
    }

    // unused for now
    protected List<Channel> getChannels() {
        return getThing().getChannels();
    }

    public void updateChannels(String channelId, DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
        ZoneId zoneid = ZoneId.systemDefault();

        ZoneOffset offset = new DateTimeType().getZonedDateTime().getOffset();
        switch (channelId) {
            case CHANNEL_POWER:
                updateState(CHANNEL_POWER, deviceStatus.getPower() ? OnOffType.ON : OnOffType.OFF);
                break;
            case CHANNEL_OPERATION_MODE:
                updateState(CHANNEL_OPERATION_MODE, new DecimalType(deviceStatus.getOperationMode()));
                break;
            case CHANNEL_SET_TEMPERATURE:
                updateState(CHANNEL_SET_TEMPERATURE,
                        new QuantityType<Temperature>(deviceStatus.getSetTemperature(), SIUnits.CELSIUS));
                break;
            case CHANNEL_SET_FAN_SPEED:
                updateState(CHANNEL_SET_FAN_SPEED, new DecimalType(deviceStatus.getSetFanSpeed()));
                break;
            case CHANNEL_VANE_HORIZONTAL:
                updateState(CHANNEL_VANE_HORIZONTAL, new DecimalType(deviceStatus.getVaneHorizontal()));
                break;
            case CHANNEL_VANE_VERTICAL:
                updateState(CHANNEL_VANE_VERTICAL, new DecimalType(deviceStatus.getVaneVertical()));
                break;
            // Read Only Channels
            case CHANNEL_OFFLINE:
                updateState(CHANNEL_OFFLINE, deviceStatus.getOffline() ? OnOffType.ON : OnOffType.OFF);
                break;
            case CHANNEL_HAS_PENDING_COMMAND:
                updateState(CHANNEL_HAS_PENDING_COMMAND,
                        deviceStatus.getHasPendingCommand() ? OnOffType.ON : OnOffType.OFF);
                break;
            case CHANNEL_ROOM_TEMPERATURE:
                updateState(CHANNEL_ROOM_TEMPERATURE, new DecimalType(deviceStatus.getRoomTemperature()));
                break;
            case CHANNEL_LAST_COMMUNICATION:
                ZonedDateTime zdtlc = ZonedDateTime.ofInstant(
                        LocalDateTime.parse(deviceStatus.getLastCommunication(), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        ZoneOffset.UTC, ZoneId.systemDefault());
                updateState(CHANNEL_LAST_COMMUNICATION, new DateTimeType(zdtlc));

                break;
            case CHANNEL_NEXT_COMMUNICATION:
                ZonedDateTime zdtnc = ZonedDateTime.ofInstant(
                        LocalDateTime.parse(deviceStatus.getNextCommunication(), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        ZoneOffset.UTC, ZoneId.systemDefault());
                updateState(CHANNEL_NEXT_COMMUNICATION, new DateTimeType(zdtnc));
                break;
        }
    }

    @Override
    protected void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }
}
