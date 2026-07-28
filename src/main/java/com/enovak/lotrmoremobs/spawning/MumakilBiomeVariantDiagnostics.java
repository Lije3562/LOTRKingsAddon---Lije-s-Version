package com.enovak.lotrmoremobs.spawning;



import java.lang.reflect.Array;

import java.lang.reflect.Field;

import java.lang.reflect.Method;

import java.lang.reflect.Modifier;

import java.util.Collection;

import java.util.Iterator;

import java.util.Locale;

import java.util.Map;

import net.minecraft.world.biome.BiomeGenBase;



/**

 * Temporary runtime inspection for LOTR v36.15 biome fields and terrain

 * variants relevant to Mumakil natural spawning.

 *

 * MUMAKIL_BIOME_VARIANT_DIAGNOSTICS_V2

 */

public final class MumakilBiomeVariantDiagnostics {

    private static final String PREFIX =

            "[LOTRMoreMobs][BiomeVariantDiagnostics] ";



    private static boolean dumped;



    private MumakilBiomeVariantDiagnostics() {

    }



    public static void dump() {

        if (dumped) {

            return;

        }



        dumped = true;



        System.out.println(PREFIX + "BEGIN");



        dumpRelevantBiomeFields();

        dumpAllBiomeVariantDefinitions();



        System.out.println(PREFIX + "END");

    }



    private static void dumpRelevantBiomeFields() {

        try {

            Class biomeClass = Class.forName(

                    "lotr.common.world.biome.LOTRBiome"

            );

            Field[] fields = biomeClass.getDeclaredFields();



            for (int i = 0; i < fields.length; ++i) {

                Field field = fields[i];



                if (!Modifier.isStatic(field.getModifiers())

                        || !BiomeGenBase.class.isAssignableFrom(

                        field.getType()

                )) {

                    continue;

                }



                field.setAccessible(true);

                Object value = field.get(null);



                if (!(value instanceof BiomeGenBase)) {

                    continue;

                }



                BiomeGenBase biome = (BiomeGenBase)value;

                String identity = normalize(field.getName())

                        + normalize(biome.biomeName)

                        + normalize(biome.getClass().getName());



                if (!isRelevantSouthernIdentity(identity)) {

                    continue;

                }



                System.out.println(

                        PREFIX

                                + "BIOME"

                                + " source=" + field.getName()

                                + " id=" + biome.biomeID

                                + " name=" + biome.biomeName

                                + " class="

                                + biome.getClass().getName()

                );



                dumpVariantMembers(

                        "BIOME_MEMBER source=" + field.getName(),

                        biome

                );

                dumpVariantMethods(

                        "BIOME_METHOD source=" + field.getName(),

                        biome

                );

            }

        } catch (Throwable t) {

            System.err.println(

                    PREFIX + "BIOME_INSPECTION_ERROR " + t

            );

        }

    }



    private static void dumpAllBiomeVariantDefinitions() {

        try {

            Class variantClass = Class.forName(

                    "lotr.common.world.biome.variant.LOTRBiomeVariant"

            );

            Field[] fields = variantClass.getDeclaredFields();

            int count = 0;



            for (int i = 0; i < fields.length; ++i) {

                Field field = fields[i];



                if (!Modifier.isStatic(field.getModifiers())

                        || !variantClass.isAssignableFrom(

                        field.getType()

                )) {

                    continue;

                }



                field.setAccessible(true);

                Object variant = field.get(null);



                if (variant == null) {

                    continue;

                }



                ++count;



                System.out.println(

                        PREFIX

                                + "VARIANT"

                                + " source=" + field.getName()

                                + " class="

                                + variant.getClass().getName()

                                + " details="

                                + describeSimpleMembers(variant)

                );

            }



            System.out.println(

                    PREFIX + "VARIANT_COUNT=" + count

            );

        } catch (Throwable t) {

            System.err.println(

                    PREFIX + "VARIANT_INSPECTION_ERROR " + t

            );

        }

    }



    private static void dumpVariantMembers(

            String label,

            Object object

    ) {

        Class current = object.getClass();



        while (current != null && current != Object.class) {

            Field[] fields = current.getDeclaredFields();



            for (int i = 0; i < fields.length; ++i) {

                Field field = fields[i];



                if (Modifier.isStatic(field.getModifiers())

                        || !normalize(field.getName())

                        .contains("variant")) {

                    continue;

                }



                try {

                    field.setAccessible(true);

                    Object value = field.get(object);



                    System.out.println(

                            PREFIX

                                    + label

                                    + " field="

                                    + current.getName()

                                    + "#"

                                    + field.getName()

                                    + " value="

                                    + describeValue(value)

                    );

                } catch (Throwable t) {

                    System.out.println(

                            PREFIX

                                    + label

                                    + " field="

                                    + current.getName()

                                    + "#"

                                    + field.getName()

                                    + " error=" + t

                    );

                }

            }



            current = current.getSuperclass();

        }

    }



    private static void dumpVariantMethods(

            String label,

            Object object

    ) {

        Class current = object.getClass();



        while (current != null && current != Object.class) {

            Method[] methods = current.getDeclaredMethods();



            for (int i = 0; i < methods.length; ++i) {

                Method method = methods[i];

                String methodKey = normalize(method.getName());



                if (Modifier.isStatic(method.getModifiers())

                        || method.getParameterTypes().length != 0

                        || method.getReturnType() == Void.TYPE

                        || !methodKey.contains("variant")) {

                    continue;

                }



                try {

                    method.setAccessible(true);

                    Object value = method.invoke(object);



                    System.out.println(

                            PREFIX

                                    + label

                                    + " method="

                                    + current.getName()

                                    + "#"

                                    + method.getName()

                                    + " value="

                                    + describeValue(value)

                    );

                } catch (Throwable t) {

                    System.out.println(

                            PREFIX

                                    + label

                                    + " method="

                                    + current.getName()

                                    + "#"

                                    + method.getName()

                                    + " error=" + t

                    );

                }

            }



            current = current.getSuperclass();

        }

    }



    private static String describeSimpleMembers(Object object) {

        StringBuilder result = new StringBuilder();

        Class current = object.getClass();

        boolean first = true;



        while (current != null && current != Object.class) {

            Field[] fields = current.getDeclaredFields();



            for (int i = 0; i < fields.length; ++i) {

                Field field = fields[i];



                if (Modifier.isStatic(field.getModifiers())) {

                    continue;

                }



                try {

                    field.setAccessible(true);

                    Object value = field.get(object);



                    if (!isSimpleValue(value)) {

                        continue;

                    }



                    if (!first) {

                        result.append(",");

                    }



                    result.append(current.getSimpleName())

                            .append("#")

                            .append(field.getName())

                            .append("=")

                            .append(String.valueOf(value));



                    first = false;

                } catch (Throwable ignored) {

                }

            }



            current = current.getSuperclass();

        }



        return result.length() == 0

                ? "(no simple fields found)"

                : result.toString();

    }



    private static String describeValue(Object value) {

        if (value == null) {

            return "null";

        }



        if (isSimpleValue(value)) {

            return String.valueOf(value);

        }



        Class valueClass = value.getClass();



        if (valueClass.isArray()) {

            int length = Array.getLength(value);

            StringBuilder result = new StringBuilder();

            result.append("array[").append(length).append("]{");



            for (int i = 0; i < length; ++i) {

                if (i > 0) {

                    result.append(",");

                }



                result.append(describeValue(Array.get(value, i)));

            }



            return result.append("}").toString();

        }



        if (value instanceof Collection) {

            Collection collection = (Collection)value;

            StringBuilder result = new StringBuilder();

            result.append("collection[")

                    .append(collection.size())

                    .append("]{");



            Iterator iterator = collection.iterator();

            boolean first = true;



            while (iterator.hasNext()) {

                if (!first) {

                    result.append(",");

                }



                result.append(describeValue(iterator.next()));

                first = false;

            }



            return result.append("}").toString();

        }



        if (value instanceof Map) {

            return "map[" + ((Map)value).size() + "]"

                    + String.valueOf(value);

        }



        return valueClass.getName()

                + "{"

                + describeSimpleMembers(value)

                + "}";

    }



    private static boolean isSimpleValue(Object value) {

        return value == null

                || value instanceof Number

                || value instanceof Boolean

                || value instanceof Character

                || value instanceof String

                || value.getClass().isEnum();

    }



    private static boolean isRelevantSouthernIdentity(
            String identity
    ) {
        // FINAL_HARAD_REGION_DIAGNOSTICS_V3
        return identity.contains("umbar")
                || identity.contains("harondor")
                || identity.contains("harnen")
                || identity.contains("nearharad")
                || identity.contains("southron")
                || identity.contains("greatdesert")
                || identity.contains("farharad")
                || identity.contains("bushland")
                || identity.contains("mangrove");
    }



    private static String normalize(String value) {

        if (value == null) {

            return "";

        }



        return value.toLowerCase(Locale.ENGLISH)

                .replaceAll("[^a-z0-9]", "");

    }

}