package com.enovak.lotrmoremobs.coremod;

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
                PathFinderGatePartTransformer.class.getName()
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
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
