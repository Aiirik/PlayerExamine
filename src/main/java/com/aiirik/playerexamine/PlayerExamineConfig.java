package com.aiirik.playerexamine;

import java.awt.Color;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(PlayerExamineConfig.CONFIG_GROUP)
public interface PlayerExamineConfig extends Config
{
	String CONFIG_GROUP = "player-examine";
	String OVERLAY_SECTION = "overlay";
	String OVERLAY_COLORS_SECTION = "overlayColors";
	String ITEM_INFO_SECTION = "itemInfo";
	String STATS_HOVER_TOOLTIP_SECTION = "statsHoverTooltip";
	String LIST_STYLE_COLORS_SECTION = "listStyleColors";
	String TOOLTIP_COLORS_SECTION = "tooltipColors";
	String COLOR_SHARING_SECTION = "colorSharing";
	String MISC_SECTION = "misc";

	enum TotalValueMode
	{
		None,
		Ge,
		HA,
		Both
	}

	enum TotalValueFormat
	{
		Long,
		Short,
		Both
	}

	enum ThemePreset
	{
		Custom,
		RuneLite,
		Classic,
		LightClassic
		{
			@Override
			public String toString()
			{
				return "Light classic";
			}
		},
		Light,
		Dark,
		Gold,
		Zaros,
		Guthix,
		Saradomin,
		Blood
	}

	enum TextShadowMode
	{
		Auto,
		On,
		Off
	}

	enum CustomColorStartingPoint
	{
		Classic,
		LightClassic
		{
			@Override
			public String toString()
			{
				return "Light classic";
			}
		},
		Light,
		Dark,
		Gold,
		Zaros,
		Guthix,
		Saradomin,
		Blood,
		Custom1
		{
			@Override
			public String toString()
			{
				return "Custom 1";
			}
		},
		Custom2
		{
			@Override
			public String toString()
			{
				return "Custom 2";
			}
		},
		Custom3
		{
			@Override
			public String toString()
			{
				return "Custom 3";
			}
		},
		SidePanelTheme
		{
			@Override
			public String toString()
			{
				return "Panel theme";
			}
		}
	}

	enum DefaultTab
	{
		Equipment,
		Stats,
		RememberLast
		{
			@Override
			public String toString()
			{
				return "Remember last";
			}
		}
	}

	enum ValueHighlightThreshold
	{
		Off(0, "Off"),
		OneMillion(1_000_000L, "1m+"),
		TenMillion(10_000_000L, "10m+"),
		HundredMillion(100_000_000L, "100m+");

		private final long value;
		private final String name;

		ValueHighlightThreshold(long value, String name)
		{
			this.value = value;
			this.name = name;
		}

		public long getValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum OverlayMode
	{
		Item
		{
			@Override
			public String toString()
			{
				return "Visual";
			}
		},
		List,
		Hybrid
	}

	enum StatsTabMode
	{
		Off,
		Visual,
		List
	}

	enum AttackIcon
	{
		Default(-1, "Default"),
		RuneScimitar(ItemID.RUNE_SCIMITAR, "Rune scimitar"),
		GildedRuneScimitar(ItemID.RUNE_SCIMITAR_GOLD, "Gilded rune scimitar"),
		DragonScimitar(ItemID.DRAGON_SCIMITAR, "Dragon scimitar"),
		DragonScimitarOrnament(ItemID.DRAGON_SCIMITAR_ORNAMENT, "Dragon scimitar (or)"),
		AbyssalWhip(ItemID.ABYSSAL_WHIP, "Abyssal whip"),
		LavaWhip(ItemID.ABYSSAL_WHIP_LAVA, "Lava whip"),
		IceWhip(ItemID.ABYSSAL_WHIP_ICE, "Ice whip"),
		DragonClaws(ItemID.DRAGON_CLAWS, "Dragon claws"),
		DragonClawsOrnament(ItemID.DRAGON_CLAWS_ORNAMENT, "Dragon claws (or)"),
		AbyssalDagger(ItemID.ABYSSAL_DAGGER, "Abyssal dagger"),
		ArmadylGodsword(ItemID.AGS, "Armadyl godsword"),
		SaradominSword(ItemID.SARADOMIN_SWORD, "Saradomin sword"),
		Arclight(ItemID.ARCLIGHT, "Arclight"),
		BladeOfSaeldor(ItemID.BLADE_OF_SAELDOR, "Blade of saeldor"),
		OsmumtensFang(ItemID.OSMUMTENS_FANG, "Osmumten's fang"),
		Voidwaker(ItemID.VOIDWAKER, "Voidwaker"),
		ScytheOfVitur(ItemID.SCYTHE_OF_VITUR, "Scythe of vitur"),
		SoulreaperAxe(ItemID.SOULREAPER, "Soulreaper axe");

		private final int itemId;
		private final String name;

		AttackIcon(int itemId, String name)
		{
			this.itemId = itemId;
			this.name = name;
		}

		public int getItemId()
		{
			return itemId;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigSection(
		name = "Overlay",
		description = "Overlay display settings",
		position = 0
	)
	String overlaySection = OVERLAY_SECTION;

	@ConfigSection(
		name = "Item Hover Tooltip",
		description = "Equipment hover tooltip and wiki settings",
		position = 1,
		closedByDefault = true
	)
	String itemInfoSection = ITEM_INFO_SECTION;

	@ConfigSection(
		name = "Stats Hover Tooltip",
		description = "Stats tab hover tooltip settings",
		position = 2,
		closedByDefault = true
	)
	String statsHoverTooltipSection = STATS_HOVER_TOOLTIP_SECTION;

	@ConfigSection(
		name = "Overlay Colors",
		description = "Overlay color settings",
		position = 3,
		closedByDefault = true
	)
	String overlayColorsSection = OVERLAY_COLORS_SECTION;

	@ConfigSection(
		name = "List Style Colors",
		description = "List and hybrid overlay color settings",
		position = 4,
		closedByDefault = true
	)
	String listStyleColorsSection = LIST_STYLE_COLORS_SECTION;

	@ConfigSection(
		name = "Tooltip Colors",
		description = "Equipment tooltip color settings",
		position = 5,
		closedByDefault = true
	)
	String tooltipColorsSection = TOOLTIP_COLORS_SECTION;

	@ConfigSection(
		name = "Color Sharing",
		description = "Named theme side panel settings",
		position = 6,
		closedByDefault = true
	)
	String colorSharingSection = COLOR_SHARING_SECTION;

	@ConfigSection(
		name = "Misc",
		description = "Miscellaneous display settings",
		position = 7,
		closedByDefault = true
	)
	String miscSection = MISC_SECTION;

	@ConfigItem(
		keyName = "disableUpdateNotifications",
		name = "Disable update notifications",
		position = 3,
		section = MISC_SECTION,
		description = "Hide the chatbox message shown when Player Examine updates"
	)
	default boolean disableUpdateNotifications()
	{
		return false;
	}

	@ConfigItem(
		keyName = "notificationTextColor",
		name = "Notification text",
		position = 4,
		section = MISC_SECTION,
		description = "Chatbox text color for Player Examine update, import, and export messages"
	)
	default Color notificationTextColor()
	{
		return new Color(160, 45, 45);
	}

	@ConfigItem(
		keyName = "enableColorSharingPanel",
		name = "Theme side panel",
		position = 0,
		section = COLOR_SHARING_SECTION,
		description = "Show the Player Examine side panel for creating, importing, exporting, and applying named color themes"
	)
	default boolean enableColorSharingPanel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "overlayMode",
		name = "Overlay mode",
		position = 1,
		section = OVERLAY_SECTION,
		description = "Choose between visual equipment boxes or a text list"
	)
	default OverlayMode overlayMode()
	{
		return OverlayMode.Item;
	}

	@ConfigItem(
		keyName = "defaultTab",
		name = "Default tab",
		position = 2,
		section = OVERLAY_SECTION,
		description = "Choose which tab opens for new player examines"
	)
	default DefaultTab defaultTab()
	{
		return DefaultTab.Equipment;
	}

	@ConfigItem(
		keyName = "themePreset",
		name = "Theme preset",
		position = 0,
		section = OVERLAY_COLORS_SECTION,
		description = "Use a preset overlay theme, or Custom to use the color settings below"
	)
	default ThemePreset themePreset()
	{
		return ThemePreset.Custom;
	}

	@ConfigItem(
		keyName = "statsTabMode",
		name = "Stats tab",
		position = 3,
		section = OVERLAY_SECTION,
		description = "Choose whether to hide the stats tab or show it with icons or text"
	)
	default StatsTabMode statsTabMode()
	{
		return StatsTabMode.Visual;
	}

	@ConfigItem(
		keyName = "overlayWidth",
		name = "Overlay width",
		position = 4,
		section = OVERLAY_SECTION,
		description = "Set the overlay width in pixels"
	)
	@Range(min = 160, max = 220)
	default int overlayWidth()
	{
		return 160;
	}

	@ConfigItem(
		keyName = "overlayCornerRadius",
		name = "Corner radius",
		position = 5,
		section = OVERLAY_SECTION,
		description = "Round the overlay frame corners in pixels"
	)
	@Range(min = 0, max = 6)
	default int overlayCornerRadius()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "overlayTransparency",
		name = "Overlay transparency",
		position = 90,
		section = OVERLAY_COLORS_SECTION,
		description = "Apply additional transparency to overlay backgrounds, borders, and slots"
	)
	@Range(min = 0, max = 100)
	default int overlayTransparency()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "overlayTextTransparency",
		name = "Text transparency",
		position = 91,
		section = OVERLAY_COLORS_SECTION,
		description = "Apply additional transparency to overlay text"
	)
	@Range(min = 0, max = 100)
	default int overlayTextTransparency()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "textShadowMode",
		name = "Text shadow",
		position = 92,
		section = OVERLAY_COLORS_SECTION,
		description = "Choose whether overlay text uses a drop shadow"
	)
	default TextShadowMode textShadowMode()
	{
		return TextShadowMode.Auto;
	}

	@ConfigItem(
		keyName = "boldOverlayText",
		name = "Bold text",
		position = 93,
		section = OVERLAY_COLORS_SECTION,
		description = "Use bold text throughout the overlay"
	)
	default boolean boldOverlayText()
	{
		return false;
	}

	@ConfigItem(
		keyName = "customColorStartingPoint",
		name = "Custom theme",
		position = 1,
		section = OVERLAY_COLORS_SECTION,
		description = "Select colors as a starting point for a custom theme. You may briefly see a RuneLite update popup while the plugin refreshes to show the new colors."
	)
	default CustomColorStartingPoint customColorStartingPoint()
	{
		return CustomColorStartingPoint.Classic;
	}

	@ConfigItem(
		keyName = "totalValueMode",
		name = "Total value",
		position = 7,
		section = OVERLAY_SECTION,
		description = "Show total equipment value in the overlay footer"
	)
	default TotalValueMode totalValueMode()
	{
		return TotalValueMode.Both;
	}

	@ConfigItem(
		keyName = "totalValueFormat",
		name = "Value format",
		position = 8,
		section = OVERLAY_SECTION,
		description = "Choose long, shortened, or combined total value text in the overlay footer"
	)
	default TotalValueFormat totalValueFormat()
	{
		return TotalValueFormat.Both;
	}

	@ConfigItem(
		keyName = "valueHighlightThreshold",
		name = "Value highlight",
		position = 11,
		section = OVERLAY_SECTION,
		description = "Glow equipment slot borders when an item is at or above this GE value"
	)
	default ValueHighlightThreshold valueHighlightThreshold()
	{
		return ValueHighlightThreshold.Off;
	}

	@ConfigItem(
		keyName = "hideNotVisibleSlots",
		name = "Hide not visible slots",
		position = 0,
		section = OVERLAY_SECTION,
		description = "Hide slots marked Not visible from examine in default, list, and hybrid modes"
	)
	default boolean hideNotVisibleSlots()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideMembersSuffix",
		name = "Show (Members)",
		position = 0,
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
		position = 1,
		section = ITEM_INFO_SECTION,
		description = "Show the item grand exchange value in tooltips"
	)
	default boolean showGeValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHaValue",
		name = "Show HA value",
		position = 2,
		section = ITEM_INFO_SECTION,
		description = "Show the item high alchemy value in tooltips"
	)
	default boolean showHaValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showEquipmentBonuses",
		name = "Show equipment bonuses",
		position = 4,
		section = ITEM_INFO_SECTION,
		description = "Show weapon and armor bonuses in item hover tooltips"
	)
	default boolean showEquipmentBonuses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "compareEquipmentBonuses",
		name = "Compare equipped item bonuses",
		position = 5,
		section = ITEM_INFO_SECTION,
		description = "Show a bonus delta column against your currently equipped item"
	)
	default boolean compareEquipmentBonuses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "itemValueFormat",
		name = "Value format",
		position = 3,
		section = ITEM_INFO_SECTION,
		description = "Choose long, shortened, or combined GE and HA values in item hover tooltips"
	)
	default TotalValueFormat itemValueFormat()
	{
		return TotalValueFormat.Both;
	}

	@ConfigItem(
		keyName = "listStyleLabelColor",
		name = "Label text",
		position = 0,
		section = LIST_STYLE_COLORS_SECTION,
		description = "Label text color for list and hybrid overlay rows"
	)
	default Color listStyleLabelColor()
	{
		return new Color(215, 125, 40);
	}

	@ConfigItem(
		keyName = "listStyleValueColor",
		name = "Value text",
		position = 1,
		section = LIST_STYLE_COLORS_SECTION,
		description = "Value text color for list and hybrid overlay rows"
	)
	default Color listStyleValueColor()
	{
		return new Color(245, 240, 228);
	}

	@ConfigItem(
		keyName = "matchTooltipContainerToTheme",
		name = "Match theme",
		position = 0,
		section = TOOLTIP_COLORS_SECTION,
		description = "Use the selected overlay theme or custom theme for tooltip background and outline"
	)
	default boolean matchTooltipContainerToTheme()
	{
		return true;
	}

	@ConfigItem(
		keyName = "matchTooltipLabelValueToTheme",
		name = "Match label/value",
		position = 1,
		section = TOOLTIP_COLORS_SECTION,
		description = "Use the selected overlay theme or custom theme for tooltip label and value text"
	)
	default boolean matchTooltipLabelValueToTheme()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tooltipItemTextColor",
		name = "Item text",
		position = 2,
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
		position = 3,
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
		position = 4,
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
		position = 5,
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
		position = 6,
		section = TOOLTIP_COLORS_SECTION,
		description = "Tooltip color for negative bonus differences"
	)
	default Color tooltipNegativeBonusColor()
	{
		return new Color(192, 48, 48);
	}

	@ConfigItem(
		keyName = "tooltipTransparency",
		name = "Tooltip transparency",
		position = 90,
		section = TOOLTIP_COLORS_SECTION,
		description = "Apply additional transparency to themed tooltip backgrounds and outlines"
	)
	@Range(min = 0, max = 100)
	default int tooltipTransparency()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "tooltipTextTransparency",
		name = "Text transparency",
		position = 91,
		section = TOOLTIP_COLORS_SECTION,
		description = "Apply additional transparency to tooltip text"
	)
	@Range(min = 0, max = 100)
	default int tooltipTextTransparency()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "openWikiOnItemClick",
		name = "Open wiki on item click",
		position = 5,
		section = ITEM_INFO_SECTION,
		description = "Open the Old School RuneScape wiki page when clicking an item slot",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean openWikiOnItemClick()
	{
		return false;
	}

	@ConfigItem(
		keyName = "attackIcon",
		name = "Attack icon",
		position = 0,
		section = MISC_SECTION,
		description = "Choose a weapon icon to use for Attack in the visual stats tab"
	)
	default AttackIcon attackIcon()
	{
		return AttackIcon.Default;
	}

	@ConfigItem(
		keyName = "openingFlair",
		name = "Opening glow",
		position = 93,
		section = OVERLAY_COLORS_SECTION,
		description = "Show a brief glow around the overlay after opening an examine"
	)
	default boolean openingFlair()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showSkillName",
		name = "Show skill name",
		position = 0,
		section = STATS_HOVER_TOOLTIP_SECTION,
		description = "Show the full skill name at the top of the stats hover tooltip"
	)
	default boolean showSkillName()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSkillRank",
		name = "Show skill rank",
		position = 1,
		section = STATS_HOVER_TOOLTIP_SECTION,
		description = "Show the hiscore rank in the stats hover tooltip"
	)
	default boolean showSkillRank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSkillXp",
		name = "Show skill XP",
		position = 2,
		section = STATS_HOVER_TOOLTIP_SECTION,
		description = "Show the skill experience in the stats hover tooltip"
	)
	default boolean showSkillXp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRemainingXp",
		name = "Show remaining XP",
		position = 3,
		section = STATS_HOVER_TOOLTIP_SECTION,
		description = "Show the XP remaining until the next level in the stats hover tooltip"
	)
	default boolean showRemainingXp()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayBackgroundColor",
		name = "Background",
		position = 2,
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
		position = 3,
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
		position = 4,
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
		position = 5,
		section = OVERLAY_COLORS_SECTION,
		description = "Combat text color"
	)
	default Color combatTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "activeTabTextColor",
		name = "Active tab",
		position = 18,
		section = OVERLAY_COLORS_SECTION,
		description = "Selected tab text color"
	)
	default Color activeTabTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "inactiveTabTextColor",
		name = "Inactive tab",
		position = 19,
		section = OVERLAY_COLORS_SECTION,
		description = "Unselected tab text color"
	)
	default Color inactiveTabTextColor()
	{
		return new Color(200, 186, 140);
	}

	@ConfigItem(
		keyName = "statsLabelTextColor",
		name = "Stats label",
		position = 20,
		section = OVERLAY_COLORS_SECTION,
		description = "Stats tab skill label text color"
	)
	default Color statsLabelTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "statsLevelTextColor",
		name = "Stats level",
		position = 21,
		section = OVERLAY_COLORS_SECTION,
		description = "Stats tab level text color"
	)
	default Color statsLevelTextColor()
	{
		return new Color(245, 240, 228);
	}

	@ConfigItem(
		keyName = "overlayCloseTextColor",
		name = "X close button",
		position = 12,
		section = OVERLAY_COLORS_SECTION,
		description = "Text color for the close button X"
	)
	default Color xTextColor()
	{
		return new Color(200, 186, 140);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayCloseColor",
		name = "Close button",
		position = 14,
		section = OVERLAY_COLORS_SECTION,
		description = "Background color for the close button"
	)
	default Color overlayCloseColor()
	{
		return new Color(44, 31, 22);
	}

	@ConfigItem(
		keyName = "overlayCloseHoverColor",
		name = "Close hover",
		position = 15,
		section = OVERLAY_COLORS_SECTION,
		description = "Hover background color for the close button"
	)
	default Color overlayCloseHoverColor()
	{
		return new Color(94, 30, 26);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlaySlotFillColor",
		name = "Slot filled",
		position = 6,
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
		position = 7,
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
		position = 8,
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
		position = 9,
		section = OVERLAY_COLORS_SECTION,
		description = "Equipment slot hover color"
	)
	default Color overlaySlotHoverColor()
	{
		return new Color(150, 122, 76);
	}

	@ConfigItem(
		keyName = "totalGeTextColor",
		name = "GE total",
		position = 10,
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
		position = 11,
		section = OVERLAY_COLORS_SECTION,
		description = "HA total value text color"
	)
	default Color totalHaTextColor()
	{
		return new Color(200, 186, 140);
	}

	@Alpha
	@ConfigItem(
		keyName = "openingFlairColor",
		name = "Opening glow",
		position = 16,
		section = OVERLAY_COLORS_SECTION,
		description = "Opening glow color when using the Custom theme preset"
	)
	default Color openingFlairColor()
	{
		return new Color(215, 125, 40, 180);
	}

	@Alpha
	@ConfigItem(
		keyName = "valueHighlightColor",
		name = "Value highlight",
		position = 17,
		section = OVERLAY_COLORS_SECTION,
		description = "Equipment value highlight color when using the Custom theme preset"
	)
	default Color valueHighlightColor()
	{
		return new Color(255, 190, 64, 190);
	}

}
