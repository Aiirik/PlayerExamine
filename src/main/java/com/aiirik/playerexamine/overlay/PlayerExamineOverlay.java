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
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.AsyncBufferedImage;

public class PlayerExamineOverlay extends Overlay
{
	private static final int FRAME_WIDTH = 188;
	private static final int FRAME_HEIGHT = 248;
	private static final int TITLE_BAR_HEIGHT = 18;
	private static final int SLOT_SIZE = 34;
	private static final int SLOT_GAP_X = 12;
	private static final int SLOT_GAP_Y = 6;
	private static final int GRID_START_X = 31;
	private static final int GRID_START_Y = 28;
	private static final int SLOT_INSET_X = 4;
	private static final int SLOT_INSET_Y = 3;
	private static final Rectangle[] SLOT_BOXES = {
		new Rectangle(GRID_START_X + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y, SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X, GRID_START_Y + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + 2 * (SLOT_SIZE + SLOT_GAP_X), GRID_START_Y + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X, GRID_START_Y + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + 2 * (SLOT_SIZE + SLOT_GAP_X), GRID_START_Y + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + 3 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X, GRID_START_Y + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + SLOT_SIZE + SLOT_GAP_X, GRID_START_Y + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
		new Rectangle(GRID_START_X + 2 * (SLOT_SIZE + SLOT_GAP_X), GRID_START_Y + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE)
	};

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

		Rectangle closeButton = new Rectangle(FRAME_WIDTH - 20, 2, 16, 14);
		List<SlotState> slots = buildSlots(data);

		drawFrame(graphics, data, closeButton);
		drawSlots(graphics, slots);

		renderState = new RenderState(closeButton, slots, new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
		addHoverTooltip();
		return renderState.getDimension();
	}

	public RenderState getRenderState()
	{
		return renderState;
	}

	private void drawFrame(Graphics2D graphics, PlayerExamineData data, Rectangle closeButton)
	{
		graphics.setColor(config.overlayBorderColor());
		graphics.drawRect(0, 0, FRAME_WIDTH - 1, FRAME_HEIGHT - 1);

		Color backgroundColor = config.overlayBackgroundColor();
		if (backgroundColor.getAlpha() > 0)
		{
			graphics.setColor(backgroundColor);
			graphics.fillRect(1, 1, FRAME_WIDTH - 2, FRAME_HEIGHT - 2);
		}

		graphics.setColor(config.overlayBorderColor());
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawLine(2, TITLE_BAR_HEIGHT - 1, FRAME_WIDTH - 3, TITLE_BAR_HEIGHT - 1);

		FontMetrics metrics = graphics.getFontMetrics();
		int titleBaseline = 2 + metrics.getAscent();

		String title = fitText(graphics, data.getName(), 74);
		drawShadowText(graphics, title, 8, titleBaseline, config.usernameTextColor());

		String combat = "Combat " + data.getCombatLevel();
		drawShadowText(graphics, combat, 96, titleBaseline, config.combatTextColor());

		boolean hoverClose = isMouseInside(closeButton);
		graphics.setColor(hoverClose ? config.overlayCloseHoverColor() : config.overlayCloseColor());
		graphics.fillRect(closeButton.x, closeButton.y, closeButton.width, closeButton.height);
		graphics.setColor(config.xBorderColor());
		graphics.drawRect(closeButton.x, closeButton.y, closeButton.width, closeButton.height);
		drawCenteredShadowText(graphics, "X", closeButton, config.xTextColor());
	}

	private List<SlotState> buildSlots(PlayerExamineData data)
	{
		Map<String, EquipmentEntry> entries = new HashMap<>();
		for (EquipmentEntry entry : data.getEquipment())
		{
			entries.put(entry.getSlotName().toLowerCase(), entry);
		}

		List<SlotState> slots = new ArrayList<>();
		slots.add(createSlot("helmet", SLOT_BOXES[0], entries.get("head"), true, true));
		slots.add(createSlot("cape", SLOT_BOXES[1], entries.get("cape"), true, true));
		slots.add(createSlot("necklace", SLOT_BOXES[2], entries.get("amulet"), true, true));
		slots.add(createSlot("arrows", SLOT_BOXES[3], null, true, false));
		slots.add(createSlot("weapon", SLOT_BOXES[4], entries.get("weapon"), true, true));
		slots.add(createSlot("body", SLOT_BOXES[5], entries.get("torso"), true, true));
		slots.add(createSlot("offhand", SLOT_BOXES[6], entries.get("shield"), true, true));
		slots.add(createSlot("legs", SLOT_BOXES[7], entries.get("legs"), true, true));
		slots.add(createSlot("gloves", SLOT_BOXES[8], entries.get("hands"), true, true));
		slots.add(createSlot("boots", SLOT_BOXES[9], entries.get("boots"), true, true));
		slots.add(createSlot("ring", SLOT_BOXES[10], null, true, false));
		return slots;
	}

	private SlotState createSlot(String key, Rectangle bounds, EquipmentEntry entry, boolean drawFrame, boolean showEmptyTooltip)
	{
		return new SlotState(key, bounds, entry, drawFrame, showEmptyTooltip);
	}

	private void drawSlots(Graphics2D graphics, List<SlotState> slots)
	{
		for (SlotState slot : slots)
		{
			drawSlot(graphics, slot);
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
				else
				{
					tooltipManager.add(new Tooltip("Not visible from examine"));
				}
				return;
			}
		}
	}

	private void addItemTooltips(EquipmentEntry entry)
	{
		tooltipManager.add(new Tooltip(getDisplayItemName(entry)));

		if (config.showGeValue())
		{
			tooltipManager.add(new Tooltip("GE: " + formatPrice(itemManager.getItemPriceWithSource(entry.getItemId(), false))));
		}

		if (config.showHaValue())
		{
			tooltipManager.add(new Tooltip("HA: " + formatPrice(client.getItemDefinition(entry.getItemId()).getHaPrice())));
		}
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
		private final Dimension dimension;

		private RenderState(Rectangle closeButton, List<SlotState> slots, Dimension dimension)
		{
			this.closeButton = closeButton;
			this.slots = slots;
			this.dimension = dimension;
		}

		public static RenderState empty()
		{
			return new RenderState(new Rectangle(), new ArrayList<>(), new Dimension(0, 0));
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
}
