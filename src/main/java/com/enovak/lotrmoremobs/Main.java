package com.enovak.lotrmoremobs;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
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

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION)
public class Main {

    public static final String MODID = "lotrmoremobs";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "LOTR More Mobs File";

    @SidedProxy(
            clientSide = "com.enovak.lotrmoremobs.proxy.ClientProxy",
            serverSide = "com.enovak.lotrmoremobs.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Item swordOfIsengard;
    public static Item helmOfIsengard;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOTREntities.registerCreature(LOTREntityMumakil.class, "Mumakil", 811, 6118481, 12171165);
        proxy.registerRenderers();

        swordOfIsengard = new LOTRItemSword(AddonMaterial.LEGENDARY.toToolMaterial())
                .setUnlocalizedName("lotrmoremobs:atalcare")
                .setTextureName("lotrmoremobs:anduril");
        this.registerItem(swordOfIsengard);

        helmOfIsengard = (new LOTRItemArmor(LOTRMaterial.MORDOR, 0, "helmet"))
                .setUnlocalizedName("Helm of Isengard")
                .setTextureName("lotrmoremobs:black_numenorean_1");
        this.registerItem(helmOfIsengard);
    }

    private void registerItem(Item item) {
        String prefixUnlocal = "item:lotr.";
        GameRegistry.registerItem(item, "item." + item.getUnlocalizedName().substring(prefixUnlocal.length()));
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
