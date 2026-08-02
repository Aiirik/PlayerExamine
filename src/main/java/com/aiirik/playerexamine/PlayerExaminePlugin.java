package com.aiirik.playerexamine;

import com.aiirik.playerexamine.model.PlayerExamineData;
import com.aiirik.playerexamine.overlay.PlayerExamineOverlay;
import com.google.inject.Provides;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.chat.ChatMessageManager;
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

	private volatile PlayerExamineData currentData;
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
		else
		{
			setCurrentData(null);
		}
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

	private void setCurrentData(PlayerExamineData data)
	{
		currentData = data;
	}

	private void clearCurrentData()
	{
		setCurrentData(null);
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

		if (renderState.getCloseButton().contains(localX, localY))
		{
			mouseEvent.consume();
			clearCurrentData();
			return mouseEvent;
		}

		if (config.openWikiOnItemClick())
		{
			PlayerExamineOverlay.SlotState slot = renderState.getSlotAt(localX, localY);
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
}
