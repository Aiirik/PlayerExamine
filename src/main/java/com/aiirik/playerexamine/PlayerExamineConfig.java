package com.aiirik.playerexamine;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("player-examine")
public interface PlayerExamineConfig extends Config
{
	String OVERLAY_SECTION = "overlay";
	String OVERLAY_COLORS_SECTION = "overlayColors";
	String ITEM_INFO_SECTION = "itemInfo";

	enum TotalValueMode
	{
		None,
		Ge,
		HA,
		Both
	}

	@ConfigSection(
		name = "Overlay",
		description = "Overlay display settings",
		position = 0
	)
	String overlaySection = OVERLAY_SECTION;

	@ConfigSection(
		name = "Overlay Colors",
		description = "Overlay color settings",
		position = 2,
		closedByDefault = true
	)
	String overlayColorsSection = OVERLAY_COLORS_SECTION;

	@ConfigSection(
		name = "Item Info",
		description = "Equipment hover and wiki settings",
		position = 1,
		closedByDefault = true
	)
	String itemInfoSection = ITEM_INFO_SECTION;

	@ConfigItem(
		keyName = "disableUpdateNotifications",
		name = "Disable update notifications",
		section = OVERLAY_SECTION,
		description = "Hide the chatbox message shown when Player Examine updates"
	)
	default boolean disableUpdateNotifications()
	{
		return false;
	}

	@ConfigItem(
		keyName = "totalValueMode",
		name = "Total value",
		section = OVERLAY_SECTION,
		description = "Show total equipment value in the overlay footer"
	)
	default TotalValueMode totalValueMode()
	{
		return TotalValueMode.None;
	}

	@ConfigItem(
		keyName = "hideMembersSuffix",
		name = "Show (Members)",
		section = ITEM_INFO_SECTION,
		description = "Show item names with the (Members) suffix"
	)
	default boolean showMembersSuffix()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showGeValue",
		name = "Show GE value",
		section = ITEM_INFO_SECTION,
		description = "Show the item grand exchange value in tooltips"
	)
	default boolean showGeValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showHaValue",
		name = "Show HA value",
		section = ITEM_INFO_SECTION,
		description = "Show the item high alchemy value in tooltips"
	)
	default boolean showHaValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "openWikiOnItemClick",
		name = "Open wiki on item click",
		section = ITEM_INFO_SECTION,
		description = "Open the Old School RuneScape wiki page when clicking an item slot"
	)
	default boolean openWikiOnItemClick()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayBackgroundColor",
		name = "Background",
		section = OVERLAY_COLORS_SECTION,
		description = "Overlay background color"
	)
	default Color overlayBackgroundColor()
	{
		return new Color(31, 24, 17, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayBorderColor",
		name = "Border",
		section = OVERLAY_COLORS_SECTION,
		description = "Overlay border color"
	)
	default Color overlayBorderColor()
	{
		return new Color(118, 94, 60, 255);
	}

	@ConfigItem(
		keyName = "overlayHeaderTextColor",
		name = "Username",
		section = OVERLAY_COLORS_SECTION,
		description = "Username text color"
	)
	default Color usernameTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "overlaySubTextColor",
		name = "Combat",
		section = OVERLAY_COLORS_SECTION,
		description = "Combat text color"
	)
	default Color combatTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "overlayCloseTextColor",
		name = "X",
		section = OVERLAY_COLORS_SECTION,
		description = "X text color"
	)
	default Color xTextColor()
	{
		return new Color(200, 186, 140);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlaySlotFillColor",
		name = "Slot filled",
		section = OVERLAY_COLORS_SECTION,
		description = "Equipment slot fill color when an item is equipped"
	)
	default Color overlaySlotFillColor()
	{
		return new Color(41, 31, 23, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlaySlotEmptyColor",
		name = "Slot empty",
		section = OVERLAY_COLORS_SECTION,
		description = "Equipment slot fill color when empty"
	)
	default Color overlaySlotEmptyColor()
	{
		return new Color(24, 19, 14, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlaySlotBorderColor",
		name = "Slot border",
		section = OVERLAY_COLORS_SECTION,
		description = "Equipment slot border color"
	)
	default Color overlaySlotBorderColor()
	{
		return new Color(111, 89, 56, 255);
	}

	@ConfigItem(
		keyName = "overlaySlotHoverColor",
		name = "Slot hover",
		section = OVERLAY_COLORS_SECTION,
		description = "Equipment slot hover color"
	)
	default Color overlaySlotHoverColor()
	{
		return new Color(150, 122, 76);
	}

	@ConfigItem(
		keyName = "overlayCloseBorderColor",
		name = "X border",
		section = OVERLAY_COLORS_SECTION,
		description = "X button border color"
	)
	default Color xBorderColor()
	{
		return new Color(53, 42, 28);
	}

	@ConfigItem(
		keyName = "overlayCloseColor",
		name = "Close button",
		section = OVERLAY_COLORS_SECTION,
		description = "Close button background color"
	)
	default Color overlayCloseColor()
	{
		return new Color(44, 31, 22);
	}

	@ConfigItem(
		keyName = "overlayCloseHoverColor",
		name = "Close hover",
		section = OVERLAY_COLORS_SECTION,
		description = "Close button hover color"
	)
	default Color overlayCloseHoverColor()
	{
		return new Color(94, 30, 26);
	}

	@ConfigItem(
		keyName = "totalGeTextColor",
		name = "GE total",
		section = OVERLAY_COLORS_SECTION,
		description = "GE total value text color"
	)
	default Color totalGeTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "totalHaTextColor",
		name = "HA total",
		section = OVERLAY_COLORS_SECTION,
		description = "HA total value text color"
	)
	default Color totalHaTextColor()
	{
		return new Color(200, 186, 140);
	}

}
