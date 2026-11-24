package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class DisplayWindowPatcher {
    private static final String DISPLAY_WINDOW = "net/neoforged/fml/earlydisplay/DisplayWindow";
    private static final String BRIDGE = "me/kall/narutoloading/agent/bootstrap/EarlyBridge";

    private static final Long FRAME_PERIOD_MILLIS = 5L;
    private static final long VANILLA_PERIOD_MILLIS = 50L;

    private static final String LIST = "Ljava/util/List;";
    private static final String FONT = "Lnet/neoforged/fml/earlydisplay/SimpleFont;";
    private static final String RENDER_ELEMENT = "Lnet/neoforged/fml/earlydisplay/RenderElement;";

    private DisplayWindowPatcher() {
    }

    @SuppressWarnings("unused")
    public static byte[] patch(byte[] classFileBuffer) {
        ClassReader reader = new ClassReader(classFileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);

                if ("paintFramebuffer".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String callName,
                                                    String callDescriptor, boolean isInterface) {
                            super.visitMethodInsn(opcode, owner, callName, callDescriptor, isInterface);
                            if ("glClear".equals(callName) && "(I)V".equals(callDescriptor)) {
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass",
                                        "()Ljava/lang/Class;", false);
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader",
                                        "()Ljava/lang/ClassLoader;", false);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "render",
                                        "(Ljava/lang/ClassLoader;)V", false);
                            }
                        }
                    };
                }

                if ("initRender".equals(name)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        private int retargetedPeriods;

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof Long constant && constant == VANILLA_PERIOD_MILLIS && this.retargetedPeriods < 2) {
                                this.retargetedPeriods++;
                                super.visitLdcInsn(FRAME_PERIOD_MILLIS);
                                return;
                            }

                            super.visitLdcInsn(value);
                        }

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            // The early display hardcodes an 854x480 framebuffer, which NeoForge keeps even
                            // when the window itself is larger; the video would only cover that corner.
                            if (opcode == Opcodes.SIPUSH && operand == 854) {
                                emitFramebufferField(delegate, "fbWidth");
                                return;
                            }

                            if (opcode == Opcodes.SIPUSH && operand == 480) {
                                emitFramebufferField(delegate, "fbHeight");
                                return;
                            }

                            super.visitIntInsn(opcode, operand);
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) emitOnlyProgressBars(delegate);
                            super.visitInsn(opcode);
                        }
                    };
                }

                if ("close".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "detach", "()V", false);
                        }
                    };
                }

                if ("addMojangTexture".equals(name) && "(I)V".equals(descriptor)) return new NoOpVisitor(delegate);

                return delegate;
            }
        }, 0);

        System.out.println("[NarutoLoading Agent] patched " + DISPLAY_WINDOW);
        return writer.toByteArray();
    }

    private static void emitFramebufferField(@NotNull MethodVisitor visitor, @NotNull String field) {
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, DISPLAY_WINDOW, field, "I");
    }

    private static void emitOnlyProgressBars(@NotNull MethodVisitor visitor) {
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, DISPLAY_WINDOW, "elements", LIST);
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "clear", "()V", true);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, DISPLAY_WINDOW, "elements", LIST);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, DISPLAY_WINDOW, "font", FONT);
        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "net/neoforged/fml/earlydisplay/RenderElement", "progressBars", "(" + FONT + ")" + RENDER_ELEMENT, false);

        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
        visitor.visitInsn(Opcodes.POP);
    }

    private static final class NoOpVisitor extends MethodVisitor {
        private NoOpVisitor(MethodVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitInsn(Opcodes.RETURN);
        }

        @Override public void visitInsn(int opcode) {}
        @Override public void visitIntInsn(int opcode, int operand) {}
        @Override public void visitVarInsn(int opcode, int varIndex) {}
        @Override public void visitTypeInsn(int opcode, String type) {}
        @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {}
        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {}
        @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {}
        @Override public void visitJumpInsn(int opcode, Label label) {}
        @Override public void visitLabel(Label label) {}
        @Override public void visitLdcInsn(Object value) {}
        @Override public void visitIincInsn(int varIndex, int increment) {}
        @Override public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {}
        @Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {}
        @Override public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {}
        @Override public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
        @Override public void visitLineNumber(int line, Label start) {}
        @Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {}
        @Override public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {}
    }
}
