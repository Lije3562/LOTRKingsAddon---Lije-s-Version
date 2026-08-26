package com.enovak.lotrmoremobs.coremod;

import com.fuzs.aquaacrobatics.core.AquaAcrobaticsCore;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.Name("LOTRMoreMobs Mortal Gandalf")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({
        "com.enovak.lotrmoremobs.coremod"
})
public final class MortalGandalfCorePlugin
        implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                MortalGandalfTransformer.class.getName(),
                RespawnMarkerProjectileCollisionTransformer.class.getName(),
                EntitySensesGateSightTransformer.class.getName(),
                PathFinderGatePartTransformer.class.getName(),
                "com.fuzs.aquaacrobatics.core.asm.AquaEntityPlayerTransformer",
                "com.fuzs.aquaacrobatics.core.asm.AquaServerPlayerTransformer",
                "com.fuzs.aquaacrobatics.core.asm.AquaBiomeTransformer",
                "com.fuzs.aquaacrobatics.core.asm.AquaCommonWorldTransformer",
                "com.fuzs.aquaacrobatics.core.asm.AquaClientEntityTransformer",
                "com.fuzs.aquaacrobatics.core.asm.AquaLateClientPlayerTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        new AquaAcrobaticsCore().injectData(data);
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
