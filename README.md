# Account Type Muter

A RuneLite plugin that hides chat from players based on their account type or level.

- Mute mains, ironman, hardcore, UIM, GIM (green / blue / HCGIM), pures, skillers, F2P
- Mute players under a total level or combat level
- Choose where it applies: public chat, overhead text, private messages, clan chat, friends chat
- Never mute friends, clan chat or friends chat (on by default)

Ironman-type accounts are read from the icon in the sender's name. Pures and skillers use the official hiscore
tables. Levels, F2P (all members-only skills at level 1) and mains use a hiscores lookup on the sender's name,
so a new player's first message may show briefly before it is hidden.

## Development

Requires JDK 11+. `./gradlew run` starts a RuneLite developer client with the plugin loaded.
