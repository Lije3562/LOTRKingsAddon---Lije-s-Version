package com.enovak.lotrmoremobs.config;

import java.io.File;
import java.util.Arrays;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public final class MumakilConfig {
    public static final String CATEGORY_GAMEPLAY =
            "general gameplay";
    public static final String CATEGORY_SPAWNING =
            "mumak spawning";

    private static final boolean DEFAULT_MORTAL_GANDALF = false;
    private static final int
            DEFAULT_HOME_UNIT_ROLL_DENOMINATOR = 50;
    private static final boolean
            DEFAULT_ENABLE_CONQUEST_FORMATIONS = true;
    private static final float
            DEFAULT_CONQUEST_FORMATION_MINIMUM_CONQUEST = 500.0F;
    private static final int
            DEFAULT_CONQUEST_UNIT_ROLL_DENOMINATOR = 100;
    private static final int
            DEFAULT_CONQUEST_MINIMUM_DENOMINATOR = 50;
    private static final float
            DEFAULT_CONQUEST_STRENGTH_PER_STEP = 100.0F;
    private static final boolean
            DEFAULT_ENABLE_INVASION_FORMATIONS = true;
    private static final int
            DEFAULT_INVASION_UNIT_ROLL_DENOMINATOR = 40;

    private static final int MIN_DENOMINATOR = 1;
    private static final int MAX_DENOMINATOR = 10000;
    private static final float MIN_CONQUEST = 0.0F;
    private static final float MAX_CONQUEST = 100000.0F;
    private static final float MIN_CONQUEST_STEP = 1.0F;
    private static final float MAX_CONQUEST_STEP = 100000.0F;

    private static final int
            DEFAULT_PLACEMENT_RETRY_INTERVAL_TICKS = 20;
    private static final int
            DEFAULT_PLACEMENT_RETRY_TIMEOUT_TICKS = 200;
    private static final int
            DEFAULT_MAX_PLACEMENT_RETRIES = 10;
    private static final int
            DEFAULT_MAX_APPROVED_RECORDS_PROCESSED_PER_TICK = 2;

    private static final String ROLL_RESULT_DESCRIPTION =
            " A denominator of 1 means every otherwise eligible NPC passes "
                    + "its random roll. Placement, capacity, terrain, "
                    + "nearby-entity, loaded-chunk, and transactional factory "
                    + "checks can still preserve the ordinary NPC.";

    public static volatile boolean mortalGandalf =
            DEFAULT_MORTAL_GANDALF;

    public static volatile int homeUnitRollDenominator =
            DEFAULT_HOME_UNIT_ROLL_DENOMINATOR;

    public static volatile boolean
            enableMumakWarFormationsInConquest =
            DEFAULT_ENABLE_CONQUEST_FORMATIONS;

    public static volatile float conquestFormationMinimumConquest =
            DEFAULT_CONQUEST_FORMATION_MINIMUM_CONQUEST;

    public static volatile int conquestUnitRollDenominator =
            DEFAULT_CONQUEST_UNIT_ROLL_DENOMINATOR;

    public static volatile int conquestMinimumDenominator =
            DEFAULT_CONQUEST_MINIMUM_DENOMINATOR;

    public static volatile float conquestStrengthPerStep =
            DEFAULT_CONQUEST_STRENGTH_PER_STEP;

    public static volatile boolean
            enableMumakWarFormationsInInvasions =
            DEFAULT_ENABLE_INVASION_FORMATIONS;

    public static volatile int invasionUnitRollDenominator =
            DEFAULT_INVASION_UNIT_ROLL_DENOMINATOR;

    public static volatile int placementRetryIntervalTicks =
            DEFAULT_PLACEMENT_RETRY_INTERVAL_TICKS;

    public static volatile int placementRetryTimeoutTicks =
            DEFAULT_PLACEMENT_RETRY_TIMEOUT_TICKS;

    public static volatile int maxPlacementRetries =
            DEFAULT_MAX_PLACEMENT_RETRIES;

    public static volatile int maxApprovedRecordsProcessedPerTick =
            DEFAULT_MAX_APPROVED_RECORDS_PROCESSED_PER_TICK;

    /*
     * Invasion-duration value assigned to each formation member.
     *
     * Total formation value:
     * 15 Mumak + 1 driver + 17 archers = 33 invasion points.
     */
    public static final int
            INVASION_MUMAK_BUDGET_VALUE = 15;

    public static final int
            INVASION_DRIVER_BUDGET_VALUE = 1;

    public static final int
            INVASION_ARCHER_BUDGET_VALUE = 1;

    public static final int
            INVASION_FORMATION_BUDGET_VALUE =
            INVASION_MUMAK_BUDGET_VALUE
                    + INVASION_DRIVER_BUDGET_VALUE
                    + 17 * INVASION_ARCHER_BUDGET_VALUE;

    private static Configuration configuration;

    private MumakilConfig() {
    }

    public static synchronized void load(File file) {
        configuration = new Configuration(file);
        configuration.load();
        syncFromConfiguration();
    }

    public static synchronized void syncFromConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException(
                    "Mumak configuration has not been initialized"
            );
        }

        configuration.setCategoryComment(
                CATEGORY_GAMEPLAY,
                "General LOTR addon gameplay settings."
        );
        configuration.setCategoryPropertyOrder(
                CATEGORY_GAMEPLAY,
                Arrays.asList("mortalGandalf")
        );
        mortalGandalf = configuration.getBoolean(
                "mortalGandalf",
                CATEGORY_GAMEPLAY,
                DEFAULT_MORTAL_GANDALF,
                "When true, Gandalf can be damaged and killed by normal "
                        + "Minecraft and LOTR damage sources. When false, "
                        + "the LOTR Mod's standard Gandalf immortality is "
                        + "preserved. Default: false.",
                "config.lotrmoremobs.mortalGandalf"
        );

        configuration.setCategoryComment(
                CATEGORY_SPAWNING,
                "Server-authoritative Mumak-with-howdah formation spawning. "
                        + "The integrated server sees saved changes "
                        + "immediately; a remote dedicated server continues "
                        + "to use its own config file."
        );
        configuration.setCategoryPropertyOrder(
                CATEGORY_SPAWNING,
                Arrays.asList(
                        "homeUnitRollDenominator",
                        "enableMumakWarFormationsInConquest",
                        "conquestFormationMinimumConquest",
                        "conquestUnitRollDenominator",
                        "conquestMinimumDenominator",
                        "conquestStrengthPerStep",
                        "enableMumakWarFormationsInInvasions",
                        "invasionUnitRollDenominator",
                        "placementRetryIntervalTicks",
                        "placementRetryTimeoutTicks",
                        "maxPlacementRetries",
                        "maxApprovedRecordsProcessedPerTick"
                )
        );

        homeUnitRollDenominator = configuration.getInt(
                "homeUnitRollDenominator",
                CATEGORY_SPAWNING,
                DEFAULT_HOME_UNIT_ROLL_DENOMINATOR,
                MIN_DENOMINATOR,
                MAX_DENOMINATOR,
                "One independent 1/N replacement roll per eligible native "
                        + "Near Harad military NPC. Lower values increase "
                        + "formation frequency; higher values reduce it."
                        + ROLL_RESULT_DESCRIPTION
                        + " Restart required: No.",
                "config.lotrmoremobs.homeUnitRollDenominator"
        );

        enableMumakWarFormationsInConquest =
                configuration.getBoolean(
                        "enableMumakWarFormationsInConquest",
                        CATEGORY_SPAWNING,
                        DEFAULT_ENABLE_CONQUEST_FORMATIONS,
                        "Controls whether Mumak-with-howdah formations may "
                                + "spawn through Near Harad conquest spawning "
                                + "at or above the configured conquest "
                                + "threshold. Default: enabled. Restart "
                                + "required: No.",
                        "config.lotrmoremobs.enableConquestFormations"
                );

        conquestFormationMinimumConquest = configuration.getFloat(
                "conquestFormationMinimumConquest",
                CATEGORY_SPAWNING,
                DEFAULT_CONQUEST_FORMATION_MINIMUM_CONQUEST,
                MIN_CONQUEST,
                MAX_CONQUEST,
                "Minimum direct Near Harad conquest required at the NPC's "
                        + "actual coordinates before a conquest replacement "
                        + "roll is allowed. Lower values allow formations "
                        + "sooner; higher values require more conquest. "
                        + "Restart required: No.",
                "config.lotrmoremobs.conquestMinimum"
        );

        conquestUnitRollDenominator = configuration.getInt(
                "conquestUnitRollDenominator",
                CATEGORY_SPAWNING,
                DEFAULT_CONQUEST_UNIT_ROLL_DENOMINATOR,
                MIN_DENOMINATOR,
                MAX_DENOMINATOR,
                "Base 1/N replacement denominator for each eligible "
                        + "Near Harad conquest military NPC. Lower values "
                        + "increase formation frequency; higher values reduce "
                        + "it."
                        + ROLL_RESULT_DESCRIPTION
                        + " Restart required: No.",
                "config.lotrmoremobs.conquestBaseDenominator"
        );

        conquestMinimumDenominator = configuration.getInt(
                "conquestMinimumDenominator",
                CATEGORY_SPAWNING,
                DEFAULT_CONQUEST_MINIMUM_DENOMINATOR,
                MIN_DENOMINATOR,
                MAX_DENOMINATOR,
                "Lowest denominator conquest scaling may reach. This value "
                        + "is clamped so it cannot exceed the conquest base "
                        + "denominator. Lower values permit a higher maximum "
                        + "spawn frequency. Restart required: No.",
                "config.lotrmoremobs.conquestMinimumDenominator"
        );

        conquestStrengthPerStep = configuration.getFloat(
                "conquestStrengthPerStep",
                CATEGORY_SPAWNING,
                DEFAULT_CONQUEST_STRENGTH_PER_STEP,
                MIN_CONQUEST_STEP,
                MAX_CONQUEST_STEP,
                "Direct Near Harad conquest above the minimum required to "
                        + "reduce the conquest denominator by one. Lower "
                        + "values increase frequency more quickly as conquest "
                        + "rises; higher values scale more slowly. Restart "
                        + "required: No.",
                "config.lotrmoremobs.conquestStrengthPerStep"
        );

        enableMumakWarFormationsInInvasions =
                configuration.getBoolean(
                        "enableMumakWarFormationsInInvasions",
                        CATEGORY_SPAWNING,
                        DEFAULT_ENABLE_INVASION_FORMATIONS,
                        "Controls whether ordinary units from eligible Near "
                                + "Harad invasions may be replaced by "
                                + "Mumak-with-howdah formations. Default: "
                                + "enabled. Restart required: No.",
                        "config.lotrmoremobs.enableInvasionFormations"
                );

        invasionUnitRollDenominator = configuration.getInt(
                "invasionUnitRollDenominator",
                CATEGORY_SPAWNING,
                DEFAULT_INVASION_UNIT_ROLL_DENOMINATOR,
                MIN_DENOMINATOR,
                MAX_DENOMINATOR,
                "One independent 1/N replacement roll per eligible ordinary "
                        + "NPC in a configured Near Harad invasion. Lower "
                        + "values increase formation frequency; higher values "
                        + "reduce it."
                        + ROLL_RESULT_DESCRIPTION
                        + " Restart required: No.",
                "config.lotrmoremobs.invasionUnitRollDenominator"
        );

        placementRetryIntervalTicks = configuration.getInt(
                "placementRetryIntervalTicks",
                CATEGORY_SPAWNING,
                DEFAULT_PLACEMENT_RETRY_INTERVAL_TICKS,
                1,
                1200,
                "Ticks between attempts for a roll-approved replacement "
                        + "record. Minecraft runs at 20 ticks per second, so "
                        + "the default 20 ticks is approximately 1 second. "
                        + "Lower values retry sooner and use more processing; "
                        + "higher values retry less often. Restart required: "
                        + "No.",
                "config.lotrmoremobs.placementRetryInterval"
        );

        placementRetryTimeoutTicks = configuration.getInt(
                "placementRetryTimeoutTicks",
                CATEGORY_SPAWNING,
                DEFAULT_PLACEMENT_RETRY_TIMEOUT_TICKS,
                20,
                12000,
                "Maximum lifetime of a roll-approved placement record, in "
                        + "ticks. Minecraft runs at 20 ticks per second, so "
                        + "the default 200 ticks is approximately 10 seconds. "
                        + "Higher values allow more time for placement to "
                        + "become available. Restart required: No.",
                "config.lotrmoremobs.placementRetryTimeout"
        );

        maxPlacementRetries = configuration.getInt(
                "maxPlacementRetries",
                CATEGORY_SPAWNING,
                DEFAULT_MAX_PLACEMENT_RETRIES,
                1,
                100,
                "Maximum actual placement searches for one roll-approved "
                        + "record, including its first placement search. "
                        + "Higher values provide more opportunities but can "
                        + "perform more work. Probability is never rerolled. "
                        + "Restart required: No.",
                "config.lotrmoremobs.maxPlacementRetries"
        );

        maxApprovedRecordsProcessedPerTick = configuration.getInt(
                "maxApprovedRecordsProcessedPerTick",
                CATEGORY_SPAWNING,
                DEFAULT_MAX_APPROVED_RECORDS_PROCESSED_PER_TICK,
                1,
                20,
                "Maximum roll-approved retry records processed in one world "
                        + "tick. Higher values drain busy retry queues faster "
                        + "but may increase server-tick cost. Restart "
                        + "required: No.",
                "config.lotrmoremobs.maxApprovedRetriesPerTick"
        );

        markPropertiesRuntimeEditable(
                CATEGORY_GAMEPLAY,
                "mortalGandalf"
        );
        markPropertiesRuntimeEditable(
                CATEGORY_SPAWNING,
                "homeUnitRollDenominator",
                "enableMumakWarFormationsInConquest",
                "conquestFormationMinimumConquest",
                "conquestUnitRollDenominator",
                "conquestMinimumDenominator",
                "conquestStrengthPerStep",
                "enableMumakWarFormationsInInvasions",
                "invasionUnitRollDenominator",
                "placementRetryIntervalTicks",
                "placementRetryTimeoutTicks",
                "maxPlacementRetries",
                "maxApprovedRecordsProcessedPerTick"
        );

        if (conquestMinimumDenominator
                > conquestUnitRollDenominator) {
            conquestMinimumDenominator =
                    conquestUnitRollDenominator;
            configuration.get(
                    CATEGORY_SPAWNING,
                    "conquestMinimumDenominator",
                    DEFAULT_CONQUEST_MINIMUM_DENOMINATOR
            ).set(conquestMinimumDenominator);
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }

        printSummary();
    }

    private static void markPropertiesRuntimeEditable(
            String categoryName,
            String... propertyNames
    ) {
        ConfigCategory category =
                configuration.getCategory(categoryName);
        for (int i = 0; i < propertyNames.length; ++i) {
            Property property = category.get(propertyNames[i]);
            if (property != null) {
                property.setRequiresMcRestart(false);
                property.setRequiresWorldRestart(false);
            }
        }
    }

    public static Configuration getConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException(
                    "Mumak configuration has not been initialized"
            );
        }
        return configuration;
    }

    private static void printSummary() {
        System.out.println(
                "[LOTRMoreMobs] Mumak formation config:"
                        + " mortalGandalf="
                        + mortalGandalf
                        + " homeUnitRoll=1/"
                        + homeUnitRollDenominator
                        + " conquest="
                        + enableMumakWarFormationsInConquest
                        + " conquestUnitRoll=1/"
                        + conquestUnitRollDenominator
                        + " conquestMinimumDenominator=1/"
                        + conquestMinimumDenominator
                        + " conquestMinimum="
                        + conquestFormationMinimumConquest
                        + " conquestStrengthPerStep="
                        + conquestStrengthPerStep
                        + " invasions="
                        + enableMumakWarFormationsInInvasions
                        + " invasionUnitRoll=1/"
                        + invasionUnitRollDenominator
                        + " placementRetryInterval="
                        + placementRetryIntervalTicks
                        + " placementRetryTimeout="
                        + placementRetryTimeoutTicks
                        + " maxPlacementRetries="
                        + maxPlacementRetries
                        + " maxApprovedPerTick="
                        + maxApprovedRecordsProcessedPerTick
                        + " invasionLimit=unlimited"
                        + " invasionFormationBudget="
                        + INVASION_FORMATION_BUDGET_VALUE
        );
    }
}
