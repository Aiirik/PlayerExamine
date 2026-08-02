package com.aiirik.playerexamine;

import java.util.Arrays;
import java.util.List;

final class PlayerExamineChangelog
{
	static final String VERSION = "1.0.0";

	private static final List<String> CHANGES = Arrays.asList(
		"Added a dedicated player examine overlay",
		"Added configurable overlay colors",
		"Added update notices on login",
		"Kept the overlay movable and closer to the RuneScape equipment layout");

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
