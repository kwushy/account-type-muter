package com.muter;

import net.runelite.api.IconID;
import net.runelite.client.hiscore.HiscoreEndpoint;

enum AccountType
{
	NORMAL(null, HiscoreEndpoint.NORMAL),
	IRONMAN(IconID.IRONMAN, HiscoreEndpoint.IRONMAN),
	HARDCORE_IRONMAN(IconID.HARDCORE_IRONMAN, HiscoreEndpoint.HARDCORE_IRONMAN),
	ULTIMATE_IRONMAN(IconID.ULTIMATE_IRONMAN, HiscoreEndpoint.ULTIMATE_IRONMAN),
	GIM_GREEN(IconID.UNRANKED_GROUP_IRONMAN, HiscoreEndpoint.NORMAL),
	GIM_BLUE(IconID.GROUP_IRONMAN, HiscoreEndpoint.NORMAL),
	GIM_HARDCORE(IconID.HARDCORE_GROUP_IRONMAN, HiscoreEndpoint.NORMAL);

	private final String iconTag;
	final HiscoreEndpoint endpoint;

	AccountType(IconID icon, HiscoreEndpoint endpoint)
	{
		this.iconTag = icon == null ? null : icon.toString();
		this.endpoint = endpoint;
	}

	static AccountType fromRawName(String rawName)
	{
		if (rawName == null)
		{
			return NORMAL;
		}
		for (AccountType type : values())
		{
			if (type.iconTag != null && rawName.contains(type.iconTag))
			{
				return type;
			}
		}
		return NORMAL;
	}
}
