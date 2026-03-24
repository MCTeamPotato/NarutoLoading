package me.kall.narutoloading.agent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger LOGGER = LogManager.getLogger(DisplayWindowTransformer.class);

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain domain, byte[] classFileBuffer) {
        if (!TARGET_CLASS.equals(className)) return null;
        LOGGER.info("[NarutoAgent] Transforming {}", className);
        try {
            ClassReader cr = new ClassReader(classFileBuffer);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            boolean ok = false;
            for (MethodNode method : classNode.methods) {
                if (TARGET_METHOD.equals(method.name)) {
                    ok |= replaceElementsInit(method);
                    removeSquirAdd(method);
                }

                if ("paintFramebuffer".equals(method.name)) {
                    injectBackgroundRender(method);
                }
            }

            if (!ok) {
                LOGGER.warn("[NarutoAgent] Pattern not found in " + TARGET_METHOD + " — bytecode may have changed, transformation skipped.");
                return null;
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);
            LOGGER.info("[NarutoAgent] Transformation successful.");
            return cw.toByteArray();
        } catch (Exception ex) {
            LOGGER.fatal("[NarutoAgent] Exception during transformation.", ex);
            return null;
        }
    }

    private boolean replaceElementsInit(@NotNull MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.PUTFIELD) continue;
            FieldInsnNode fin = (FieldInsnNode) node;
            if (!"elements".equals(fin.name)) continue;

            AbstractInsnNode newArrayList = node.getPrevious();
            while (newArrayList != null) {
                if (newArrayList.getOpcode() == Opcodes.NEW && "java/util/ArrayList".equals(((TypeInsnNode) newArrayList).desc)) {
                    break;
                }
                newArrayList = newArrayList.getPrevious();
            }

            if (newArrayList == null) {
                LOGGER.warn("[NarutoAgent] NEW ArrayList not found before PUTFIELD elements");
                continue;
            }

            AbstractInsnNode aload0 = newArrayList.getPrevious();
            while ((aload0 instanceof LabelNode || aload0 instanceof LineNumberNode || aload0 instanceof FrameNode)) {
                aload0 = aload0.getPrevious();
            }

            if (aload0 == null || aload0.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) aload0).var != 0) {
                LOGGER.warn("[NarutoAgent] Expected ALOAD 0 before NEW ArrayList, got: {}", aload0 == null ? "null" : aload0.getOpcode());
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

            String owner = "net/minecraftforge/fml/earlydisplay/DisplayWindow";
            String reOwner = "net/minecraftforge/fml/earlydisplay/RenderElement";
            String fontDesc = "Lnet/minecraftforge/fml/earlydisplay/SimpleFont;";
            String reDesc = "Lnet/minecraftforge/fml/earlydisplay/RenderElement;";

            InsnList addProgressBars = new InsnList();
            addProgressBars.add(new VarInsnNode(Opcodes.ALOAD, 0));
            addProgressBars.add(new FieldInsnNode(Opcodes.GETFIELD, owner, "elements", "Ljava/util/List;"));
            addProgressBars.add(new VarInsnNode(Opcodes.ALOAD, 0));
            addProgressBars.add(new FieldInsnNode(Opcodes.GETFIELD, owner, "font", fontDesc));
            addProgressBars.add(new MethodInsnNode(Opcodes.INVOKESTATIC, reOwner, "progressBars", "(" + fontDesc + ")" + reDesc, false));
            addProgressBars.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
            addProgressBars.add(new InsnNode(Opcodes.POP));

            method.instructions.insert(node, addProgressBars);

            LOGGER.info("[NarutoAgent] Replaced elements init with new ArrayList<>() + progressBars(font)");
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
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "me/kall/narutoloading/agent/NarutoBackgroundHelper", "render", "()V", false));

            method.instructions.insert(insn, inject);
            LOGGER.info("[NarutoAgent] Injected NarutoBackgroundHelper.render() after glClear()");
            return;
        }
        LOGGER.warn("[NarutoAgent] glClear() not found in paintFramebuffer — background injection skipped.");
    }

    private void removeSquirAdd(@NotNull MethodNode method) {
        AbstractInsnNode squirCall = null;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if ("squir".equals(min.name)) {
                    squirCall = insn; break;
                }
            }
        }

        if (squirCall == null) return;

        AbstractInsnNode addCall = squirCall.getNext();
        while ((addCall instanceof LabelNode || addCall instanceof LineNumberNode || addCall instanceof FrameNode)) {
            addCall = addCall.getNext();
        }

        if (addCall == null || addCall.getOpcode() != Opcodes.INVOKEINTERFACE) {
            LOGGER.warn("[NarutoAgent] Expected INVOKEINTERFACE add() after squir(), skipping squir removal");
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

            LOGGER.info("[NarutoAgent] Removed elements.add(0, RenderElement.squir()) call");
        } else {
            method.instructions.remove(squirCall);
            method.instructions.remove(addCall);
            LOGGER.info("[NarutoAgent] Partially removed squir() call");
        }
    }
}