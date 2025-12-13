package com.github.exiostorm.utils.msdf;

import java.awt.image.BufferedImage;

public class BitmapUtil {
    public static BufferedImage toBufferedImage(BitmapRef<float[]> bmp) {
        BufferedImage img = new BufferedImage(bmp.getWidth(), bmp.getHeight(), BufferedImage.TYPE_INT_ARGB);

        float[] px = bmp.getPixels();
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int c = bmp.getChannels();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = (y * w + x) * c;

                float r = px[index];
                float g = px[index + 1];
                float b = px[index + 2];

                int ir = Math.min(255, Math.max(0, (int)(r * 255)));
                int ig = Math.min(255, Math.max(0, (int)(g * 255)));
                int ib = Math.min(255, Math.max(0, (int)(b * 255)));

                int rgba = (255 << 24) | (ir << 16) | (ig << 8) | ib;
                img.setRGB(x, y, rgba);
            }
        }

        return img;
    }
}
