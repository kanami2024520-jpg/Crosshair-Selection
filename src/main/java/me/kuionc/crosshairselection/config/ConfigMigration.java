package me.kuionc.crosshairselection.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.kuionc.crosshairselection.CrosshairSelection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * One-time migration of the old Custom Selection Box JSON into the new GTNHLib config.
 */
public final class ConfigMigration {

    private ConfigMigration() {
    }

    public static boolean migrateFromLegacyJson(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
            JsonElement element = new JsonParser().parse(reader);
            if (element == null || !element.isJsonObject()) {
                return false;
            }
            JsonObject json = element.getAsJsonObject();

            SelectionBoxConfig.enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
            int red = json.has("colourRed") ? json.get("colourRed").getAsInt() : 35;
            int green = json.has("colourGreen") ? json.get("colourGreen").getAsInt() : 110;
            int blue = json.has("colourBlue") ? json.get("colourBlue").getAsInt() : 255;
            int alpha = json.has("alpha") ? json.get("alpha").getAsInt() : 255;
            SelectionBoxConfig.colour = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
            SelectionBoxConfig.thickness = json.has("thickness") ? json.get("thickness").getAsInt() : 1.0F;
            SelectionBoxConfig.blinkAlpha = json.has("blinkAlpha") ? json.get("blinkAlpha").getAsInt() : 99;
            SelectionBoxConfig.blinkSpeed = json.has("blinkSpeed") ? json.get("blinkSpeed").getAsInt() / 100.0F : 0.2F;
            SelectionBoxConfig.rainbow = json.has("rainbow") && json.get("rainbow").getAsBoolean();
            SelectionBoxConfig.linkBlocks = json.has("linkBlocks") && json.get("linkBlocks").getAsBoolean();
            SelectionBoxConfig.showHidden = json.has("showHidden") && json.get("showHidden").getAsBoolean();
            return true;
        } catch (Exception exception) {
            CrosshairSelection.LOGGER.warn("Could not migrate legacy config " + file, exception);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
