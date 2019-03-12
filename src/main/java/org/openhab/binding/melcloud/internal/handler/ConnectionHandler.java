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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.melcloud.internal.MelCloudBindingConstants;
import org.openhab.binding.melcloud.json.DeviceStatus;
import org.openhab.binding.melcloud.json.ListDevicesResponse;
import org.openhab.binding.melcloud.json.LoginClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link ConnectionHandler} Manage connection to Mitsubishi Cloud.
 *
 * @author Luca Calcaterra - Initial Contribution
 */
@NonNullByDefault
public class ConnectionHandler {

    private final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private final Configuration config;
    public boolean isConnected = false;
    private static LoginClientResponse loginClientRes = new LoginClientResponse();
    private static ListDevicesResponse listDevicesResponse = new ListDevicesResponse();
    private DeviceStatus deviceStatus = new DeviceStatus();

    public static ListDevicesResponse getListDevicesResponse() {
        return listDevicesResponse;
    }

    public static LoginClientResponse getLoginClientRes() {
        return loginClientRes;
    }

    public ConnectionHandler(Configuration config) {
        this.config = config;
    }

    public boolean login() {
        if (config.get(MelCloudBindingConstants.LOGIN_USERNAME) == null
                || config.get(MelCloudBindingConstants.LOGIN_PASS) == null) {
            logger.debug("null parameter error, check config...!");
        } else {
            try {
                // Document document = null;
                String loginResponse = null;
                JsonObject jsonReq = new JsonObject();
                jsonReq.addProperty("Email", (String) config.get(MelCloudBindingConstants.LOGIN_USERNAME));
                jsonReq.addProperty("Password", (String) config.get(MelCloudBindingConstants.LOGIN_PASS));
                jsonReq.addProperty("Language", (Number) config.get(MelCloudBindingConstants.LOGIN_LANG));
                jsonReq.addProperty("AppVersion", "1.17.5.0");
                jsonReq.addProperty("Persist", false);
                jsonReq.addProperty("CaptchaResponse", (String) null);

                // String content = MelCloudBindingConstants.WEBPASS;
                String content = jsonReq.toString();
                InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

                loginResponse = HttpUtil.executeUrl("POST", (String) config.get(MelCloudBindingConstants.LOGIN_URL),
                        null, stream, "application/json", 20000);
                logger.debug("loginPage=" + loginResponse);
                Gson gson = new Gson();
                ConnectionHandler.loginClientRes = gson.fromJson(loginResponse, LoginClientResponse.class);
                this.isConnected = true;
                logger.debug("LoginClientRes assigned");
                return true;
            } catch (IOException e) {
                logger.error("Connection error to: " + config.get(MelCloudBindingConstants.LOGIN_URL));
                // loginResult.error += "Connection error to " + config.get(MelCloudBindingConstants.LOGIN_URL);
                // loginResult.errorDetail = e.getMessage();
                // loginResult.statusDescr = "@text/offline.uri-error-1";
            } catch (IllegalArgumentException e) {
                // loginResult.error += "caught exception !";
                // loginResult.errorDetail = e.getMessage();
                // loginResult.statusDescr = "@text/offline.uri-error-2";
            }
        }
        return false;
    }

    public boolean pollDevices() {
        if (isConnected) {
            try {
                String response = null;

                Properties headers = new Properties();
                headers.put("X-MitsContextKey", loginClientRes.getLoginData().getContextKey());

                response = HttpUtil.executeUrl("GET",
                        "https://app.melcloud.com/Mitsubishi.Wifi.Client/User/ListDevices", headers, null, null, 20000);
                logger.debug("get response for list devices");
                // return serverDatasHandler;
                Gson gson = new Gson();
                // ServerDatasHandler[] s = gson.fromJson(response, ServerDatasHandler[].class);
                listDevicesResponse = gson.fromJson(response, ListDevicesResponse[].class)[0];

                logger.debug("get response for list devices in json class");
                return true;
            } catch (IOException e) {
                logger.debug("IO exception on PollDevices: " + e);
            } catch (IllegalArgumentException e) {
                logger.debug("IllArguments exception on PollDevices: " + e);
            }
        }
        return false;
    }

    @Nullable
    public DeviceStatus pollDeviceStatus(Integer id) {
        if (isConnected) {
            try {
                String response = null;
                Properties headers = new Properties();
                headers.put("X-MitsContextKey", loginClientRes.getLoginData().getContextKey());
                String url = "https://app.melcloud.com/Mitsubishi.Wifi.Client/Device/Get?" + "id=" + id.toString()
                        + "&buildingID=" + listDevicesResponse.getID();

                response = HttpUtil.executeUrl("GET", url, headers, null, null, 2000);
                Gson gson = new Gson();
                this.deviceStatus = gson.fromJson(response, DeviceStatus.class);
                logger.debug("returned device status");
                return this.deviceStatus;
            } catch (IOException e) {
                logger.debug("IO exception on polling specific device status: " + e);
            } catch (IllegalArgumentException e) {
                logger.debug("IllArguments exception on polling specific device status: " + e);
            }
        }
        return null;
    }

    public boolean sendCommand(DeviceStatus deviceStatusToPost) {
        if (isConnected) {
            try {
                deviceStatusToPost.setHasPendingCommand(true);

                // deviceStatusToPost.setEffectiveFlags(0x1F);

                // Prepare params
                Gson gson = new Gson();

                String content = gson.toJson(deviceStatusToPost, DeviceStatus.class);
                InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

                Properties headers = new Properties();
                headers.put("X-MitsContextKey", loginClientRes.getLoginData().getContextKey());
                String response = HttpUtil.executeUrl("POST",
                        "https://app.melcloud.com/Mitsubishi.Wifi.Client/Device/SetAta", headers, stream,
                        "application/json", 2000);
                logger.debug("command sent with response: " + response);

                // setting the new status of device from response (instead waiting for scheduler refresh)
            } catch (IOException e) {
                logger.debug("IO exception on sending command : " + e);
            } catch (IllegalArgumentException e) {
                logger.debug("IllArguments exception on sending command: " + e);
            }
        }
        return false;
    }
}
