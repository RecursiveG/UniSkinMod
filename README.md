### Universal Skin Mod
This is a mod for Minecraft, allowing players to select any "SkinServer" they like.
Latest version is v1.2-dev3 for Minecraft 1.7.10

### Features (1.7.10-v1.2-dev3)

- Unlimited number of custom URLs
- Transparent pixels allowed
- HD skins supported via Optifine
- Auto cache clean

### Configure

- The config file is located in `config/UniSkinMod.cfg`
- Comment lines start with `#`
- Line starts with `Version: ` indicates the configure version, do not modify.
- Line starts with `Skin: ` indicates a skin server url.
- Line starts with `Cape: ` indicates a cloak server url.
- The `#` part at the end of these two kinds of lines will be interperted as part of the url.
- Use `%s` to represent the name of the player.
- The mod will check all the links in that order.
- If none of the links is available, Mojang's server will be used.

### License
Licensed under GPLv2

