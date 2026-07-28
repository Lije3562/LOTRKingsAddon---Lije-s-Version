package com.enovak.lotrmoremobs.spawning;

import com.enovak.lotrmoremobs.config.MumakilConfig;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import java.util.List;
import lotr.common.entity.LOTREntityInvasionSpawner;
import lotr.common.world.spawning.LOTRInvasions;

/**
 * Idempotent invasion-list integration and per-invasion formation allowance.
 */
public final class MumakilInvasionFormationRegistry {
    public static final String INVASION_FORMATION_SPAWNED_KEY =
            "lotrmoremobs_mumakFormationSpawned";
    public static final String INVASION_MEMBER_WEIGHT_KEY =
            "lotrmoremobs_mumakInvasionWeight";
    private static final String INVASION_FORMATION_ROLL_DECIDED_KEY =
            "lotrmoremobs_mumakFormationRollDecided";
    private static final String INVASION_FORMATION_ELIGIBLE_KEY =
            "lotrmoremobs_mumakFormationEligible";
    private static final double INVASION_BOOTSTRAP_LOOKUP_RANGE = 16.0D;
    private static boolean registered;

    private static final LOTRInvasions[] ELIGIBLE_INVASIONS =
            new LOTRInvasions[] {
                    LOTRInvasions.NEAR_HARAD_CORSAIR,
                    LOTRInvasions.NEAR_HARAD_COAST,
                    LOTRInvasions.NEAR_HARAD_HARNEDOR
            };

    private MumakilInvasionFormationRegistry() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        int added = 0;
        if (MumakilConfig.enableMumakWarFormationsInInvasions) {
            for (int i = 0; i < ELIGIBLE_INVASIONS.length; ++i) {
                LOTRInvasions invasion = ELIGIBLE_INVASIONS[i];
                if (!containsBootstrap(invasion)) {
                    invasion.invasionMobs.add(
                            new LOTRInvasions.InvasionSpawnEntry(
                                    LOTREntityMumakilHowdahArcher.class,
                                    MumakilConfig
                                            .INVASION_FORMATION_ENTRY_WEIGHT
                            )
                    );
                    ++added;
                }
            }
        }

        System.out.println(
                "[LOTRMoreMobs] Mumak invasion formation injection: "
                        + "eligibleLists="
                        + ELIGIBLE_INVASIONS.length
                        + " entriesAdded="
                        + added
                        + " names="
                        + getEligibleInvasionNames()
                        + " enabled="
                        + MumakilConfig
                        .enableMumakWarFormationsInInvasions
                        + " eligibility=1/"
                        + MumakilConfig
                        .INVASION_FORMATION_ELIGIBILITY_DENOMINATOR
                        + " weight="
                        + MumakilConfig.INVASION_FORMATION_ENTRY_WEIGHT
                        + " maxPerInvasion="
                        + MumakilConfig.MAX_FORMATIONS_PER_INVASION
                        + " budget="
                        + MumakilConfig.INVASION_FORMATION_BUDGET_VALUE
        );
    }

    public static LOTREntityInvasionSpawner getEligibleSpawner(
            LOTREntityMumakilHowdahArcher bootstrap
    ) {
        if (!MumakilConfig
                .enableMumakWarFormationsInInvasions
                || bootstrap == null
                || bootstrap.worldObj == null
                || bootstrap.worldObj.isRemote) {
            return null;
        }

        LOTREntityInvasionSpawner spawner = null;
        if (bootstrap.isInvasionSpawned()
                && bootstrap.getInvasionID() != null) {
            spawner = LOTREntityInvasionSpawner.locateInvasionNearby(
                    bootstrap,
                    bootstrap.getInvasionID()
            );
        } else if (bootstrap.liftSpawnRestrictions) {
            /*
             * LOTR marks invasion candidates with liftSpawnRestrictions while
             * getCanSpawnHere runs, but assigns their invasion UUID only after
             * that check succeeds. This is the reliable pre-UUID discriminator
             * from ordinary biome/conquest bootstraps.
             */
            spawner = findNearbyEligibleSpawner(bootstrap);
        }

        return spawner != null
                && isEligibleInvasion(spawner.getInvasionType())
                && !spawner.getEntityData().getBoolean(
                INVASION_FORMATION_SPAWNED_KEY
        ) && isPersistentlyEligible(spawner)
                ? spawner
                : null;
    }

    public static void markFormationCreated(
            LOTREntityInvasionSpawner spawner
    ) {
        if (spawner != null) {
            spawner.getEntityData().setBoolean(
                    INVASION_FORMATION_SPAWNED_KEY,
                    true
            );
        }
    }

    public static boolean isEligibleInvasion(
            LOTRInvasions invasion
    ) {
        for (int i = 0; i < ELIGIBLE_INVASIONS.length; ++i) {
            if (ELIGIBLE_INVASIONS[i] == invasion) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsBootstrap(
            LOTRInvasions invasion
    ) {
        for (int i = 0; i < invasion.invasionMobs.size(); ++i) {
            LOTRInvasions.InvasionSpawnEntry entry =
                    (LOTRInvasions.InvasionSpawnEntry)
                            invasion.invasionMobs.get(i);
            if (entry.getEntityClass()
                    == LOTREntityMumakilHowdahArcher.class) {
                return true;
            }
        }
        return false;
    }

    private static LOTREntityInvasionSpawner findNearbyEligibleSpawner(
            LOTREntityMumakilHowdahArcher bootstrap
    ) {
        List nearby = bootstrap.worldObj.getEntitiesWithinAABB(
                LOTREntityInvasionSpawner.class,
                bootstrap.boundingBox.expand(
                        INVASION_BOOTSTRAP_LOOKUP_RANGE,
                        INVASION_BOOTSTRAP_LOOKUP_RANGE,
                        INVASION_BOOTSTRAP_LOOKUP_RANGE
                )
        );
        LOTREntityInvasionSpawner nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (int i = 0; i < nearby.size(); ++i) {
            LOTREntityInvasionSpawner candidate =
                    (LOTREntityInvasionSpawner)nearby.get(i);
            if (candidate.isDead
                    || !isEligibleInvasion(candidate.getInvasionType())
                    || candidate.getEntityData().getBoolean(
                    INVASION_FORMATION_SPAWNED_KEY
            )) {
                continue;
            }

            double distanceSq =
                    bootstrap.getDistanceSqToEntity(candidate);
            if (distanceSq < nearestDistanceSq) {
                nearest = candidate;
                nearestDistanceSq = distanceSq;
            }
        }
        return nearest;
    }

    private static boolean isPersistentlyEligible(
            LOTREntityInvasionSpawner spawner
    ) {
        if (spawner == null || spawner.worldObj == null) {
            return false;
        }

        if (!spawner.getEntityData().getBoolean(
                INVASION_FORMATION_ROLL_DECIDED_KEY
        )) {
            boolean eligible = spawner.worldObj.rand.nextInt(
                    MumakilConfig
                            .INVASION_FORMATION_ELIGIBILITY_DENOMINATOR
            ) == 0;
            spawner.getEntityData().setBoolean(
                    INVASION_FORMATION_ELIGIBLE_KEY,
                    eligible
            );
            spawner.getEntityData().setBoolean(
                    INVASION_FORMATION_ROLL_DECIDED_KEY,
                    true
            );
        }
        return spawner.getEntityData().getBoolean(
                INVASION_FORMATION_ELIGIBLE_KEY
        );
    }

    private static String getEligibleInvasionNames() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < ELIGIBLE_INVASIONS.length; ++i) {
            if (i > 0) {
                names.append(',');
            }
            names.append(ELIGIBLE_INVASIONS[i].name());
        }
        return names.toString();
    }
}
