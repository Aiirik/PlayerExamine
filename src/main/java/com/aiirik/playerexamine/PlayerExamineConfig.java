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
	String TOOLTIP_COLORS_SECTION = "tooltipColors";

	enum TotalValueMode
	{
		None,
		Ge,
		HA,
		Both
	}

	enum OverlayMode
	{
		Item
		{
			@Override
			public String toString()
			{
				return "Default";
			}
		},
		List,
		Hybrid
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

	@ConfigSection(
		name = "Tooltip Colors",
		description = "Equipment tooltip color settings",
		position = 3,
		closedByDefault = true
	)
	String tooltipColorsSection = TOOLTIP_COLORS_SECTION;

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
		keyName = "overlayMode",
		name = "Overlay mode",
		section = OVERLAY_SECTION,
		description = "Choose between item icons or a text list"
	)
	default OverlayMode overlayMode()
	{
		return OverlayMode.Item;
	}

	@ConfigItem(
		keyName = "hideNotVisibleSlots",
		name = "Hide not visible slots",
		section = OVERLAY_SECTION,
		description = "Hide slots marked Not visible from examine in list and hybrid modes"
	)
	default boolean hideNotVisibleSlots()
	{
		return false;
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
		keyName = "showEquipmentBonuses",
		name = "Show equipment bonuses",
		section = ITEM_INFO_SECTION,
		description = "Show weapon and armor bonuses in item hover tooltips"
	)
	default boolean showEquipmentBonuses()
	{
		return false;
	}

	@ConfigItem(
		keyName = "compareEquipmentBonuses",
		name = "Compare equipped item bonuses",
		section = ITEM_INFO_SECTION,
		description = "Show a bonus delta column against your currently equipped item"
	)
	default boolean compareEquipmentBonuses()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tooltipItemTextColor",
		name = "Item text",
		section = TOOLTIP_COLORS_SECTION,
		description = "Hovered item name and tooltip section header color"
	)
	default Color tooltipItemTextColor()
	{
		return new Color(215, 125, 40);
	}

	@ConfigItem(
		keyName = "tooltipOtherTextColor",
		name = "Label text",
		section = TOOLTIP_COLORS_SECTION,
		description = "Tooltip labels and section headers color"
	)
	default Color tooltipOtherTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "tooltipValueTextColor",
		name = "Value text",
		section = TOOLTIP_COLORS_SECTION,
		description = "Tooltip numeric value color"
	)
	default Color tooltipValueTextColor()
	{
		return new Color(245, 240, 228);
	}

	@ConfigItem(
		keyName = "tooltipPositiveBonusColor",
		name = "+ bonus",
		section = TOOLTIP_COLORS_SECTION,
		description = "Tooltip color for positive bonus differences"
	)
	default Color tooltipPositiveBonusColor()
	{
		return new Color(0, 192, 0);
	}

	@ConfigItem(
		keyName = "tooltipNegativeBonusColor",
		name = "- bonus",
		section = TOOLTIP_COLORS_SECTION,
		description = "Tooltip color for negative bonus differences"
	)
	default Color tooltipNegativeBonusColor()
	{
		return new Color(192, 48, 48);
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
