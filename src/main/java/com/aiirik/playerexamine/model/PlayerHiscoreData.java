package com.aiirik.playerexamine.model;

import java.util.EnumMap;
import java.util.Map;

public final class PlayerHiscoreData
{
	public enum Skill
	{
		OVERALL("Total level", "Total level", net.runelite.api.Skill.OVERALL),
		ATTACK("Atk", "Attack", net.runelite.api.Skill.ATTACK),
		DEFENCE("Def", "Defence", net.runelite.api.Skill.DEFENCE),
		STRENGTH("Str", "Strength", net.runelite.api.Skill.STRENGTH),
		HITPOINTS("Hp", "Hitpoints", net.runelite.api.Skill.HITPOINTS),
		RANGED("Rng", "Ranged", net.runelite.api.Skill.RANGED),
		PRAYER("Pray", "Prayer", net.runelite.api.Skill.PRAYER),
		MAGIC("Mage", "Magic", net.runelite.api.Skill.MAGIC),
		COOKING("Cook", "Cooking", net.runelite.api.Skill.COOKING),
		WOODCUTTING("WC", "Woodcutting", net.runelite.api.Skill.WOODCUTTING),
		FLETCHING("Fletch", "Fletching", net.runelite.api.Skill.FLETCHING),
		FISHING("Fish", "Fishing", net.runelite.api.Skill.FISHING),
		FIREMAKING("FM", "Firemaking", net.runelite.api.Skill.FIREMAKING),
		CRAFTING("Craft", "Crafting", net.runelite.api.Skill.CRAFTING),
		SMITHING("Smith", "Smithing", net.runelite.api.Skill.SMITHING),
		MINING("Mine", "Mining", net.runelite.api.Skill.MINING),
		HERBLORE("Herb", "Herblore", net.runelite.api.Skill.HERBLORE),
		AGILITY("Agi", "Agility", net.runelite.api.Skill.AGILITY),
		THIEVING("Thiev", "Thieving", net.runelite.api.Skill.THIEVING),
		SLAYER("Slay", "Slayer", net.runelite.api.Skill.SLAYER),
		FARMING("Farm", "Farming", net.runelite.api.Skill.FARMING),
		RUNECRAFT("RC", "Runecraft", net.runelite.api.Skill.RUNECRAFT),
		HUNTER("Hunt", "Hunter", net.runelite.api.Skill.HUNTER),
		CONSTRUCTION("Con", "Construction", net.runelite.api.Skill.CONSTRUCTION),
		SAILING("Sail", "Sailing", net.runelite.api.Skill.SAILING);

		private final String label;
		private final String fullName;
		private final net.runelite.api.Skill apiSkill;

		Skill(String label, String fullName, net.runelite.api.Skill apiSkill)
		{
			this.label = label;
			this.fullName = fullName;
			this.apiSkill = apiSkill;
		}

		public String getLabel()
		{
			return label;
		}

		public String getFullName()
		{
			return fullName;
		}

		public net.runelite.api.Skill getApiSkill()
		{
			return apiSkill;
		}
	}

	private static final Skill[] ORDER = {
		Skill.OVERALL,
		Skill.ATTACK,
		Skill.DEFENCE,
		Skill.STRENGTH,
		Skill.HITPOINTS,
		Skill.RANGED,
		Skill.PRAYER,
		Skill.MAGIC,
		Skill.COOKING,
		Skill.WOODCUTTING,
		Skill.FLETCHING,
		Skill.FISHING,
		Skill.FIREMAKING,
		Skill.CRAFTING,
		Skill.SMITHING,
		Skill.MINING,
		Skill.HERBLORE,
		Skill.AGILITY,
		Skill.THIEVING,
		Skill.SLAYER,
		Skill.FARMING,
		Skill.RUNECRAFT,
		Skill.HUNTER,
		Skill.CONSTRUCTION,
		Skill.SAILING
	};

	private static final Skill[] DISPLAY_ORDER = {
		Skill.ATTACK,
		Skill.HITPOINTS,
		Skill.MINING,
		Skill.STRENGTH,
		Skill.AGILITY,
		Skill.SMITHING,
		Skill.DEFENCE,
		Skill.HERBLORE,
		Skill.FISHING,
		Skill.RANGED,
		Skill.THIEVING,
		Skill.COOKING,
		Skill.PRAYER,
		Skill.CRAFTING,
		Skill.FIREMAKING,
		Skill.MAGIC,
		Skill.FLETCHING,
		Skill.WOODCUTTING,
		Skill.RUNECRAFT,
		Skill.SLAYER,
		Skill.FARMING,
		Skill.CONSTRUCTION,
		Skill.HUNTER,
		Skill.SAILING
	};

	private final String source;
	private final EnumMap<Skill, Integer> ranks;
	private final EnumMap<Skill, Integer> levels;
	private final EnumMap<Skill, Long> experiences;

	private PlayerHiscoreData(String source, EnumMap<Skill, Integer> ranks, EnumMap<Skill, Integer> levels, EnumMap<Skill, Long> experiences)
	{
		this.source = source;
		this.ranks = ranks;
		this.levels = levels;
		this.experiences = experiences;
	}

	public static PlayerHiscoreData fromParsedValues(String source, int[] parsedRanks, int[] parsedLevels, long[] parsedExperiences)
	{
		EnumMap<Skill, Integer> ranks = new EnumMap<>(Skill.class);
		EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
		EnumMap<Skill, Long> experiences = new EnumMap<>(Skill.class);
		for (int i = 0; i < ORDER.length; i++)
		{
			int rank = i < parsedRanks.length ? parsedRanks[i] : 0;
			int level = i < parsedLevels.length ? parsedLevels[i] : 0;
			long experience = i < parsedExperiences.length ? parsedExperiences[i] : 0L;
			ranks.put(ORDER[i], Math.max(rank, 0));
			levels.put(ORDER[i], Math.max(level, 0));
			experiences.put(ORDER[i], Math.max(experience, 0L));
		}

		return new PlayerHiscoreData(source, ranks, levels, experiences);
	}

	public String getSource()
	{
		return source;
	}

	public int getLevel(Skill skill)
	{
		Integer level = levels.get(skill);
		return level != null ? level : 0;
	}

	public int getRank(Skill skill)
	{
		Integer rank = ranks.get(skill);
		return rank != null ? rank : 0;
	}

	public long getExperience(Skill skill)
	{
		Long experience = experiences.get(skill);
		return experience != null ? experience : 0L;
	}

	public Map<Skill, Integer> getLevels()
	{
		return levels;
	}

	public Map<Skill, Integer> getRanks()
	{
		return ranks;
	}

	public Map<Skill, Long> getExperiences()
	{
		return experiences;
	}

	public static int skillCount()
	{
		return ORDER.length;
	}

	public static Skill[] orderedSkills()
	{
		return ORDER.clone();
	}

	public static Skill[] displaySkills()
	{
		return DISPLAY_ORDER.clone();
	}
}
