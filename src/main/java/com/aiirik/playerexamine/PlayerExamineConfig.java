package com.aiirik.playerexamine;

import java.awt.Color;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("player-examine")
public interface PlayerExamineConfig extends Config
{
	String OVERLAY_SECTION = "overlay";
	String OVERLAY_COLORS_SECTION = "overlayColors";
	String ITEM_INFO_SECTION = "itemInfo";
	String STATS_HOVER_TOOLTIP_SECTION = "statsHoverTooltip";
	String LIST_STYLE_COLORS_SECTION = "listStyleColors";
	String TOOLTIP_COLORS_SECTION = "tooltipColors";
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
		Short
	}

	enum ThemePreset
	{
		Custom,
		Classic,
		Dark,
		Gold,
		Zaros,
		Guthix,
		Blood
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
		BladeOfSaeldorIthell(ItemID.BLADE_OF_SAELDOR_INFINITE_ITHELL, "Blade of saeldor (white)"),
		BladeOfSaeldorCadarn(ItemID.BLADE_OF_SAELDOR_INFINITE_CADARN, "Blade of saeldor (green)"),
		BladeOfSaeldorAmlodd(ItemID.BLADE_OF_SAELDOR_INFINITE_AMLODD, "Blade of saeldor (purple)"),
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
		name = "Misc",
		description = "Miscellaneous display settings",
		position = 6,
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
		keyName = "overlayTransparency",
		name = "Overlay transparency",
		position = 5,
		section = OVERLAY_SECTION,
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
		position = 6,
		section = OVERLAY_SECTION,
		description = "Apply additional transparency to overlay and tooltip text"
	)
	@Range(min = 0, max = 100)
	default int overlayTextTransparency()
	{
		return 0;
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
		description = "Choose long or shortened total value text in the overlay footer"
	)
	default TotalValueFormat totalValueFormat()
	{
		return TotalValueFormat.Long;
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
		description = "Choose long or shortened GE and HA values in item hover tooltips"
	)
	default TotalValueFormat itemValueFormat()
	{
		return TotalValueFormat.Long;
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
		keyName = "tooltipItemTextColor",
		name = "Item text",
		position = 0,
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
		position = 1,
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
		position = 2,
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
		position = 3,
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
		position = 4,
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
		keyName = "rememberLastTab",
		name = "Remember last tab",
		position = 9,
		section = OVERLAY_SECTION,
		description = "Open new player examines on the last selected overlay tab"
	)
	default boolean rememberLastTab()
	{
		return false;
	}

	@ConfigItem(
		keyName = "openingFlair",
		name = "Opening flair",
		position = 10,
		section = OVERLAY_SECTION,
		description = "Pulse the overlay border briefly after opening an examine"
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
		position = 1,
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
		position = 2,
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
		position = 3,
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
		position = 4,
		section = OVERLAY_COLORS_SECTION,
		description = "Combat text color"
	)
	default Color combatTextColor()
	{
		return new Color(235, 226, 193);
	}

	@ConfigItem(
		keyName = "overlayCloseTextColor",
		name = "X close button",
		position = 11,
		section = OVERLAY_COLORS_SECTION,
		description = "Text color for the close button X"
	)
	default Color xTextColor()
	{
		return new Color(200, 186, 140);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayCloseBorderColor",
		name = "Close border",
		position = 12,
		section = OVERLAY_COLORS_SECTION,
		description = "Border color for the close button"
	)
	default Color xBorderColor()
	{
		return new Color(53, 42, 28);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayCloseColor",
		name = "Close button",
		position = 13,
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
		position = 14,
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
		position = 5,
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
		position = 6,
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
		position = 7,
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
		position = 8,
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
		position = 9,
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
		position = 10,
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
		name = "Opening flair",
		position = 15,
		section = OVERLAY_COLORS_SECTION,
		description = "Opening flair color when using the Custom theme preset"
	)
	default Color openingFlairColor()
	{
		return new Color(215, 125, 40, 180);
	}

}
