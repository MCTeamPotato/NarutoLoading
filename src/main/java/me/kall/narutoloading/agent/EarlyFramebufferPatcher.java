package me.kall.narutoloading.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class EarlyFramebufferPatcher {
    private static final String EARLY_FRAMEBUFFER = "net/neoforged/fml/earlydisplay/EarlyFramebuffer";

    private EarlyFramebufferPatcher() {
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

                if (!"draw".equals(name) || !"(II)V".equals(descriptor)) return delegate;

                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private int conversions;

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode != Opcodes.F2I) {
                            super.visitInsn(opcode);
                            return;
                        }

                        // The blit destination is aspect-fitted, then truncated towards zero, which leaves up to one
                        // pixel of the cleared background visible on the right and bottom edges. Round outwards so the
                        // video reaches the window border.
                        String rounding = this.conversions < 2 ? "floor" : "ceil";
                        this.conversions++;

                        super.visitInsn(Opcodes.F2D);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", rounding, "(D)D", false);
                        super.visitInsn(Opcodes.D2I);
                    }
                };
            }
        }, 0);

        System.out.println("[NarutoLoading Agent] patched " + EARLY_FRAMEBUFFER);
        return writer.toByteArray();
    }
}
