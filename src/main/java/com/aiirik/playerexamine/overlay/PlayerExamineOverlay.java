package com.aiirik.playerexamine.overlay;

import com.aiirik.playerexamine.PlayerExamineConfig;
import com.aiirik.playerexamine.PlayerExaminePlugin;
import com.aiirik.playerexamine.PlayerExamineColorSettings;
import com.aiirik.playerexamine.model.PlayerExamineData;
import com.aiirik.playerexamine.model.PlayerExamineData.EquipmentEntry;
import com.aiirik.playerexamine.model.PlayerHiscoreData;
import com.google.gson.Gson;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.AsyncBufferedImage;

public class PlayerExamineOverlay extends Overlay
{
	private static final int BASE_FRAME_WIDTH = 188;
	private static final int BASE_FRAME_HEIGHT = 248;
	private static final int TITLE_BAR_HEIGHT = 18;
	private static final int TAB_BAR_Y = 21;
	private static final int TAB_BAR_HEIGHT = 15;
	private static final int TAB_SIDE_PADDING = 8;
	private static final int CONTENT_TOP_WITH_TABS = 43;
	private static final int CONTENT_TOP_NO_TABS = 25;
	private static final int SLOT_SIZE = 34;
	private static final int SLOT_GAP_X = 12;
	private static final int SLOT_GAP_Y = 6;
	private static final int GRID_START_X = 31;
	private static final int GRID_START_Y = 43;
	private static final int SLOT_INSET_X = 4;
	private static final int SLOT_INSET_Y = 3;
	private static final int FOOTER_BOTTOM_MARGIN = 5;
	private static final int FOOTER_TOP_MARGIN = 4;
	private static final int FOOTER_LINE_GAP = 1;
	private static final int FOOTER_SIDE_PADDING = 12;
	private static final int SLOT_GRID_BOTTOM = GRID_START_Y + (4 * (SLOT_SIZE + SLOT_GAP_Y)) + SLOT_SIZE;
	private static final int LIST_START_Y = 43;
	private static final int LIST_SIDE_PADDING = 8;
	private static final int LIST_ICON_SIZE = 16;
	private static final int LIST_ICON_GAP = 4;
	private static final int LIST_ROW_PADDING_Y = 2;
	private static final int STATS_SIDE_PADDING = 8;
	private static final int STATS_COLUMN_GAP = 8;
	private static final int STATS_ROW_GAP = 4;
	private static final int STATS_ICON_SIZE = 20;
	private static final int STATS_ATTACK_ICON_SIZE = 24;
	private static final int STATS_ICON_GAP = 4;
	private static final int STATS_OVERALL_GAP = 6;
	private static final int STATS_COLUMNS = 3;
	private static final int OPENING_FLAIR_DURATION_MS = 1200;
	private static final int HYBRID_ICON_SIZE = 20;
	private static final int HYBRID_ICON_GAP = 6;
	private static final int HYBRID_ROW_PADDING_Y = 4;
	private static final String CUSTOM_PRESET_PREFIX = "customPreset.";
	private static final String[] CUSTOM_COLOR_KEYS = {
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
	private final PlayerExaminePlugin plugin;
	private final PlayerExamineConfig config;
	private final Client client;
	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final TooltipManager tooltipManager;
	private volatile RenderState renderState = RenderState.empty();
	private volatile OverlayTab selectedTab = OverlayTab.EQUIPMENT;
	private volatile long openedAtMillis;

	public enum OverlayTab
	{
		EQUIPMENT,
		STATS
	}

	@Inject
	public PlayerExamineOverlay(
		PlayerExaminePlugin plugin,
		PlayerExamineConfig config,
		Client client,
		ItemManager itemManager,
		SkillIconManager skillIconManager,
		TooltipManager tooltipManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
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
		Font overlayFont = FontManager.getRunescapeSmallFont();
		if (config.boldOverlayText())
		{
			overlayFont = FontManager.getRunescapeBoldFont().deriveFont(Math.max(1.0f, overlayFont.getSize2D() - 3.0f));
		}
		graphics.setFont(overlayFont);

		PlayerExamineConfig.OverlayMode overlayMode = config.overlayMode();
		PlayerExamineConfig.StatsTabMode statsTabMode = config.statsTabMode();
		boolean showStatsTab = statsTabMode != PlayerExamineConfig.StatsTabMode.Off;
		boolean showStatsIcons = statsTabMode == PlayerExamineConfig.StatsTabMode.Visual;
		if (!showStatsTab && selectedTab == OverlayTab.STATS)
		{
			selectedTab = OverlayTab.EQUIPMENT;
		}

		int frameWidth = config.overlayWidth();
		PlayerHiscoreData hiscoreData = plugin.getCurrentHiscoreData();
		if (overlayMode == PlayerExamineConfig.OverlayMode.List || overlayMode == PlayerExamineConfig.OverlayMode.Hybrid)
		{
			frameWidth = Math.max(frameWidth, calculateListFrameWidth(graphics, data, overlayMode == PlayerExamineConfig.OverlayMode.Hybrid, frameWidth));
		}
		if (showStatsTab && (selectedTab == OverlayTab.STATS || hiscoreData != null))
		{
			frameWidth = Math.max(frameWidth, calculateStatsFrameWidth(graphics, hiscoreData, frameWidth, showStatsIcons));
		}

		int contentTop = showStatsTab ? CONTENT_TOP_WITH_TABS : CONTENT_TOP_NO_TABS;
		ContentLayout contentLayout = buildContentLayout(graphics, data, overlayMode, frameWidth, contentTop);
		FooterLayout footerLayout = buildFooterLayout(graphics, data, contentLayout.getContentBottom(), frameWidth);
		TabLayout tabLayout = showStatsTab ? buildTabLayout(frameWidth) : null;
		StatsLayout statsLayout = showStatsTab ? buildStatsLayout(graphics, hiscoreData, frameWidth, showStatsIcons, contentTop) : StatsLayout.empty(contentTop);
		boolean statsSelected = selectedTab == OverlayTab.STATS;
		if (showStatsTab && !statsSelected)
		{
			int footerDelta = statsLayout.getFrameHeight() - footerLayout.getFrameHeight();
			if (footerDelta > 0)
			{
				footerLayout = footerLayout.withVerticalOffset(footerDelta);
			}
		}
		int frameHeight = statsSelected
			? Math.max(statsLayout.getFrameHeight(), BASE_FRAME_HEIGHT)
			: Math.max(Math.max(contentLayout.getFrameHeight(), footerLayout.getFrameHeight()), showStatsTab ? statsLayout.getFrameHeight() : BASE_FRAME_HEIGHT);
		if (!statsSelected)
		{
			contentLayout = contentLayout.withVerticalOffset(calculateEquipmentContentOffset(contentLayout, footerLayout, frameHeight, contentTop));
		}
		Rectangle closeButton = new Rectangle(frameWidth - TITLE_BAR_HEIGHT, 0, TITLE_BAR_HEIGHT, TITLE_BAR_HEIGHT);

		drawFrame(graphics, data, closeButton, frameHeight, frameWidth);
		if (showStatsTab)
		{
			drawTabs(graphics, tabLayout, frameWidth);
		}
		if (statsSelected)
		{
			drawStats(graphics, statsLayout, frameWidth, hiscoreData);
		}
		else
		{
			drawContent(graphics, contentLayout, frameWidth);
			drawFooter(graphics, footerLayout, frameWidth);
		}

		renderState = new RenderState(closeButton, contentLayout.getSlots(), contentLayout.getRows(), statsLayout.getCells(), overlayMode, tabLayout, selectedTab, new Dimension(frameWidth, frameHeight));
		addHoverTooltip();
		return renderState.getDimension();
	}

	public RenderState getRenderState()
	{
		return renderState;
	}

	public void setSelectedTab(OverlayTab tab)
	{
		if (config.statsTabMode() == PlayerExamineConfig.StatsTabMode.Off && tab == OverlayTab.STATS)
		{
			selectedTab = OverlayTab.EQUIPMENT;
			return;
		}

		selectedTab = tab == null ? OverlayTab.EQUIPMENT : tab;
	}

	public OverlayTab getSelectedTab()
	{
		return selectedTab;
	}

	public void markOpened()
	{
		openedAtMillis = System.currentTimeMillis();
	}

	private void drawFrame(Graphics2D graphics, PlayerExamineData data, Rectangle closeButton, int frameHeight, int frameWidth)
	{
		Color backgroundColor = overlayBackgroundColor();
		if (backgroundColor.getAlpha() > 0)
		{
			graphics.setColor(applyOverlayTransparency(backgroundColor));
			graphics.fillRect(1, 1, frameWidth - 2, frameHeight - 2);
		}

		FontMetrics metrics = graphics.getFontMetrics();
		int titleBaseline = ((TITLE_BAR_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent() + 1;

		String combat = "Lvl: " + data.getCombatLevel();
		int combatWidth = metrics.stringWidth(combat);
		int combatX = Math.max(8, frameWidth - combatWidth - 24);

		String title = fitText(graphics, data.getName(), Math.max(0, combatX - 16));
		drawShadowText(graphics, title, 8, titleBaseline, usernameTextColor());
		drawShadowText(graphics, combat, combatX, titleBaseline, combatTextColor());

		boolean hoverClose = isMouseInside(closeButton);
		Color closeFill = hoverClose ? overlayCloseHoverColor() : overlayCloseColor();
		if (config.overlayTransparency() > 0)
		{
			closeFill = applyOverlayTransparency(closeFill);
		}
		Rectangle closeInnerBounds = new Rectangle(closeButton.x + 1, closeButton.y + 1, closeButton.width - 2, closeButton.height - 2);
		graphics.setColor(closeFill);
		graphics.fillRect(closeInnerBounds.x, closeInnerBounds.y, closeInnerBounds.width, closeInnerBounds.height);
		graphics.setColor(applyOverlayTransparency(overlayBorderColor()));
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawRect(0, 0, frameWidth - 1, frameHeight - 1);
		graphics.drawLine(1, TITLE_BAR_HEIGHT - 1, frameWidth - 2, TITLE_BAR_HEIGHT - 1);
		graphics.drawLine(closeButton.x, closeButton.y + 1, closeButton.x, closeButton.y + closeButton.height - 2);
		drawCloseButtonIcon(graphics, closeInnerBounds, xTextColor());

		drawOpeningFlair(graphics, frameWidth, frameHeight);
	}

	private TabLayout buildTabLayout(int frameWidth)
	{
		int innerWidth = frameWidth - (TAB_SIDE_PADDING * 2) - 2;
		int tabWidth = innerWidth / 2;
		Rectangle equipmentTab = new Rectangle(TAB_SIDE_PADDING, TAB_BAR_Y, tabWidth, TAB_BAR_HEIGHT);
		Rectangle statsTab = new Rectangle(TAB_SIDE_PADDING + tabWidth + 2, TAB_BAR_Y, innerWidth - tabWidth, TAB_BAR_HEIGHT);
		return new TabLayout(equipmentTab, statsTab);
	}

	private void drawTabs(Graphics2D graphics, TabLayout tabLayout, int frameWidth)
	{
		Rectangle equipmentTab = tabLayout.getEquipmentTab();
		Rectangle statsTab = tabLayout.getStatsTab();
		drawTabButton(graphics, equipmentTab, selectedTab == OverlayTab.EQUIPMENT, "Equipment");
		drawTabButton(graphics, statsTab, selectedTab == OverlayTab.STATS, "Stats");
	}

	private void drawTabButton(Graphics2D graphics, Rectangle bounds, boolean selected, String text)
	{
		graphics.setColor(applyOverlayTransparency(selected ? overlaySlotFillColor() : overlaySlotEmptyColor()));
		graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		graphics.setColor(applyOverlayTransparency(selected ? overlaySlotHoverColor() : overlaySlotBorderColor()));
		graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		Rectangle textBounds = new Rectangle(bounds.x, bounds.y + 2, bounds.width, Math.max(0, bounds.height - 2));
		drawCenteredShadowText(graphics, text, textBounds, selected ? activeTabTextColor() : inactiveTabTextColor());
	}

	private void drawOpeningFlair(Graphics2D graphics, int frameWidth, int frameHeight)
	{
		if (!config.openingFlair())
		{
			return;
		}

		long elapsed = System.currentTimeMillis() - openedAtMillis;
		if (elapsed < 0 || elapsed > OPENING_FLAIR_DURATION_MS)
		{
			return;
		}

		float remaining = 1.0f - ((float) elapsed / OPENING_FLAIR_DURATION_MS);
		Color flair = openingFlairColor();
		Stroke originalStroke = graphics.getStroke();
		for (int i = 4; i >= 0; i--)
		{
			float ringAlpha = remaining * (0.15f + (0.11f * (4 - i)));
			int alpha = Math.min(230, Math.max(0, Math.round(flair.getAlpha() * ringAlpha)));
			if (alpha <= 0)
			{
				continue;
			}

			graphics.setColor(applyOverlayTransparency(new Color(flair.getRed(), flair.getGreen(), flair.getBlue(), alpha)));
			graphics.setStroke(new BasicStroke(i <= 1 ? 2f : 1f));
			graphics.drawRect(i, i, frameWidth - 1 - (i * 2), frameHeight - 1 - (i * 2));
		}
		graphics.setStroke(originalStroke);
	}

	private ContentLayout buildContentLayout(Graphics2D graphics, PlayerExamineData data, PlayerExamineConfig.OverlayMode overlayMode, int frameWidth, int contentTop)
	{
		Map<String, EquipmentEntry> entries = new HashMap<>();
		for (EquipmentEntry entry : data.getEquipment())
		{
			entries.put(entry.getSlotName().toLowerCase(), entry);
		}

		Rectangle[] slotBoxes = buildSlotBoxes(frameWidth, contentTop);
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
			return buildListContent(graphics, data, slots, overlayMode == PlayerExamineConfig.OverlayMode.Hybrid, frameWidth, contentTop);
		}

		return ContentLayout.forSlots(slots, contentTop);
	}

	private int calculateStatsFrameWidth(Graphics2D graphics, PlayerHiscoreData hiscoreData, int minimumWidth, boolean showIcons)
	{
		if (hiscoreData == null)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			int loadingWidth = Math.max(
				metrics.stringWidth("Looking up hiscores..."),
				metrics.stringWidth("Hiscores unavailable"));
			return Math.max(minimumWidth, loadingWidth + (STATS_SIDE_PADDING * 2));
		}

		FontMetrics metrics = graphics.getFontMetrics();
		PlayerHiscoreData.Skill[] skills = PlayerHiscoreData.displaySkills();
		int widestCell = 0;
		for (PlayerHiscoreData.Skill skill : skills)
		{
			widestCell = Math.max(widestCell, measureStatCellWidth(metrics, skill, hiscoreData.getLevel(skill), showIcons));
		}

		int totalLevelWidth = metrics.stringWidth("Total level: " + formatNumber(hiscoreData.getLevel(PlayerHiscoreData.Skill.OVERALL)));
		int gridWidth = (widestCell * STATS_COLUMNS) + (STATS_COLUMN_GAP * (STATS_COLUMNS - 1)) + (STATS_SIDE_PADDING * 2);
		int totalWidth = totalLevelWidth + (STATS_SIDE_PADDING * 2);
		return Math.max(minimumWidth, Math.max(gridWidth, totalWidth));
	}

	private StatsLayout buildStatsLayout(Graphics2D graphics, PlayerHiscoreData hiscoreData, int frameWidth, boolean showIcons, int contentTop)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		if (hiscoreData == null)
		{
			String message = plugin.getHiscoreLookupState() == PlayerExaminePlugin.HiscoreLookupState.LOADING
				? "Looking up hiscores..."
				: "Hiscores unavailable";
			int reservedHeight = estimateStatsFrameHeight(metrics, contentTop, showIcons);
			return new StatsLayout(
				reservedHeight,
				contentTop,
				showIcons,
				plugin.getHiscoreLookupState() == PlayerExaminePlugin.HiscoreLookupState.LOADING,
				plugin.getHiscoreLookupState() == PlayerExaminePlugin.HiscoreLookupState.UNAVAILABLE,
				new ArrayList<>(),
				message,
				contentTop,
				metrics.getHeight());
		}

		PlayerHiscoreData.Skill[] skills = PlayerHiscoreData.displaySkills();
		int rowsPerColumn = (skills.length + STATS_COLUMNS - 1) / STATS_COLUMNS;
		int rowHeight = Math.max(metrics.getHeight(), showIcons ? STATS_ICON_SIZE : 0) + STATS_ROW_GAP + 2;
		int gridCellWidth = (frameWidth - (STATS_SIDE_PADDING * 2) - (STATS_COLUMN_GAP * (STATS_COLUMNS - 1))) / STATS_COLUMNS;
		List<StatCell> cells = new ArrayList<>();
		for (int i = 0; i < skills.length; i++)
		{
			PlayerHiscoreData.Skill skill = skills[i];
			int row = i / STATS_COLUMNS;
			int column = i % STATS_COLUMNS;
			int x = STATS_SIDE_PADDING + (column * (gridCellWidth + STATS_COLUMN_GAP));
			int y = contentTop + (row * rowHeight);
			cells.add(new StatCell(skill, hiscoreData.getRank(skill), hiscoreData.getLevel(skill), hiscoreData.getExperience(skill), new Rectangle(x, y, gridCellWidth, rowHeight)));
		}

		int gridBottom = contentTop + (rowsPerColumn * rowHeight);
		int totalLevelY = gridBottom + STATS_OVERALL_GAP;
		String totalLevelText = "Total level: " + formatNumber(hiscoreData.getLevel(PlayerHiscoreData.Skill.OVERALL));
		int frameHeight = estimateStatsFrameHeight(metrics, contentTop, showIcons);
		return new StatsLayout(frameHeight, contentTop, showIcons, false, false, cells, totalLevelText, totalLevelY, metrics.getHeight());
	}

	private int estimateStatsFrameHeight(FontMetrics metrics, int contentTop, boolean showIcons)
	{
		PlayerHiscoreData.Skill[] skills = PlayerHiscoreData.displaySkills();
		int rowsPerColumn = (skills.length + STATS_COLUMNS - 1) / STATS_COLUMNS;
		int rowHeight = Math.max(metrics.getHeight(), showIcons ? STATS_ICON_SIZE : 0) + STATS_ROW_GAP + 2;
		int gridBottom = contentTop + (rowsPerColumn * rowHeight);
		int totalLevelY = gridBottom + STATS_OVERALL_GAP;
		return Math.max(BASE_FRAME_HEIGHT, totalLevelY + metrics.getHeight() + FOOTER_BOTTOM_MARGIN);
	}

	private void drawStats(Graphics2D graphics, StatsLayout layout, int frameWidth, PlayerHiscoreData hiscoreData)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int topY = layout.getContentTop();
		if (layout.isLoading() || layout.isUnavailable() || hiscoreData == null)
		{
			String message = layout.isLoading() ? "Looking up hiscores..." : "Hiscores unavailable";
			drawCenteredShadowText(graphics, message, new Rectangle(0, topY, frameWidth, metrics.getHeight()), combatTextColor());
			return;
		}

		for (StatCell cell : layout.getCells())
		{
			drawStatCell(graphics, cell, layout.isShowIcons());
		}

		drawCenteredShadowText(graphics, layout.getTotalLevelText(), new Rectangle(0, layout.getTotalLevelY(), frameWidth, metrics.getHeight()), statsLevelTextColor());
	}

	private void drawStatCell(Graphics2D graphics, StatCell cell, boolean showIcons)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		Rectangle bounds = cell.getBounds();
		String value = formatNumber(cell.getLevel());
		int valueWidth = metrics.stringWidth(value);
		if (showIcons)
		{
			int iconWidth = STATS_ICON_SIZE;
			int valueSlotWidth = measureStatsValueSlotWidth(metrics);
			int groupWidth = iconWidth + STATS_ICON_GAP + valueSlotWidth;
			int groupX = bounds.x + Math.max((bounds.width - groupWidth) / 2, 0);
			int groupHeight = Math.max(metrics.getHeight(), STATS_ICON_SIZE);
			int groupY = bounds.y + Math.max((bounds.height - groupHeight) / 2, 0);
			BufferedImage icon = getStatIcon(cell.getSkill());
			if (icon != null)
			{
				int iconMaxSize = hasAttackIconOverride(cell.getSkill()) ? STATS_ATTACK_ICON_SIZE : STATS_ICON_SIZE;
				Dimension iconSize = fitImage(icon, iconMaxSize, iconMaxSize);
				int iconX = groupX + ((STATS_ICON_SIZE - iconSize.width) / 2);
				int iconY = groupY + ((groupHeight - iconSize.height) / 2);
				graphics.drawImage(icon, iconX, iconY, iconSize.width, iconSize.height, null);
			}

			int valueX = groupX + STATS_ICON_SIZE + STATS_ICON_GAP;
			int valueBaseline = groupY + ((groupHeight - metrics.getHeight()) / 2) + metrics.getAscent();
			drawShadowText(graphics, value, valueX, valueBaseline, statsLevelTextColor());
			return;
		}

		int baseline = bounds.y + metrics.getAscent();
		String label = cell.getSkill().getLabel() + ": ";
		int labelWidth = metrics.stringWidth(label);
		int availableValueWidth = Math.max(0, bounds.width - labelWidth);
		String fittedValue = fitText(graphics, value, availableValueWidth);
		drawShadowText(graphics, label, bounds.x, baseline, statsLabelTextColor());
		drawShadowText(graphics, fittedValue, bounds.x + labelWidth, baseline, statsLevelTextColor());
	}

	private int measureStatCellWidth(FontMetrics metrics, PlayerHiscoreData.Skill skill, int level, boolean showIcons)
	{
		String value = formatNumber(level);
		if (showIcons)
		{
			return STATS_ICON_SIZE + STATS_ICON_GAP + measureStatsValueSlotWidth(metrics);
		}

		return metrics.stringWidth(skill.getLabel() + ": ") + metrics.stringWidth(value);
	}

	private BufferedImage getStatIcon(PlayerHiscoreData.Skill skill)
	{
		if (skill == PlayerHiscoreData.Skill.ATTACK)
		{
			PlayerExamineConfig.AttackIcon attackIcon = config.attackIcon();
			if (attackIcon != null && attackIcon.getItemId() >= 0)
			{
				AsyncBufferedImage icon = itemManager.getImage(attackIcon.getItemId());
				if (icon != null)
				{
					return icon;
				}
			}
		}

		return skillIconManager.getSkillImage(skill.getApiSkill(), false);
	}

	private boolean hasAttackIconOverride(PlayerHiscoreData.Skill skill)
	{
		PlayerExamineConfig.AttackIcon attackIcon = config.attackIcon();
		return skill == PlayerHiscoreData.Skill.ATTACK
			&& attackIcon != null
			&& attackIcon.getItemId() >= 0;
	}

	private static int measureStatsValueSlotWidth(FontMetrics metrics)
	{
		return metrics.stringWidth("99");
	}

	private static Dimension fitImage(BufferedImage image, int maxWidth, int maxHeight)
	{
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		if (imageWidth <= 0 || imageHeight <= 0)
		{
			return new Dimension(maxWidth, maxHeight);
		}

		double scale = Math.min((double) maxWidth / imageWidth, (double) maxHeight / imageHeight);
		int width = Math.max(1, (int) Math.round(imageWidth * scale));
		int height = Math.max(1, (int) Math.round(imageHeight * scale));
		return new Dimension(width, height);
	}

	private int calculateListFrameWidth(Graphics2D graphics, PlayerExamineData data, boolean showIcons, int minimumWidth)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		Map<String, EquipmentEntry> entries = new HashMap<>();
		for (EquipmentEntry entry : data.getEquipment())
		{
			entries.put(entry.getSlotName().toLowerCase(), entry);
		}

		int frameWidth = minimumWidth;
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "helmet", entries.get("head"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "cape", entries.get("cape"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "necklace", entries.get("amulet"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "arrows", null, false, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "weapon", entries.get("weapon"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "body", entries.get("torso"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "offhand", entries.get("shield"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "legs", entries.get("legs"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "gloves", entries.get("hands"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "boots", entries.get("boots"), true, showIcons));
		frameWidth = Math.max(frameWidth, measureListRowWidth(metrics, "ring", null, false, showIcons));
		return frameWidth;
	}

	private int measureListRowWidth(FontMetrics metrics, String key, EquipmentEntry entry, boolean showEmptyTooltip, boolean showIcons)
	{
		boolean hasItem = entry != null && entry.hasItem();
		boolean visibleSlot = hasItem || showEmptyTooltip;
		if (config.hideNotVisibleSlots() && !visibleSlot)
		{
			return 0;
		}

		String label = formatSlotLabel(key);
		String value = entry != null && entry.hasItem()
			? getDisplayItemName(entry)
			: (showEmptyTooltip ? "None" : "Not visible");

		int textWidth;
		if (showIcons)
		{
			textWidth = Math.max(metrics.stringWidth(label), metrics.stringWidth(value));
			textWidth += HYBRID_ICON_SIZE + HYBRID_ICON_GAP;
		}
		else
		{
			textWidth = metrics.stringWidth(label) + metrics.stringWidth(": ") + metrics.stringWidth(value);
		}

		return (LIST_SIDE_PADDING * 2) + textWidth;
	}

	private SlotState createSlot(String key, Rectangle bounds, EquipmentEntry entry, boolean drawFrame, boolean showEmptyTooltip)
	{
		return new SlotState(key, bounds, entry, drawFrame, showEmptyTooltip);
	}

	private ContentLayout buildListContent(Graphics2D graphics, PlayerExamineData data, List<SlotState> slots, boolean showIcons, int frameWidth, int contentTop)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		List<ListRow> rows = new ArrayList<>();
		int currentY = contentTop;
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

		return ContentLayout.forRows(slots, rows, rows.isEmpty() ? contentTop : currentY - 1);
	}

	private int calculateEquipmentContentOffset(ContentLayout contentLayout, FooterLayout footerLayout, int frameHeight, int contentTop)
	{
		if (contentLayout.isListMode())
		{
			return 0;
		}

		int contentHeight = contentLayout.getContentBottom() - contentTop;
		if (contentHeight <= 0)
		{
			return 0;
		}

		boolean hasFooter = !footerLayout.isEmpty();
		int availableBottom = hasFooter
			? footerLayout.getStartY() - FOOTER_TOP_MARGIN
			: frameHeight - FOOTER_BOTTOM_MARGIN;
		int availableHeight = availableBottom - contentTop;
		if (availableHeight <= contentHeight)
		{
			return 0;
		}

		int extraHeight = availableHeight - contentHeight;
		return hasFooter ? extraHeight / 2 : extraHeight / 3;
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
			Color fillColor = hasItem ? overlaySlotFillColor() : overlaySlotEmptyColor();
			if (fillColor.getAlpha() > 0)
			{
				graphics.setColor(applyOverlayTransparency(fillColor));
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}

			Color valueHighlight = getValueHighlightColor(entry);
			if (valueHighlight != null)
			{
				drawSlotValueHighlight(graphics, bounds, valueHighlight);
			}

			graphics.setColor(applyOverlayTransparency(valueHighlight != null ? valueHighlight : (hover ? overlaySlotHoverColor() : overlaySlotBorderColor())));
			graphics.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 1, bounds.height + 1);
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

	private Color getValueHighlightColor(EquipmentEntry entry)
	{
		PlayerExamineConfig.ValueHighlightThreshold threshold = config.valueHighlightThreshold();
		if (threshold == null || threshold == PlayerExamineConfig.ValueHighlightThreshold.Off || entry == null || !entry.hasItem() || entry.getItemId() < 0)
		{
			return null;
		}

		int geValue = itemManager.getItemPriceWithSource(entry.getItemId(), false);
		return geValue >= threshold.getValue() ? valueHighlightColor() : null;
	}

	private void drawSlotValueHighlight(Graphics2D graphics, Rectangle bounds, Color color)
	{
		Stroke originalStroke = graphics.getStroke();
		for (int i = 4; i >= 1; i--)
		{
			float ringAlpha = 0.08f + (0.05f * (4 - i));
			int alpha = Math.min(210, Math.max(0, Math.round(color.getAlpha() * ringAlpha)));
			if (alpha <= 0)
			{
				continue;
			}

			graphics.setColor(applyOverlayTransparency(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha)));
			graphics.setStroke(new BasicStroke(1f));
			graphics.drawRect(bounds.x - i, bounds.y - i, bounds.width - 1 + (i * 2), bounds.height - 1 + (i * 2));
		}

		graphics.setStroke(originalStroke);
	}

	private Rectangle[] buildSlotBoxes(int frameWidth, int contentTop)
	{
		int gridWidth = (3 * SLOT_SIZE) + (2 * SLOT_GAP_X);
		int gridStartX = Math.max((frameWidth - gridWidth) / 2, 0);

		return new Rectangle[] {
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, contentTop, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX, contentTop + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, contentTop + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + 2 * (SLOT_SIZE + SLOT_GAP_X), contentTop + SLOT_SIZE + SLOT_GAP_Y, SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX, contentTop + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, contentTop + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + 2 * (SLOT_SIZE + SLOT_GAP_X), contentTop + 2 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, contentTop + 3 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX, contentTop + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + SLOT_SIZE + SLOT_GAP_X, contentTop + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE),
			new Rectangle(gridStartX + 2 * (SLOT_SIZE + SLOT_GAP_X), contentTop + 4 * (SLOT_SIZE + SLOT_GAP_Y), SLOT_SIZE, SLOT_SIZE)
		};
	}

	private void addHoverTooltip()
	{
		RenderState state = renderState;
		if (state == null || state.isEmpty())
		{
			return;
		}

		if (state.getSelectedTab() == OverlayTab.STATS)
		{
			Point mouse = client.getMouseCanvasPosition();
			Rectangle bounds = getBounds();
			if (mouse == null || bounds == null || !bounds.contains(mouse.getX(), mouse.getY()))
			{
				return;
			}

			int localX = mouse.getX() - bounds.x;
			int localY = mouse.getY() - bounds.y;
			StatCell statCell = state.getStatAt(localX, localY);
			if (statCell != null)
			{
				String tooltip = buildStatsTooltip(statCell);
				if (tooltip != null && !tooltip.isEmpty())
				{
					addTooltip(tooltip);
				}
			}
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
						addTooltip("Nothing equipped");
					}
					else if (!config.hideNotVisibleSlots())
					{
						addTooltip("Not visible from examine");
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
					addTooltip("Nothing equipped");
				}
				else if (!config.hideNotVisibleSlots())
				{
					addTooltip("Not visible from examine");
				}
				return;
			}
		}
	}

	private String buildStatsTooltip(StatCell cell)
	{
		List<String> lines = new ArrayList<>();
		if (config.showSkillName())
		{
			lines.add(formatTooltipLabel(cell.getSkill().getFullName()));
		}

		if (config.showSkillRank())
		{
			lines.add(formatTooltipStatLine("Rank", formatRank(cell.getRank()), null));
		}

		if (config.showSkillXp())
		{
			lines.add(formatTooltipStatLine("Experience", formatPrice(cell.getExperience()), null));
		}

		if (cell.getSkill() != PlayerHiscoreData.Skill.OVERALL)
		{
			if (config.showRemainingXp())
			{
				lines.add(formatTooltipStatLine("Remaining XP", formatPrice(getRemainingXp(cell.getLevel(), cell.getExperience())), null));
			}
		}

		if (lines.isEmpty())
		{
			return null;
		}

		return String.join("<br>", lines);
	}

	private void addTooltip(String text)
	{
		if (!config.matchTooltipContainerToTheme())
		{
			tooltipManager.add(new Tooltip(text));
			return;
		}

		tooltipManager.add(new Tooltip(new ThemedTooltipComponent(
			text,
			applyTooltipTransparency(overlayBackgroundColor()),
			applyTooltipTransparency(overlayBorderColor()))));
	}

	private static String formatRank(int rank)
	{
		return rank > 0 ? "#" + formatPrice(rank) : "Unranked";
	}

	private long getRemainingXp(int level, long experience)
	{
		if (level >= 99)
		{
			return 0L;
		}

		int nextLevel = Math.min(level + 1, 126);
		long xpForNextLevel = Experience.getXpForLevel(nextLevel);
		return Math.max(xpForNextLevel - experience, 0L);
	}

	private void addItemTooltips(EquipmentEntry entry)
	{
		addTooltip(formatTooltipLabel(getDisplayItemName(entry)));

		List<String> valueLines = new ArrayList<>();
		if (config.showGeValue())
		{
			valueLines.add(formatTooltipStatLine("GE", formatItemValue(itemManager.getItemPriceWithSource(entry.getItemId(), false)), null));
		}

		if (config.showHaValue())
		{
			valueLines.add(formatTooltipStatLine("HA", formatItemValue(client.getItemDefinition(entry.getItemId()).getHaPrice()), null));
		}

		if (!valueLines.isEmpty())
		{
			addTooltip(String.join("<br>", valueLines));
		}

		if (config.showEquipmentBonuses())
		{
			String equipmentBonusTooltip = buildEquipmentBonusTooltip(entry.getItemId());
			if (!equipmentBonusTooltip.isEmpty())
			{
				addTooltip(equipmentBonusTooltip);
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

		return ColorUtil.wrapWithColorTag("(" + formatted + ")", applyTooltipTextTransparency(color));
	}

	private String formatTooltipLabel(String text)
	{
		return ColorUtil.wrapWithColorTag(text, applyTooltipTextTransparency(config.tooltipItemTextColor()));
	}

	private String formatTooltipStatLine(String label, String value, String delta)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(ColorUtil.wrapWithColorTag(label + ": ", applyTooltipTextTransparency(tooltipLabelTextColor())));
		builder.append(ColorUtil.wrapWithColorTag(value, applyTooltipTextTransparency(tooltipValueTextColor())));
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

		String geText = "GE: " + formatTotalValue(geTotal);
		String haText = "HA: " + formatTotalValue(haTotal);
		int footerWidth = frameWidth - (FOOTER_SIDE_PADDING * 2);
		int lineHeight = metrics.getHeight();
		int footerStartY = contentBottom + FOOTER_TOP_MARGIN;

		switch (totalValueMode)
		{
			case Ge:
				return FooterLayout.single(new FooterLine(geText, totalGeTextColor()), footerStartY, lineHeight, FOOTER_BOTTOM_MARGIN);
			case HA:
				return FooterLayout.single(new FooterLine(haText, totalHaTextColor()), footerStartY, lineHeight, FOOTER_BOTTOM_MARGIN);
			case Both:
				int inlineWidth = metrics.stringWidth(geText)
					+ metrics.stringWidth("  |  ")
					+ metrics.stringWidth(haText);
				if (inlineWidth <= footerWidth)
				{
					return FooterLayout.inline(
						new FooterLine(geText, totalGeTextColor()),
						new FooterLine(haText, totalHaTextColor()),
						footerStartY,
						lineHeight,
						FOOTER_BOTTOM_MARGIN);
				}
				return FooterLayout.stacked(
					new FooterLine(geText, totalGeTextColor()),
					new FooterLine(haText, totalHaTextColor()),
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
		drawShadowText(graphics, separator, x, baseline, combatTextColor());
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

	private String formatTotalValue(long value)
	{
		return formatValue(value, config.totalValueFormat());
	}

	private String formatItemValue(long value)
	{
		return formatValue(value, config.itemValueFormat());
	}

	private String formatValue(long value, PlayerExamineConfig.TotalValueFormat format)
	{
		switch (format)
		{
			case Short:
				return formatCompactValue(value);
			case Both:
				return formatCombinedValue(value);
			case Long:
			default:
				return formatPrice(value);
		}
	}

	private String formatCombinedValue(long value)
	{
		String longValue = formatPrice(value);
		String compactValue = formatCompactValue(value);
		if (longValue.equals(compactValue))
		{
			return longValue;
		}

		return longValue + " (" + compactValue + ")";
	}

	private String formatCompactValue(long value)
	{
		long absoluteValue = Math.abs(value);
		if (absoluteValue < 10_000)
		{
			return formatPrice(value);
		}

		if (absoluteValue < 100_000)
		{
			return formatShortValue(value, 1_000L, "k", 1);
		}

		if (absoluteValue < 1_000_000)
		{
			return (value / 1_000L) + "k";
		}

		if (absoluteValue < 1_000_000_000)
		{
			return formatShortValue(value, 1_000_000L, "m", 2);
		}

		return formatShortValue(value, 1_000_000_000L, "b", 2);
	}

	private static String formatShortValue(long value, long divisor, String suffix, int decimalPlaces)
	{
		double shortValue = value / (double) divisor;
		String formatted = String.format(Locale.US, "%." + decimalPlaces + "f", shortValue);
		while (formatted.contains(".") && formatted.endsWith("0"))
		{
			formatted = formatted.substring(0, formatted.length() - 1);
		}
		if (formatted.endsWith("."))
		{
			formatted = formatted.substring(0, formatted.length() - 1);
		}

		return formatted + suffix;
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

		FooterLayout withVerticalOffset(int delta)
		{
			if (delta <= 0)
			{
				return this;
			}

			return new FooterLayout(lines, inlinePair, stacked, startY + delta, frameHeight + delta);
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

		static ContentLayout forSlots(List<SlotState> slots, int contentTop)
		{
			int contentBottom = contentTop + (4 * (SLOT_SIZE + SLOT_GAP_Y)) + SLOT_SIZE;
			int frameHeight = Math.max(BASE_FRAME_HEIGHT, contentBottom + FOOTER_BOTTOM_MARGIN);
			return new ContentLayout(slots, new ArrayList<>(), false, frameHeight, contentBottom);
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

		ContentLayout withVerticalOffset(int delta)
		{
			if (delta <= 0 || listMode)
			{
				return this;
			}

			List<SlotState> movedSlots = new ArrayList<>();
			for (SlotState slot : slots)
			{
				Rectangle bounds = new Rectangle(slot.getBounds());
				bounds.y += delta;
				movedSlots.add(new SlotState(slot.getKey(), bounds, slot.getEntry(), slot.isDrawFrame(), slot.isShowEmptyTooltip()));
			}

			return new ContentLayout(movedSlots, rows, false, frameHeight + delta, contentBottom + delta);
		}
	}

	private static final class TabLayout
	{
		private final Rectangle equipmentTab;
		private final Rectangle statsTab;

		private TabLayout(Rectangle equipmentTab, Rectangle statsTab)
		{
			this.equipmentTab = equipmentTab;
			this.statsTab = statsTab;
		}

		Rectangle getEquipmentTab()
		{
			return equipmentTab;
		}

		Rectangle getStatsTab()
		{
			return statsTab;
		}

		OverlayTab getTabAt(int localX, int localY)
		{
			if (equipmentTab.contains(localX, localY))
			{
				return OverlayTab.EQUIPMENT;
			}
			if (statsTab.contains(localX, localY))
			{
				return OverlayTab.STATS;
			}
			return null;
		}
	}

	private static final class StatsLayout
	{
		private final int frameHeight;
		private final int contentTop;
		private final boolean showIcons;
		private final boolean loading;
		private final boolean unavailable;
		private final List<StatCell> cells;
		private final String totalLevelText;
		private final int totalLevelY;
		private final int totalLevelHeight;

		private StatsLayout(int frameHeight, int contentTop, boolean showIcons, boolean loading, boolean unavailable, List<StatCell> cells, String totalLevelText, int totalLevelY, int totalLevelHeight)
		{
			this.frameHeight = frameHeight;
			this.contentTop = contentTop;
			this.showIcons = showIcons;
			this.loading = loading;
			this.unavailable = unavailable;
			this.cells = cells;
			this.totalLevelText = totalLevelText;
			this.totalLevelY = totalLevelY;
			this.totalLevelHeight = totalLevelHeight;
		}

		static StatsLayout empty(int contentTop)
		{
			return new StatsLayout(BASE_FRAME_HEIGHT, contentTop, false, false, false, new ArrayList<>(), "", contentTop, 0);
		}

		int getFrameHeight()
		{
			return frameHeight;
		}

		boolean isLoading()
		{
			return loading;
		}

		boolean isUnavailable()
		{
			return unavailable;
		}

		int getContentTop()
		{
			return contentTop;
		}

		boolean isShowIcons()
		{
			return showIcons;
		}

		List<StatCell> getCells()
		{
			return cells;
		}

		String getTotalLevelText()
		{
			return totalLevelText;
		}

		int getTotalLevelY()
		{
			return totalLevelY;
		}

		int getTotalLevelHeight()
		{
			return totalLevelHeight;
		}
	}

	private static final class StatCell
	{
		private final PlayerHiscoreData.Skill skill;
		private final int rank;
		private final int level;
		private final long experience;
		private final Rectangle bounds;

		private StatCell(PlayerHiscoreData.Skill skill, int rank, int level, long experience, Rectangle bounds)
		{
			this.skill = skill;
			this.rank = rank;
			this.level = level;
			this.experience = experience;
			this.bounds = bounds;
		}

		PlayerHiscoreData.Skill getSkill()
		{
			return skill;
		}

		int getLevel()
		{
			return level;
		}

		int getRank()
		{
			return rank;
		}

		long getExperience()
		{
			return experience;
		}

		Rectangle getBounds()
		{
			return bounds;
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

	private static final class ThemeColors
	{
		private final Color background;
		private final Color border;
		private final Color headerText;
		private final Color subText;
		private final Color closeText;
		private final Color closeBorder;
		private final Color closeFill;
		private final Color closeHover;
		private final Color slotFill;
		private final Color slotEmpty;
		private final Color slotBorder;
		private final Color slotHover;
		private final Color totalGe;
		private final Color totalHa;
		private final Color flair;
		private final Color valueHighlight;

		private ThemeColors(
			Color background,
			Color border,
			Color headerText,
			Color subText,
			Color closeText,
			Color closeBorder,
			Color closeFill,
			Color closeHover,
			Color slotFill,
			Color slotEmpty,
			Color slotBorder,
			Color slotHover,
			Color totalGe,
			Color totalHa,
			Color flair,
			Color valueHighlight)
		{
			this.background = background;
			this.border = border;
			this.headerText = headerText;
			this.subText = subText;
			this.closeText = closeText;
			this.closeBorder = closeBorder;
			this.closeFill = closeFill;
			this.closeHover = closeHover;
			this.slotFill = slotFill;
			this.slotEmpty = slotEmpty;
			this.slotBorder = slotBorder;
			this.slotHover = slotHover;
			this.totalGe = totalGe;
			this.totalHa = totalHa;
			this.flair = flair;
			this.valueHighlight = valueHighlight;
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

	private Color overlayBackgroundColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlayBackgroundColor() : theme.background;
	}

	private Color overlayBorderColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlayBorderColor() : theme.border;
	}

	private Color usernameTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.usernameTextColor() : theme.headerText;
	}

	private Color combatTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.combatTextColor() : theme.subText;
	}

	private Color activeTabTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.activeTabTextColor() : theme.headerText;
	}

	private Color inactiveTabTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.inactiveTabTextColor() : theme.subText;
	}

	private Color statsLabelTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.statsLabelTextColor() : theme.subText;
	}

	private Color statsLevelTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.statsLevelTextColor() : theme.headerText;
	}

	private Color xTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.xTextColor() : theme.closeText;
	}

	private Color overlayCloseColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlayCloseColor() : theme.closeFill;
	}

	private Color overlayCloseHoverColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlayCloseHoverColor() : theme.closeHover;
	}

	private Color overlaySlotFillColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlaySlotFillColor() : theme.slotFill;
	}

	private Color overlaySlotEmptyColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlaySlotEmptyColor() : theme.slotEmpty;
	}

	private Color overlaySlotBorderColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlaySlotBorderColor() : theme.slotBorder;
	}

	private Color overlaySlotHoverColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.overlaySlotHoverColor() : theme.slotHover;
	}

	private Color totalGeTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.totalGeTextColor() : theme.totalGe;
	}

	private Color totalHaTextColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.totalHaTextColor() : theme.totalHa;
	}

	private Color openingFlairColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.openingFlairColor() : theme.flair;
	}

	private Color valueHighlightColor()
	{
		ThemeColors theme = themeColors();
		return theme == null ? config.valueHighlightColor() : theme.valueHighlight;
	}

	private Color tooltipLabelTextColor()
	{
		if (!config.matchTooltipLabelValueToTheme())
		{
			return config.tooltipOtherTextColor();
		}

		ThemeColors theme = themeColors();
		return theme == null ? config.combatTextColor() : theme.subText;
	}

	private Color tooltipValueTextColor()
	{
		if (!config.matchTooltipLabelValueToTheme())
		{
			return config.tooltipValueTextColor();
		}

		ThemeColors theme = themeColors();
		return theme == null ? config.usernameTextColor() : theme.headerText;
	}

	private ThemeColors themeColors()
	{
		return themeColors(config.themePreset());
	}

	private static ThemeColors themeColors(PlayerExamineConfig.ThemePreset preset)
	{
		if (preset == null || preset == PlayerExamineConfig.ThemePreset.Custom)
		{
			return null;
		}

		switch (preset)
		{
			case Classic:
				return new ThemeColors(
					new Color(31, 24, 17, 230),
					new Color(118, 94, 60, 255),
					new Color(235, 226, 193),
					new Color(235, 226, 193),
					new Color(200, 186, 140),
					new Color(53, 42, 28),
					new Color(44, 31, 22),
					new Color(94, 30, 26),
					new Color(41, 31, 23, 255),
					new Color(24, 19, 14, 255),
					new Color(111, 89, 56, 255),
					new Color(150, 122, 76),
					new Color(235, 226, 193),
					new Color(200, 186, 140),
					new Color(215, 125, 40, 220),
					new Color(255, 190, 64, 210));
			case LightClassic:
				return new ThemeColors(
					new Color(226, 214, 188, 235),
					new Color(116, 95, 60, 255),
					new Color(50, 38, 24),
					new Color(78, 62, 39),
					new Color(58, 42, 26),
					new Color(135, 111, 70),
					new Color(210, 193, 158),
					new Color(188, 154, 91),
					new Color(204, 187, 151, 255),
					new Color(184, 169, 138, 255),
					new Color(126, 103, 65, 255),
					new Color(168, 130, 68),
					new Color(50, 38, 24),
					new Color(78, 62, 39),
					new Color(202, 139, 50, 220),
					new Color(218, 154, 58, 210));
			case Light:
				return new ThemeColors(
					new Color(238, 241, 244, 210),
					new Color(136, 146, 156, 255),
					new Color(32, 38, 44),
					new Color(80, 88, 96),
					new Color(34, 40, 46),
					new Color(144, 152, 160),
					new Color(222, 226, 230, 210),
					new Color(198, 207, 216, 210),
					new Color(224, 228, 232, 210),
					new Color(210, 215, 220, 210),
					new Color(146, 156, 166, 255),
					new Color(116, 131, 145),
					new Color(32, 38, 44),
					new Color(80, 88, 96),
					new Color(116, 160, 190, 198),
					new Color(128, 190, 220, 189));
			case Dark:
				return new ThemeColors(
					new Color(13, 15, 18, 235),
					new Color(86, 96, 106, 255),
					new Color(222, 229, 235),
					new Color(185, 196, 205),
					new Color(220, 226, 232),
					new Color(45, 52, 60),
					new Color(28, 32, 37),
					new Color(80, 91, 103),
					new Color(28, 32, 37, 255),
					new Color(17, 20, 24, 255),
					new Color(75, 85, 96, 255),
					new Color(116, 131, 145),
					new Color(222, 229, 235),
					new Color(174, 184, 194),
					new Color(116, 160, 190, 220),
					new Color(128, 190, 220, 210));
			case Gold:
				return new ThemeColors(
					new Color(28, 22, 10, 235),
					new Color(172, 130, 38, 255),
					new Color(255, 232, 154),
					new Color(232, 201, 104),
					new Color(255, 236, 164),
					new Color(77, 57, 19),
					new Color(61, 43, 15),
					new Color(138, 84, 22),
					new Color(53, 39, 16, 255),
					new Color(28, 21, 10, 255),
					new Color(147, 110, 37, 255),
					new Color(211, 164, 59),
					new Color(255, 232, 154),
					new Color(232, 201, 104),
					new Color(255, 190, 64, 230),
					new Color(255, 218, 96, 220));
			case Zaros:
				return new ThemeColors(
					new Color(19, 17, 31, 235),
					new Color(101, 76, 160, 255),
					new Color(230, 219, 255),
					new Color(190, 176, 230),
					new Color(230, 219, 255),
					new Color(48, 38, 78),
					new Color(36, 28, 58),
					new Color(93, 49, 129),
					new Color(34, 29, 53, 255),
					new Color(22, 19, 36, 255),
					new Color(88, 70, 133, 255),
					new Color(133, 93, 188),
					new Color(230, 219, 255),
					new Color(190, 176, 230),
					new Color(162, 105, 232, 225),
					new Color(186, 134, 255, 215));
			case Guthix:
				return new ThemeColors(
					new Color(14, 27, 20, 235),
					new Color(70, 126, 81, 255),
					new Color(219, 239, 207),
					new Color(180, 218, 160),
					new Color(219, 239, 207),
					new Color(34, 68, 43),
					new Color(25, 49, 33),
					new Color(65, 117, 61),
					new Color(27, 50, 35, 255),
					new Color(17, 31, 23, 255),
					new Color(64, 105, 67, 255),
					new Color(102, 159, 90),
					new Color(219, 239, 207),
					new Color(180, 218, 160),
					new Color(116, 210, 92, 225),
					new Color(142, 226, 108, 215));
			case Saradomin:
				return new ThemeColors(
					new Color(13, 22, 36, 235),
					new Color(64, 111, 178, 255),
					new Color(222, 238, 255),
					new Color(170, 207, 245),
					new Color(222, 238, 255),
					new Color(31, 55, 89),
					new Color(24, 43, 70),
					new Color(55, 103, 166),
					new Color(26, 45, 73, 255),
					new Color(16, 29, 48, 255),
					new Color(54, 93, 146, 255),
					new Color(85, 143, 214),
					new Color(222, 238, 255),
					new Color(170, 207, 245),
					new Color(92, 166, 245, 225),
					new Color(116, 190, 255, 215));
			case Blood:
				return new ThemeColors(
					new Color(31, 12, 13, 235),
					new Color(151, 48, 48, 255),
					new Color(247, 220, 205),
					new Color(226, 153, 137),
					new Color(247, 220, 205),
					new Color(78, 28, 29),
					new Color(55, 20, 21),
					new Color(138, 42, 42),
					new Color(48, 24, 24, 255),
					new Color(28, 14, 15, 255),
					new Color(122, 42, 42, 255),
					new Color(190, 64, 64),
					new Color(247, 220, 205),
					new Color(226, 153, 137),
					new Color(230, 72, 72, 225),
					new Color(255, 92, 76, 215));
			default:
				return null;
		}
	}

	public void copyThemeColorsToCustom(ConfigManager configManager, PlayerExamineConfig.CustomColorStartingPoint startingPoint)
	{
		ThemeColors defaultColors = defaultThemeColors(startingPoint);
		if (defaultColors == null)
		{
			return;
		}

		copySavedStartingPointToCustom(configManager, startingPoint, defaultColors);
		configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, "themePreset", PlayerExamineConfig.ThemePreset.Custom.name());
	}

	public String exportCurrentColorTheme(String themeName, Gson gson)
	{
		ThemeColors colors = themeColors();
		return PlayerExamineColorSettings.exportToJson(themeName, themeColorMap(colors == null ? currentCustomColors() : colors), gson);
	}

	public int applyColorTheme(ConfigManager configManager, String json)
	{
		Map<String, String> colors = PlayerExamineColorSettings.importFromJson(json);

		for (Map.Entry<String, String> entry : colors.entrySet())
		{
			configManager.setConfiguration(
				PlayerExamineConfig.CONFIG_GROUP,
				customPresetKey(PlayerExamineConfig.CustomColorStartingPoint.SidePanelTheme, entry.getKey()),
				entry.getValue());
			setColorConfig(configManager, entry.getKey(), new Color(Integer.parseInt(entry.getValue()), true));
		}

		configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, "themePreset", PlayerExamineConfig.ThemePreset.Custom.name());
		configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, "customColorStartingPoint", PlayerExamineConfig.CustomColorStartingPoint.SidePanelTheme.name());
		return colors.size();
	}

	public void saveCustomPresetColor(ConfigManager configManager, String keyName)
	{
		PlayerExamineConfig.CustomColorStartingPoint startingPoint = config.customColorStartingPoint();
		if (startingPoint == null || !isCustomColorKey(keyName))
		{
			return;
		}

		if (!customPresetExists(configManager, startingPoint))
		{
			saveCurrentCustomColorsToPreset(configManager, startingPoint);
		}

		String value = configManager.getConfiguration(PlayerExamineConfig.CONFIG_GROUP, keyName);
		if (value != null)
		{
			configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, customPresetKey(startingPoint, keyName), value);
			return;
		}

		ThemeColors defaultColors = defaultThemeColors(startingPoint);
		Color defaultColor = colorForKey(defaultColors, keyName);
		if (defaultColor != null)
		{
			setColorConfig(configManager, keyName, defaultColor);
			setColorConfig(configManager, customPresetKey(startingPoint, keyName), defaultColor);
		}
	}

	private static Map<String, String> themeColorMap(ThemeColors colors)
	{
		Map<String, String> colorMap = new LinkedHashMap<>();
		for (String key : PlayerExamineColorSettings.THEME_COLOR_KEYS)
		{
			Color color = colorForKey(colors, key);
			if (color != null)
			{
				colorMap.put(key, Integer.toString(color.getRGB()));
			}
		}

		return colorMap;
	}

	private void copySavedStartingPointToCustom(
		ConfigManager configManager,
		PlayerExamineConfig.CustomColorStartingPoint startingPoint,
		ThemeColors defaultColors)
	{
		if (!customPresetExists(configManager, startingPoint))
		{
			setThemeColorsToCustom(configManager, defaultColors);
			saveThemeColorsToPreset(configManager, startingPoint, defaultColors);
			return;
		}

		for (String keyName : CUSTOM_COLOR_KEYS)
		{
			Color color = configManager.getConfiguration(PlayerExamineConfig.CONFIG_GROUP, customPresetKey(startingPoint, keyName), Color.class);
			if (color != null)
			{
				setColorConfig(configManager, keyName, color);
			}
		}
	}

	private static void saveThemeColorsToPreset(ConfigManager configManager, PlayerExamineConfig.CustomColorStartingPoint startingPoint, ThemeColors colors)
	{
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayBackgroundColor"), colors.background);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayBorderColor"), colors.border);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayHeaderTextColor"), colors.headerText);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySubTextColor"), colors.subText);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayCloseTextColor"), colors.closeText);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayCloseColor"), colors.closeFill);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayCloseHoverColor"), colors.closeHover);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotFillColor"), colors.slotFill);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotEmptyColor"), colors.slotEmpty);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotBorderColor"), colors.slotBorder);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotHoverColor"), colors.slotHover);
		setColorConfig(configManager, customPresetKey(startingPoint, "totalGeTextColor"), colors.totalGe);
		setColorConfig(configManager, customPresetKey(startingPoint, "totalHaTextColor"), colors.totalHa);
		setColorConfig(configManager, customPresetKey(startingPoint, "openingFlairColor"), colors.flair);
		setColorConfig(configManager, customPresetKey(startingPoint, "valueHighlightColor"), colors.valueHighlight);
		setColorConfig(configManager, customPresetKey(startingPoint, "activeTabTextColor"), colors.headerText);
		setColorConfig(configManager, customPresetKey(startingPoint, "inactiveTabTextColor"), colors.subText);
		setColorConfig(configManager, customPresetKey(startingPoint, "statsLabelTextColor"), colors.subText);
		setColorConfig(configManager, customPresetKey(startingPoint, "statsLevelTextColor"), colors.headerText);
	}

	private void saveCurrentCustomColorsToPreset(ConfigManager configManager, PlayerExamineConfig.CustomColorStartingPoint startingPoint)
	{
		ThemeColors colors = currentCustomColors();
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayBackgroundColor"), colors.background);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayBorderColor"), colors.border);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayHeaderTextColor"), colors.headerText);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySubTextColor"), colors.subText);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayCloseTextColor"), colors.closeText);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayCloseColor"), colors.closeFill);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlayCloseHoverColor"), colors.closeHover);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotFillColor"), colors.slotFill);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotEmptyColor"), colors.slotEmpty);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotBorderColor"), colors.slotBorder);
		setColorConfig(configManager, customPresetKey(startingPoint, "overlaySlotHoverColor"), colors.slotHover);
		setColorConfig(configManager, customPresetKey(startingPoint, "totalGeTextColor"), colors.totalGe);
		setColorConfig(configManager, customPresetKey(startingPoint, "totalHaTextColor"), colors.totalHa);
		setColorConfig(configManager, customPresetKey(startingPoint, "openingFlairColor"), colors.flair);
		setColorConfig(configManager, customPresetKey(startingPoint, "valueHighlightColor"), colors.valueHighlight);
		setColorConfig(configManager, customPresetKey(startingPoint, "activeTabTextColor"), config.activeTabTextColor());
		setColorConfig(configManager, customPresetKey(startingPoint, "inactiveTabTextColor"), config.inactiveTabTextColor());
		setColorConfig(configManager, customPresetKey(startingPoint, "statsLabelTextColor"), config.statsLabelTextColor());
		setColorConfig(configManager, customPresetKey(startingPoint, "statsLevelTextColor"), config.statsLevelTextColor());
	}

	private ThemeColors currentCustomColors()
	{
		return new ThemeColors(
			config.overlayBackgroundColor(),
			config.overlayBorderColor(),
			config.usernameTextColor(),
			config.combatTextColor(),
			config.xTextColor(),
			config.overlayBorderColor(),
			config.overlayCloseColor(),
			config.overlayCloseHoverColor(),
			config.overlaySlotFillColor(),
			config.overlaySlotEmptyColor(),
			config.overlaySlotBorderColor(),
			config.overlaySlotHoverColor(),
			config.totalGeTextColor(),
			config.totalHaTextColor(),
			config.openingFlairColor(),
			config.valueHighlightColor());
	}

	private static void setThemeColorsToCustom(ConfigManager configManager, ThemeColors theme)
	{
		setColorConfig(configManager, "overlayBackgroundColor", theme.background);
		setColorConfig(configManager, "overlayBorderColor", theme.border);
		setColorConfig(configManager, "overlayHeaderTextColor", theme.headerText);
		setColorConfig(configManager, "overlaySubTextColor", theme.subText);
		setColorConfig(configManager, "overlayCloseTextColor", theme.closeText);
		setColorConfig(configManager, "overlayCloseColor", theme.closeFill);
		setColorConfig(configManager, "overlayCloseHoverColor", theme.closeHover);
		setColorConfig(configManager, "overlaySlotFillColor", theme.slotFill);
		setColorConfig(configManager, "overlaySlotEmptyColor", theme.slotEmpty);
		setColorConfig(configManager, "overlaySlotBorderColor", theme.slotBorder);
		setColorConfig(configManager, "overlaySlotHoverColor", theme.slotHover);
		setColorConfig(configManager, "totalGeTextColor", theme.totalGe);
		setColorConfig(configManager, "totalHaTextColor", theme.totalHa);
		setColorConfig(configManager, "openingFlairColor", theme.flair);
		setColorConfig(configManager, "valueHighlightColor", theme.valueHighlight);
		setColorConfig(configManager, "activeTabTextColor", theme.headerText);
		setColorConfig(configManager, "inactiveTabTextColor", theme.subText);
		setColorConfig(configManager, "statsLabelTextColor", theme.subText);
		setColorConfig(configManager, "statsLevelTextColor", theme.headerText);
	}

	private static boolean customPresetExists(ConfigManager configManager, PlayerExamineConfig.CustomColorStartingPoint startingPoint)
	{
		return configManager.getConfiguration(PlayerExamineConfig.CONFIG_GROUP, customPresetKey(startingPoint, "overlayBackgroundColor")) != null;
	}

	private static boolean isCustomStartingPoint(PlayerExamineConfig.CustomColorStartingPoint startingPoint)
	{
		return startingPoint == PlayerExamineConfig.CustomColorStartingPoint.Custom1
			|| startingPoint == PlayerExamineConfig.CustomColorStartingPoint.Custom2
			|| startingPoint == PlayerExamineConfig.CustomColorStartingPoint.Custom3
			|| startingPoint == PlayerExamineConfig.CustomColorStartingPoint.SidePanelTheme;
	}

	private static ThemeColors defaultThemeColors(PlayerExamineConfig.CustomColorStartingPoint startingPoint)
	{
		if (startingPoint == null)
		{
			return null;
		}

		if (isCustomStartingPoint(startingPoint))
		{
			return themeColors(PlayerExamineConfig.ThemePreset.Classic);
		}

		return themeColors(themePresetFromStartingPoint(startingPoint));
	}

	private static boolean isCustomColorKey(String keyName)
	{
		for (String customColorKey : CUSTOM_COLOR_KEYS)
		{
			if (customColorKey.equals(keyName))
			{
				return true;
			}
		}

		return false;
	}

	private static Color colorForKey(ThemeColors colors, String keyName)
	{
		if (colors == null)
		{
			return null;
		}

		switch (keyName)
		{
			case "overlayBackgroundColor":
				return colors.background;
			case "overlayBorderColor":
				return colors.border;
			case "overlayHeaderTextColor":
			case "activeTabTextColor":
			case "statsLevelTextColor":
				return colors.headerText;
			case "overlaySubTextColor":
			case "inactiveTabTextColor":
			case "statsLabelTextColor":
				return colors.subText;
			case "overlayCloseTextColor":
				return colors.closeText;
			case "overlayCloseColor":
				return colors.closeFill;
			case "overlayCloseHoverColor":
				return colors.closeHover;
			case "overlaySlotFillColor":
				return colors.slotFill;
			case "overlaySlotEmptyColor":
				return colors.slotEmpty;
			case "overlaySlotBorderColor":
				return colors.slotBorder;
			case "overlaySlotHoverColor":
				return colors.slotHover;
			case "totalGeTextColor":
				return colors.totalGe;
			case "totalHaTextColor":
				return colors.totalHa;
			case "openingFlairColor":
				return colors.flair;
			case "valueHighlightColor":
				return colors.valueHighlight;
			default:
				return null;
		}
	}

	private static String customPresetKey(PlayerExamineConfig.CustomColorStartingPoint startingPoint, String keyName)
	{
		return CUSTOM_PRESET_PREFIX + startingPoint.name() + "." + keyName;
	}

	private static PlayerExamineConfig.ThemePreset themePresetFromStartingPoint(PlayerExamineConfig.CustomColorStartingPoint startingPoint)
	{
		if (startingPoint == null)
		{
			return PlayerExamineConfig.ThemePreset.Custom;
		}

		return PlayerExamineConfig.ThemePreset.valueOf(startingPoint.name());
	}

	private static void setColorConfig(ConfigManager configManager, String keyName, Color color)
	{
		configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, keyName, Integer.toString(color.getRGB()));
	}

	private boolean textShadowEnabled()
	{
		PlayerExamineConfig.TextShadowMode mode = config.textShadowMode();
		if (mode == PlayerExamineConfig.TextShadowMode.On)
		{
			return true;
		}
		if (mode == PlayerExamineConfig.TextShadowMode.Off)
		{
			return false;
		}

		Color background = overlayBackgroundColor();
		if (background == null)
		{
			return true;
		}

		int luminance = (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) / 1000;
		return luminance < 140;
	}

	private void drawShadowText(Graphics2D graphics, String text, int x, int y, Color color)
	{
		if (textShadowEnabled())
		{
			graphics.setColor(applyTextTransparency(new Color(16, 12, 8)));
			graphics.drawString(text, x + 1, y + 1);
		}
		graphics.setColor(applyTextTransparency(color));
		graphics.drawString(text, x, y);
	}

	private void drawCenteredShadowText(Graphics2D graphics, String text, Rectangle bounds, Color color)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int x = bounds.x + (bounds.width - metrics.stringWidth(text)) / 2;
		int y = bounds.y + ((bounds.height - metrics.getHeight()) / 2) + metrics.getAscent();
		drawShadowText(graphics, text, x, y, color);
	}

	private void drawCloseButtonIcon(Graphics2D graphics, Rectangle bounds, Color color)
	{
		int inset = 3;
		int left = bounds.x + inset;
		int top = bounds.y + inset;
		int right = bounds.x + bounds.width - 1 - inset;
		int bottom = bounds.y + bounds.height - 1 - inset;
		Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(1f));
		graphics.setColor(applyTextTransparency(color));
		graphics.drawLine(left, top, right, bottom);
		graphics.drawLine(right, top, left, bottom);
		graphics.setStroke(originalStroke);
	}

	private Color applyOverlayTransparency(Color color)
	{
		return applyOverlayTransparency(color, 1.0f);
	}

	private Color applyOverlayTransparency(Color color, float transparencyScale)
	{
		if (color == null)
		{
			return null;
		}

		int extraTransparency = Math.round(config.overlayTransparency() * transparencyScale);
		if (extraTransparency <= 0)
		{
			return color;
		}

		int alpha = color.getAlpha();
		int adjustedAlpha = (alpha * Math.max(0, 100 - extraTransparency)) / 100;
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), adjustedAlpha);
	}

	private Color applyTextTransparency(Color color)
	{
		if (color == null)
		{
			return null;
		}

		int extraTransparency = config.overlayTextTransparency();
		if (extraTransparency <= 0)
		{
			return color;
		}

		int alpha = color.getAlpha();
		int adjustedAlpha = (alpha * Math.max(0, 100 - extraTransparency)) / 100;
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), adjustedAlpha);
	}

	private Color applyTooltipTransparency(Color color)
	{
		if (color == null)
		{
			return null;
		}

		int extraTransparency = config.tooltipTransparency();
		if (extraTransparency <= 0)
		{
			return color;
		}

		int alpha = color.getAlpha();
		int adjustedAlpha = (alpha * Math.max(0, 100 - extraTransparency)) / 100;
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), adjustedAlpha);
	}

	private Color applyTooltipTextTransparency(Color color)
	{
		if (color == null)
		{
			return null;
		}

		int extraTransparency = config.tooltipTextTransparency();
		if (extraTransparency <= 0)
		{
			return color;
		}

		int alpha = color.getAlpha();
		int adjustedAlpha = (alpha * Math.max(0, 100 - extraTransparency)) / 100;
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), adjustedAlpha);
	}

	private static final class ThemedTooltipComponent implements LayoutableRenderableEntity
	{
		private static final int PADDING = 4;
		private static final String COLOR_TAG_PREFIX = "col=";
		private final String text;
		private final Color backgroundColor;
		private final Color borderColor;
		private java.awt.Point position = new java.awt.Point();

		private ThemedTooltipComponent(String text, Color backgroundColor, Color borderColor)
		{
			this.text = text == null ? "" : text;
			this.backgroundColor = backgroundColor;
			this.borderColor = borderColor;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			String[] lines = text.split("(?i)</?br>");
			int width = 0;
			for (String line : lines)
			{
				width = Math.max(width, measureTaggedText(metrics, line));
			}

			int height = metrics.getHeight() * lines.length;
			Rectangle bounds = new Rectangle(position.x, position.y, width + (PADDING * 2), height + (PADDING * 2));
			graphics.setColor(backgroundColor);
			graphics.fill(bounds);
			graphics.setColor(borderColor);
			graphics.draw(bounds);

			int y = position.y + PADDING + metrics.getAscent();
			for (String line : lines)
			{
				drawTaggedText(graphics, metrics, line, position.x + PADDING, y);
				y += metrics.getHeight();
			}

			return bounds.getSize();
		}

		@Override
		public Rectangle getBounds()
		{
			return null;
		}

		@Override
		public void setPreferredLocation(java.awt.Point position)
		{
			this.position = position == null ? new java.awt.Point() : position;
		}

		@Override
		public void setPreferredSize(Dimension dimension)
		{
		}

		private static int measureTaggedText(FontMetrics metrics, String line)
		{
			int width = 0;
			int index = 0;
			while (index < line.length())
			{
				int tagStart = line.indexOf('<', index);
				if (tagStart < 0)
				{
					return width + metrics.stringWidth(line.substring(index));
				}

				width += metrics.stringWidth(line.substring(index, tagStart));
				int tagEnd = line.indexOf('>', tagStart);
				if (tagEnd < 0)
				{
					return width + metrics.stringWidth(line.substring(tagStart));
				}
				index = tagEnd + 1;
			}

			return width;
		}

		private static void drawTaggedText(Graphics2D graphics, FontMetrics metrics, String line, int x, int y)
		{
			Color color = Color.WHITE;
			int drawX = x;
			int index = 0;
			while (index < line.length())
			{
				int tagStart = line.indexOf('<', index);
				if (tagStart < 0)
				{
					graphics.setColor(color);
					graphics.drawString(line.substring(index), drawX, y);
					return;
				}

				String textPart = line.substring(index, tagStart);
				graphics.setColor(color);
				graphics.drawString(textPart, drawX, y);
				drawX += metrics.stringWidth(textPart);

				int tagEnd = line.indexOf('>', tagStart);
				if (tagEnd < 0)
				{
					graphics.drawString(line.substring(tagStart), drawX, y);
					return;
				}

				String tag = line.substring(tagStart + 1, tagEnd);
				if (tag.startsWith(COLOR_TAG_PREFIX))
				{
					color = Color.decode("#" + tag.substring(COLOR_TAG_PREFIX.length()));
				}
				else if ("/col".equals(tag))
				{
					color = Color.WHITE;
				}
				index = tagEnd + 1;
			}
		}
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
		private final List<StatCell> statsCells;
		private final PlayerExamineConfig.OverlayMode overlayMode;
		private final TabLayout tabLayout;
		private final OverlayTab selectedTab;
		private final Dimension dimension;

		private RenderState(Rectangle closeButton, List<SlotState> slots, List<ListRow> rows, List<StatCell> statsCells, PlayerExamineConfig.OverlayMode overlayMode, TabLayout tabLayout, OverlayTab selectedTab, Dimension dimension)
		{
			this.closeButton = closeButton;
			this.slots = slots;
			this.rows = rows;
			this.statsCells = statsCells;
			this.overlayMode = overlayMode;
			this.tabLayout = tabLayout;
			this.selectedTab = selectedTab;
			this.dimension = dimension;
		}

		public static RenderState empty()
		{
			return new RenderState(new Rectangle(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), PlayerExamineConfig.OverlayMode.Item, new TabLayout(new Rectangle(), new Rectangle()), OverlayTab.EQUIPMENT, new Dimension(0, 0));
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

		public List<StatCell> getStatsCells()
		{
			return statsCells;
		}

		public OverlayTab getSelectedTab()
		{
			return selectedTab;
		}

		public OverlayTab getTabAt(int localX, int localY)
		{
			return tabLayout != null ? tabLayout.getTabAt(localX, localY) : null;
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

		public StatCell getStatAt(int localX, int localY)
		{
			for (StatCell statCell : statsCells)
			{
				if (statCell.getBounds().contains(localX, localY))
				{
					return statCell;
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
