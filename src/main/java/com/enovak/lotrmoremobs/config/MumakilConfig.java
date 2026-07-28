package com.enovak.lotrmoremobs.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class MumakilConfig {
    public static final String CATEGORY_SPAWNING = "mumak spawning";

    public static final int HOME_MILITARY_SPAWN_BASE_DENOMINATOR = 150;
    public static final float CONQUEST_MINIMUM_EFFECTIVE_STRENGTH = 500.0F;
    public static final int CONQUEST_BASE_DENOMINATOR = 150;
    public static final int CONQUEST_MAX_PROBABILITY_DENOMINATOR = 100;
    public static final int INVASION_FORMATION_ELIGIBILITY_DENOMINATOR = 3;
    public static final int INVASION_FORMATION_ENTRY_WEIGHT = 2;
    public static final int MAX_FORMATIONS_PER_INVASION = 1;
    public static final int INVASION_MUMAK_BUDGET_VALUE = 8;
    public static final int INVASION_DRIVER_BUDGET_VALUE = 5;
    public static final int INVASION_ARCHER_BUDGET_VALUE = 1;
    public static final int INVASION_FORMATION_BUDGET_VALUE =
            INVASION_MUMAK_BUDGET_VALUE
                    + INVASION_DRIVER_BUDGET_VALUE
                    + 17 * INVASION_ARCHER_BUDGET_VALUE;

    public static boolean enableMumakWarFormationsInConquest = true;
    public static boolean enableMumakWarFormationsInInvasions = true;

    private static Configuration configuration;

    private MumakilConfig() {
    }

    public static void load(File file) {
        configuration = new Configuration(file);
        configuration.load();
        enableMumakWarFormationsInConquest =
                configuration.getBoolean(
                        "enableMumakWarFormationsInConquest",
                        CATEGORY_SPAWNING,
                        true,
                        "Controls whether Mumak-with-howdah formations may "
                                + "spawn through Near Harad conquest spawning "
                                + "at or above the configured conquest threshold."
                );
        enableMumakWarFormationsInInvasions =
                configuration.getBoolean(
                        "enableMumakWarFormationsInInvasions",
                        CATEGORY_SPAWNING,
                        true,
                        "Controls whether Mumak-with-howdah formations may "
                                + "spawn as part of eligible Near Harad invasions."
                );
        if (configuration.hasChanged()) {
            configuration.save();
        }

        System.out.println(
                "[LOTRMoreMobs] Mumak formation config: conquest="
                        + enableMumakWarFormationsInConquest
                        + " invasions="
                        + enableMumakWarFormationsInInvasions
        );
    }
}
