package com.aiirik.playerexamine;

import java.util.Arrays;
import java.util.List;

final class PlayerExamineChangelog
{
	static final String VERSION = "1.1.2";

	private static final List<String> CHANGES = Arrays.asList(
			"Custom theme base edits now stay saved when switching between bases",
			"Resetting a custom theme color now restores the selected base theme's default");

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
