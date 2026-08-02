package com.aiirik.playerexamine;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PlayerExaminePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PlayerExaminePlugin.class);
		RuneLite.main(args);
	}
}
