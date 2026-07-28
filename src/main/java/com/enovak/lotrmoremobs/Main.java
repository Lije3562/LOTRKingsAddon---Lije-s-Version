package com.enovak.lotrmoremobs;

import net.minecraft.item.ItemStack;
import com.enovak.lotrmoremobs.trade.MumakilItemTradeInjector; // MUMAKIL_SHANK_SYSTEM_V1_1
import com.enovak.lotrmoremobs.achievement.MumakilAchievements;
import com.enovak.lotrmoremobs.config.MumakilConfig;
import com.enovak.lotrmoremobs.recipe.MumakilHowdahRecipeRegistry; // MUMAKIL_NEAR_HARAD_HOWDAH_RECIPE_V1_1
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.hiring.MumakilUnitTradeInjector;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilHowdah;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilCalfSpawnEgg; // MUMAKIL_CALF_SPAWN_EGG_V1
import com.enovak.lotrmoremobs.item.LOTRItemMumakilHowdahSpawnEgg;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilShank;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilTusk;
import com.enovak.lotrmoremobs.materials.AddonMaterial;
import com.enovak.lotrmoremobs.proxy.CommonProxy;
import com.enovak.lotrmoremobs.spawning.MumakilNaturalSpawnRegistry;
import com.enovak.lotrmoremobs.spawning.MumakilWarFormationSpawnRegistry;
import com.enovak.lotrmoremobs.spawning.MumakilInvasionFormationRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import lotr.common.entity.LOTREntities;
import lotr.common.item.LOTRItemArmor;
import lotr.common.item.LOTRItemSword;
import lotr.common.item.LOTRMaterial;
import net.minecraft.item.Item;
import com.enovak.lotrmoremobs.network.MumakilOpenGuiPacket;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION)
public class Main {

    public static final String MODID = "lotrmoremobs";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "LOTR More Mobs File";
    public static SimpleNetworkWrapper network;

    @SidedProxy(
            clientSide = "com.enovak.lotrmoremobs.proxy.ClientProxy",
            serverSide = "com.enovak.lotrmoremobs.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Item swordOfIsengard;
    public static Item helmOfIsengard;
    public static Item mumakilTusk;
    public static Item mumakilShank;
    public static Item mumakilCookedShank;
    public static Item mumakilHowdah;
    public static Item mumakilCalfSpawnEgg;
    public static Item mumakilHowdahSpawnEgg;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MumakilConfig.load(event.getSuggestedConfigurationFile());
        network = NetworkRegistry.INSTANCE.newSimpleChannel("lotrmoremobs");
        network.registerMessage(
                MumakilOpenGuiPacket.Handler.class,
                MumakilOpenGuiPacket.class,
                0,
                Side.SERVER
        );
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOTREntities.registerCreature(LOTREntityMumakil.class, "Mumakil", 811, 6118481, 12171165);
        LOTREntities.registerCreature(LOTREntityMumakilHowdahArcher.class, "MumakilHowdahArcher", 812, 12171165, 6118481);
        proxy.registerRenderers();
        proxy.registerEventHandlers();

        mumakilTusk = new LOTRItemMumakilTusk();
        GameRegistry.registerItem(mumakilTusk, "mumakil_tusk");

        mumakilShank = new LOTRItemMumakilShank(false);
        GameRegistry.registerItem(mumakilShank, "mumakil_shank");

        mumakilCookedShank = new LOTRItemMumakilShank(true);
        GameRegistry.registerItem(mumakilCookedShank, "cooked_mumakil_shank");

        GameRegistry.addSmelting(
                mumakilShank,
                new ItemStack(mumakilCookedShank),
                0.35F
        );

        mumakilHowdah = new LOTRItemMumakilHowdah();
        GameRegistry.registerItem(mumakilHowdah, "mumakil_howdah");

        /*
         * MUMAKIL_CALF_SPAWN_EGG_V1
         * A dedicated custom egg spawns the normal Mumakil entity directly
         * into its wild-baby lifecycle. No duplicate entity ID is needed.
         */
        mumakilCalfSpawnEgg = new LOTRItemMumakilCalfSpawnEgg();
        GameRegistry.registerItem(
                mumakilCalfSpawnEgg,
                "mumakil_calf_spawn_egg"
        );

        mumakilHowdahSpawnEgg =
                new LOTRItemMumakilHowdahSpawnEgg();
        GameRegistry.registerItem(
                mumakilHowdahSpawnEgg,
                "mumakil_howdah_spawn_egg"
        );

        MumakilUnitTradeInjector.inject();
        MumakilItemTradeInjector.inject();

        swordOfIsengard = new LOTRItemSword(AddonMaterial.LEGENDARY.toToolMaterial())
                .setUnlocalizedName("atalcare")
                .setTextureName("lotrmoremobs:anduril");
        GameRegistry.registerItem(swordOfIsengard, "atalcare");

        helmOfIsengard = (new LOTRItemArmor(LOTRMaterial.MORDOR, 0, "helmet"))
                .setUnlocalizedName("Helm of Isengard")
                .setTextureName("lotrmoremobs:black_numenorean_1");
        GameRegistry.registerItem(helmOfIsengard, "helm_of_isengard");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        /*
         * Register after LOTR has populated its faction crafting lists.
         */
        MumakilAchievements.register();
        MumakilHowdahRecipeRegistry.register();
        MumakilNaturalSpawnRegistry.register(); // MUMAKIL_NATURAL_SPAWNING_V1
        MumakilWarFormationSpawnRegistry.register();
        MumakilInvasionFormationRegistry.register();
    }
}
