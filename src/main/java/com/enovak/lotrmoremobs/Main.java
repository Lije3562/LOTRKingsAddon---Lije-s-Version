package com.enovak.lotrmoremobs;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilHowdah;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilShank;
import com.enovak.lotrmoremobs.item.LOTRItemMumakilTusk;
import com.enovak.lotrmoremobs.materials.AddonMaterial;
import com.enovak.lotrmoremobs.proxy.CommonProxy;
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
    public static Item mumakilHowdah;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
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
        proxy.registerRenderers();
        proxy.registerEventHandlers();

        mumakilTusk = new LOTRItemMumakilTusk();
        GameRegistry.registerItem(mumakilTusk, "mumakil_tusk");

        mumakilShank = new LOTRItemMumakilShank();
        GameRegistry.registerItem(mumakilShank, "mumakil_shank");

        mumakilHowdah = new LOTRItemMumakilHowdah();
        GameRegistry.registerItem(mumakilHowdah, "mumakil_howdah");

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
    }
}
