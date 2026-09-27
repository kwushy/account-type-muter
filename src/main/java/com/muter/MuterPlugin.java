package com.muter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Provides;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Account Type Muter",
	description = "Mute players by account type or level",
	tags = {"chat", "mute", "filter", "ironman", "hiscores"}
)
public class MuterPlugin extends Plugin
{
	private static final int MAX_CONCURRENT_LOOKUPS = 6;
	private static final Set<ChatMessageType> PUBLIC = EnumSet.of(
		ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT, ChatMessageType.AUTOTYPER);
	private static final Set<ChatMessageType> CLAN = EnumSet.of(
		ChatMessageType.CLAN_CHAT, ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GIM_CHAT);
	private static final Set<ChatMessageType> FRIENDS_CHAT = EnumSet.of(ChatMessageType.FRIENDSCHAT);
	private static final Set<ChatMessageType> PRIVATE = EnumSet.of(
		ChatMessageType.PRIVATECHAT, ChatMessageType.MODPRIVATECHAT);
	private static final HiscoreSkill[] MEMBERS_SKILLS = {
		HiscoreSkill.AGILITY, HiscoreSkill.HERBLORE, HiscoreSkill.THIEVING, HiscoreSkill.FLETCHING,
		HiscoreSkill.SLAYER, HiscoreSkill.FARMING, HiscoreSkill.CONSTRUCTION, HiscoreSkill.HUNTER
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MuterConfig config;

	@Inject
	private HiscoreClient hiscoreClient;

	private final Cache<String, Profile> profiles = CacheBuilder.newBuilder()
		.maximumSize(2000)
		.expireAfterWrite(30, TimeUnit.MINUTES)
		.build();
	private final AtomicInteger inFlight = new AtomicInteger();

	private static final class Profile
	{
		volatile AccountType type = AccountType.NORMAL;
		volatile Stats stats;
		volatile boolean lookupStarted;
	}

	private static final class Stats
	{
		boolean found;
		int total;
		int combat;
		boolean f2p;
		boolean pure;
		boolean skiller;
	}

	@Provides
	MuterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MuterConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invoke(client::refreshChat);
	}

	@Override
	protected void shutDown()
	{
		profiles.invalidateAll();
		clientThread.invoke(client::refreshChat);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("accounttypemuter".equals(event.getGroup()))
		{
			clientThread.invoke(client::refreshChat);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (PUBLIC.contains(event.getType()) || CLAN.contains(event.getType())
			|| FRIENDS_CHAT.contains(event.getType()) || PRIVATE.contains(event.getType()))
		{
			profileFor(event.getName());
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		ChatMessageType type = ChatMessageType.of(intStack[intStackSize - 2]);
		if (!inScope(type))
		{
			return;
		}

		MessageNode node = client.getMessages().get(intStack[intStackSize - 1]);
		if (node == null || node.getName() == null)
		{
			return;
		}

		if (shouldMute(node.getName()))
		{
			intStack[intStackSize - 3] = 0;
		}
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!config.muteOverhead() || !(event.getActor() instanceof Player) || event.getActor().getName() == null)
		{
			return;
		}

		if (shouldMute(event.getActor().getName()))
		{
			event.getActor().setOverheadText(" ");
		}
	}

	private boolean inScope(ChatMessageType type)
	{
		return (config.mutePublic() && PUBLIC.contains(type))
			|| (!config.neverMuteClan() && CLAN.contains(type))
			|| (!config.neverMuteFriendsChat() && FRIENDS_CHAT.contains(type))
			|| (config.mutePrivate() && PRIVATE.contains(type));
	}

	private static String key(String rawName)
	{
		return Text.toJagexName(Text.removeTags(rawName)).toLowerCase();
	}

	private Profile profileFor(String rawName)
	{
		String key = key(rawName);
		Profile profile = profiles.getIfPresent(key);
		if (profile == null)
		{
			profile = new Profile();
			profiles.put(key, profile);
		}
		AccountType icon = AccountType.fromRawName(rawName);
		if (icon != AccountType.NORMAL)
		{
			profile.type = icon;
		}
		return profile;
	}

	private boolean shouldMute(String rawName)
	{
		Player local = client.getLocalPlayer();
		String key = key(rawName);
		if (local != null && local.getName() != null && key.equals(key(local.getName())))
		{
			return false;
		}

		if (config.neverMuteFriends() && client.isFriended(Text.sanitize(rawName), false))
		{
			return false;
		}

		Profile profile = profileFor(rawName);
		if (typeMuted(profile.type))
		{
			return true;
		}

		if (!needsStats(profile.type))
		{
			return false;
		}

		Stats stats = profile.stats;
		if (stats == null)
		{
			startLookup(key, profile);
			return false;
		}

		if ((config.mutePures() && stats.pure) || (config.muteSkillers() && stats.skiller)
			|| (config.muteF2p() && stats.f2p))
		{
			return true;
		}
		if (stats.found)
		{
			if (config.minTotalLevel() > 0 && stats.total < config.minTotalLevel())
			{
				return true;
			}
			if (config.minCombatLevel() > 3 && stats.combat < config.minCombatLevel())
			{
				return true;
			}
		}
		return config.muteMains() && profile.type == AccountType.NORMAL
			&& !stats.pure && !stats.skiller && !stats.f2p;
	}

	private boolean typeMuted(AccountType type)
	{
		switch (type)
		{
			case IRONMAN:
				return config.muteIronman();
			case HARDCORE_IRONMAN:
				return config.muteHardcore();
			case ULTIMATE_IRONMAN:
				return config.muteUim();
			case GIM_GREEN:
				return config.muteGimGreen();
			case GIM_BLUE:
				return config.muteGimBlue();
			case GIM_HARDCORE:
				return config.muteGimHardcore();
			default:
				return false;
		}
	}

	private boolean needsStats(AccountType type)
	{
		boolean statRules = config.muteF2p() || config.minTotalLevel() > 0 || config.minCombatLevel() > 3;
		if (type != AccountType.NORMAL)
		{
			return statRules;
		}
		return statRules || config.muteMains() || config.mutePures() || config.muteSkillers();
	}

	private void startLookup(String name, Profile profile)
	{
		if (profile.lookupStarted || inFlight.get() >= MAX_CONCURRENT_LOOKUPS)
		{
			return;
		}
		profile.lookupStarted = true;
		inFlight.incrementAndGet();

		AccountType type = profile.type;
		boolean categories = type == AccountType.NORMAL
			&& (config.muteMains() || config.mutePures() || config.muteSkillers());

		CompletableFuture<HiscoreResult> main = lookup(name, type.endpoint, true);
		CompletableFuture<HiscoreResult> pure = lookup(name, HiscoreEndpoint.PURE, categories);
		CompletableFuture<HiscoreResult> skiller = lookup(name, HiscoreEndpoint.LEVEL_3_SKILLER, categories);

		CompletableFuture.allOf(main, pure, skiller).thenRun(() ->
		{
			try
			{
				profile.stats = buildStats(main.join(), pure.join(), skiller.join());
			}
			finally
			{
				inFlight.decrementAndGet();
			}
			clientThread.invoke(client::refreshChat);
		});
	}

	private CompletableFuture<HiscoreResult> lookup(String name, HiscoreEndpoint endpoint, boolean enabled)
	{
		if (!enabled)
		{
			return CompletableFuture.completedFuture(null);
		}
		return hiscoreClient.lookupAsync(name, endpoint).exceptionally(e ->
		{
			log.debug("Hiscore lookup failed for {} on {}", name, endpoint, e);
			return null;
		});
	}

	private static Stats buildStats(HiscoreResult main, HiscoreResult pure, HiscoreResult skiller)
	{
		Stats stats = new Stats();
		stats.pure = pure != null;
		stats.skiller = skiller != null;
		if (main == null)
		{
			return stats;
		}

		stats.total = level(main, HiscoreSkill.OVERALL);
		stats.found = stats.total > 0;
		stats.combat = Experience.getCombatLevel(
			level(main, HiscoreSkill.ATTACK), level(main, HiscoreSkill.STRENGTH),
			level(main, HiscoreSkill.DEFENCE), Math.max(10, level(main, HiscoreSkill.HITPOINTS)),
			level(main, HiscoreSkill.MAGIC), level(main, HiscoreSkill.RANGED),
			level(main, HiscoreSkill.PRAYER));

		boolean f2p = stats.found;
		for (HiscoreSkill skill : MEMBERS_SKILLS)
		{
			f2p &= level(main, skill) <= 1;
		}
		stats.f2p = f2p;
		return stats;
	}

	private static int level(HiscoreResult result, HiscoreSkill skill)
	{
		Skill s = result.getSkill(skill);
		return s == null ? 0 : Math.max(0, s.getLevel());
	}
}
