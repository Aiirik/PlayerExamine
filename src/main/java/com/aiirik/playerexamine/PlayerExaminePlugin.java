package com.aiirik.playerexamine;

import com.aiirik.playerexamine.model.PlayerExamineData;
import com.aiirik.playerexamine.model.PlayerHiscoreData;
import com.aiirik.playerexamine.overlay.PlayerExamineOverlay;
import com.google.inject.Provides;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
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
	private ConfigManager configManager;

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

	private volatile PlayerExamineData currentData;
	private volatile PlayerHiscoreData currentHiscoreData;
	private volatile HiscoreLookupState hiscoreLookupState = HiscoreLookupState.IDLE;
	private volatile String currentHiscoreName;
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
		overlay.setSelectedTab(PlayerExamineOverlay.OverlayTab.EQUIPMENT);
		log.debug("Player Examine started");
	}

	@Override
	protected void shutDown()
	{
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
				config.disableUpdateNotifications());
		}
		else if (shouldClearCurrentData(event.getGameState()))
		{
			setCurrentData(null);
		}
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
		overlay.setSelectedTab(PlayerExamineOverlay.OverlayTab.EQUIPMENT);
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
