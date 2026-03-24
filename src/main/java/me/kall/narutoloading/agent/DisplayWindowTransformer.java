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

        System.out.println("[NarutoLoading] Transforming class: " + className);

        try {
            ClassReader cr = new ClassReader(classFileBuffer);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            boolean ok = false;

            for (MethodNode method : classNode.methods) {
                if (TARGET_METHOD.equals(method.name)) {
                    System.out.println("[NarutoLoading] Found target method: initRender");

                    boolean replaced = replaceElementsInit(method);
                    ok |= replaced;
                    System.out.println("[NarutoLoading] replaceElementsInit: " + replaced);

                    removeSquirAdd(method);
                }

                if ("paintFramebuffer".equals(method.name)) {
                    System.out.println("[NarutoLoading] Found method: paintFramebuffer");
                    injectBackgroundRender(method);
                }
            }

            if (!ok) {
                System.out.println("[NarutoLoading] WARNING: elements init NOT replaced!");
                return null;
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);

            System.out.println("[NarutoLoading] Transform success: " + className);
            return cw.toByteArray();

        } catch (Exception ex) {
            System.out.println("[NarutoLoading] ERROR during transform!");
            ex.printStackTrace();
            return null;
        }
    }

    private boolean replaceElementsInit(@NotNull MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.PUTFIELD) continue;

            FieldInsnNode fin = (FieldInsnNode) node;
            if (!"elements".equals(fin.name)) continue;

            System.out.println("[NarutoLoading] Found elements field assignment");

            // 找到 new ArrayList 的位置
            AbstractInsnNode newArrayList = node.getPrevious();
            while (newArrayList != null) {
                if (newArrayList.getOpcode() == Opcodes.NEW &&
                        "java/util/ArrayList".equals(((TypeInsnNode) newArrayList).desc)) {
                    break;
                }
                newArrayList = newArrayList.getPrevious();
            }

            if (newArrayList == null) {
                System.out.println("[NarutoLoading] ERROR: NEW ArrayList not found");
                continue;
            }

            AbstractInsnNode aload0 = newArrayList.getPrevious();
            while ((aload0 instanceof LabelNode || aload0 instanceof LineNumberNode || aload0 instanceof FrameNode)) {
                aload0 = aload0.getPrevious();
            }

            if (aload0 == null || aload0.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) aload0).var != 0) {
                System.out.println("[NarutoLoading] ERROR: ALOAD 0 not found before ArrayList init");
                continue;
            }

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

            System.out.println("[NarutoLoading] Replaced elements initialization → EMPTY ArrayList (no progress bar, no anvil, no overlays)");

            return true;
        }

        return false;
    }

    private void injectBackgroundRender(@NotNull MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != Opcodes.INVOKESTATIC) continue;
            MethodInsnNode min = (MethodInsnNode) insn;
            if (!"glClear".equals(min.name)) continue;

            InsnList inject = new InsnList();
            inject.add(new InsnNode(Opcodes.POP));               // 消费 glClear 的参数
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "me/kall/narutoloading/agent/NarutoBackgroundHelper", "render", "()V", false));

            method.instructions.insert(insn, inject);
            method.instructions.remove(insn);                    // 移除原 glClear 调用

            System.out.println("[NarutoLoading] Replaced glClear with background render (POP + render)");
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
            System.out.println("[NarutoLoading] WARNING: squir call not found");
            return;
        }

        AbstractInsnNode addCall = squirCall.getNext();
        while ((addCall instanceof LabelNode || addCall instanceof LineNumberNode || addCall instanceof FrameNode)) {
            addCall = addCall.getNext();
        }

        if (addCall == null || addCall.getOpcode() != Opcodes.INVOKEINTERFACE) {
            System.out.println("[NarutoLoading] WARNING: add call after squir not found");
            return;
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

            System.out.println("[NarutoLoading] Removed full squir -> add sequence");
        } else {
            method.instructions.remove(squirCall);
            method.instructions.remove(addCall);

            System.out.println("[NarutoLoading] Removed partial squir/add");
        }
    }
}