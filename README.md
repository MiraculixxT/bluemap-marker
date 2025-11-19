# BlueMap Marker Manager
-> [**Download BlueMap here**](https://modrinth.com/mod/bluemap)

BlueMap supports a variety of marker setups by default, but it requires some work from editing configs and reading docs to understand what is actually required. Who likes reading... 
The Marker Manager takes this task completely off your hands and lets each player create their own markers!

## Creating Markers / Marker-Sets
| Command | Permission | Short Description |
|:-:|:-:|:-:|
| /bmarker | `bmarker.command.main` | Main permission to access the command |
| /bmarker **create** |  `bmarker.command.create` | Start a new marker setup to create a new custom marker inside a set |
| /bmarker **create-set** | `bmarker.command.create-set` | Start a new marker-set setup. Every marker needs to be inside a set |
| /bmarker **delete** | `bmarker.command.delete` | Delete a custom marker from any set |
| /bmarker **delete-set** | `bmarker.command.delete-set` | Delete a whole set with all markers inside |

Creating a new marker or marker-set will start a interactive setup menu to define your favorite values. Every marker type has unique arguments to define with different displays. Hover over each argument to see what it does and how it modifies the output. All input types trying to help you what kind of value is needed and auto complete it if that is possible to make it as easy as possible for all players.

![Imgur](https://imgur.com/LHeMbg2.png)

## Support 
Simply hop on the [BlueMap Discord](https://discord.gg/zmkyJa3) and ask in #3rd-party-support :)

Note, this is a 3rd party extension and not official by BlueMap in any way! 

If you have issues with entering colors, please use a generator instead of trying to enter the RGB values for your self. There are some hidden rules that has to be followed, otherwise the system will just pick an other color.

## Localization
Change the language of BMarker via ``/bmarker language <key>``<br>
Currently english (en_US), german (de_DE), chinese (zh_CN), and japanese (ja_JP) are provided from installation.
You can create your own translation by coping an existing language file (``<config-folder>/langauge/<key>.yml``),
renaming it to your target langauge code and editing all values inside. 
You are responsible for updating it on BMarker updates, missing keys will be displayed as just the key in red.

## TODO - Maybe coming soon™
- Adding a way to edit markers and marker-sets after build/creating them
- Adding a marker type that supports iframes (like embed youtube videos, tweets, ...)
