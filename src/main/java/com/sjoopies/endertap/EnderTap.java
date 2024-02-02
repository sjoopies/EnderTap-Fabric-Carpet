package com.sjoopies.endertap;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class EnderTap implements CarpetExtension {
    public static final Logger LOGGER = LoggerFactory.getLogger("EnderTap");

    public static void noop() {
    }
    static {
        CarpetServer.manageExtension(new EnderTap());
    }


    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(EnderTapSettings.class);
    }


    public Map<String, String> canHasTranslations(String lang) {
        InputStream langFile = EnderTap.class.getClassLoader().getResourceAsStream("assets/endertap/lang/%s.json".formatted(lang));
        if (langFile == null) {
            return Collections.emptyMap();
        }
        String jsonData;
        try {
            jsonData = IOUtils.toString(langFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        return new GsonBuilder().create().fromJson(jsonData, new TypeToken<Map<String, String>>() {}.getType());
    }
}
