package org.solhost.folko.slclient.views;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.solhost.folko.slclient.controllers.MainController;
import org.solhost.folko.slclient.models.GameState;
import org.solhost.folko.slclient.models.TexturePool;
import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLMap;
import org.solhost.folko.uosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Point3D;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class GameView {
    private static final Logger log = Logger.getLogger("slclient.gameview");
    private static final int DEFAULT_WIDTH  = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int FPS = 20;

    private final MainController mainController;
    private final GameState game;
    private Transform projection;
    private final Transform view, model;
    private ShaderProgram shader;
    private Integer vaoID, vboID, eboID, texID, texTextureID;

    public GameView(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();
        try {
            Display.setDisplayMode(new DisplayMode(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        } catch (LWJGLException e) {
            log.log(Level.SEVERE, "Couldn't set display mode: " + e.getMessage(), e);
            mainController.onGameError("Couldn't set display mode: " + e.getMessage());
        }

        projection = new Transform();
        view = new Transform();
        model = new Transform();
    }

    public void run() {
        try {
            PixelFormat pixFormat = new PixelFormat();
            ContextAttribs contextAttribs = new ContextAttribs(3, 2)
                .withForwardCompatible(true)
                .withProfileCore(true);
            Display.setTitle("Ultima Online: Shattered Legacy");
            Display.setVSyncEnabled(true);
            Display.setResizable(true);
            Display.create(pixFormat, contextAttribs);
            mainLoop();
        } catch (LWJGLException e) {
            log.log(Level.SEVERE, "Couldn't create display: " + e.getMessage(), e);
            mainController.onGameError("Couldn't create display: " + e.getMessage());
        }
    }

    private long getTimeMillis() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    private void mainLoop() {
        long lastFrameTime = getTimeMillis();
        long thisFrameTime = getTimeMillis();

        initGL();
        while(!Display.isCloseRequested()) {
            handleInput();

            thisFrameTime = getTimeMillis();
            update(thisFrameTime - lastFrameTime);

            render();
            lastFrameTime = thisFrameTime;

            Display.update();

            if(Display.wasResized()) {
                onResize();
            }

            Display.sync(FPS);
        }
        dispose();
    }

    private void handleInput() {
        while(Keyboard.next()) {
            if(Keyboard.getEventKeyState()) {
                // pressed
            } else {
                // released
            }
        }
    }

    private long lastMove;
    private final long moveDelay = 50;
    private void update(long elapsedMillis) {
        lastMove += elapsedMillis;
        while(lastMove > moveDelay) {
            lastMove -= moveDelay;
            if(Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                mainController.onRequestMove(Direction.SOUTH_EAST);
            } else if(Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                mainController.onRequestMove(Direction.NORTH_WEST);
            } else if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                mainController.onRequestMove(Direction.SOUTH_WEST);
            } else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                mainController.onRequestMove(Direction.NORTH_EAST);
            }
        }
    }

    private void initGL() {
        shader = new ShaderProgram();
        try {
            shader.setVertexShader(Paths.get("shaders", "tile.vert"));
            shader.setFragmentShader(Paths.get("shaders", "tile.frag"));
            shader.link();
        } catch (Exception e) {
            shader.dispose();
            log.log(Level.SEVERE, "Couldn't load shader: " + e.getMessage(), e);
            mainController.onGameError("Couldn't load shader: " + e.getMessage());
            return;
        }

        log.fine("Loading textures into GPU...");
        TexturePool.load();
        log.fine("Done loading textures");

        onResize();
        glEnable(GL_DEPTH_TEST);
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        FloatBuffer vertices = BufferUtils.createFloatBuffer(12);
        vertices.put(new float[] {
                -0.5f, -0.5f, +0.0f, // left bottom     -> South
                -0.5f, +0.5f, +0.0f, // left top        -> NorthWest
                +0.5f, +0.5f, +0.0f, // right top       -> East
                +0.5f, -0.5f, +0.0f, // right bottom    -> SouthEast
        });
        vertices.rewind();

        FloatBuffer texCoords = BufferUtils.createFloatBuffer(8);
        texCoords.put(new float[] {
                +0.5f, +0.0f, // bottom
                +0.0f, +0.5f, // left
                +0.5f, +1.0f, // top
                +1.0f, +0.5f, // right

        });
        texCoords.rewind();

        FloatBuffer texCoordsTexture = BufferUtils.createFloatBuffer(8);
        texCoordsTexture.put(new float[] {
                +0.0f, +0.0f, // bottom
                +0.0f, +1.0f, // left
                +1.0f, +1.0f, // top
                +1.0f, +0.0f, // right

        });
        texCoordsTexture.rewind();

        ShortBuffer elements = BufferUtils.createShortBuffer(4);
        elements.put(new short[] {
                0, 1, 3, 2
        });
        elements.rewind();

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);
            vboID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            texID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, texID);
            glBufferData(GL_ARRAY_BUFFER, texCoords, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

            texTextureID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, texTextureID);
            glBufferData(GL_ARRAY_BUFFER, texCoordsTexture, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);

            eboID = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    protected void projectOrtho(int width, int height) {
        projection = Transform.orthographic(0, 0, width, height, 128f, -128f);
        projection.translate(width / 2.0f, height / 2.0f, 0);
        projection.rotate(0, 0, 45);
        float scale = SLArt.TILE_DIAMETER / (float) Math.sqrt(2);
        projection.scale(scale, scale, 1f);
    }

    protected void projectPerspective(int width, int height) {
        projection = Transform.perspective(45, (float) width / height, 0.1f, 100f);
        projection.rotate(0, 0, -45);
        projection.scale(1, -1, 1);
    }

    private void onResize() {
        int width = Display.getWidth();
        int height = Display.getHeight();
        glViewport(0, 0, width, height);
        //projectPerspective(width, height);
        projectOrtho(width, height);

        double radiusX = (width / (double) SLArt.TILE_DIAMETER);
        double radiusY = (height / (double) SLArt.TILE_DIAMETER);
        int radius = (int) (Math.max(radiusX, radiusY) + 0.5);
        game.setUpdateRange(radius);
    }

    private int getZ(int x, int y) {
        return SLData.get().getMap().getTileElevation(x, y);
    }

    private void render() {
        int centerX = game.getPlayer().getLocation().getX();
        int centerY = game.getPlayer().getLocation().getY();
        int centerZ = game.getPlayer().getLocation().getZ();
        int radius = game.getUpdateRange();

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        view.reset();
        view.translate(-centerX, -centerY, -centerZ - 20);
        view.scale(1.0f / SLArt.TILE_DIAMETER, 1.0f / SLArt.TILE_DIAMETER, 4.0f / SLArt.TILE_DIAMETER);

        for(int x = centerX - radius; x < centerX + radius; x++) {
            for(int y = centerY - radius; y < centerY + radius; y++) {
                boolean shouldProject = false, canProject = false;
                int selfZ = 0, eastZ = 0, southZ = 0, southEastZ = 0;
                Texture texture;

                if(x < 0 || x >= SLMap.MAP_WIDTH || y < 0 || y >= SLMap.MAP_HEIGHT) {
                    texture = TexturePool.getLandTexture(1); // VOID texture like in real client
                } else {
                    Point3D self = new Point3D(x, y, SLData.get().getMap().getTileElevation(x, y));
                    int landID = SLData.get().getMap().getTextureID(self);
                    LandTile landTile = SLData.get().getTiles().getLandTile(landID);
                    selfZ = self.getZ();
                    eastZ = getZ(x + 1, y);
                    southZ = getZ(x, y + 1);
                    southEastZ = getZ(x + 1, y + 1);
                    shouldProject = (selfZ != eastZ) || (selfZ != southZ) || (selfZ != southEastZ);
                    canProject = (landTile != null && landTile.textureID != 0);
                    if(shouldProject && canProject) {
                        texture = TexturePool.getStaticTexture(landTile.textureID);
                        shader.setUniform("isLandTexture", true);
                    } else {
                        texture = TexturePool.getLandTexture(landID);
                        shader.setUniform("isLandTexture", false);
                    }
                }
                texture.setTextureUnit(0);
                texture.bind();
                shader.setUniform("tex", 0);
                shader.setUniform("zOffsets", southZ, selfZ, eastZ, southEastZ);
                model.reset();
                model.scale(SLArt.TILE_DIAMETER, SLArt.TILE_DIAMETER, 1);
                model.translate(x, y, 0);
                shader.setUniform("mat", model.combine(view).combine(projection));
                glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, 0);
            }
        }
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
        shader.unbind();
    }

    public void dispose() {
        if(shader != null) {
            shader.dispose();
            shader = null;
        }

        if(vaoID != null) {
            glBindVertexArray(0);
            glDeleteVertexArrays(vaoID);
            vaoID = null;
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        if(vboID != null) {
            glDeleteBuffers(vboID);
            vboID = null;
        }

        if(texID != null) {
            glDeleteBuffers(texID);
            texID = null;
        }

        if(texTextureID != null) {
            glDeleteBuffers(texTextureID);
            texTextureID = null;
        }

        if(eboID != null) {
            glDeleteBuffers(eboID);
            eboID = null;
        }

        Display.destroy();
        mainController.onGameClosed();
    }
}
