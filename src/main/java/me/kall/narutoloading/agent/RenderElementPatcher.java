package me.kall.narutoloading.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The early display draws its progress bars at a fixed 400x20 in the framebuffer. Now that the
 * framebuffer matches the window instead of being upscaled from 854x480, the bars would look
 * much smaller; widen them to keep the previous on-screen size.
 */
public final class RenderElementPatcher {
    private RenderElementPatcher() {}

    @SuppressWarnings("unused")
    public static byte[] patch(byte[] classFileBuffer) {
        ClassReader reader = new ClassReader(classFileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);

                if (!name.contains("progressBar") && !name.contains("barRenderer")) return delegate;

                boolean barRenderer = name.contains("barRenderer");

                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        // The bars were laid out for the 854x480 context and then upscaled; now that
                        // the context is the window size, keep them at the same relative height.
                        if (barRenderer && opcode == Opcodes.SIPUSH && operand == 250) {
                            super.visitVarInsn(Opcodes.ALOAD, 4);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/neoforged/fml/earlydisplay/RenderElement$DisplayContext", "scaledHeight", "()I", false);
                            super.visitInsn(Opcodes.I2F);
                            super.visitLdcInsn(0.55F);
                            super.visitInsn(Opcodes.FMUL);
                            super.visitInsn(Opcodes.F2I);
                            return;
                        }

                        if (opcode == Opcodes.SIPUSH && operand == 400) operand = 610;
                        else if (opcode == Opcodes.BIPUSH && operand == 20) operand = 28;
                        super.visitIntInsn(opcode, operand);
                    }
                };
            }
        }, 0);

        System.out.println("[NarutoLoading Agent] patched net/neoforged/fml/earlydisplay/RenderElement");
        return writer.toByteArray();
    }
}
