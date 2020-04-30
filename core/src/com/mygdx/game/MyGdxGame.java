package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class MyGdxGame extends ApplicationAdapter {
    SpriteBatch batch;
    BitmapFont font;
    ShapeRenderer shape;
    int x_text = 0;
    int x_dir_text = 1;
    int x_shape = 0;
    int x_dir_shape = 2;
    final int rect_width;

    {
        rect_width = 16;
    }

    final String text = "Dobar Dan";
    GlyphLayout glyphLayout = new GlyphLayout();
    Texture img;
    Sprite spriteWithPower;
    Sprite spriteNoPower;
    Sprite spriteActive;
    int rotate_dir = 0;

    final int window_height_in_meters = 200;
    World world;
    Body body;
    float pixels_per_meter;

    float force_x = 0.0f;
    float force_y = 0.0f;
    float torque = 0.0f;
    boolean landed = false;

    Box2DDebugRenderer debugRenderer;
    Matrix4 debugMatrix;
    OrthographicCamera camera;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        shape = new ShapeRenderer();
        glyphLayout.setText(font, text);
        img = new Texture("test-sprite-fire.png");

        spriteWithPower = SpriteCreator.createSpriteWithPower();
        spriteNoPower = SpriteCreator.createSprite();
        spriteActive = spriteNoPower;
        spriteWithPower.setPosition(Gdx.graphics.getWidth() / 2f - spriteWithPower.getWidth() / 2f,
                Gdx.graphics.getHeight() - spriteWithPower.getHeight() - 10);
        spriteNoPower.setPosition(spriteWithPower.getX(), spriteWithPower.getY());

        // Create a physics world, the heart of the simulation.  The Vector passed in is gravity
        world = new World(new Vector2(0, -1.62f), true);
        // Now create a BodyDefinition.  This defines the physics objects type and position in the simulation
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        pixels_per_meter = spriteActive.getY() / window_height_in_meters;
        // Set our body to the same position as our sprite
        bodyDef.position.set((spriteActive.getX() + spriteActive.getWidth() / 2f) / pixels_per_meter,
                (spriteActive.getY() + spriteActive.getHeight() / 2f) / pixels_per_meter);
        // Create a body in the world using our definition
        body = world.createBody(bodyDef);

        // Now define the dimensions of the physics shape
        PolygonShape shape = new PolygonShape();
        // We are a box, so this makes sense, no?
        // Basically set the physics polygon to a box with the same dimension as our sprite
        shape.setAsBox(spriteActive.getWidth() / 2f / pixels_per_meter,
                spriteActive.getHeight() / 2f /pixels_per_meter);

        // FixtureDef is a confusing expression for physical properties
        // Basically this is where you, in addition to defining the shape of the body
        // you also define it's properties like density, restitution and others we will see shortly
        // If you are wondering, density and area are used to calculate over all mass
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = .1f;  // Adjust mass and body's reaction to force.

        body.createFixture(fixtureDef);

        shape.dispose();

        // Create a Box2DDebugRenderer, this allows us to see the physics simulation controlling the scene
        debugRenderer = new Box2DDebugRenderer();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.
                getHeight());
        camera.translate(camera.viewportWidth/2,camera.viewportHeight/2);
    }

    @Override
    public void render() {
        camera.update();
        // Advance the world, by the amount of time that has elapsed since the last frame
        // Generally in a real game, don't do this in the render loop, as you are tying the physics
        // update rate to the frame rate, and vice versa
        world.step(Gdx.graphics.getDeltaTime(), 6, 2);

        // Apply torque to the physics body.  At start this is 0 and will do nothing.
        // Torque is applied per frame instead of just once
        body.applyForceToCenter(force_x, force_y,true);
        body.applyTorque(torque, true);

        float x_in_pixels = body.getPosition().x * pixels_per_meter - spriteActive.getWidth() / 2f;
        float y_in_pixels = body.getPosition().y * pixels_per_meter - spriteActive.getHeight() / 2f;
        if (y_in_pixels < 0) {
            if (!landed) {
                Vector2 speed = body.getLinearVelocity();
                System.out.printf("Landed vel: %s angle: %s\n", speed.len2(), body.getAngle());
            }
            body.setAngularVelocity(0f);
            body.setLinearVelocity(0f, 0f);
            landed = true;
        }
        spriteWithPower.setPosition(x_in_pixels, y_in_pixels);
        spriteNoPower.setPosition(x_in_pixels, y_in_pixels);

        rotate_dir = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            torque = 20f;
            if (spriteActive.getRotation() > -30) {
                rotate_dir = -1;
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            torque = -20f;
            if (spriteActive.getRotation() < 30) {
                rotate_dir = 1;
            }
        } else {
            rotate_dir = 0;
        }

        /*
        if (rotate_dir != 0) {
            spriteWithPower.rotate(rotate_dir);
            spriteNoPower.rotate(rotate_dir);
        }
        */
        spriteWithPower.setRotation((float)Math.toDegrees(body.getAngle()));
        spriteNoPower.setRotation((float)Math.toDegrees(body.getAngle()));

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            final float force;
            force = 30f;
            torque = 0f;
            body.setAngularVelocity(0f);
            spriteActive = spriteWithPower;
            float angle = body.getAngle();
            force_y = (float) (Math.cos(angle) * force);
            force_x = (float) (- Math.sin(angle) * force);
        } else {
            force_y = 0f;
            spriteActive = spriteNoPower;
        }

        x_text += x_dir_text;
        x_shape += x_dir_shape;

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // This moves the origin and hides the lander. Why?
        batch.setProjectionMatrix(camera.combined);

        // Scale down the sprite batches projection matrix to box2D size
        debugMatrix = batch.getProjectionMatrix().cpy().scale(pixels_per_meter, pixels_per_meter, 0);
        batch.begin();
        font.draw(batch, text, x_text, 30);
        batch.draw(img, 100, 100);


        // spriteActive.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);
        spriteActive.draw(batch);
        batch.end();

        // FOr text sample:
        if (x_text > Gdx.graphics.getWidth() - glyphLayout.width) {
            x_dir_text = -1;
        } else if (x_text < 0) {
            x_dir_text = 1;
        }

        if (x_shape > Gdx.graphics.getWidth() - rect_width) {
            x_dir_shape = -x_dir_shape;
        } else if (x_shape < 0) {
            x_dir_shape = -x_dir_shape;
        }

        // shape.begin(ShapeRenderer.ShapeType.Filled);
        // shape.circle(50, 50, 50);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.GREEN);
        shape.rect(x_shape, 50, rect_width, 16);
        shape.end();

        // Now render the physics world using our scaled down matrix
        // Note, this is strictly optional and is, as the name suggests, just for debugging purposes
        debugRenderer.render(world, debugMatrix);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        font.dispose();
        shape.dispose();
        img.dispose();
        batch.dispose();
    }
}
