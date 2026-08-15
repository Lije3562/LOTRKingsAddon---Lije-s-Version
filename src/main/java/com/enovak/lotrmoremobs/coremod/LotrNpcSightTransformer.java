package com.enovak.lotrmoremobs.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adds only LOTREntityNPC's inherited visibility override. The method body is
 * deliberately a single helper call so Java try/finally owns context cleanup.
 */
public final class LotrNpcSightTransformer implements IClassTransformer {

    private static final String LOTR_NPC_CLASS =
            "lotr.common.entity.npc.LOTREntityNPC";
    private static final String DEOBFUSCATED_SUPER =
            "net/minecraft/entity/EntityCreature";
    private static final String NOTCH_SUPER = "td";
    private static final String DEOBFUSCATED_DESCRIPTOR =
            "(Lnet/minecraft/entity/Entity;)Z";
    private static final String NOTCH_DESCRIPTOR = "(Lsa;)Z";
    private static final String HOOK_OWNER =
            "com/enovak/lotrmoremobs/siege/gate/"
                    + "SiegeGateNpcSightHelper";
    private static final String HOOK_METHOD = "canEntityBeSeen";
    private static final String HOOK_DESCRIPTOR =
            "(Ljava/lang/Object;Ljava/lang/Object;)Z";

    @Override
    public byte[] transform(
            String name,
            String transformedName,
            byte[] basicClass
    ) {
        if (basicClass == null
                || (!LOTR_NPC_CLASS.equals(name)
                && !LOTR_NPC_CLASS.equals(transformedName))) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);
        MethodSignature signature = resolveSignature(classNode);
        if (signature == null) {
            System.err.println(
                    "[LOTRMoreMobs] Siege NPC sight compatibility could not "
                            + "identify LOTREntityNPC's vanilla naming form; "
                            + "NPC GatePart LOS behavior is unchanged."
            );
            return basicClass;
        }
        if (declaresMethod(classNode, signature)) {
            System.err.println(
                    "[LOTRMoreMobs] Siege NPC sight compatibility found an "
                            + "existing LOTREntityNPC visibility override; "
                            + "NPC GatePart LOS behavior is unchanged."
            );
            return basicClass;
        }

        MethodNode override = new MethodNode(
                Opcodes.ACC_PUBLIC,
                signature.name,
                signature.descriptor,
                null,
                null
        );
        InsnList instructions = override.instructions;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOK_OWNER,
                HOOK_METHOD,
                HOOK_DESCRIPTOR,
                false
        ));
        instructions.add(new InsnNode(Opcodes.IRETURN));
        classNode.methods.add(override);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        System.out.println(
                "[LOTRMoreMobs] Installed Siege Gate LOTR NPC sight "
                        + "override in " + signature.name
                        + signature.descriptor
        );
        return writer.toByteArray();
    }

    private static MethodSignature resolveSignature(ClassNode classNode) {
        if (NOTCH_SUPER.equals(classNode.superName)) {
            return new MethodSignature("p", NOTCH_DESCRIPTOR);
        }
        if (!DEOBFUSCATED_SUPER.equals(classNode.superName)) {
            return null;
        }
        return usesSrgMethodNames(classNode)
                ? new MethodSignature("func_70685_l", DEOBFUSCATED_DESCRIPTOR)
                : new MethodSignature("canEntityBeSeen", DEOBFUSCATED_DESCRIPTOR);
    }

    private static boolean usesSrgMethodNames(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            for (org.objectweb.asm.tree.AbstractInsnNode instruction =
                    method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode
                        && ((MethodInsnNode)instruction).owner.startsWith(
                        "net/minecraft/"
                ) && ((MethodInsnNode)instruction).name.startsWith("func_")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean declaresMethod(
            ClassNode classNode,
            MethodSignature signature
    ) {
        for (MethodNode method : classNode.methods) {
            if (signature.name.equals(method.name)
                    && signature.descriptor.equals(method.desc)) {
                return true;
            }
        }
        return false;
    }

    private static final class MethodSignature {
        private final String name;
        private final String descriptor;

        private MethodSignature(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }
    }
}
