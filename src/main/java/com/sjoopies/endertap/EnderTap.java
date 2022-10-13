package com.sjoopies.endertap;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderTap implements CarpetExtension {
    public static final Logger LOGGER = LoggerFactory.getLogger("EnderTap");

    public static void noop() {
    }

    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(EnderTapSettings.class);
    }

    static {
        CarpetServer.manageExtension(new EnderTap());
    }
}
