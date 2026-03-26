package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class NarutoTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain domain, byte[] classFileBuffer) {
        if ("net/minecraftforge/fml/earlydisplay/DisplayWindow".equals(className)) {
            try {
                ClassReader classReader = new ClassReader(classFileBuffer);
                ClassNode classNode = new ClassNode();
                classReader.accept(classNode, 0);

                for (MethodNode method : classNode.methods) {
                    if ("initRender".equals(method.name)) {
                        this.replaceElementsInit(method);
                        this.removeSquirAdd(method);
                    }

                    if ("paintFramebuffer".equals(method.name)) this.injectBackgroundRender(method);
                }

                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(classWriter);
                return classWriter.toByteArray();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if ("net/minecraft/client/Minecraft".equals(className)) {
            try {
                ClassReader classReader = new ClassReader(classFileBuffer);
                ClassNode classNode = new ClassNode();
                classReader.accept(classNode, 0);

                for (MethodNode method : classNode.methods) {
                    if ("<init>".equals(method.name)) this.injectShutdownAfterSetOverlay(method);
                }

                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(classWriter);
                return classWriter.toByteArray();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private void injectShutdownAfterSetOverlay(@NotNull MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;

            MethodInsnNode min = (MethodInsnNode) node;
            if ("net/minecraft/client/Minecraft".equals(min.owner) && "m_91150_".equals(min.name)) {
                InsnList inject = new InsnList();
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "me/kall/narutoloading/agent/NarutoRenderBridge", "shutdown", "()V", false));
                method.instructions.insert(node, inject);
                return;
            }
        }

        throw new RuntimeException("[NarutoLoading] setOverlay call not found in <init>");
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private void replaceElementsInit(@NotNull MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.PUTFIELD) continue;

            FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
            if (!"elements".equals(fieldInsnNode.name)) continue;

            AbstractInsnNode newArrayList = node.getPrevious();
            while (newArrayList != null) {
                if (newArrayList.getOpcode() == Opcodes.NEW && "java/util/ArrayList".equals(((TypeInsnNode) newArrayList).desc)) break;
                newArrayList = newArrayList.getPrevious();
            }

            if (newArrayList == null) throw new RuntimeException("[NarutoLoading] ERROR: NEW ArrayList not found");

            AbstractInsnNode previousNode = newArrayList.getPrevious();
            while ((previousNode instanceof LabelNode || previousNode instanceof LineNumberNode || previousNode instanceof FrameNode)) previousNode = previousNode.getPrevious();

            if (previousNode == null || previousNode.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) previousNode).var != 0) throw new RuntimeException("[NarutoLoading] ERROR: ALOAD 0 not found before ArrayList init");

            InsnList replacement = new InsnList();
            replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
            replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
            replacement.add(new InsnNode(Opcodes.DUP));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
            replacement.add(new InsnNode(Opcodes.DUP));
            replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
            replacement.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraftforge/fml/earlydisplay/DisplayWindow", "font", "Lnet/minecraftforge/fml/earlydisplay/SimpleFont;"));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/earlydisplay/RenderElement", "progressBars", "(Lnet/minecraftforge/fml/earlydisplay/SimpleFont;)Lnet/minecraftforge/fml/earlydisplay/RenderElement;", false));
            replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
            replacement.add(new InsnNode(Opcodes.POP));

            AbstractInsnNode current = previousNode;
            while (current != node) {
                AbstractInsnNode next = current.getNext();
                method.instructions.remove(current);
                current = next;
            }

            method.instructions.insertBefore(node, replacement);
            return;
        }
    }

    private void injectBackgroundRender(@NotNull MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.INVOKESTATIC) continue;
            MethodInsnNode min = (MethodInsnNode) node;
            if (!"glClear".equals(min.name)) continue;

            InsnList inject = new InsnList();
            inject.add(new InsnNode(Opcodes.POP));
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "me/kall/narutoloading/agent/NarutoRenderBridge", "render", "()V", false));

            method.instructions.insert(node, inject);
            method.instructions.remove(node);
            return;
        }
    }

    private void removeSquirAdd(@NotNull MethodNode method) {
        AbstractInsnNode squirCall = null;

        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode min = (MethodInsnNode) node;
                if ("squir".equals(min.name)) {
                    squirCall = node;
                    break;
                }
            }
        }

        if (squirCall == null) throw new RuntimeException("[NarutoLoading] WARNING: squir call not found");

        AbstractInsnNode addCall = squirCall.getNext();
        while ((addCall instanceof LabelNode || addCall instanceof LineNumberNode || addCall instanceof FrameNode)) {
            addCall = addCall.getNext();
        }

        if (addCall == null || addCall.getOpcode() != Opcodes.INVOKEINTERFACE) throw new RuntimeException("[NarutoLoading] Add call after squir not found");

        AbstractInsnNode start = squirCall.getPrevious();
        while (start != null && start.getOpcode() == Opcodes.ICONST_0) start = start.getPrevious();
        while (start != null && start.getOpcode() == Opcodes.GETFIELD) start = start.getPrevious();

        if (start != null && start.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) start).var == 0) {
            AbstractInsnNode current = start;
            AbstractInsnNode end = addCall.getNext();

            while (current != end) {
                AbstractInsnNode next = current.getNext();
                method.instructions.remove(current);
                current = next;
            }
        } else {
            method.instructions.remove(squirCall);
            method.instructions.remove(addCall);
        }
    }
}