package com.aiirik.playerexamine;

import java.util.Arrays;
import java.util.List;

final class PlayerExamineChangelog
{
	static final String VERSION = "1.0.0";

	private static final List<String> CHANGES = Arrays.asList(
		"Player Examine plugin has updated. See the plugin config for new settings!");

	private PlayerExamineChangelog()
	{
	}

	static String getMessage()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Player Examine v").append(VERSION).append(" changelog:");
		for (String change : CHANGES)
		{
			builder.append(" ").append(change).append(".");
		}
		return builder.toString();
	}
}
