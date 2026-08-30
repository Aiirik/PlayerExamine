package com.aiirik.playerexamine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerExamineColorSettings
{
	public static final String[] THEME_COLOR_KEYS = {
		"overlayBackgroundColor",
		"overlayBorderColor",
		"overlayHeaderTextColor",
		"overlaySubTextColor",
		"overlayCloseTextColor",
		"overlayCloseColor",
		"overlayCloseHoverColor",
		"overlaySlotFillColor",
		"overlaySlotEmptyColor",
		"overlaySlotBorderColor",
		"overlaySlotHoverColor",
		"totalGeTextColor",
		"totalHaTextColor",
		"openingFlairColor",
		"valueHighlightColor",
		"activeTabTextColor",
		"inactiveTabTextColor",
		"statsLabelTextColor",
		"statsLevelTextColor"
	};

	private static final String EXPORT_PLUGIN = "player-examine";
	private static final int EXPORT_VERSION = 1;

	private PlayerExamineColorSettings()
	{
	}

	public static String exportToJson(String themeName, Map<String, String> colors, Gson gson)
	{
		JsonObject root = new JsonObject();
		JsonObject colorObject = new JsonObject();
		root.addProperty("plugin", EXPORT_PLUGIN);
		root.addProperty("version", EXPORT_VERSION);
		root.addProperty("theme", themeName);
		root.add("colors", colorObject);

		for (Map.Entry<String, String> entry : colors.entrySet())
		{
			if (isThemeColorKey(entry.getKey()) && entry.getValue() != null)
			{
				colorObject.addProperty(entry.getKey(), entry.getValue());
			}
		}

		return gson.toJson(root);
	}

	public static Map<String, String> importFromJson(String json)
	{
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		JsonElement plugin = root.get("plugin");
		if (plugin == null || !EXPORT_PLUGIN.equals(plugin.getAsString()))
		{
			throw new IllegalArgumentException("Text does not contain Player Examine colors.");
		}

		JsonObject colors = root.getAsJsonObject("colors");
		if (colors == null)
		{
			throw new IllegalArgumentException("Text does not contain theme colors.");
		}

		Map<String, String> importedColors = new LinkedHashMap<>();
		for (String key : colors.keySet())
		{
			if (!isThemeColorKey(key))
			{
				continue;
			}

			JsonElement value = colors.get(key);
			if (value != null && !value.isJsonNull())
			{
				importedColors.put(key, value.getAsString());
			}
		}

		return importedColors;
	}

	public static boolean isThemeColorKey(String key)
	{
		for (String themeColorKey : THEME_COLOR_KEYS)
		{
			if (themeColorKey.equals(key))
			{
				return true;
			}
		}

		return false;
	}
}
