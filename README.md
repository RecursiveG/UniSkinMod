### Universal Skin Mod
This is a mod for Minecraft, allowing players to select any "SkinServer" they like.
Latest version is v1.3-dev1 for Minecraft 1.8

### Features

- Unlimited number of custom URLs
- HD skins supported via Optifine
- Transparent pixels allowed (v1.2-dev2)
- Auto cache clean (v1.2-dev3)
- Smart cache clean (v1.2-dev4)
- Skull support (v1.2-dev4)
- UniSkinAPI support (v1.3-dev1)

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

