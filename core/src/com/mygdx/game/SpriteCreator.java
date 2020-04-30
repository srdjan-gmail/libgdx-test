package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

class SpriteCreator {

    static void createMechanical(Pixmap pixmap) {
        pixmap.setColor(Color.BLACK);
        pixmap.drawLine(10, 0, 21, 0);  // top
        pixmap.drawLine(10, 0, 0, 20);  // left
        pixmap.drawLine(21, 0, 31, 20);  // right
        pixmap.drawLine(0, 20, 31, 20);  // bottom

        pixmap.drawLine(13, 0, 8, 20);  // left slant
        pixmap.drawLine(18, 0, 23, 20); // right slant

        pixmap.drawLine(12, 20, 3, 31);
        pixmap.drawLine(0, 31, 6, 31);  // left lander

        pixmap.drawLine(19, 20, 28, 31);
        pixmap.drawLine(25, 31, 31, 31); // right lander
    }

    static void createFire(Pixmap pixmap) {
        pixmap.setColor(Color.RED);
        pixmap.drawLine(15, 21, 13, 28);
        pixmap.drawLine(15, 21, 14, 28);
        pixmap.drawLine(15, 21, 15, 28);
        pixmap.drawLine(16, 21, 18, 28);
        pixmap.drawLine(16, 21, 17, 28);
        pixmap.drawLine(16, 21, 16, 28);
    }

    static Sprite createSprite() {
        return new Sprite(createTexture());
    }

    static Texture createTexture() {
        // RGBA8888: each pixel has four color components (red, green, blue, and alpha; in that order)
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        createMechanical(pixmap);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    static Sprite createSpriteWithPower() {
        // RGBA8888: each pixel has four color components (red, green, blue, and alpha; in that order)
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        createMechanical(pixmap);
        createFire(pixmap);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new Sprite(texture);
    }
}