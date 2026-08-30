package com.aiirik.playerexamine;

import com.aiirik.playerexamine.model.PlayerExamineData;
import com.aiirik.playerexamine.model.PlayerHiscoreData;
import com.aiirik.playerexamine.overlay.PlayerExamineOverlay;
import com.aiirik.playerexamine.ui.PlayerExamineThemePanel;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Provider;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.LinkBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Player Examine"
)
public class PlayerExaminePlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(PlayerExaminePlugin.class);
	private static final String EXAMINE_OPTION = "Examine";
	private static final String CUSTOM_THEMES_KEY = "customColorThemes";
	private static final String ACTIVE_SIDE_PANEL_THEME_KEY = "activeSidePanelTheme";
	private static final Duration HISCORE_CACHE_TTL = Duration.ofMinutes(10);
	private static final String[] HISCORE_BASES = {
		"https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws",
		"https://secure.runescape.com/m=hiscore_oldschool_ironman/index_lite.ws",
		"https://secure.runescape.com/m=hiscore_oldschool_hardcore_ironman/index_lite.ws",
		"https://secure.runescape.com/m=hiscore_oldschool_ultimate_ironman/index_lite.ws"
	};

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private PlayerExamineConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private PlayerExamineOverlay overlay;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	private volatile PlayerExamineData currentData;
	private volatile PlayerHiscoreData currentHiscoreData;
	private volatile HiscoreLookupState hiscoreLookupState = HiscoreLookupState.IDLE;
	private volatile String currentHiscoreName;
	private boolean applyingNamedTheme;
	private PlayerExamineThemePanel themePanel;
	private NavigationButton navigationButton;
	private final AtomicLong hiscoreLookupRequestId = new AtomicLong();
	private final Map<String, CachedHiscoreData> hiscoreCache = new ConcurrentHashMap<>();
	private final MouseAdapter mouseAdapter = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent mouseEvent)
		{
			return handleOverlayMouseEvent(mouseEvent);
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent mouseEvent)
		{
			return mouseEvent;
		}

		@Override
		public MouseEvent mouseClicked(MouseEvent mouseEvent)
		{
			return mouseEvent;
		}
	};

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		menuManager.get().addPlayerMenuItem(EXAMINE_OPTION);
		mouseManager.registerMouseListener(mouseAdapter);
		updateThemePanelNavigation();
		overlay.setSelectedTab(PlayerExamineOverlay.OverlayTab.EQUIPMENT);
		log.debug("Player Examine started");
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
			themePanel = null;
		}
		mouseManager.unregisterMouseListener(mouseAdapter);
		overlayManager.remove(overlay);
		menuManager.get().removePlayerMenuItem(EXAMINE_OPTION);
		clearCurrentData();
		log.debug("Player Examine stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			PlayerExamineUpdateNotice.announceIfNeeded(
				configManager,
				chatMessageManager,
				config,
				config.disableUpdateNotifications());
		}
		else if (shouldClearCurrentData(event.getGameState()))
		{
			setCurrentData(null);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PlayerExamineConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("enableColorSharingPanel".equals(event.getKey()))
		{
			updateThemePanelNavigation();
			return;
		}

		if ("themePreset".equals(event.getKey()))
		{
			if (!applyingNamedTheme && !"Custom".equals(event.getNewValue()))
			{
				clearActiveSidePanelTheme();
			}

			overlay.saveCustomPresetColor(configManager, event.getKey());
			return;
		}

		if (!"customColorStartingPoint".equals(event.getKey()))
		{
			if (applyingNamedTheme)
			{
				return;
			}

			if (!applyingNamedTheme
				&& isThemeColorConfigKey(event.getKey())
				&& config.customColorStartingPoint() == PlayerExamineConfig.CustomColorStartingPoint.SidePanelTheme)
			{
				clearActiveSidePanelTheme();
			}
			overlay.saveCustomPresetColor(configManager, event.getKey());
			return;
		}

		if (event.getNewValue() == null)
		{
			return;
		}

		PlayerExamineConfig.CustomColorStartingPoint startingPoint;
		try
		{
			startingPoint = PlayerExamineConfig.CustomColorStartingPoint.valueOf(event.getNewValue());
		}
		catch (IllegalArgumentException ex)
		{
			return;
		}

		overlay.copyThemeColorsToCustom(configManager, startingPoint);
		if (startingPoint != PlayerExamineConfig.CustomColorStartingPoint.SidePanelTheme)
		{
			clearActiveSidePanelTheme();
		}
		eventBus.post(new ProfileChanged());
	}

	private void updateThemePanelNavigation()
	{
		if (!config.enableColorSharingPanel())
		{
			if (navigationButton != null)
			{
				clientToolbar.removeNavigation(navigationButton);
				navigationButton = null;
				themePanel = null;
			}
			return;
		}

		if (navigationButton != null)
		{
			return;
		}

		themePanel = new PlayerExamineThemePanel(this);
		navigationButton = NavigationButton.builder()
			.tooltip("Player Examine")
			.icon(createNavigationIcon())
			.panel(themePanel)
			.priority(6)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	public Map<String, String> getNamedColorThemes()
	{
		Map<String, String> themes = new LinkedHashMap<>();
		String json = configManager.getConfiguration(PlayerExamineConfig.CONFIG_GROUP, CUSTOM_THEMES_KEY);
		if (json == null || json.isEmpty())
		{
			return themes;
		}

		try
		{
			JsonObject root = new JsonParser().parse(json).getAsJsonObject();
			for (String name : root.keySet())
			{
				JsonElement value = root.get(name);
				if (value != null && !value.isJsonNull())
				{
					themes.put(name, value.getAsString());
				}
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Unable to read Player Examine custom themes", ex);
		}

		return themes;
	}

	public String getActiveSidePanelTheme()
	{
		if (config.themePreset() != PlayerExamineConfig.ThemePreset.Custom
			|| config.customColorStartingPoint() != PlayerExamineConfig.CustomColorStartingPoint.SidePanelTheme)
		{
			return null;
		}

		return configManager.getConfiguration(PlayerExamineConfig.CONFIG_GROUP, ACTIVE_SIDE_PANEL_THEME_KEY);
	}

	public String createThemeFromCurrentColors(String name)
	{
		String normalizedName = normalizeThemeName(name);
		String themeJson = overlay.exportCurrentColorTheme(normalizedName, gson);
		saveNamedColorTheme(normalizedName, themeJson);
		queueColorSharingMessage("Saved Player Examine theme: " + normalizedName);
		return themeJson;
	}

	public String importNamedColorTheme(String name, String json)
	{
		String normalizedName = normalizeThemeName(name);
		Map<String, String> colors = PlayerExamineColorSettings.importFromJson(json);
		String themeJson = PlayerExamineColorSettings.exportToJson(normalizedName, colors, gson);
		saveNamedColorTheme(normalizedName, themeJson);
		queueColorSharingMessage("Imported Player Examine theme: " + normalizedName);
		return themeJson;
	}

	public void updateNamedColorThemeFromCurrent(String name)
	{
		String normalizedName = normalizeThemeName(name);
		saveNamedColorTheme(normalizedName, overlay.exportCurrentColorTheme(normalizedName, gson));
		queueColorSharingMessage("Updated Player Examine theme: " + normalizedName);
	}

	public int applyNamedColorTheme(String name)
	{
		String themeJson = getNamedColorThemes().get(name);
		if (themeJson == null)
		{
			throw new IllegalArgumentException("Unknown Player Examine theme: " + name);
		}

		int imported;
		applyingNamedTheme = true;
		try
		{
			imported = overlay.applyColorTheme(configManager, themeJson);
			configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, ACTIVE_SIDE_PANEL_THEME_KEY, name);
			eventBus.post(new ProfileChanged());
		}
		finally
		{
			applyingNamedTheme = false;
		}
		if (themePanel != null)
		{
			themePanel.rebuild();
		}
		queueColorSharingMessage("Applied Player Examine theme: " + name);
		return imported;
	}

	public String exportNamedColorTheme(String name)
	{
		String themeJson = getNamedColorThemes().get(name);
		if (themeJson == null)
		{
			throw new IllegalArgumentException("Unknown Player Examine theme: " + name);
		}

		return themeJson;
	}

	public void copyNamedColorThemeToClipboard(String name)
	{
		String themeJson = exportNamedColorTheme(name);
		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(new StringSelection(themeJson), null);
		queueColorSharingMessage("Copied Player Examine theme: " + name);
	}

	public void deleteNamedColorTheme(String name)
	{
		Map<String, String> themes = getNamedColorThemes();
		if (themes.remove(name) != null)
		{
			saveNamedColorThemes(themes);
			if (name.equals(getActiveSidePanelTheme()))
			{
				clearActiveSidePanelTheme();
			}
			queueColorSharingMessage("Deleted Player Examine theme: " + name);
		}
	}

	private void clearActiveSidePanelTheme()
	{
		configManager.unsetConfiguration(PlayerExamineConfig.CONFIG_GROUP, ACTIVE_SIDE_PANEL_THEME_KEY);
		if (themePanel != null)
		{
			themePanel.rebuild();
		}
	}

	private void saveNamedColorTheme(String name, String themeJson)
	{
		Map<String, String> themes = getNamedColorThemes();
		themes.put(name, themeJson);
		saveNamedColorThemes(themes);
		if (themePanel != null)
		{
			themePanel.rebuild();
		}
	}

	private void saveNamedColorThemes(Map<String, String> themes)
	{
		JsonObject root = new JsonObject();
		for (Map.Entry<String, String> entry : themes.entrySet())
		{
			root.addProperty(entry.getKey(), entry.getValue());
		}

		configManager.setConfiguration(PlayerExamineConfig.CONFIG_GROUP, CUSTOM_THEMES_KEY, gson.toJson(root));
	}

	private static String normalizeThemeName(String name)
	{
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isEmpty())
		{
			throw new IllegalArgumentException("Theme name is required.");
		}
		if (normalizedName.length() > 40)
		{
			throw new IllegalArgumentException("Theme name must be 40 characters or less.");
		}

		return normalizedName;
	}

	private static boolean isThemeColorConfigKey(String key)
	{
		return PlayerExamineColorSettings.isThemeColorKey(key);
	}

	private void queueColorSharingMessage(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(ColorUtil.wrapWithColorTag(message, config.notificationTextColor()))
			.build());
	}

	private static BufferedImage createNavigationIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.setColor(new Color(31, 24, 17, 245));
		graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
		graphics.setColor(new Color(118, 94, 60, 255));
		graphics.drawRoundRect(1, 1, 13, 13, 4, 4);

		graphics.setColor(new Color(235, 226, 193));
		graphics.fillOval(5, 3, 5, 5);
		graphics.setColor(new Color(215, 125, 40));
		graphics.fillRoundRect(4, 8, 7, 5, 2, 2);

		graphics.setColor(new Color(245, 240, 228));
		graphics.drawOval(2, 2, 7, 7);
		graphics.drawLine(8, 8, 12, 12);
		graphics.setColor(new Color(31, 24, 17));
		graphics.drawOval(4, 4, 3, 3);

		graphics.dispose();
		return image;
	}

	private static boolean shouldClearCurrentData(GameState gameState)
	{
		return gameState == GameState.LOGIN_SCREEN
			|| gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR
			|| gameState == GameState.LOGGING_IN
			|| gameState == GameState.CONNECTION_LOST
			|| gameState == GameState.HOPPING;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE_PLAYER
			|| !EXAMINE_OPTION.equalsIgnoreCase(event.getMenuOption()))
		{
			return;
		}

		Player examinedPlayer = event.getMenuEntry() != null ? event.getMenuEntry().getPlayer() : null;
		if (examinedPlayer == null)
		{
			examinedPlayer = resolveExaminedPlayer(event.getMenuTarget()).orElse(null);
		}
		if (examinedPlayer == null)
		{
			return;
		}

		PlayerExamineData data = PlayerExamineData.from(examinedPlayer, client);
		setCurrentData(data);
		startHiscoreLookup(data.getName());
	}

	@Provides
	PlayerExamineConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerExamineConfig.class);
	}

	public PlayerExamineData getCurrentData()
	{
		return currentData;
	}

	public PlayerExamineConfig getConfig()
	{
		return config;
	}

	public PlayerHiscoreData getCurrentHiscoreData()
	{
		return currentHiscoreData;
	}

	public HiscoreLookupState getHiscoreLookupState()
	{
		return hiscoreLookupState;
	}

	private void setCurrentData(PlayerExamineData data)
	{
		hiscoreLookupRequestId.incrementAndGet();
		currentData = data;
		currentHiscoreData = null;
		hiscoreLookupState = data == null ? HiscoreLookupState.IDLE : HiscoreLookupState.LOADING;
		currentHiscoreName = data != null ? normalizeName(data.getName()) : null;
		if (data != null && config.defaultTab() != PlayerExamineConfig.DefaultTab.RememberLast)
		{
			overlay.setSelectedTab(getDefaultTab());
		}
		if (data != null)
		{
			overlay.markOpened();
		}
	}

	private PlayerExamineOverlay.OverlayTab getDefaultTab()
	{
		return config.defaultTab() == PlayerExamineConfig.DefaultTab.Stats
			? PlayerExamineOverlay.OverlayTab.STATS
			: PlayerExamineOverlay.OverlayTab.EQUIPMENT;
	}

	private void clearCurrentData()
	{
		setCurrentData(null);
		currentHiscoreName = null;
	}

	private void startHiscoreLookup(String name)
	{
		String normalizedName = normalizeName(name);
		currentHiscoreName = normalizedName;

		CachedHiscoreData cached = hiscoreCache.get(normalizedName);
		if (cached != null && !cached.isExpired())
		{
			currentHiscoreData = cached.getData();
			hiscoreLookupState = HiscoreLookupState.READY;
			return;
		}

		currentHiscoreData = null;
		hiscoreLookupState = HiscoreLookupState.LOADING;
		long requestId = hiscoreLookupRequestId.incrementAndGet();
		lookupHiscoreEndpoint(normalizedName, requestId, 0);
	}

	private void lookupHiscoreEndpoint(String normalizedName, long requestId, int endpointIndex)
	{
		if (requestId != hiscoreLookupRequestId.get())
		{
			return;
		}

		if (endpointIndex >= HISCORE_BASES.length)
		{
			clientThread.invoke(() ->
			{
				if (requestId == hiscoreLookupRequestId.get() && normalizedName.equals(currentHiscoreName))
				{
					hiscoreLookupState = HiscoreLookupState.UNAVAILABLE;
				}
			});
			return;
		}

		HttpUrl url = HttpUrl.parse(HISCORE_BASES[endpointIndex]).newBuilder()
			.addQueryParameter("player", normalizedName)
			.build();
		Request request = new Request.Builder()
			.url(url)
			.get()
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				lookupHiscoreEndpoint(normalizedName, requestId, endpointIndex + 1);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response ignored = response)
				{
					if (!response.isSuccessful() || response.body() == null)
					{
						lookupHiscoreEndpoint(normalizedName, requestId, endpointIndex + 1);
						return;
					}

					String body = response.body().string().trim();
					PlayerHiscoreData parsed = parseHiscoreBody(HISCORE_BASES[endpointIndex], body);
					if (parsed == null)
					{
						lookupHiscoreEndpoint(normalizedName, requestId, endpointIndex + 1);
						return;
					}

					clientThread.invoke(() ->
					{
						if (requestId != hiscoreLookupRequestId.get() || !normalizedName.equals(currentHiscoreName))
						{
							return;
						}

						currentHiscoreData = parsed;
						hiscoreLookupState = HiscoreLookupState.READY;
						hiscoreCache.put(normalizedName, new CachedHiscoreData(parsed));
					});
				}
			}
		});
	}

	private PlayerHiscoreData parseHiscoreBody(String source, String body)
	{
		if (body == null || body.isEmpty())
		{
			return null;
		}

		String[] lines = body.split("\\R");
		if (lines.length < PlayerHiscoreData.skillCount())
		{
			return null;
		}

		int[] ranks = new int[PlayerHiscoreData.skillCount()];
		int[] levels = new int[PlayerHiscoreData.skillCount()];
		long[] experiences = new long[PlayerHiscoreData.skillCount()];
		for (int i = 0; i < levels.length; i++)
		{
			String line = lines[i].trim();
			String[] fields = line.split(",");
			if (fields.length < 3)
			{
				return null;
			}

			ranks[i] = parseInt(fields[0]);
			levels[i] = parseInt(fields[1]);
			experiences[i] = parseLong(fields[2]);
		}

		return PlayerHiscoreData.fromParsedValues(source, ranks, levels, experiences);
	}

	private static int parseInt(String text)
	{
		try
		{
			return Integer.parseInt(text.trim());
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private static long parseLong(String text)
	{
		try
		{
			return Long.parseLong(text.trim());
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private static String normalizeName(String name)
	{
		return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private MouseEvent handleOverlayMouseEvent(MouseEvent mouseEvent)
	{
		PlayerExamineData data = currentData;
		if (data == null || mouseEvent == null)
		{
			return mouseEvent;
		}

		java.awt.Rectangle overlayBounds = overlay.getBounds();
		PlayerExamineOverlay.RenderState renderState = overlay.getRenderState();
		if (overlayBounds == null || renderState == null || renderState.isEmpty() || !overlayBounds.contains(mouseEvent.getPoint()))
		{
			return mouseEvent;
		}

		if (mouseEvent.getButton() != MouseEvent.BUTTON1)
		{
			return mouseEvent;
		}

		int localX = mouseEvent.getX() - overlayBounds.x;
		int localY = mouseEvent.getY() - overlayBounds.y;

		PlayerExamineOverlay.RenderState overlayState = overlay.getRenderState();
		PlayerExamineOverlay.OverlayTab tab = overlayState.getTabAt(localX, localY);
		if (tab != null)
		{
			mouseEvent.consume();
			overlay.setSelectedTab(tab);
			return mouseEvent;
		}

		if (overlayState.getCloseButton().contains(localX, localY))
		{
			mouseEvent.consume();
			clearCurrentData();
			return mouseEvent;
		}

		if (config.openWikiOnItemClick() && overlayState.getSelectedTab() == PlayerExamineOverlay.OverlayTab.EQUIPMENT)
		{
			PlayerExamineOverlay.SlotState slot = overlayState.getSlotAt(localX, localY);
			if (slot != null && slot.getEntry() != null && slot.getEntry().hasItem())
			{
				mouseEvent.consume();
				LinkBrowser.browse(overlay.buildWikiUrl(slot.getEntry()));
			}
		}

		return mouseEvent;
	}

	private Optional<Player> resolveExaminedPlayer(String target)
	{
		if (target == null || client.getLocalPlayer() == null)
		{
			return Optional.empty();
		}

		String cleanTarget = stripTarget(target);
		List<Player> players = client.getPlayers();
		for (Player player : players)
		{
			if (player == null || player == client.getLocalPlayer() || player.getName() == null)
			{
				continue;
			}

			String name = player.getName();
			if (cleanTarget.equalsIgnoreCase(name) || cleanTarget.startsWith(name + " ") || cleanTarget.startsWith(name + "("))
			{
				return Optional.of(player);
			}
		}

		return Optional.empty();
	}

	private static String stripTarget(String target)
	{
		String stripped = target.replaceAll("<[^>]+>", "").trim();
		int levelSeparator = stripped.indexOf(" (");
		if (levelSeparator > 0)
		{
			stripped = stripped.substring(0, levelSeparator).trim();
		}
		return stripped;
	}

	public enum HiscoreLookupState
	{
		IDLE,
		LOADING,
		READY,
		UNAVAILABLE
	}

	private static final class CachedHiscoreData
	{
		private final PlayerHiscoreData data;
		private final Instant fetchedAt;

		private CachedHiscoreData(PlayerHiscoreData data)
		{
			this.data = data;
			this.fetchedAt = Instant.now();
		}

		private PlayerHiscoreData getData()
		{
			return data;
		}

		private boolean isExpired()
		{
			return Instant.now().isAfter(fetchedAt.plus(HISCORE_CACHE_TTL));
		}
	}
}
