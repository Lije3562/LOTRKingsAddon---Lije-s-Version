package com.enovak.lotrmoremobs.trade;

import com.enovak.lotrmoremobs.Main;
import lotr.common.entity.npc.LOTRTradeEntries;
import lotr.common.entity.npc.LOTRTradeEntry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * MUMAKIL_SHANK_SYSTEM_V1_1
 *
 * Adds Mumakil materials to the Harad hunter's buy pool.
 * Prices are LOTR trade-pool base prices and therefore use the LOTR mod's
 * normal per-trader price variation when trades are generated.
 */
public final class MumakilItemTradeInjector {
    public static final int MUMAK_TUSK_HUNTSMAN_PRICE = 50;
    public static final int MUMAK_TUSK_GOLDSMITH_PRICE = 25;

    private MumakilItemTradeInjector() {
    }

    public static void inject() {
        if (Main.mumakilShank == null
                || Main.mumakilCookedShank == null
                || Main.mumakilTusk == null) {
            System.out.println(
                    "[LOTRMoreMobs] Mumakil item trades were requested"
                            + " before the items were registered."
            );
            return;
        }

        LOTRTradeEntries huntsmanPool =
                LOTRTradeEntries.HARAD_HUNTER_BUY;
        LOTRTradeEntries goldsmithPool =
                LOTRTradeEntries.HARAD_GOLDSMITH_SELL;

        if (!isReady(huntsmanPool) || !isReady(goldsmithPool)) {
            System.out.println(
                    "[LOTRMoreMobs] Near Harad material trades were not ready"
                            + " for Mumakil item injection."
            );
            return;
        }

        ensureTrade(
                huntsmanPool,
                Main.mumakilShank,
                15
        );
        ensureTrade(
                huntsmanPool,
                Main.mumakilCookedShank,
                20
        );
        ensureTrade(
                huntsmanPool,
                Main.mumakilTusk,
                MUMAK_TUSK_HUNTSMAN_PRICE
        );
        ensureTrade(
                goldsmithPool,
                Main.mumakilTusk,
                MUMAK_TUSK_GOLDSMITH_PRICE
        );

        System.out.println(
                "[LOTRMoreMobs] Ensured Near Harad Mumak trades:"
                        + " huntsman tusk=" + MUMAK_TUSK_HUNTSMAN_PRICE
                        + ", goldsmith tusk="
                        + MUMAK_TUSK_GOLDSMITH_PRICE
                        + ", shank trades preserved."
        );
    }

    private static boolean isReady(LOTRTradeEntries tradePool) {
        return tradePool != null && tradePool.tradeEntries != null;
    }

    private static void ensureTrade(
            LOTRTradeEntries tradePool,
            Item item,
            int basePrice
    ) {
        LOTRTradeEntry existing = findItemTrade(tradePool, item);
        if (existing != null) {
            existing.setCost(basePrice);
            return;
        }

        LOTRTradeEntry[] oldEntries = tradePool.tradeEntries;
        LOTRTradeEntry[] newEntries =
                new LOTRTradeEntry[oldEntries.length + 1];
        System.arraycopy(
                oldEntries,
                0,
                newEntries,
                0,
                oldEntries.length
        );
        newEntries[oldEntries.length] =
                new LOTRTradeEntry(new ItemStack(item), basePrice);
        tradePool.tradeEntries = newEntries;
    }

    private static LOTRTradeEntry findItemTrade(
            LOTRTradeEntries tradePool,
            Item item
    ) {
        for (int i = 0; i < tradePool.tradeEntries.length; ++i) {
            LOTRTradeEntry entry = tradePool.tradeEntries[i];

            if (entry == null) {
                continue;
            }

            ItemStack tradeStack = entry.createTradeItem();

            if (tradeStack != null && tradeStack.getItem() == item) {
                return entry;
            }
        }

        return null;
    }
}
