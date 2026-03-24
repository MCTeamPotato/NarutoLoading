package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class DisplayWindowTransformer implements ClassFileTransformer {
    private static final String TARGET_CLASS = "net/minecraftforge/fml/earlydisplay/DisplayWindow";
    private static final String TARGET_METHOD = "initRender";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain domain, byte[] classFileBuffer) {
        if (!TARGET_CLASS.equals(className)) return null;

        try {
            ClassReader cr = new ClassReader(classFileBuffer);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            boolean ok = false;

            for (MethodNode method : classNode.methods) {
                if (TARGET_METHOD.equals(method.name)) {

                    boolean replaced = replaceElementsInit(method);
                    ok |= replaced;

                    removeSquirAdd(method);
                }

                if ("paintFramebuffer".equals(method.name)) {
                    injectBackgroundRender(method);
                }
            }

            if (!ok) {
                throw new RuntimeException("[NarutoLoading] WARNING: elements init NOT replaced!");
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);

            return cw.toByteArray();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean replaceElementsInit(@NotNull MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.PUTFIELD) continue;

            FieldInsnNode fin = (FieldInsnNode) node;
            if (!"elements".equals(fin.name)) continue;

            System.out.println("[NarutoLoading] Found elements field assignment");

            AbstractInsnNode aload0 = getNode(node);

            InsnList replacement = new InsnList();
            replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
            replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
            replacement.add(new InsnNode(Opcodes.DUP));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));

            AbstractInsnNode cur = aload0;
            while (cur != node) {
                AbstractInsnNode next = cur.getNext();
                method.instructions.remove(cur);
                cur = next;
            }

            method.instructions.insertBefore(node, replacement);

            return true;
        }

        return false;
    }

    private static @NotNull AbstractInsnNode getNode(AbstractInsnNode node) {
        AbstractInsnNode aload0 = getAload0(node);
        while ((aload0 instanceof LabelNode || aload0 instanceof LineNumberNode || aload0 instanceof FrameNode)) {
            aload0 = aload0.getPrevious();
        }

        if (aload0 == null || aload0.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) aload0).var != 0) {
            throw new RuntimeException("[NarutoLoading] ERROR: ALOAD 0 not found before ArrayList init");
        }
        return aload0;
    }

    private static AbstractInsnNode getAload0(@NotNull AbstractInsnNode node) {
        AbstractInsnNode newArrayList = node.getPrevious();
        while (newArrayList != null) {
            if (newArrayList.getOpcode() == Opcodes.NEW && "java/util/ArrayList".equals(((TypeInsnNode) newArrayList).desc)) {
                break;
            }
            newArrayList = newArrayList.getPrevious();
        }

        if (newArrayList == null) {
            throw new RuntimeException("[NarutoLoading] ERROR: NEW ArrayList not found");
        }

        return newArrayList.getPrevious();
    }

    private void injectBackgroundRender(@NotNull MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != Opcodes.INVOKESTATIC) continue;
            MethodInsnNode min = (MethodInsnNode) insn;
            if (!"glClear".equals(min.name)) continue;

            InsnList inject = new InsnList();
            inject.add(new InsnNode(Opcodes.POP));
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "me/kall/narutoloading/agent/NarutoRenderBridge", "render", "()V", false));

            method.instructions.insert(insn, inject);
            method.instructions.remove(insn);
            return;
        }
    }

    private void removeSquirAdd(@NotNull MethodNode method) {
        AbstractInsnNode squirCall = null;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if ("squir".equals(min.name)) {
                    squirCall = insn;
                    break;
                }
            }
        }

        if (squirCall == null) {
            throw new RuntimeException("[NarutoLoading] WARNING: squir call not found");
        }

        AbstractInsnNode addCall = squirCall.getNext();
        while ((addCall instanceof LabelNode || addCall instanceof LineNumberNode || addCall instanceof FrameNode)) {
            addCall = addCall.getNext();
        }

        if (addCall == null || addCall.getOpcode() != Opcodes.INVOKEINTERFACE) {
            throw new RuntimeException("[NarutoLoading] Add call after squir not found");
        }

        AbstractInsnNode start = squirCall.getPrevious();
        while (start != null && start.getOpcode() == Opcodes.ICONST_0) {
            start = start.getPrevious();
        }

        while (start != null && start.getOpcode() == Opcodes.GETFIELD) {
            start = start.getPrevious();
        }

        if (start != null && start.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) start).var == 0) {
            AbstractInsnNode cur = start;
            AbstractInsnNode end = addCall.getNext();

            while (cur != end) {
                AbstractInsnNode next = cur.getNext();
                method.instructions.remove(cur);
                cur = next;
            }
        } else {
            method.instructions.remove(squirCall);
            method.instructions.remove(addCall);
        }
    }
}