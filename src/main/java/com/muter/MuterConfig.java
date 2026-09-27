package com.muter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("accounttypemuter")
public interface MuterConfig extends Config
{
	@ConfigSection(name = "Account types", description = "Mute players of these account types", position = 0)
	String typeSection = "types";

	@ConfigSection(name = "Levels", description = "Mute players below a level threshold", position = 1)
	String levelSection = "levels";

	@ConfigSection(name = "Where to mute", description = "Which chat channels are affected", position = 2)
	String scopeSection = "scope";

	@ConfigItem(keyName = "muteMains", name = "Mute mains", position = 0, section = typeSection,
		description = "Regular accounts: no ironman icon and not on the pure or skiller hiscores. Needs Hiscores lookups.")
	default boolean muteMains() { return false; }

	@ConfigItem(keyName = "muteIronman", name = "Mute ironman", position = 1, section = typeSection,
		description = "Regular ironmen")
	default boolean muteIronman() { return false; }

	@ConfigItem(keyName = "muteHardcore", name = "Mute hardcores", position = 2, section = typeSection,
		description = "Hardcore ironmen")
	default boolean muteHardcore() { return false; }

	@ConfigItem(keyName = "muteGimGreen", name = "Mute GIM (green)", position = 3, section = typeSection,
		description = "Unranked group ironmen (green icon)")
	default boolean muteGimGreen() { return false; }

	@ConfigItem(keyName = "muteGimBlue", name = "Mute GIM (blue)", position = 4, section = typeSection,
		description = "Group ironmen (blue icon)")
	default boolean muteGimBlue() { return false; }

	@ConfigItem(keyName = "muteGimHardcore", name = "Mute GIM (HCGIM)", position = 5, section = typeSection,
		description = "Hardcore group ironmen")
	default boolean muteGimHardcore() { return false; }

	@ConfigItem(keyName = "muteUim", name = "Mute UIM", position = 6, section = typeSection,
		description = "Ultimate ironmen")
	default boolean muteUim() { return false; }

	@ConfigItem(keyName = "mutePures", name = "Mute pures", position = 7, section = typeSection,
		description = "Players listed on the official 1 Defence Pure hiscores. Needs Hiscores lookups.")
	default boolean mutePures() { return false; }

	@ConfigItem(keyName = "muteSkillers", name = "Mute skillers", position = 8, section = typeSection,
		description = "Players listed on the official Level 3 Skiller hiscores. Needs Hiscores lookups.")
	default boolean muteSkillers() { return false; }

	@ConfigItem(keyName = "muteF2p", name = "Mute F2P", position = 9, section = typeSection,
		description = "Best guess: all members-only skills are still level 1 on the Hiscores. Needs Hiscores lookups.")
	default boolean muteF2p() { return false; }

	@Range(min = 0, max = 2376)
	@ConfigItem(keyName = "minTotalLevel", name = "Mute total level under", position = 0, section = levelSection,
		description = "Mute players whose total level is below this. 0 = off. Needs Hiscores lookups.")
	default int minTotalLevel() { return 0; }

	@Range(min = 3, max = 126)
	@ConfigItem(keyName = "minCombatLevel", name = "Mute combat level under", position = 1, section = levelSection,
		description = "Mute players whose combat level is below this. 3 = off. Needs Hiscores lookups.")
	default int minCombatLevel() { return 3; }

	@ConfigItem(keyName = "mutePublic", name = "Public chat", position = 0, section = scopeSection,
		description = "Public chat tab")
	default boolean mutePublic() { return true; }

	@ConfigItem(keyName = "muteOverhead", name = "Overhead text", position = 1, section = scopeSection,
		description = "Speech bubbles above players")
	default boolean muteOverhead() { return true; }

	@ConfigItem(keyName = "neverMuteClan", name = "Don't mute clan chat", position = 2, section = scopeSection,
		description = "Never mute messages in clan, guest clan and GIM clan chat")
	default boolean neverMuteClan() { return true; }

	@ConfigItem(keyName = "neverMuteFriendsChat", name = "Don't mute friends chat", position = 3, section = scopeSection,
		description = "Never mute messages in friends chat channels")
	default boolean neverMuteFriendsChat() { return true; }

	@ConfigItem(keyName = "neverMuteFriends", name = "Don't mute friends", position = 4, section = scopeSection,
		description = "Never mute anyone on your friends list, in any channel")
	default boolean neverMuteFriends() { return true; }

	@ConfigItem(keyName = "mutePrivate", name = "Private messages", position = 5, section = scopeSection,
		description = "Private messages")
	default boolean mutePrivate() { return false; }
}
