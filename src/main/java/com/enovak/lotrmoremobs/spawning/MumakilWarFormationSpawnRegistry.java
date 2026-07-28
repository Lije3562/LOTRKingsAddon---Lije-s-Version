package com.enovak.lotrmoremobs.spawning;

import com.enovak.lotrmoremobs.config.MumakilConfig;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lotr.common.fac.LOTRFaction;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.map.LOTRConquestGrid;
import lotr.common.world.map.LOTRConquestZone;
import lotr.common.world.spawning.LOTRBiomeSpawnList;
import lotr.common.world.spawning.LOTRSpawnEntry;
import lotr.common.world.spawning.LOTRSpawnList;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Adds the formation bootstrap to native LOTR NPC faction containers.
 *
 * Eligibility is derived from the containers themselves: no biome-name or
 * biome-class whitelist is maintained here.
 */
public final class MumakilWarFormationSpawnRegistry {
    public static final int HOME_FORMATION_SPAWN_WEIGHT = 1;
    public static final int HOME_FORMATION_EFFECTIVE_DENOMINATOR =
            MumakilConfig.HOME_MILITARY_SPAWN_BASE_DENOMINATOR;
    public static final int CONQUEST_FORMATION_SPAWN_WEIGHT = 1;
    public static final int CONQUEST_FORMATION_SPAWN_CHANCE = 1;
    public static final float CONQUEST_FORMATION_THRESHOLD =
            MumakilConfig.CONQUEST_MINIMUM_EFFECTIVE_STRENGTH;
    private static final float NATIVE_INCLUSIVE_CONQUEST_THRESHOLD =
            Math.nextAfter(
                    CONQUEST_FORMATION_THRESHOLD,
                    Float.NEGATIVE_INFINITY
            );

    private static final int FORMATION_ENTRY_WEIGHT = 1;

    private static boolean handlerRegistered;
    private static boolean reflectionResolved;
    private static boolean reflectionAvailable;
    private static boolean loggedSuccess;
    private static boolean loggedFailure;
    private static Field factionContainersField;
    private static Field factionSpawnListsField;
    private static Field containedSpawnListField;
    private static Field spawnListWeightField;
    private static Constructor spawnListConstructor;
    private static LOTRSpawnList homeFormationSpawnList;
    private static LOTRSpawnList conquestFormationSpawnList;

    private MumakilWarFormationSpawnRegistry() {
    }

    public static void register() {
        if (handlerRegistered) {
            return;
        }

        handlerRegistered = true;
        MinecraftForge.EVENT_BUS.register(
                new MumakilWarFormationSpawnRegistry()
        );
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event == null
                || event.world == null
                || event.world.isRemote) {
            return;
        }

        injectNativeSpawnLists(event.world);
    }

    private static void injectNativeSpawnLists(World world) {
        if (!resolveReflection()) {
            logFailureOnce(
                    "native LOTR spawn-list reflection is unavailable"
            );
            return;
        }

        try {
            if (LOTRSpawnList.SOUTHRON_WARRIORS
                    .getListCommonFaction(world)
                    != LOTRFaction.NEAR_HARAD) {
                logFailureOnce(
                        "the native SOUTHRON_WARRIORS list is not Near Harad"
                );
                return;
            }

            if (homeFormationSpawnList == null) {
                homeFormationSpawnList = createFormationSpawnList();
            }
            if (conquestFormationSpawnList == null) {
                conquestFormationSpawnList = createFormationSpawnList();
            }

            int homeContainersAdded = 0;
            int conquestContainersAdded = 0;
            Set<String> homeContainerNames =
                    new LinkedHashSet<String>();
            Set<String> conquestContainerNames =
                    new LinkedHashSet<String>();
            Set<LOTRBiomeSpawnList> processedLists =
                    new HashSet<LOTRBiomeSpawnList>();
            BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();

            for (int i = 0; i < biomes.length; ++i) {
                if (!(biomes[i] instanceof LOTRBiome)) {
                    continue;
                }

                LOTRBiomeSpawnList biomeSpawnList =
                        ((LOTRBiome)biomes[i]).npcSpawnList;
                if (biomeSpawnList == null
                        || !processedLists.add(biomeSpawnList)) {
                    continue;
                }

                List factionContainers =
                        (List)factionContainersField.get(biomeSpawnList);
                for (int containerIndex = 0;
                     containerIndex < factionContainers.size();
                     ++containerIndex) {
                    Object object = factionContainers.get(containerIndex);
                    if (!(object
                            instanceof LOTRBiomeSpawnList.FactionContainer)) {
                        continue;
                    }

                    LOTRBiomeSpawnList.FactionContainer factionContainer =
                            (LOTRBiomeSpawnList.FactionContainer)object;
                    if (!containsSpawnList(
                            factionContainer,
                            LOTRSpawnList.SOUTHRON_WARRIORS
                    )) {
                        continue;
                    }

                    if (factionContainer.isConquestFaction()) {
                        conquestContainerNames.add(
                                getContainerName(
                                        biomes[i],
                                        containerIndex,
                                        true
                                )
                        );
                        if (MumakilConfig
                                .enableMumakWarFormationsInConquest
                                && !containsSpawnList(
                                factionContainer,
                                conquestFormationSpawnList
                        )) {
                            factionContainer.add(
                                    LOTRBiomeSpawnList.entry(
                                            conquestFormationSpawnList,
                                            CONQUEST_FORMATION_SPAWN_WEIGHT
                                    ).setSpawnChance(
                                            CONQUEST_FORMATION_SPAWN_CHANCE
                                    ).setConquestThreshold(
                                            NATIVE_INCLUSIVE_CONQUEST_THRESHOLD
                                    )
                            );
                            ++conquestContainersAdded;
                        }
                    } else {
                        homeContainerNames.add(
                                getContainerName(
                                        biomes[i],
                                        containerIndex,
                                        false
                                )
                        );
                        if (!containsSpawnList(
                                factionContainer,
                                homeFormationSpawnList
                        )) {
                            factionContainer.add(
                                    LOTRBiomeSpawnList.entry(
                                            homeFormationSpawnList,
                                            HOME_FORMATION_SPAWN_WEIGHT
                                    ).setSpawnChance(
                                        1
                                )
                        );
                        ++homeContainersAdded;
                        }
                    }
                }
            }

            if (!loggedSuccess) {
                loggedSuccess = true;
                System.out.println(
                        "[LOTRMoreMobs] Mumak war-formation NPC spawn"
                                + " injection complete: homeContainers="
                                + homeContainerNames.size()
                                + " homeNames="
                                + homeContainerNames
                                + " homeEntriesAdded="
                                + homeContainersAdded
                                + " homeWeight="
                                + HOME_FORMATION_SPAWN_WEIGHT
                                + " homeChance=1/"
                                + HOME_FORMATION_EFFECTIVE_DENOMINATOR
                                + " conquestContainers="
                                + conquestContainerNames.size()
                                + " conquestNames="
                                + conquestContainerNames
                                + " conquestEntriesAdded="
                                + conquestContainersAdded
                                + " conquestWeight="
                                + CONQUEST_FORMATION_SPAWN_WEIGHT
                                + " conquestChance=1/"
                                + MumakilConfig.CONQUEST_BASE_DENOMINATOR
                                + "..1/"
                                + MumakilConfig
                                .CONQUEST_MAX_PROBABILITY_DENOMINATOR
                                + " conquestThreshold=>="
                                + CONQUEST_FORMATION_THRESHOLD
                                + " conquestEnabled="
                                + MumakilConfig
                                .enableMumakWarFormationsInConquest
                                + " invasionEnabled="
                                + MumakilConfig
                                .enableMumakWarFormationsInInvasions
                );
            }
        } catch (Exception e) {
            logFailureOnce(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Called once per conquest bootstrap entity. Native spawning has already
     * selected an existing Near Harad conquest military container, so this is
     * not a global or player-driven roll.
     */
    public static boolean passesConquestBootstrapChance(
            LOTREntityMumakilHowdahArcher bootstrap
    ) {
        if (!MumakilConfig.enableMumakWarFormationsInConquest
                || bootstrap == null
                || bootstrap.worldObj == null
                || bootstrap.worldObj.isRemote) {
            return false;
        }

        float effectiveConquest = getEffectiveNearHaradConquest(
                bootstrap.worldObj,
                MathHelper.floor_double(bootstrap.posX),
                MathHelper.floor_double(bootstrap.posZ)
        );
        if (effectiveConquest
                < MumakilConfig.CONQUEST_MINIMUM_EFFECTIVE_STRENGTH) {
            return false;
        }

        int denominator =
                getConquestSpawnDenominator(effectiveConquest);
        int eligibleContainerWeight = getEligibleContainerWeight(
                bootstrap,
                true,
                effectiveConquest
        );
        return passesEffectiveDenominator(
                bootstrap,
                denominator,
                eligibleContainerWeight
        );
    }

    /**
     * The custom spawn-list has weight one. Compensating its native
     * container-selection probability here makes the complete probability
     * exactly 1/150 per eligible Near Harad military-container selection.
     */
    public static boolean passesHomeBootstrapChance(
            LOTREntityMumakilHowdahArcher bootstrap
    ) {
        if (bootstrap == null
                || bootstrap.worldObj == null
                || bootstrap.worldObj.isRemote) {
            return false;
        }
        int eligibleContainerWeight = getEligibleContainerWeight(
                bootstrap,
                false,
                0.0F
        );
        return passesEffectiveDenominator(
                bootstrap,
                HOME_FORMATION_EFFECTIVE_DENOMINATOR,
                eligibleContainerWeight
        );
    }

    public static int getConquestSpawnDenominator(
            float effectiveConquest
    ) {
        float extraConquest = Math.max(
                0.0F,
                effectiveConquest
                        - MumakilConfig
                        .CONQUEST_MINIMUM_EFFECTIVE_STRENGTH
        );
        int denominatorReduction =
                MathHelper.floor_float(extraConquest / 100.0F);
        return Math.max(
                MumakilConfig.CONQUEST_MAX_PROBABILITY_DENOMINATOR,
                MumakilConfig.CONQUEST_BASE_DENOMINATOR
                        - denominatorReduction
        );
    }

    private static float getEffectiveNearHaradConquest(
            World world,
            int x,
            int z
    ) {
        if (!LOTRConquestGrid.conquestEnabled(world)) {
            return 0.0F;
        }

        LOTRConquestZone zone =
                LOTRConquestGrid.getZoneByWorldCoords(x, z);
        if (zone == null || zone.isEmpty()) {
            return 0.0F;
        }

        float strength =
                zone.getConquestStrength(LOTRFaction.NEAR_HARAD, world);
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        LOTRBiomeSpawnList spawnList =
                biome instanceof LOTRBiome
                        ? ((LOTRBiome)biome).npcSpawnList
                        : null;
        List relations =
                LOTRFaction.NEAR_HARAD.getConquestBoostRelations();
        for (int i = 0; i < relations.size(); ++i) {
            LOTRFaction relation = (LOTRFaction)relations.get(i);
            if (spawnList == null
                    || !spawnList.isFactionPresent(world, relation)) {
                strength +=
                        zone.getConquestStrength(relation, world)
                                * 0.333F;
            }
        }
        return strength;
    }

    private static boolean passesEffectiveDenominator(
            LOTREntityMumakilHowdahArcher bootstrap,
            int denominator,
            int eligibleContainerWeight
    ) {
        return denominator > 0
                && eligibleContainerWeight > 0
                && bootstrap.getRNG().nextInt(denominator)
                < Math.min(denominator, eligibleContainerWeight);
    }

    private static int getEligibleContainerWeight(
            LOTREntityMumakilHowdahArcher bootstrap,
            boolean conquest,
            float effectiveConquest
    ) {
        if (!resolveReflection()) {
            return 0;
        }

        BiomeGenBase biome = bootstrap.worldObj.getBiomeGenForCoords(
                MathHelper.floor_double(bootstrap.posX),
                MathHelper.floor_double(bootstrap.posZ)
        );
        if (!(biome instanceof LOTRBiome)) {
            return 0;
        }

        LOTRBiomeSpawnList biomeSpawnList =
                ((LOTRBiome)biome).npcSpawnList;
        LOTRSpawnList expectedFormationList = conquest
                ? conquestFormationSpawnList
                : homeFormationSpawnList;
        if (biomeSpawnList == null
                || expectedFormationList == null) {
            return 0;
        }

        try {
            List factionContainers =
                    (List)factionContainersField.get(biomeSpawnList);
            for (int containerIndex = 0;
                 containerIndex < factionContainers.size();
                 ++containerIndex) {
                Object object = factionContainers.get(containerIndex);
                if (!(object
                        instanceof LOTRBiomeSpawnList.FactionContainer)) {
                    continue;
                }

                LOTRBiomeSpawnList.FactionContainer factionContainer =
                        (LOTRBiomeSpawnList.FactionContainer)object;
                if (factionContainer.isConquestFaction() != conquest
                        || !containsSpawnList(
                        factionContainer,
                        expectedFormationList
                )) {
                    continue;
                }

                int eligibleWeight = 0;
                List spawnLists =
                        (List)factionSpawnListsField.get(factionContainer);
                for (int listIndex = 0;
                     listIndex < spawnLists.size();
                     ++listIndex) {
                    Object spawnListObject = spawnLists.get(listIndex);
                    if (spawnListObject
                            instanceof LOTRBiomeSpawnList
                            .SpawnListContainer) {
                        LOTRBiomeSpawnList.SpawnListContainer
                                spawnListContainer =
                                (LOTRBiomeSpawnList.SpawnListContainer)
                                        spawnListObject;
                        if (spawnListContainer.canSpawnAtConquestLevel(
                                effectiveConquest
                        )) {
                            eligibleWeight +=
                                    spawnListWeightField.getInt(
                                            spawnListContainer
                                    );
                        }
                    }
                }
                return eligibleWeight;
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 0;
    }

    private static boolean containsSpawnList(
            LOTRBiomeSpawnList.FactionContainer factionContainer,
            LOTRSpawnList expected
    ) throws IllegalAccessException {
        List spawnLists =
                (List)factionSpawnListsField.get(factionContainer);
        for (int i = 0; i < spawnLists.size(); ++i) {
            Object object = spawnLists.get(i);
            if (object instanceof LOTRBiomeSpawnList.SpawnListContainer
                    && containedSpawnListField.get(object) == expected) {
                return true;
            }
        }
        return false;
    }

    private static LOTRSpawnList createFormationSpawnList()
            throws Exception {
        LOTRSpawnEntry[] entries = new LOTRSpawnEntry[] {
                new LOTRSpawnEntry(
                        LOTREntityMumakilHowdahArcher.class,
                        FORMATION_ENTRY_WEIGHT,
                        1,
                        1
                )
        };
        return (LOTRSpawnList)spawnListConstructor.newInstance(
                new Object[] {entries}
        );
    }

    private static String getContainerName(
            BiomeGenBase biome,
            int containerIndex,
            boolean conquest
    ) {
        String biomeName = biome == null || biome.biomeName == null
                ? "unknown"
                : biome.biomeName;
        int biomeId = biome == null ? -1 : biome.biomeID;
        return biomeName
                + "["
                + biomeId
                + "]#"
                + containerIndex
                + (conquest ? ":conquest" : ":home");
    }

    private static boolean resolveReflection() {
        if (reflectionResolved) {
            return reflectionAvailable;
        }

        synchronized (MumakilWarFormationSpawnRegistry.class) {
            if (!reflectionResolved) {
                try {
                    factionContainersField =
                            LOTRBiomeSpawnList.class.getDeclaredField(
                                    "factionContainers"
                            );
                    factionSpawnListsField =
                            LOTRBiomeSpawnList.FactionContainer.class
                                    .getDeclaredField("spawnLists");
                    containedSpawnListField =
                            LOTRBiomeSpawnList.SpawnListContainer.class
                                    .getDeclaredField("spawnList");
                    spawnListWeightField =
                            LOTRBiomeSpawnList.SpawnListContainer.class
                                    .getDeclaredField("weight");
                    spawnListConstructor =
                            LOTRSpawnList.class.getDeclaredConstructor(
                                    new Class[] {LOTRSpawnEntry[].class}
                            );

                    factionContainersField.setAccessible(true);
                    factionSpawnListsField.setAccessible(true);
                    containedSpawnListField.setAccessible(true);
                    spawnListWeightField.setAccessible(true);
                    spawnListConstructor.setAccessible(true);
                    reflectionAvailable = true;
                } catch (Exception e) {
                    reflectionAvailable = false;
                }
                reflectionResolved = true;
            }
        }
        return reflectionAvailable;
    }

    private static void logFailureOnce(String reason) {
        if (loggedFailure) {
            return;
        }

        loggedFailure = true;
        System.err.println(
                "[LOTRMoreMobs] Mumak war-formation spawn injection"
                        + " skipped: "
                        + reason
        );
    }
}
