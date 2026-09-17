# Shop Sale Value

A RuneLite plugin by **Jinx** that estimates how much NPC general stores will pay for inventory items, accounting for current stock and price decreases across a sale.

## Status and installation

Development version 0.1.0. Not yet submitted to or approved by the RuneLite Plugin Hub. Automated tests pass; human in-game verification is still pending. No claim of Jagex or RuneLite approval is made.

The intended installation route is RuneLite's Plugin Hub after review. Once accepted, it can be installed in the standard client and used when RuneLite is opened through the Jagex Launcher. Uploading this repository alone does not make the plugin available in the Hub.

## Features

- Adds total GP and average GP per item to existing Value and Sell menu labels.
- Displays a comparison overlay for sale quantities, capped to the number of items actually available.
- Reads current shop stock and updates estimates as stock changes.
- Matches noted inventory items to their unnoted shop equivalent.
- Compares the current batch with a hypothetical sale at normal stock on another world. It does not inspect or hop to other worlds.
- Supports 45 automatic general-store profiles, plus explicit custom pricing rules including zero price decline.

The plugin only displays information. It does not add server-action menu entries, change menu actions, automate input, sell items, or hop worlds. The plugin itself makes no network requests or accesses account credentials.

## Pricing and limitations

Each successive item is priced and rounded down separately before summing the total. For an oak shortbow (u), value 50, a normal 40%/3% shop at zero stock gives 20 GP for one, 84 GP for five, and 130 GP for ten. These are calculation fixtures, not observations from live sales.

Shop titles are matched ignoring punctuation and case. A generic General Store title requires an unambiguous match against ordered default stock. Unknown shops show a message rather than an invented price.

Karamja General Store, Jiminua's and Obli's require custom configuration because their diary/glove variants are not automatically resolved. The catalogue includes these three disabled automatic profiles in addition to the 45 enabled ones. Stock below the recorded baseline is flagged for checking with the game's Value action. Non-coin shops are unsupported.

Custom rules apply only to a matching shop title. A custom drop of 300 means 3%, 30 means 0.3%, and 0 means fixed pricing. A generic-title override affects every shop sharing that title. Clear it before visiting a different such store. The custom normal-stock setting applies to every item under that override.

Sell X is not labelled because its future input is unknown. Totals assume no intervening restocks, other-player activity, coin-stack cap, or server refusal. Shop data may change. Price estimates do not establish whether the server will accept an item.

## Development

Java 11 target, RuneLite 1.12.39, Gradle 8.10 wrapper.

```text
gradlew.bat test jar
```

On Unix-like systems, use `sh gradlew test jar`.

The optional `run` task follows RuneLite's development-plugin workflow. A development client may reuse the usual RuneLite configuration and login state; it is not an isolated account environment. See the official [development instructions](https://github.com/runelite/plugin-hub#creating-new-plugins) and [Jagex Account guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

The `launcherJar` task is development tooling; it is not needed for Plugin Hub installation.

Tests cover pricing, per-item rounding, stock decline, fixed rates, large stacks, inventory caps, noted items, menu action preservation, cache updates, shop matching, hop-state cleanup and overlay rendering. Test reports and compiled outputs are excluded from the repository.

## Sources and licences

Pricing references: [OSRS Wiki general stores](https://oldschool.runescape.wiki/w/General_store) and [shops](https://oldschool.runescape.wiki/w/Shop).

Bundled stock baselines and change rates are selected from [Kasparas-G55/shop-prices](https://github.com/Kasparas-G55/shop-prices/tree/f341712da0d29337573671747e23ff0f185fd454), commit `f341712da0d29337573671747e23ff0f185fd454`. Its BSD-2-Clause notice is preserved in `src/main/resources/SHOP-DATA-LICENSE.txt`.

The build wrapper and development bootstrap follow [RuneLite's example plugin](https://github.com/runelite/example-plugin). Original plugin code is BSD-2-Clause; see `LICENSE`.
