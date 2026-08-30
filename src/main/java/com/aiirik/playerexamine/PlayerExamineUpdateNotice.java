package com.aiirik.playerexamine;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ColorUtil;

final class PlayerExamineUpdateNotice
{
	static final String NOTICE_ID = PlayerExamineChangelog.VERSION;
	static final String MESSAGE = PlayerExamineChangelog.getMessage();

	private static final String CONFIG_GROUP = "player-examine";
	private static final String LAST_NOTICE_ID_KEY = "lastUpdateNoticeId";

	private PlayerExamineUpdateNotice()
	{
	}

	static void announceIfNeeded(
		ConfigManager configManager,
		ChatMessageManager chatMessageManager,
		PlayerExamineConfig config,
		boolean disableUpdateNotifications)
	{
		String lastNoticeId = configManager.getConfiguration(CONFIG_GROUP, LAST_NOTICE_ID_KEY);
		if (NOTICE_ID.equals(lastNoticeId))
		{
			return;
		}

		if (!disableUpdateNotifications)
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(ColorUtil.wrapWithColorTag(MESSAGE, config.notificationTextColor()))
				.build());
		}

		configManager.setConfiguration(CONFIG_GROUP, LAST_NOTICE_ID_KEY, NOTICE_ID);
	}
}
