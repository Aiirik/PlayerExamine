package com.aiirik.playerexamine.overlay;

import com.aiirik.playerexamine.PlayerExamineConfig;
import com.aiirik.playerexamine.PlayerExaminePlugin;
import com.aiirik.playerexamine.model.PlayerExamineData;
import com.aiirik.playerexamine.model.PlayerExamineData.EquipmentEntry;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.AsyncBufferedImage;

public class PlayerExamineOverlay extends Overlay
{
	private static final int BASE_FRAME_WIDTH = 188;
	private static final int BASE_FRAME_HEIGHT = 248;
	private static final int TITLE_BAR_HEIGHT = 18;
	private static final int SLOT_SIZE = 34;
	private static final int SLOT_GAP_X = 12;
	private static final int SLOT_GAP_Y = 6;
	private static final int GRID_START_X = 31;
	private static final int GRID_START_Y = 28;
	private static final int SLOT_INSET_X = 4;
	private static final int SLOT_INSET_Y = 3;
	private static final int FOOTER_BOTTOM_MARGIN = 5;
	private static final int FOOTER_TOP_MARGIN = 4;
	private static final int FOOTER_LINE_GAP = 1;
	private static final int FOOTER_SIDE_PADDING = 12;
	private static final int SLOT_GRID_BOTTOM = GRID_START_Y + (4 * (SLOT_SIZE + SLOT_GAP_Y)) + SLOT_SIZE;
	private static final int LIST_START_Y = 30;
	private static final int LIST_SIDE_PADDING = 8;
	private static final int LIST_ICON_SIZE = 16;
	private static final int LIST_ICON_GAP = 4;
	private static final int LIST_ROW_PADDING_Y = 2;
	private static final int HYBRID_ICON_SIZE = 20;
	private static final int HYBRID_ICON_GAP = 6;
	private static final int HYBRID_ROW_PADDING_Y = 4;
	private final PlayerExaminePlugin plugin;
	private final PlayerExamineConfig config;
	private final Client client;
	private final ItemManager itemManager;
	private final TooltipManager tooltipManager;
	private volatile RenderState renderState = RenderState.empty();

	@Inject
	public PlayerExamineOverlay(
		PlayerExaminePlugin plugin,
		PlayerExamineConfig config,
		Client client,
		ItemManager itemManager,
		TooltipManager tooltipManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		this.itemManager = itemManager;
		this.tooltipManager = tooltipManager;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DETACHED);
		setMovable(true);
		setSnappable(true);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		PlayerExamineData data = plugin.getCurrentData();
		if (data == null)
		{
			renderState = RenderState.empty();
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(FontManager.getRunescapeSmallFont());

		PlayerExamineConfig.OverlayMode overlayMode = config.overlayMode();
		int frameWidth = config.overlayWidth();
		ContentLayout contentLayout = buildContentLayout(graphics, data, overlayMode, frameWidth);
		FooterLayout footerLayout = buildFooterLayout(graphics, data, contentLayout.getContentBottom(), frameWidth);
		int frameHeight = Math.max(contentLayout.getFrameHeight(), footerLayout.getFrameHeight());
		Rectangle closeButton = new Rectangle(frameWidth - 20, 2, 16, 14);

		drawFrame(graphics, data, closeButton, frameHeight, frameWidth);
		drawContent(graphics, contentLayout, frameWidth);
		drawFooter(graphics, footerLayout, frameWidth);

		renderState = new RenderState(closeButton, contentLayout.getSlots(), contentLayout.getRows(), overlayMode, new Dimension(frameWidth, frameHeight));
		addHoverTooltip();
		return renderState.getDimension();
	}

	public RenderState getRenderState()
	{
		return renderState;
	}

	private void drawFrame(Graphics2D graphics, PlayerExamineData data, Rectangle closeButton, int frameHeight, int frameWidth)
	{
		graphics.setColor(config.overlayBorderColor());
		graphics.drawRect(0, 0, frameWidth - 1, frameHeight - 1);

		Color backgroundColor = config.overlayBackgroundColor();
		if (backgroundColor.getAlpha() > 0)
		{
			graphics.setColor(backgroundColor);
			graphics.fillRect(1, 1, frameWidth - 2, frameHeight - 2);
		}

		graphics.setColor(config.overlayBorderColor());
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawLine(2, TITLE_BAR_HEIGHT - 1, frameWidth - 3, TITLE_BAR_HEIGHT - 1);

		FontMetrics metrics = graphics.getFontMetrics();
		int titleBaseline = ((TITLE_BAR_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent() + 1;

		String title = fitText(graphics, data.getName(), Math.max(0, frameWidth - 114));
		drawShadowText(graphics, title, 8, titleBaseline, config.usernameTextColor());

		String combat = "Combat " + data.getCombatLevel();
		drawShadowText(graphics, combat, Math.max(8, frameWidth - 92), titleBaseline, config.combatTextColor());

		boolean hoverClose = isMouseInside(closeButton);
		graphics.setColor(hoverClose ? config.overlayCloseHoverColor() : config.overlayCloseColor());
		graphics.fillRect(closeButton.x, closeButton.y, closeButton.width, closeButton.height);
		graphics.setColor(config.xBorderColor());
		graphics.drawRect(closeButton.x, closeButton.y, closeButton.width, closeButton.height);
		drawCenteredShadowText(graphics, "X", closeButton, config.xTextColor());
	}

	private ContentLayout buildContentLayout(Graphics2D graphics, PlayerExamineData data, PlayerExamineConfig.OverlayMode overlayMode, int frameWidth)
	{
		Map<String, EquipmentEntry> entries = new HashMap<>();
		for (EquipmentEntry entry : data.getEquipment())
		{
			entries.put(entry.getSlotName().toLowerCase(), entry);
		}

		Rectangle[] slotBoxes = buildSlotBoxes(frameWidth);
		List<SlotState> slots = new ArrayList<>();
		slots.add(createSlot("helmet", slotBoxes[0], entries.get("head"), true, true));
		slots.add(createSlot("cape", slotBoxes[1], entries.get("cape"), true, true));
		slots.add(createSlot("necklace", slotBoxes[2], entries.get("amulet"), true, true));
		slots.add(createSlot("arrows", slotBoxes[3], null, !config.hideNotVisibleSlots(), false));
		slots.add(createSlot("weapon", slotBoxes[4], entries.get("weapon"), true, true));
		slots.add(createSlot("body", slotBoxes[5], entries.get("torso"), true, true));
		slots.add(createSlot("offhand", slotBoxes[6], entries.get("shield"), true, true));
		slots.add(createSlot("legs", slotBoxes[7], entries.get("legs"), true, true));
		slots.add(createSlot("gloves", slotBoxes[8], entries.get("hands"), true, true));
		slots.add(createSlot("boots", slotBoxes[9], entries.get("boots"), true, true));
		slots.add(createSlot("ring", slotBoxes[10], null, !config.hideNotVisibleSlots(), false));

		if (overlayMode == PlayerExamineConfig.OverlayMode.List || overlayMode == PlayerExamineConfig.OverlayMode.Hybrid)
		{
			return buildListContent(graphics, data, slots, overlayMode == PlayerExamineConfig.OverlayMode.Hybrid, frameWidth);
		}

		return ContentLayout.forSlots(slots);
	}

	private SlotState createSlot(String key, Rectangle bounds, EquipmentEntry entry, boolean drawFrame, boolean showEmptyTooltip)
	{
		return new SlotState(key, bounds, entry, drawFrame, showEmptyTooltip);
	}

	private ContentLayout buildListContent(Graphics2D graphics, PlayerExamineData data, List<SlotState> slots, boolean showIcons, int frameWidth)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		List<ListRow> rows = new ArrayList<>();
		int currentY = LIST_START_Y;
		int rowHeight = showIcons
			? Math.max((metrics.getHeight() * 2), HYBRID_ICON_SIZE) + HYBRID_ROW_PADDING_Y
			: Math.max(metrics.getHeight(), LIST_ICON_SIZE) + LIST_ROW_PADDING_Y;

		for (SlotState slot : slots)
		{
			EquipmentEntry entry = slot.getEntry();
			boolean hasItem = entry != null && entry.hasItem();
			boolean visibleSlot = hasItem || slot.isShowEmptyTooltip();
			if (config.hideNotVisibleSlots() && !visibleSlot)
			{
				continue;
			}

			if (showIcons)
			{
				rows.add(new ListRow(
					slot,
					new Rectangle(LIST_SIDE_PADDING, currentY, frameWidth - (LIST_SIDE_PADDING * 2), rowHeight),
					formatSlotLabel(slot.getKey()),
					buildSlotValue(slot),
					true,
					true));
			}
			else
			{
				rows.add(new ListRow(
					slot,
					new Rectangle(LIST_SIDE_PADDING, currentY, frameWidth - (LIST_SIDE_PADDING * 2), rowHeight),
					formatSlotLabel(slot.getKey()),
					buildSlotValue(slot),
					false,
					false));
			}
			currentY += rowHeight + 1;
		}

		return ContentLayout.forRows(slots, rows, rows.isEmpty() ? LIST_START_Y : currentY - 1);
	}

	private String buildSlotValue(SlotState slot)
	{
		EquipmentEntry entry = slot.getEntry();
		if (entry != null && entry.hasItem())
		{
			return getDisplayItemName(entry);
		}
		if (slot.isShowEmptyTooltip())
		{
			return "None";
		}
		return "Not visible";
	}

	private static String formatSlotLabel(String key)
	{
		if (key == null || key.isEmpty())
		{
			return "";
		}

		StringBuilder builder = new StringBuilder(key.length());
		boolean capitalizeNext = true;
		for (int i = 0; i < key.length(); i++)
		{
			char c = key.charAt(i);
			if (capitalizeNext)
			{
				builder.append(Character.toUpperCase(c));
				capitalizeNext = false;
			}
			else
			{
				builder.append(c);
			}
		}
		return builder.toString();
	}

	private void drawContent(Graphics2D graphics, ContentLayout contentLayout, int frameWidth)
	{
		if (contentLayout.isListMode())
		{
			drawList(graphics, contentLayout.getRows(), frameWidth);
			return;
		}

		drawSlots(graphics, contentLayout.getSlots());
	}

	private void drawSlots(Graphics2D graphics, List<SlotState> slots)
	{
		for (SlotState slot : slots)
		{
			drawSlot(graphics, slot);
		}
	}

	private void drawList(Graphics2D graphics, List<ListRow> rows, int frameWidth)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		for (ListRow row : rows)
		{
			Rectangle bounds = row.getBounds();
			int textX = bounds.x;
			if (row.isShowIcon())
			{
				EquipmentEntry entry = row.getSlot().getEntry();
				if (entry != null && entry.hasItem() && entry.getItemId() >= 0)
				{
					AsyncBufferedImage sprite = itemManager.getImage(entry.getItemId());
					if (sprite != null)
					{
						int iconSize = HYBRID_ICON_SIZE;
						int iconY = bounds.y + ((bounds.height - iconSize) / 2);
						graphics.drawImage(sprite, bounds.x, iconY, iconSize, iconSize, null);
					}
				}

				textX += HYBRID_ICON_SIZE + HYBRID_ICON_GAP;
			}

			String primaryText = row.getPrimaryText();
			String secondaryText = row.getSecondaryText();
			int availableWidth = frameWidth - textX - LIST_SIDE_PADDING;
			if (secondaryText == null)
			{
				int y = bounds.y + metrics.getAscent();
				String fittedText = fitText(graphics, primaryText, availableWidth);
				drawShadowText(graphics, fittedText, textX, y, config.listStyleLabelColor());
				continue;
			}

			if (!row.isStackedText())
			{
				int y = bounds.y + metrics.getAscent();
				String fittedPrimary = fitText(graphics, primaryText, availableWidth);
				int labelWidth = metrics.stringWidth(fittedPrimary);
				int separatorWidth = metrics.stringWidth(": ");
				int valueWidth = availableWidth - labelWidth - separatorWidth;
				if (valueWidth < 0)
				{
					valueWidth = 0;
				}

				String fittedSecondary = fitText(graphics, secondaryText, valueWidth);
				drawShadowText(graphics, fittedPrimary + ": ", textX, y, config.listStyleLabelColor());
				drawShadowText(graphics, fittedSecondary, textX + labelWidth + separatorWidth, y, config.listStyleValueColor());
				continue;
			}

			int topBaseline = bounds.y + metrics.getAscent();
			int bottomBaseline = topBaseline + metrics.getHeight();
			String fittedPrimary = fitText(graphics, primaryText, availableWidth);
			String fittedSecondary = fitText(graphics, secondaryText, availableWidth);
			drawShadowText(graphics, fittedPrimary, textX, topBaseline, config.listStyleLabelColor());
			drawShadowText(graphics, fittedSecondary, textX, bottomBaseline, config.listStyleValueColor());
		}
	}

	private void drawSlot(Graphics2D graphics, SlotState slot)
	{
		Rectangle bounds = slot.getBounds();
		EquipmentEntry entry = slot.getEntry();
		boolean hasItem = entry != null && entry.hasItem();
		boolean hover = isMouseInside(bounds);

		if (slot.isDrawFrame())
		{
			graphics.setColor(hover ? config.overlaySlotHoverColor() : config.overlaySlotBorderColor());
			graphics.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 1, bounds.height + 1);

			Color fillColor = hasItem ? config.overlaySlotFillColor() : config.overlaySlotEmptyColor();
			if (fillColor.getAlpha() > 0)
			{
				graphics.setColor(fillColor);
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}

		if (hasItem && entry.getItemId() >= 0)
		{
			AsyncBufferedImage sprite = itemManager.getImage(entry.getItemId());
			if (sprite != null)
			{
				int iconSize = SLOT_SIZE - (SLOT_INSET_Y * 2);
				graphics.drawImage(sprite, bounds.x + SLOT_INSET_X, bounds.y + SLOT_INSET_Y, iconSize, iconSize, null);
			}
		}
	}

	private Rectangle[] buildSlotBoxes(int frameWidth)
	{
		int gridWidth = (3 * SLOT_SIZE) + (2 * SLOT_GAP_X);
		int gridStartX = Math.max((frameWidth - gridWidth) / 2, 0);

		return new Rectangle[] {
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX, GRID_START_Y + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + 2 * (SLOT_SIZE + SLOT_GAP_X), GRID_START_Y + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX, GRID_START_Y + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + 2 * (SLOT_SIZE + SLOT_GAP_X), GRID_START_Y + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + 3 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX, GRID_START_Y + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + 2 * (SLOT_SIZE + SLOT_GAP_X), GRID_START_Y + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE)
		};
	}

	private void addHoverTooltip()
	{
		RenderState state = renderState;
		if (state == null || state.isEmpty())
		{
			return;
		}

		Point mouse = client.getMouseCanvasPosition();
		Rectangle bounds = getBounds();
		if (mouse == null || bounds == null || !bounds.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		int localX = mouse.getX() - bounds.x;
		int localY = mouse.getY() - bounds.y;
		if (state.isListMode())
		{
			for (ListRow row : state.getRows())
			{
				if (row.getBounds().contains(localX, localY))
				{
					SlotState slot = row.getSlot();
					if (slot.getEntry() != null && slot.getEntry().hasItem())
					{
						addItemTooltips(slot.getEntry());
					}
					else if (slot.isShowEmptyTooltip())
					{
						tooltipManager.add(new Tooltip("Nothing equipped"));
					}
					else if (!config.hideNotVisibleSlots())
					{
						tooltipManager.add(new Tooltip("Not visible from examine"));
					}
					return;
				}
			}
			return;
		}

		for (SlotState slot : state.getSlots())
		{
			if (slot.getBounds().contains(localX, localY))
			{
				if (slot.getEntry() != null && slot.getEntry().hasItem())
				{
					addItemTooltips(slot.getEntry());
				}
				else if (slot.isShowEmptyTooltip())
				{
					tooltipManager.add(new Tooltip("Nothing equipped"));
				}
				else if (!config.hideNotVisibleSlots())
				{
					tooltipManager.add(new Tooltip("Not visible from examine"));
				}
				return;
			}
		}
	}

	private void addItemTooltips(EquipmentEntry entry)
	{
		tooltipManager.add(new Tooltip(formatTooltipLabel(getDisplayItemName(entry))));

		List<String> valueLines = new ArrayList<>();
		if (config.showGeValue())
		{
			valueLines.add(formatTooltipStatLine("GE", formatPrice(itemManager.getItemPriceWithSource(entry.getItemId(), false)), null));
		}

		if (config.showHaValue())
		{
			valueLines.add(formatTooltipStatLine("HA", formatPrice(client.getItemDefinition(entry.getItemId()).getHaPrice()), null));
		}

		if (!valueLines.isEmpty())
		{
			tooltipManager.add(new Tooltip(String.join("<br>", valueLines)));
		}

		if (config.showEquipmentBonuses())
		{
			String equipmentBonusTooltip = buildEquipmentBonusTooltip(entry.getItemId());
			if (!equipmentBonusTooltip.isEmpty())
			{
				tooltipManager.add(new Tooltip(equipmentBonusTooltip));
			}
		}
	}

	private String buildEquipmentBonusTooltip(int itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId);
		if (stats == null || !stats.isEquipable())
		{
			return "";
		}

		ItemEquipmentStats equipment = stats.getEquipment();
		if (equipment == null)
		{
			return "";
		}

		ItemEquipmentStats equippedEquipment = null;
		if (config.compareEquipmentBonuses())
		{
			equippedEquipment = getEquippedEquipmentStats(equipment.getSlot());
		}

		List<String> lines = new ArrayList<>();

		List<String> attackLines = new ArrayList<>();
		addBonusLine(attackLines, "Stab", equipment.getAstab(), getCurrentBonus(equippedEquipment, BonusType.ASTAB), false);
		addBonusLine(attackLines, "Slash", equipment.getAslash(), getCurrentBonus(equippedEquipment, BonusType.ASLASH), false);
		addBonusLine(attackLines, "Crush", equipment.getAcrush(), getCurrentBonus(equippedEquipment, BonusType.ACRUSH), false);
		addBonusLine(attackLines, "Magic", equipment.getAmagic(), getCurrentBonus(equippedEquipment, BonusType.AMAGIC), false);
		addBonusLine(attackLines, "Range", equipment.getArange(), getCurrentBonus(equippedEquipment, BonusType.ARANGE), false);
		if (!attackLines.isEmpty())
		{
			lines.add(formatTooltipLabel("Attack Bonus"));
			lines.addAll(attackLines);
		}

		List<String> defenceLines = new ArrayList<>();
		addBonusLine(defenceLines, "Stab", equipment.getDstab(), getCurrentBonus(equippedEquipment, BonusType.DSTAB), false);
		addBonusLine(defenceLines, "Slash", equipment.getDslash(), getCurrentBonus(equippedEquipment, BonusType.DSLASH), false);
		addBonusLine(defenceLines, "Crush", equipment.getDcrush(), getCurrentBonus(equippedEquipment, BonusType.DCRUSH), false);
		addBonusLine(defenceLines, "Magic", equipment.getDmagic(), getCurrentBonus(equippedEquipment, BonusType.DMAGIC), false);
		addBonusLine(defenceLines, "Range", equipment.getDrange(), getCurrentBonus(equippedEquipment, BonusType.DRANGE), false);
		if (!defenceLines.isEmpty())
		{
			lines.add(formatTooltipLabel("Defence Bonus"));
			lines.addAll(defenceLines);
		}

		addBonusLine(lines, "Strength", equipment.getStr(), getCurrentBonus(equippedEquipment, BonusType.STR), false);
		addBonusLine(lines, "Ranged Str", equipment.getRstr(), getCurrentBonus(equippedEquipment, BonusType.RSTR), false);
		addBonusLine(lines, "Magic Dmg", equipment.getMdmg(), getCurrentBonus(equippedEquipment, BonusType.MDMG), true);
		addBonusLine(lines, "Prayer", equipment.getPrayer(), getCurrentBonus(equippedEquipment, BonusType.PRAYER), false);
		addBonusLine(lines, "Speed", equipment.getAspeed(), getCurrentBonus(equippedEquipment, BonusType.ASPEED), false);

		return lines.isEmpty() ? "" : String.join("<br>", lines);
	}

	private ItemEquipmentStats getEquippedEquipmentStats(int slot)
	{
		ItemContainer container = client.getItemContainer(InventoryID.WORN);
		if (container == null)
		{
			return null;
		}

		Item item = container.getItem(slot);
		if (item == null)
		{
			return null;
		}

		ItemStats stats = itemManager.getItemStats(item.getId());
		return stats != null ? stats.getEquipment() : null;
	}

	private double getCurrentBonus(ItemEquipmentStats equipment, BonusType type)
	{
		if (equipment == null)
		{
			return 0;
		}

		switch (type)
		{
			case ASTAB:
				return equipment.getAstab();
			case ASLASH:
				return equipment.getAslash();
			case ACRUSH:
				return equipment.getAcrush();
			case AMAGIC:
				return equipment.getAmagic();
			case ARANGE:
				return equipment.getArange();
			case DSTAB:
				return equipment.getDstab();
			case DSLASH:
				return equipment.getDslash();
			case DCRUSH:
				return equipment.getDcrush();
			case DMAGIC:
				return equipment.getDmagic();
			case DRANGE:
				return equipment.getDrange();
			case STR:
				return equipment.getStr();
			case RSTR:
				return equipment.getRstr();
			case MDMG:
				return equipment.getMdmg();
			case PRAYER:
				return equipment.getPrayer();
			case ASPEED:
				return equipment.getAspeed();
			default:
				return 0;
		}
	}

	private void addBonusLine(List<String> lines, String label, double value, double currentValue, boolean percent)
	{
		if (value == 0 && currentValue == 0)
		{
			return;
		}

		String formattedValue = formatSignedValue(value, percent);
		if (config.compareEquipmentBonuses())
		{
			double delta = value - currentValue;
			String formattedDelta = formatDelta(delta, percent, label.equals("Speed"));
			lines.add(formatTooltipStatLine(label, formattedValue, formattedDelta));
			return;
		}

		lines.add(formatTooltipStatLine(label, formattedValue, null));
	}

	private String formatSignedValue(double value, boolean percent)
	{
		String formatted = formatNumber(value);
		if (value > 0)
		{
			formatted = "+" + formatted;
		}

		return percent ? formatted + "%" : formatted;
	}

	private String formatDelta(double delta, boolean percent, boolean lowerIsBetter)
	{
		String formatted = formatSignedValue(delta, percent);
		Color color;
		if (delta == 0)
		{
			color = Color.GRAY;
		}
		else if (lowerIsBetter)
		{
			color = delta < 0 ? config.tooltipPositiveBonusColor() : config.tooltipNegativeBonusColor();
		}
		else
		{
			color = delta > 0 ? config.tooltipPositiveBonusColor() : config.tooltipNegativeBonusColor();
		}

		return ColorUtil.wrapWithColorTag("(" + formatted + ")", color);
	}

	private String formatTooltipLabel(String text)
	{
		return ColorUtil.wrapWithColorTag(text, config.tooltipItemTextColor());
	}

	private String formatTooltipStatLine(String label, String value, String delta)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(ColorUtil.wrapWithColorTag(label + ": ", config.tooltipOtherTextColor()));
		builder.append(ColorUtil.wrapWithColorTag(value, config.tooltipValueTextColor()));
		if (delta != null)
		{
			builder.append("  ").append(delta);
		}

		return builder.toString();
	}

	private String formatNumber(double value)
	{
		if (value == (long) value)
		{
			return Long.toString((long) value);
		}

		String text = Double.toString(value);
		if (text.endsWith(".0"))
		{
			return text.substring(0, text.length() - 2);
		}

		return text;
	}

	private enum BonusType
	{
		ASTAB,
		ASLASH,
		ACRUSH,
		AMAGIC,
		ARANGE,
		DSTAB,
		DSLASH,
		DCRUSH,
		DMAGIC,
		DRANGE,
		STR,
		RSTR,
		MDMG,
		PRAYER,
		ASPEED
	}

	private FooterLayout buildFooterLayout(Graphics2D graphics, PlayerExamineData data, int contentBottom, int frameWidth)
	{
		PlayerExamineConfig.TotalValueMode totalValueMode = config.totalValueMode();
		if (totalValueMode == null || totalValueMode == PlayerExamineConfig.TotalValueMode.None)
		{
			return FooterLayout.empty(contentBottom);
		}

		FontMetrics metrics = graphics.getFontMetrics();
		FooterLayout layout = buildTotalValueLayout(data, totalValueMode, metrics, contentBottom, frameWidth);
		if (layout.isEmpty())
		{
			return FooterLayout.empty(contentBottom);
		}

		return layout;
	}

	private FooterLayout buildTotalValueLayout(PlayerExamineData data, PlayerExamineConfig.TotalValueMode totalValueMode, FontMetrics metrics, int contentBottom, int frameWidth)
	{
		long geTotal = 0;
		long haTotal = 0;

		for (EquipmentEntry entry : data.getEquipment())
		{
			if (entry == null || !entry.hasItem())
			{
				continue;
			}

			int itemId = entry.getItemId();
			geTotal += itemManager.getItemPriceWithSource(itemId, false);
			haTotal += client.getItemDefinition(itemId).getHaPrice();
		}

		String geText = "GE Total: " + formatPrice(geTotal);
		String haText = "HA Total: " + formatPrice(haTotal);
		int footerWidth = frameWidth - (FOOTER_SIDE_PADDING * 2);
		int lineHeight = metrics.getHeight();
		int footerStartY = contentBottom + FOOTER_TOP_MARGIN;

		switch (totalValueMode)
		{
			case Ge:
				return FooterLayout.single(new FooterLine(geText, config.totalGeTextColor()), footerStartY, lineHeight, FOOTER_BOTTOM_MARGIN);
			case HA:
				return FooterLayout.single(new FooterLine(haText, config.totalHaTextColor()), footerStartY, lineHeight, FOOTER_BOTTOM_MARGIN);
			case Both:
				int inlineWidth = metrics.stringWidth(geText)
					+ metrics.stringWidth("  |  ")
					+ metrics.stringWidth(haText);
				if (inlineWidth <= footerWidth)
				{
					return FooterLayout.inline(
						new FooterLine(geText, config.totalGeTextColor()),
						new FooterLine(haText, config.totalHaTextColor()),
						footerStartY,
						lineHeight,
						FOOTER_BOTTOM_MARGIN);
				}
				return FooterLayout.stacked(
					new FooterLine(geText, config.totalGeTextColor()),
					new FooterLine(haText, config.totalHaTextColor()),
					footerStartY,
					lineHeight,
					FOOTER_LINE_GAP,
					FOOTER_BOTTOM_MARGIN);
			default:
				return FooterLayout.empty(contentBottom);
		}
	}

	private void drawFooter(Graphics2D graphics, FooterLayout footerLayout, int frameWidth)
	{
		if (footerLayout.isEmpty())
		{
			return;
		}

		FontMetrics metrics = graphics.getFontMetrics();
		if (footerLayout.isStacked())
		{
			int startY = footerLayout.getStartY();
			int currentY = startY;
			for (FooterLine line : footerLayout.getLines())
			{
				drawCenteredShadowText(graphics, line.getText(), new Rectangle(0, currentY, frameWidth, metrics.getHeight()), line.getColor());
				currentY += metrics.getHeight() + FOOTER_LINE_GAP;
			}
			return;
		}

		if (footerLayout.isInlinePair())
		{
			int lineY = footerLayout.getStartY();
			drawCenteredInlineFooter(
				graphics,
				footerLayout.getLines().get(0),
				footerLayout.getLines().get(1),
				lineY,
				metrics,
				frameWidth);
			return;
		}

		FooterLine line = footerLayout.getLines().get(0);
		int lineY = footerLayout.getStartY();
		drawCenteredShadowText(graphics, line.getText(), new Rectangle(0, lineY, frameWidth, metrics.getHeight()), line.getColor());
	}

	private void drawCenteredInlineFooter(Graphics2D graphics, FooterLine left, FooterLine right, int y, FontMetrics metrics, int frameWidth)
	{
		String separator = "  |  ";
		int leftWidth = metrics.stringWidth(left.getText());
		int separatorWidth = metrics.stringWidth(separator);
		int rightWidth = metrics.stringWidth(right.getText());
		int totalWidth = leftWidth + separatorWidth + rightWidth;
		int x = (frameWidth - totalWidth) / 2;
		int baseline = y + metrics.getAscent();

		drawShadowText(graphics, left.getText(), x, baseline, left.getColor());
		x += leftWidth;
		drawShadowText(graphics, separator, x, baseline, config.combatTextColor());
		x += separatorWidth;
		drawShadowText(graphics, right.getText(), x, baseline, right.getColor());
	}

	private String getDisplayItemName(EquipmentEntry entry)
	{
		String itemName = entry.getItemName();
		if (itemName == null || itemName.isEmpty())
		{
			return "Unknown item";
		}

		if (!config.showMembersSuffix() && itemName.endsWith(" (Members)"))
		{
			return itemName.substring(0, itemName.length() - " (Members)".length());
		}

		return itemName;
	}

	private static String formatPrice(int value)
	{
		return String.format("%,d", value);
	}

	private static String formatPrice(long value)
	{
		return String.format("%,d", value);
	}

	private static final class FooterLayout
	{
		private final List<FooterLine> lines;
		private final boolean inlinePair;
		private final boolean stacked;
		private final int startY;
		private final int frameHeight;

		private FooterLayout(List<FooterLine> lines, boolean inlinePair, boolean stacked, int startY, int frameHeight)
		{
			this.lines = lines;
			this.inlinePair = inlinePair;
			this.stacked = stacked;
			this.startY = startY;
			this.frameHeight = frameHeight;
		}

		static FooterLayout empty(int contentBottom)
		{
			return new FooterLayout(new ArrayList<>(), false, false, contentBottom + FOOTER_BOTTOM_MARGIN, contentBottom + FOOTER_BOTTOM_MARGIN);
		}

		static FooterLayout single(FooterLine line, int startY, int lineHeight, int bottomMargin)
		{
			List<FooterLine> lines = new ArrayList<>();
			lines.add(line);
			int frameHeight = Math.max(BASE_FRAME_HEIGHT, startY + lineHeight + bottomMargin);
			return new FooterLayout(lines, false, false, startY, frameHeight);
		}

		static FooterLayout inline(FooterLine left, FooterLine right, int startY, int lineHeight, int bottomMargin)
		{
			List<FooterLine> lines = new ArrayList<>();
			lines.add(left);
			lines.add(right);
			int frameHeight = Math.max(BASE_FRAME_HEIGHT, startY + lineHeight + bottomMargin);
			return new FooterLayout(lines, true, false, startY, frameHeight);
		}

		static FooterLayout stacked(FooterLine left, FooterLine right, int startY, int lineHeight, int lineGap, int bottomMargin)
		{
			List<FooterLine> lines = new ArrayList<>();
			lines.add(left);
			lines.add(right);
			int frameHeight = Math.max(BASE_FRAME_HEIGHT, startY + (lineHeight * 2) + lineGap + bottomMargin);
			return new FooterLayout(lines, false, true, startY, frameHeight);
		}

		boolean isEmpty()
		{
			return lines.isEmpty();
		}

		boolean isInlinePair()
		{
			return inlinePair;
		}

		boolean isStacked()
		{
			return stacked;
		}

		List<FooterLine> getLines()
		{
			return lines;
		}

		int getStartY()
		{
			return startY;
		}

		int getFrameHeight()
		{
			return frameHeight;
		}
	}

	private static final class ContentLayout
	{
		private final List<SlotState> slots;
		private final List<ListRow> rows;
		private final boolean listMode;
		private final int frameHeight;
		private final int contentBottom;

		private ContentLayout(List<SlotState> slots, List<ListRow> rows, boolean listMode, int frameHeight, int contentBottom)
		{
			this.slots = slots;
			this.rows = rows;
			this.listMode = listMode;
			this.frameHeight = frameHeight;
			this.contentBottom = contentBottom;
		}

		static ContentLayout forSlots(List<SlotState> slots)
		{
			return new ContentLayout(slots, new ArrayList<>(), false, BASE_FRAME_HEIGHT, SLOT_GRID_BOTTOM);
		}

		static ContentLayout forRows(List<SlotState> slots, List<ListRow> rows, int contentBottom)
		{
			return new ContentLayout(slots, rows, true, contentBottom + FOOTER_BOTTOM_MARGIN, contentBottom);
		}

		boolean isListMode()
		{
			return listMode;
		}

		List<SlotState> getSlots()
		{
			return slots;
		}

		List<ListRow> getRows()
		{
			return rows;
		}

		int getContentBottom()
		{
			return contentBottom;
		}

		int getFrameHeight()
		{
			return frameHeight;
		}
	}

	private static final class FooterLine
	{
		private final String text;
		private final Color color;

		private FooterLine(String text, Color color)
		{
			this.text = text;
			this.color = color;
		}

		String getText()
		{
			return text;
		}

		Color getColor()
		{
			return color;
		}
	}

	public String buildWikiUrl(EquipmentEntry entry)
	{
		return "https://oldschool.runescape.wiki/w/Special:Search?search="
			+ URLEncoder.encode(getWikiSearchName(entry), StandardCharsets.UTF_8);
	}

	private String getWikiSearchName(EquipmentEntry entry)
	{
		String itemName = entry.getItemName();
		if (itemName == null || itemName.isEmpty())
		{
			return "Unknown item";
		}

		if (itemName.endsWith(" (Members)"))
		{
			return itemName.substring(0, itemName.length() - " (Members)".length());
		}

		return itemName;
	}

	private boolean isMouseInside(Rectangle rectangle)
	{
		Point mouse = client.getMouseCanvasPosition();
		Rectangle bounds = getBounds();
		return mouse != null && bounds != null
			&& bounds.contains(mouse.getX(), mouse.getY())
			&& rectangle.contains(mouse.getX() - bounds.x, mouse.getY() - bounds.y);
	}

	private static void drawShadowText(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(new Color(16, 12, 8));
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	private static void drawCenteredShadowText(Graphics2D graphics, String text, Rectangle bounds, Color color)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int x = bounds.x + (bounds.width - metrics.stringWidth(text)) / 2;
		int y = bounds.y + ((bounds.height - metrics.getHeight()) / 2) + metrics.getAscent();
		drawShadowText(graphics, text, x, y, color);
	}

	private String fitText(Graphics2D graphics, String text, int maxWidth)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}

		FontMetrics metrics = graphics.getFontMetrics();
		if (metrics.stringWidth(text) <= maxWidth)
		{
			return text;
		}

		String suffix = "...";
		int suffixWidth = metrics.stringWidth(suffix);
		int end = text.length();
		while (end > 0 && metrics.stringWidth(text.substring(0, end)) + suffixWidth > maxWidth)
		{
			end--;
		}

		return end <= 0 ? suffix : text.substring(0, end) + suffix;
	}

	public static final class RenderState
	{
		private final Rectangle closeButton;
		private final List<SlotState> slots;
		private final List<ListRow> rows;
		private final PlayerExamineConfig.OverlayMode overlayMode;
		private final Dimension dimension;

		private RenderState(Rectangle closeButton, List<SlotState> slots, List<ListRow> rows, PlayerExamineConfig.OverlayMode overlayMode, Dimension dimension)
		{
			this.closeButton = closeButton;
			this.slots = slots;
			this.rows = rows;
			this.overlayMode = overlayMode;
			this.dimension = dimension;
		}

		public static RenderState empty()
		{
			return new RenderState(new Rectangle(), new ArrayList<>(), new ArrayList<>(), PlayerExamineConfig.OverlayMode.Item, new Dimension(0, 0));
		}

		public boolean isEmpty()
		{
			return dimension.width == 0 || dimension.height == 0;
		}

		public Rectangle getCloseButton()
		{
			return closeButton;
		}

		public List<SlotState> getSlots()
		{
			return slots;
		}

		public List<ListRow> getRows()
		{
			return rows;
		}

		public boolean isListMode()
		{
			return overlayMode == PlayerExamineConfig.OverlayMode.List || overlayMode == PlayerExamineConfig.OverlayMode.Hybrid;
		}

		public SlotState getSlotAt(int localX, int localY)
		{
			for (SlotState slot : slots)
			{
				if (slot.getBounds().contains(localX, localY))
				{
					return slot;
				}
			}
			return null;
		}

		public Dimension getDimension()
		{
			return dimension;
		}
	}

	public static final class SlotState
	{
		private final String key;
		private final Rectangle bounds;
		private final EquipmentEntry entry;
		private final boolean drawFrame;
		private final boolean showEmptyTooltip;

		private SlotState(String key, Rectangle bounds, EquipmentEntry entry, boolean drawFrame, boolean showEmptyTooltip)
		{
			this.key = key;
			this.bounds = bounds;
			this.entry = entry;
			this.drawFrame = drawFrame;
			this.showEmptyTooltip = showEmptyTooltip;
		}

		public String getKey()
		{
			return key;
		}

		public Rectangle getBounds()
		{
			return bounds;
		}

		public EquipmentEntry getEntry()
		{
			return entry;
		}

		public boolean isDrawFrame()
		{
			return drawFrame;
		}

		public boolean isShowEmptyTooltip()
		{
			return showEmptyTooltip;
		}
	}

	private static final class ListRow
	{
		private final SlotState slot;
		private final Rectangle bounds;
		private final String primaryText;
		private final String secondaryText;
		private final boolean showIcon;
		private final boolean stackedText;

		private ListRow(SlotState slot, Rectangle bounds, String primaryText, String secondaryText, boolean showIcon, boolean stackedText)
		{
			this.slot = slot;
			this.bounds = bounds;
			this.primaryText = primaryText;
			this.secondaryText = secondaryText;
			this.showIcon = showIcon;
			this.stackedText = stackedText;
		}

		private SlotState getSlot()
		{
			return slot;
		}

		private Rectangle getBounds()
		{
			return bounds;
		}

		private String getPrimaryText()
		{
			return primaryText;
		}

		private String getSecondaryText()
		{
			return secondaryText;
		}

		private boolean isShowIcon()
		{
			return showIcon;
		}

		private boolean isStackedText()
		{
			return stackedText;
		}
	}
}
