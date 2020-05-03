package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class MyGdxGame extends ApplicationAdapter {
    SpriteBatch batch;
    ShapeRenderer shape;
    int x_text = 0;
    int x_dir_text = 1;
    int x_shape = 0;
    int x_dir_shape = 2;
    final int rect_width;

    {
        rect_width = 16;
    }

    Sprite spriteWithPower;
    Sprite spriteNoPower;
    Sprite spriteActive;
    int rotate_dir = 0;

    final int window_height_in_meters = 200;
    World world;
    Body body;
    Body bodyLandingPlatform;
    Body bodySurface;
    float pixels_per_meter;
    boolean crashed = false;

    float force_x = 0.0f;
    float force_y = 0.0f;
    float torque = 0.0f;
    boolean landed = false;
    int camera_delta_y = 0;

    Box2DDebugRenderer debugRenderer;
    Matrix4 debugMatrix;
    OrthographicCamera camera;
    BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shape = new ShapeRenderer();

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
        // Basically set the physics polygon to a box with the same dimension as our sprite.
        // Uses half-widths and half-heights as parameters
        shape.setAsBox(spriteActive.getWidth() / 2f / pixels_per_meter,
                spriteActive.getHeight() / 2f / pixels_per_meter);

        // FixtureDef is a confusing expression for physical properties
        // Basically this is where you, in addition to defining the shape of the body
        // you also define it's properties like density, restitution and others we will see shortly
        // If you are wondering, density and area are used to calculate over all mass
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = .1f;  // Adjust mass and body's reaction to force.
        fixtureDef.restitution = 0.3f;  // How bouncy the body is (rubber vs rock)
        fixtureDef.friction = .8f;

        body.createFixture(fixtureDef);
        shape.dispose();

        float width_in_m = Gdx.graphics.getWidth() / pixels_per_meter;
        float height_in_m = Gdx.graphics.getHeight() / pixels_per_meter - 50 / pixels_per_meter;

        float platform_width_in_m = 20;
        float platform_height_in_m = 10;
        float platform_x_in_m = (width_in_m - platform_width_in_m) / 2f;

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Create Body for landing pad.
        BodyDef bodyDef2 = new BodyDef();
        bodyDef2.type = BodyDef.BodyType.StaticBody;

        bodyDef2.position.set(0, 0);
        FixtureDef fixtureDef2 = new FixtureDef();
        fixtureDef2.friction = .6f;

        EdgeShape edgeShape = new EdgeShape();
        edgeShape.set(platform_x_in_m, 5f, platform_x_in_m+platform_width_in_m, 5f);

        fixtureDef2.shape = edgeShape;

        bodyLandingPlatform = world.createBody(bodyDef2);
        bodyLandingPlatform.createFixture(fixtureDef2);
        edgeShape.dispose();

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Create Body for planet surface.
        BodyDef bodyDef3 = new BodyDef();
        bodyDef3.type = BodyDef.BodyType.StaticBody;
        bodyDef3.position.set(0, 0);
        FixtureDef fixtureDef3 = new FixtureDef();
        fixtureDef3.friction = .9f;

        EdgeShape edgeShape2 = new EdgeShape();
        // Line across whole screen (ground)
        edgeShape2.set(0f, 2f, width_in_m, 2f);

        fixtureDef3.shape = edgeShape2;

        bodySurface = world.createBody(bodyDef3);
        bodySurface.createFixture(fixtureDef3);
        edgeShape2.dispose();

        // Create a Box2DDebugRenderer, this allows us to see the physics simulation controlling the scene
        debugRenderer = new Box2DDebugRenderer();

        font = new BitmapFont(Gdx.files.internal("test-font.fnt"));
        font.setColor(Color.BLACK);

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.
                getHeight());
        camera.translate(camera.viewportWidth / 2, camera.viewportHeight / 2);

        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                // Check to see if the collision is between the second sprite and the bottom of the screen
                // If so apply a random amount of upward force to both objects... just because
                if ((contact.getFixtureA().getBody() == bodyLandingPlatform &&
                        contact.getFixtureB().getBody() == body)
                        ||
                        (contact.getFixtureA().getBody() == body &&
                                contact.getFixtureB().getBody() == bodyLandingPlatform)) {

                    body.applyForceToCenter(0, MathUtils.random(20, 50), true);
                    Vector2 speed = body.getLinearVelocity();
                    if (speed.y > 1f) {
                        System.out.println("Crash!");
                        crashed = true;
                    }
                    System.out.printf("Landed vel: %s angle: %s\n", speed.len(), Math.toDegrees(body.getAngle()));
                    System.out.println(speed.y);
                    System.out.printf("Camera moved %d\n", camera_delta_y);
                }
            }

            @Override
            public void endContact(Contact contact) {
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
            }
        });
    }

    private float accumulator = 0;
    private float TIME_STEP = 1/60f;

    private void doPhysicsStep(float deltaTime) {
        /*
         fixed time step
         max frame time to avoid spiral of death (on slow devices)
        */
        float frameTime = Math.min(deltaTime, 0.25f);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }
    }

    private float normalizeZero(float f) {
        if (Math.abs(f) < 0.03) return 0f;
        return f;
    }

    boolean reset_angle = false;

    final float ROTATION_TORQUE = 80f;

    @Override
    public void render() {
        float x_in_pixels = body.getPosition().x * pixels_per_meter - spriteActive.getWidth() / 2f;
        float y_in_pixels = body.getPosition().y * pixels_per_meter - spriteActive.getHeight() / 2f;
        boolean zoom_enabled = false;
        if (y_in_pixels < Gdx.graphics.getHeight() - 400) {
            if (camera.zoom > 0.5f && zoom_enabled) {
                camera.zoom = .5f;
                camera.translate(0, -170);
            }
        }
        camera.update();
        // Advance the world, by the amount of time that has elapsed since the last frame
        // Generally in a real game, don't do this in the render loop, as you are tying the physics
        // update rate to the frame rate, and vice versa
        // world.step(Gdx.graphics.getDeltaTime(), 6, 2);

        doPhysicsStep(Gdx.graphics.getDeltaTime());

        // Apply torque to the physics body.  At start this is 0 and will do nothing.
        // Torque is applied per frame instead of just once
        body.applyForceToCenter(force_x, force_y, true);
        body.applyTorque(torque, true);

        spriteWithPower.setPosition(x_in_pixels, y_in_pixels);
        spriteNoPower.setPosition(x_in_pixels, y_in_pixels);

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            reset_angle = false;
            torque = ROTATION_TORQUE;
            if (spriteActive.getRotation() > 30) {
                torque = 0f;
                body.setAngularVelocity(0f);
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            reset_angle = false;
            torque = -ROTATION_TORQUE;
            if (spriteActive.getRotation() < -30) {
                torque = 0f;
                body.setAngularVelocity(0f);
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            // Initiate auto-rotation
            torque = 0f;
            reset_angle = true;
        } else if (!reset_angle) {
            torque = 0f;
            body.setAngularVelocity(0f);
        }

        // Not working!
        if (reset_angle) {
            float q = -body.getAngle();
            float c = 2f; // initial speed of rotation
            if (Math.abs(body.getAngle()) > 0.001f) {
                body.setAngularVelocity(c * q);
            } else {
                // Auto rotation finished.
                reset_angle = false;
                body.setAngularVelocity(0f);
            }
        }
        spriteWithPower.setRotation((float) Math.toDegrees(body.getAngle()));
        spriteNoPower.setRotation((float) Math.toDegrees(body.getAngle()));

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            // Execute thrust.
            final float force;
            force = 30f;
            spriteActive = spriteWithPower;
            float angle = body.getAngle();
            force_y = (float) (Math.cos(angle) * force);
            force_x = (float) (-Math.sin(angle) * force);
        } else {
            force_y = 0f;
            force_x = 0f;
            spriteActive = spriteNoPower;
        }

        x_text += x_dir_text;
        x_shape += x_dir_shape;

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);

        // Scale down the sprite batches projection matrix to box2D size
        debugMatrix = batch.getProjectionMatrix().cpy().scale(pixels_per_meter, pixels_per_meter, 0);
        batch.begin();
        spriteActive.draw(batch);

        font.draw(batch, "VelX: " + String.format("%.0f", normalizeZero(body.getLinearVelocity().x)) + " m/sec",
                10, font.getLineHeight() + 5);
        font.draw(batch, "VelY: " + String.format("%.0f", normalizeZero(body.getLinearVelocity().y)) + " m/sec",
                10, font.getLineHeight() * 2 + 5);
        font.draw(batch, "Angle: " + String.format("%.0f", Math.toDegrees(body.getAngle())) + " deg",
                10, font.getLineHeight() * 3 + 5);
        batch.end();

        if (x_shape > Gdx.graphics.getWidth() - rect_width) {
            x_dir_shape = -x_dir_shape;
        } else if (x_shape < 0) {
            x_dir_shape = -x_dir_shape;
        }

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
        shape.dispose();
        batch.dispose();
    }
}
