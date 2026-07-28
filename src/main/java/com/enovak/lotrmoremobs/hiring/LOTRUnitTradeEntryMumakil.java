package com.enovak.lotrmoremobs.hiring;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.animal.MumakilFormationOrigin;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHirePreviewDriver;
import com.enovak.lotrmoremobs.handler.MumakilAchievementEventHandler;
import com.enovak.lotrmoremobs.spawning.MumakilWarFormationFactory;
import java.lang.reflect.Method;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.entity.npc.LOTREntitySouthronChampion;
import lotr.common.entity.npc.LOTRUnitTradeEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public class LOTRUnitTradeEntryMumakil extends LOTRUnitTradeEntry {
    public static final int MUMAKIL_HIRE_COST = 1000;
    public static final float MUMAKIL_ALIGNMENT_REQUIRED = 2000.0F;

    private static final float HIRE_GUI_DISPLAY_WIDTH = 14.0F;
    private static final float HIRE_GUI_DISPLAY_HEIGHT = 30.0F;

    public LOTRUnitTradeEntryMumakil() {
        super(
                LOTREntitySouthronChampion.class,
                LOTREntityMumakil.class,
                "Mumakil_Howdah",
                MUMAKIL_HIRE_COST,
                MUMAKIL_ALIGNMENT_REQUIRED
        );
        this.setMountArmor(Main.mumakilHowdah, 1.0F);
        this.setPledgeExclusive();
        this.setExtraInfo("Mumakil_Howdah");
    }

    @Override
    public LOTREntityNPC getOrCreateHiredNPC(World world) {
        if (!world.isRemote) {
            return super.getOrCreateHiredNPC(world);
        }

        LOTREntityMumakilHirePreviewDriver driver = new LOTREntityMumakilHirePreviewDriver(world);
        driver.initCreatureForHire(null);
        driver.refreshCurrentAttackMode();
        driver.setCurrentItemOrArmor(0, null);
        return driver;
    }

    @Override
    public EntityLiving createHiredMount(World world) {
        LOTREntityMumakil mumakil = new LOTREntityMumakil(world);
        mumakil.onSpawnWithEgg(null);
        if (!MumakilWarFormationFactory.initializeMount(
                mumakil,
                MumakilFormationOrigin.PLAYER_HIRED
        )) {
            System.out.println(
                    "[LOTRMoreMobs] Could not fully equip player-hired Mumak."
            );
        }

        if (world.isRemote) {
            this.applyHireGuiDisplayScale(mumakil);
            mumakil.setMumakilHowdahPreviewEquipped(true);
        } else {
            /*
             * The mount created by this server-side native trade path is the
             * only formation eligible for the hiring achievement. The event
             * handler waits until the paid driver and all seventeen archers
             * exist, so failed transactions and old loaded formations cannot
             * award it.
             */
            MumakilAchievementEventHandler
                    .markHiringAchievementPending(mumakil);
        }

        return mumakil;
    }

    private void applyHireGuiDisplayScale(LOTREntityMumakil mumakil) {
        /*
         * LOTR's hire GUI computes preview size from entity width/height.
         * This only runs for the temporary client-side GUI preview entity.
         */
        Method setSize = this.findMethod(Entity.class, "setSize", new Class[] { Float.TYPE, Float.TYPE });
        if (setSize == null) {
            setSize = this.findMethod(Entity.class, "func_70105_a", new Class[] { Float.TYPE, Float.TYPE });
        }

        if (setSize != null) {
            try {
                setSize.invoke(mumakil, new Object[] {
                        Float.valueOf(HIRE_GUI_DISPLAY_WIDTH),
                        Float.valueOf(HIRE_GUI_DISPLAY_HEIGHT)
                });
            } catch (Exception e) {
            }
        }
    }

    private Method findMethod(Class type, String name, Class[] parameterTypes) {
        Class current = type;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

}
