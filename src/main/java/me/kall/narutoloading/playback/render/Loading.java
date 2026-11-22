package me.kall.narutoloading.playback.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

public final class Loading {
    private int width;
    private int height;

    private @Nullable ByteBuffer buffer;

    public @NotNull ByteBuffer get(int width, int height) {
        if (this.width == width && this.height == height && this.buffer != null) return this.buffer.rewind();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        genImage(width, height, image);

        int ySize = width * height;
        int uvSize = (width / 2) * (height / 2);
        int total = ySize + 2 * uvSize;

        ByteBuffer buffer = this.buffer;
        if (buffer == null || buffer.capacity() < total) buffer = ByteBuffer.allocateDirect(total);

        buffer.clear();

        byte[] gray = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        buffer.put(gray, 0, ySize);
        for (int i = 0; i < 2 * uvSize; i++) buffer.put((byte) 128);
        buffer.flip();

        image.flush();
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        return buffer;
    }

    public static void genImage(int width, int height, @NotNull BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.WHITE);

        int fontSize = Math.max(12, height / 6);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        String text = "Loading...";
        int textWidth = metrics.stringWidth(text);
        while (textWidth > width * 0.9 && fontSize > 10) {
            fontSize--;
            font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics();
            textWidth = metrics.stringWidth(text);
        }

        int textHeight = metrics.getHeight();
        int x = (width - textWidth) / 2;
        int y = (height - textHeight) / 2 + metrics.getAscent();
        graphics.drawString(text, x, y);
        graphics.dispose();
    }
}