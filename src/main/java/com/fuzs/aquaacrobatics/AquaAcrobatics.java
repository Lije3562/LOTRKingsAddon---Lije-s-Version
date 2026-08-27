package com.fuzs.aquaacrobatics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.enovak.lotrmoremobs.Main;
import com.fuzs.aquaacrobatics.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLModIdMappingEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@SuppressWarnings("unused")
@Mod(
    modid = AquaAcrobatics.MODID,
    name = AquaAcrobatics.NAME,
    version = AquaAcrobatics.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class AquaAcrobatics {

    public static final String MODID = "aquaacrobatics";
    public static final String NAME = "Aqua Acrobatics";
    public static final String VERSION = "phase3-mixin-free-asm-stable";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    private static final String CLIENT_PROXY = "com.fuzs." + MODID + ".proxy.ClientProxy";
    private static final String COMMON_PROXY = "com.fuzs." + MODID + ".proxy.CommonProxy";

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    private static CommonProxy proxy;

    @Mod.EventHandler
    public void onPreInit(final FMLPreInitializationEvent evt) {
        if (evt.getModMetadata() != null) {
            /*
             * Aqua Acrobatics is bundled as an internal KOME component. Mark
             * it as a child mod so Forge keeps it out of the top-level Mods
             * list while preserving its normal loading and config file.
             */
            evt.getModMetadata().parent = Main.MODID;
        }
        proxy.onPreInit(evt);
    }

    @Mod.EventHandler
    public void onInit(final FMLInitializationEvent evt) {
        proxy.onInit(evt);
    }

    @Mod.EventHandler
    public void onPostInit(final FMLPostInitializationEvent evt) {
        proxy.onPostInit(evt);
    }

    @Mod.EventHandler
    public void onMappings(FMLModIdMappingEvent evt) {
        proxy.onMappings();
    }

}
