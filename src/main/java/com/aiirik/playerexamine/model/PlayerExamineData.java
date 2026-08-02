package com.aiirik.playerexamine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.SkullIcon;
import net.runelite.api.kit.KitType;

public final class PlayerExamineData
{
	private final String name;
	private final int combatLevel;
	private final int team;
	private final int skullIcon;
	private final HeadIcon overheadIcon;
	private final boolean friend;
	private final boolean friendsChatMember;
	private final boolean clanMember;
	private final List<EquipmentEntry> equipment;

	private PlayerExamineData(
		String name,
		int combatLevel,
		int team,
		int skullIcon,
		HeadIcon overheadIcon,
		boolean friend,
		boolean friendsChatMember,
		boolean clanMember,
		List<EquipmentEntry> equipment)
	{
		this.name = name;
		this.combatLevel = combatLevel;
		this.team = team;
		this.skullIcon = skullIcon;
		this.overheadIcon = overheadIcon;
		this.friend = friend;
		this.friendsChatMember = friendsChatMember;
		this.clanMember = clanMember;
		this.equipment = Collections.unmodifiableList(equipment);
	}

	public static PlayerExamineData from(Player player, Client client)
	{
		PlayerComposition composition = player.getPlayerComposition();
		List<EquipmentEntry> equipment = new ArrayList<>();

		if (composition != null)
		{
			int[] equipmentIds = composition.getEquipmentIds();
			KitType[] kitTypes = KitType.values();
			int slotCount = Math.min(equipmentIds.length, kitTypes.length);

			for (int i = 0; i < slotCount; i++)
			{
				int rawEquipmentId = equipmentIds[i];
				if (rawEquipmentId >= PlayerComposition.ITEM_OFFSET)
				{
					int itemId = rawEquipmentId - PlayerComposition.ITEM_OFFSET;
					String itemName = client.getItemDefinition(itemId).getName();
					if (itemName == null || itemName.trim().isEmpty())
					{
						itemName = "Item " + itemId;
					}

					equipment.add(new EquipmentEntry(toSlotName(kitTypes[i]), itemId, itemName, true));
				}
				else
				{
					equipment.add(new EquipmentEntry(toSlotName(kitTypes[i]), -1, null, false));
				}
			}
		}

		return new PlayerExamineData(
			Objects.requireNonNullElse(player.getName(), "Unknown player"),
			player.getCombatLevel(),
			player.getTeam(),
			player.getSkullIcon(),
			player.getOverheadIcon(),
			player.isFriend(),
			player.isFriendsChatMember(),
			player.isClanMember(),
			equipment);
	}

	private static String toSlotName(KitType kitType)
	{
		String lower = kitType.name().toLowerCase(Locale.ROOT);
		StringBuilder builder = new StringBuilder(lower.length());
		boolean capitalizeNext = true;

		for (int i = 0; i < lower.length(); i++)
		{
			char c = lower.charAt(i);
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

	public String getName()
	{
		return name;
	}

	public int getCombatLevel()
	{
		return combatLevel;
	}

	public int getTeam()
	{
		return team;
	}

	public int getSkullIcon()
	{
		return skullIcon;
	}

	public HeadIcon getOverheadIcon()
	{
		return overheadIcon;
	}

	public boolean isFriend()
	{
		return friend;
	}

	public boolean isFriendsChatMember()
	{
		return friendsChatMember;
	}

	public boolean isClanMember()
	{
		return clanMember;
	}

	public List<EquipmentEntry> getEquipment()
	{
		return equipment;
	}

	public boolean hasSkull()
	{
		return skullIcon != SkullIcon.NONE;
	}

	public static final class EquipmentEntry
	{
		private final String slotName;
		private final int itemId;
		private final String itemName;
		private final boolean hasItem;

		private EquipmentEntry(String slotName, int itemId, String itemName, boolean hasItem)
		{
			this.slotName = slotName;
			this.itemId = itemId;
			this.itemName = itemName;
			this.hasItem = hasItem;
		}

		public String getSlotName()
		{
			return slotName;
		}

		public int getItemId()
		{
			return itemId;
		}

		public String getItemName()
		{
			return itemName;
		}

		public boolean hasItem()
		{
			return hasItem;
		}
	}
}
