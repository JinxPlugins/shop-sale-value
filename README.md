# Shop Sale Value

RuneLite plugin by Jinx. Shows estimated sale prices for inventory items in supported general stores.

## Features

- White next-sale GP labels above sellable inventory items while a shop is open.
- Value and Sell menu labels showing total GP and the whole-GP average per item, rounded down.
- Live stock-dependent pricing and supported special-store rules.
- Ctrl+click an inventory item for a local chat estimate at a normal general store, even outside a shop. This consumes the normal item action; it does not sell, equip, use or drop the item.
- Noted item support and quantities capped to the matching inventory item type.

Each item's sale price is rounded down before totals are summed. The displayed average is also rounded down, so multiplying it by the quantity may not equal the exact total.

## Installation

This plugin is not yet approved or available on the Plugin Hub. After approval, install Shop Sale Value from the normal RuneLite Plugin Hub.

## Development

Requires JDK 11. Run `./gradlew test` for tests and `./gradlew run` for the development client. The run task creates a separate ShopSaleValue-DevProfile directory beside the project. It does not use your normal RuneLite settings.

For Jagex accounts, see the official development login guide:
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

Keep credentials and profiles private and outside this repository.

## Limitations

Prices are estimates. Server restrictions, restocking during a sale and unsupported shop variants can affect actual proceeds. Unknown stores and non-coin stores do not receive automatic quotes. Understock pricing is not supported. Custom rules can be configured for an exact shop title.

The plugin does not automate sales, world hopping or other game actions. Initial Lumbridge item labels, menu prices and Ctrl+click lookup have been manually checked; other supported stores still need broader in-game validation.

## Attribution

See LICENSE and the source resource attribution files for licensing and shop-data attribution.
