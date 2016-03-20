### Universal Skin Mod
This is a mod for Minecraft, allowing players to select any "SkinServer" they like.

Current version is v1.4-dev1 for Minecraft 1.9

### Features

- Unlimited number of custom URLs
- HD skins supported via Optifine (Unconfirmed)
- Transparent pixels allowed
- Skull support
- UniSkinAPI support
- Legacy style link supported again, skin and cape only
- Dynamic skin, cape and elytra
- Genuine and local skin loading

### Configure

The config file is located in `config/UniSkinMod/UniSkinMod.json`.
Here is the default configure:

    {
      "rootURIs": [
        "http://www.skinme.cc/uniskin",
        "https://skin.prinzeugen.net"
      ],
      "legacySkinURIs": [],
      "legacyCapeURIs": []
    }

These are the two default skin servers and you are free to edit them.

"Legacy" refers to old-style skin links. Here's an example

    https://skins.minecraft.net/MinecraftSkins/%s.png

`%s` will be replaced by the player's name.

All skins will be loaded in this order:

1. Local dynamic textures
2. Remote dynamic textures
3. Official static textures (Cannot disable from config)
4. Local static textures
5. Remote static textures
6. Legacy static textures

##### Local Textures

Local skin folder is `config/UniSkinMod/local_skins/` which uses the same structures as UniSkinAPI.
That is `config/UniSkinMod/local_skins/{playername}.json` is per-player setting and all textures goes
into `config/UniSkinMod/local_skins/textures/{hash}`

For more info about the "Root" URL, visit [UniSkinAPI Document](https://github.com/RecursiveG/UniSkinServer/blob/master/doc/UniSkinAPI_en.md) please!

### 配置说明

配置文件位于`config/UniSkinMod/UniSkinMod.json`。````````
默认配置如下：

    {
      "rootURIs": [
        "http://www.skinme.cc/uniskin",
        "https://skin.prinzeugen.net"
      ],
      "legacySkinURIs": [],
      "legacyCapeURIs": []
    }

你可以随意更改服务器。

“Legacy” 指旧式的链接地址。 样例如下：

    https://skins.minecraft.net/MinecraftSkins/%s.png

其中的 `%s` 会被玩家名称替代。

所有服务器按以下顺序加载：

1. 本地动态皮肤
2. 皮肤站动态皮肤
3. 官方皮肤
4. 本地皮肤
5. 皮肤站皮肤
6. Legacy 皮肤

##### 本地皮肤

本地皮肤目录位于 `config/UniSkinMod/local_skins/` 并使用与UniSkinAPI相同的目录结构。
即：`config/UniSkinMod/local_skins/{playername}.json` 是各玩家的配置文件。
`config/UniSkinMod/local_skins/textures/{hash}` 是具体的皮肤文件。

更多关于“根地址”的信息，请访问[UniSkinAPI 文档](https://github.com/RecursiveG/UniSkinServer/blob/master/doc/UniSkinAPI_zh-CN.md)。

### Known bug

Main thread may freeze when loading skins. In multi-player games, this may cause timeout.

### License
Licensed under GPLv2
