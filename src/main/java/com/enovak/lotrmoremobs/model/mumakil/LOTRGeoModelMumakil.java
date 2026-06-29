package com.enovak.lotrmoremobs.model.mumakil;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * Experimental GeckoLib model provider for rendering the exported Blockbench/Bedrock geometry directly.
 *
 * This branch intentionally keeps the Mumakil static. The goal is to test whether the original .geo.json
 * geometry and matching texture solve the UV bleed without passing through the Java ModelRenderer conversion.
 */
public class LOTRGeoModelMumakil extends AnimatedGeoModel<LOTREntityMumakil> {
    public static final ResourceLocation PLAIN_MODEL =
            new ResourceLocation("lotrmoremobs", "geo/entity/mumakil/LOTRMumakilModel.geo.json");
    public static final ResourceLocation WAR_MODEL =
            new ResourceLocation("lotrmoremobs", "geo/entity/mumakil/LOTRMumakilWarModel.geo.json");
    public static final ResourceLocation WILD_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_wild.png");
    public static final ResourceLocation SADDLED_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_saddled.png");
    public static final ResourceLocation WAR_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");

    private static final ResourceLocation ANIMATION =
            new ResourceLocation("lotrmoremobs", "animations/entity/mumakil/LOTRMumakil.animations.json");

    private static final String[] BOOLEAN_ARMOR_METHODS = new String[] {
            "isMountArmored",
            "isMountArmorEquipped",
            "hasMountArmor",
            "isHorseArmored",
            "hasHorseArmor",
            "isArmored"
    };
    private static final String[] NUMERIC_ARMOR_METHODS = new String[] {
            "getHorseArmorIndex",
            "getMountArmorIndex",
            "getArmorIndex",
            "func_110241_cb"
    };
    private static final String[] ITEM_ARMOR_METHODS = new String[] {
            "getMountArmor",
            "getMountArmorItem",
            "getMountArmorItemStack",
            "getHorseArmor",
            "getHorseArmorItem",
            "getArmorItem",
            "getArmorItemStack"
    };
    private static final String[] ARMOR_FIELDS = new String[] {
            "mountArmor",
            "mountArmorItem",
            "horseArmor",
            "armorItem",
            "armor"
    };
    private static final String[] INVENTORY_FIELDS = new String[] {
            "horseChest",
            "mountInventory",
            "horseInventory",
            "inventory"
    };
    private static final String[] BOOLEAN_SADDLE_METHODS = new String[] {
            "isMountSaddled",
            "isHorseSaddled",
            "isSaddled",
            "getSaddled",
            "func_110257_ck"
    };

    private static final String[] ITEM_SADDLE_METHODS = new String[] {
            "getSaddle",
            "getSaddleItem",
            "getSaddleItemStack",
            "getMountSaddle",
            "getHorseSaddle"
    };

    private static final String[] SADDLE_FIELDS = new String[] {
            "saddle",
            "saddleItem",
            "mountSaddle",
            "horseSaddle"
    };

    @Override
    public ResourceLocation getAnimationFileLocation(LOTREntityMumakil entity) {
        return ANIMATION;
    }

    @Override
    public ResourceLocation getModelLocation(LOTREntityMumakil entity) {
        return PLAIN_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(LOTREntityMumakil entity) {
        boolean saddle = shouldRenderSaddle(entity);
        boolean warEquipment = shouldRenderHowdahOrWarEquipment(entity);

        if (saddle && warEquipment) {
            return WAR_TEXTURE;
        }

        if (saddle) {
            return SADDLED_TEXTURE;
        }

        return WILD_TEXTURE;
    }

    public static boolean shouldRenderSaddle(LOTREntityMumakil entity) {
        return detectSaddleState(entity).equipped;
    }

    public static String getSaddleDebugValue(LOTREntityMumakil entity) {
        return detectSaddleState(entity).debugValue;
    }

    private static ArmorState detectSaddleState(LOTREntityMumakil entity) {
        if (entity == null) {
            return new ArmorState(false, "entity=null");
        }

        ArmorState state = findSaddleStateFromMethods(entity, BOOLEAN_SADDLE_METHODS);
        if (state.equipped) {
            return state;
        }

        state = findSaddleStateFromMethods(entity, ITEM_SADDLE_METHODS);
        if (state.equipped) {
            return state;
        }

        state = findSaddleStateFromFields(entity, SADDLE_FIELDS);
        if (state.equipped) {
            return state;
        }

        state = findSaddleStateFromInventoryFields(entity);
        if (state.equipped) {
            return state;
        }

        return new ArmorState(false, state.debugValue == null ? "none" : state.debugValue);
    }

    public static boolean shouldRenderHowdahOrWarEquipment(LOTREntityMumakil entity) {
        return entity != null && shouldRenderSaddle(entity) && detectHowdahOrWarEquipmentState(entity).equipped;
    }

    public static String getHowdahOrWarEquipmentDebugValue(LOTREntityMumakil entity) {
        return detectHowdahOrWarEquipmentState(entity).debugValue;
    }

    private static ArmorState detectHowdahOrWarEquipmentState(LOTREntityMumakil entity) {
        if (entity == null) {
            return new ArmorState(false, "entity=null");
        }

        String firstObservedState = null;
        ArmorState state = findStateFromMethods(entity, BOOLEAN_ARMOR_METHODS);
        if (state.equipped) {
            return state;
        }
        firstObservedState = firstObservedState == null ? state.debugValue : firstObservedState;

        state = findStateFromMethods(entity, NUMERIC_ARMOR_METHODS);
        if (state.equipped) {
            return state;
        }
        firstObservedState = firstObservedState == null ? state.debugValue : firstObservedState;

        state = findStateFromMethods(entity, ITEM_ARMOR_METHODS);
        if (state.equipped) {
            return state;
        }
        firstObservedState = firstObservedState == null ? state.debugValue : firstObservedState;

        state = findStateFromFields(entity, ARMOR_FIELDS);
        if (state.equipped) {
            return state;
        }
        firstObservedState = firstObservedState == null ? state.debugValue : firstObservedState;

        state = findStateFromInventoryFields(entity);
        if (state.equipped) {
            return state;
        }
        firstObservedState = firstObservedState == null ? state.debugValue : firstObservedState;

        return new ArmorState(false, firstObservedState == null ? "none" : firstObservedState);
    }

    private static ArmorState findStateFromMethods(LOTREntityMumakil entity, String[] methodNames) {
        String firstObservedState = null;
        for (int i = 0; i < methodNames.length; ++i) {
            String methodName = methodNames[i];
            Method method = findNoArgMethod(entity.getClass(), methodName);
            if (method != null) {
                try {
                    Object value = method.invoke(entity);
                    ArmorState state = stateFromValue("method " + methodName, value);
                    if (state.equipped) {
                        return state;
                    }
                    if (firstObservedState == null) {
                        firstObservedState = state.debugValue;
                    }
                } catch (Exception e) {
                    if (firstObservedState == null) {
                        firstObservedState = "method " + methodName + "=<error>";
                    }
                }
            }
        }
        return new ArmorState(false, firstObservedState);
    }

    private static ArmorState findStateFromFields(LOTREntityMumakil entity, String[] fieldNames) {
        String firstObservedState = null;
        for (int i = 0; i < fieldNames.length; ++i) {
            String fieldName = fieldNames[i];
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                try {
                    Object value = field.get(entity);
                    ArmorState state = stateFromValue("field " + fieldName, value);
                    if (state.equipped) {
                        return state;
                    }
                    if (firstObservedState == null) {
                        firstObservedState = state.debugValue;
                    }
                } catch (Exception e) {
                    if (firstObservedState == null) {
                        firstObservedState = "field " + fieldName + "=<error>";
                    }
                }
            }
        }
        return new ArmorState(false, firstObservedState);
    }

    private static ArmorState findStateFromInventoryFields(LOTREntityMumakil entity) {
        String firstObservedState = null;
        for (int i = 0; i < INVENTORY_FIELDS.length; ++i) {
            String fieldName = INVENTORY_FIELDS[i];
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                try {
                    Object value = field.get(entity);
                    if (value instanceof IInventory) {
                        IInventory inventory = (IInventory)value;
                        if (inventory.getSizeInventory() > 1) {
                            ArmorState state = stateFromValue("inventory " + fieldName + "[1]", inventory.getStackInSlot(1));
                            if (state.equipped) {
                                return state;
                            }
                            if (firstObservedState == null) {
                                firstObservedState = state.debugValue;
                            }
                        }
                    }
                } catch (Exception e) {
                    if (firstObservedState == null) {
                        firstObservedState = "inventory " + fieldName + "=<error>";
                    }
                }
            }
        }
        return new ArmorState(false, firstObservedState);
    }

    private static ArmorState findSaddleStateFromMethods(LOTREntityMumakil entity, String[] methodNames) {
        String firstObservedState = null;

        for (int i = 0; i < methodNames.length; ++i) {
            String methodName = methodNames[i];
            Method method = findNoArgMethod(entity.getClass(), methodName);
            if (method != null) {
                try {
                    Object value = method.invoke(entity);
                    ArmorState state = saddleStateFromValue("method " + methodName, value);
                    if (state.equipped) {
                        return state;
                    }
                    if (firstObservedState == null) {
                        firstObservedState = state.debugValue;
                    }
                } catch (Exception e) {
                    if (firstObservedState == null) {
                        firstObservedState = "method " + methodName + "=<error>";
                    }
                }
            }
        }

        return new ArmorState(false, firstObservedState);
    }

    private static ArmorState findSaddleStateFromFields(LOTREntityMumakil entity, String[] fieldNames) {
        String firstObservedState = null;

        for (int i = 0; i < fieldNames.length; ++i) {
            String fieldName = fieldNames[i];
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                try {
                    Object value = field.get(entity);
                    ArmorState state = saddleStateFromValue("field " + fieldName, value);
                    if (state.equipped) {
                        return state;
                    }
                    if (firstObservedState == null) {
                        firstObservedState = state.debugValue;
                    }
                } catch (Exception e) {
                    if (firstObservedState == null) {
                        firstObservedState = "field " + fieldName + "=<error>";
                    }
                }
            }
        }

        return new ArmorState(false, firstObservedState);
    }

    private static ArmorState findSaddleStateFromInventoryFields(LOTREntityMumakil entity) {
        String firstObservedState = null;

        for (int i = 0; i < INVENTORY_FIELDS.length; ++i) {
            String fieldName = INVENTORY_FIELDS[i];
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                try {
                    Object value = field.get(entity);
                    if (value instanceof IInventory) {
                        IInventory inventory = (IInventory)value;

                        if (inventory.getSizeInventory() > 0) {
                            ArmorState state = saddleStateFromValue("inventory " + fieldName + "[0]", inventory.getStackInSlot(0));
                            if (state.equipped) {
                                return state;
                            }
                            if (firstObservedState == null) {
                                firstObservedState = state.debugValue;
                            }
                        }
                    }
                } catch (Exception e) {
                    if (firstObservedState == null) {
                        firstObservedState = "inventory " + fieldName + "=<error>";
                    }
                }
            }
        }

        return new ArmorState(false, firstObservedState);
    }

    private static ArmorState saddleStateFromValue(String source, Object value) {
        if (value == null) {
            return new ArmorState(false, source + "=null");
        }

        if (value instanceof Boolean) {
            boolean equipped = ((Boolean)value).booleanValue();
            return new ArmorState(equipped, source + "=" + value);
        }

        if (value instanceof Number) {
            int index = ((Number)value).intValue();
            return new ArmorState(index > 0, source + "=" + index);
        }

        if (value instanceof ItemStack) {
            ItemStack stack = (ItemStack)value;
            boolean equipped = stack != null && stack.getItem() == Items.saddle;
            return new ArmorState(equipped, source + "=" + describeStack(stack));
        }

        if (value instanceof Item) {
            Item item = (Item)value;
            return new ArmorState(item == Items.saddle, source + "=" + item);
        }

        return new ArmorState(false, source + "=" + value);
    }

    private static ArmorState stateFromValue(String source, Object value) {
        if (value == null) {
            return new ArmorState(false, source + "=null");
        }
        if (value instanceof Boolean) {
            boolean equipped = ((Boolean)value).booleanValue();
            return new ArmorState(equipped, source + "=" + value);
        }
        if (value instanceof Number) {
            int armorIndex = ((Number)value).intValue();
            return new ArmorState(armorIndex > 0, source + "=" + armorIndex);
        }
        if (value instanceof ItemStack) {
            ItemStack stack = (ItemStack)value;
            return new ArmorState(isNonSaddleStack(stack), source + "=" + describeStack(stack));
        }
        if (value instanceof Item) {
            Item item = (Item)value;
            return new ArmorState(item != Items.saddle, source + "=" + item);
        }
        return new ArmorState(false, source + "=" + value);
    }

    private static boolean isNonSaddleStack(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.getItem() != Items.saddle;
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null) {
            return "empty";
        }
        return String.valueOf(stack.getItem()) + "x" + stack.stackSize;
    }

    private static Method findNoArgMethod(Class type, String name) {
        Class current = type;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name);
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

    private static Field findField(Class type, String name) {
        Class current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * GeckoLib-Unofficial's AnimatedGeoModel#setLivingAnimations calls its AnimationProcessor and Molang setup.
     * In this ForgeGradle 1.2 / Minecraft 1.7.10 deobfuscated runtime that path still references obfuscated
     * World.field_72996_f, causing a NoSuchFieldError while rendering. For this UV experiment we do not need
     * animations, so leave the baked Geo model in its exported static pose and skip the unsafe setup entirely.
     */
    @Override
    public void setLivingAnimations(LOTREntityMumakil entity, Integer uniqueID, AnimationEvent customPredicate) {
    }

    private static final class ArmorState {
        private final boolean equipped;
        private final String debugValue;

        private ArmorState(boolean equipped, String debugValue) {
            this.equipped = equipped;
            this.debugValue = debugValue;
        }
    }
}
