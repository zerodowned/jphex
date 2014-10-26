package org.solhost.folko.slclient.views;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import org.solhost.folko.uosl.data.SLStatic;
import org.solhost.folko.uosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;
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
    private static final int FPS = 30;

    private final MainController mainController;
    private final GameState game;
    private Transform projection, view;
    private final Transform model;
    private ShaderProgram shader;
    private Integer vaoID, vboID, eboID;
    private boolean gridOnly;

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
        model = new Transform();
        gridOnly = false;
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
        } catch (LWJGLException e) {
            log.log(Level.SEVERE, "Couldn't create display: " + e.getMessage(), e);
            mainController.onGameError("Couldn't create display: " + e.getMessage());
            return;
        }

        try {
            mainLoop();
        } catch(Exception e) {
            log.log(Level.SEVERE, "Game crashed: " + e.getMessage(), e);
            mainController.onGameError("Game crashed: " + e.getMessage());
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
                if(Keyboard.getEventCharacter() == 'g') {
                    gridOnly = !gridOnly;
                    if(gridOnly) {
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                    } else {
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                    }
                }
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
            } else if(Keyboard.isKeyDown(Keyboard.KEY_SLASH)) {
                projection.scale(0.95f, 0.95f, 1);
            } else if(Keyboard.isKeyDown(Keyboard.KEY_RBRACKET)) {
                projection.scale(1.05f, 1.05f, 1);
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
        // glEnable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);

        FloatBuffer vertices = BufferUtils.createFloatBuffer(12);
        vertices.put(new float[] {
                -0.5f, -0.5f, +0.0f, // left bottom
                -0.5f, +0.5f, +0.0f, // left top
                +0.5f, +0.5f, +0.0f, // right top
                +0.5f, -0.5f, +0.0f, // right bottom
        });
        vertices.rewind();

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

            eboID = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void onResize() {
        int width = Display.getWidth();
        int height = Display.getHeight();
        glViewport(0, 0, width, height);
        projection = Transform.orthographic(-width / 2.0f, -height / 2.0f, width / 2.0f, height / 2.0f, 128f, -128f);

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

        glClear(GL_COLOR_BUFFER_BIT);

        shader.bind();
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);

        view = Transform.UO(SLArt.TILE_DIAMETER, 2.5f);
        view.translate(-centerX, -centerY, -centerZ);

        shader.setUniformFloat("tex", 0);

        for(int x = centerX - radius; x < centerX + radius; x++) {
            for(int y = centerY - radius; y < centerY + radius; y++) {
                Point3D point;
                boolean shouldProject = false, canProject = false;
                int selfZ = 0, eastZ = 0, southZ = 0, southEastZ = 0;
                Texture texture;

                if(x < 0 || x >= SLMap.MAP_WIDTH || y < 0 || y >= SLMap.MAP_HEIGHT) {
                    point = null;
                    texture = TexturePool.getLandTexture(1); // VOID texture like in real client
                } else {
                    point = new Point3D(x, y, SLData.get().getMap().getTileElevation(x, y));
                    int landID = SLData.get().getMap().getTextureID(point);
                    LandTile landTile = SLData.get().getTiles().getLandTile(landID);
                    selfZ = point.getZ();
                    eastZ = getZ(x + 1, y);
                    southZ = getZ(x, y + 1);
                    southEastZ = getZ(x + 1, y + 1);
                    shouldProject = (selfZ != eastZ) || (selfZ != southZ) || (selfZ != southEastZ);
                    canProject = (landTile != null && landTile.textureID != 0);
                    if(shouldProject && canProject) {
                        texture = TexturePool.getStaticTexture(landTile.textureID);
                        shader.setUniformInt("textureType", 1);
                    } else {
                        texture = TexturePool.getLandTexture(landID);
                        shader.setUniformInt("textureType", 0);
                    }
                    if(texture == null) {
                        texture = TexturePool.getLandTexture(0);
                    }
                }
                texture.setTextureUnit(0);
                texture.bind();
                shader.setUniformFloat("zOffsets", selfZ, southZ, southEastZ, eastZ);
                model.reset();
                model.translate(x + 0.5f, y + 0.5f, 0);
                shader.setUniformMatrix("mat", model.combine(view).combine(projection));
                glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, 0);

                if(point == null) {
                    continue;
                }

                shader.setUniformInt("textureType", 2);
                shader.setUniformFloat("zOffsets", 0, 0, 0, 0);
                for(SLStatic sta : sortStatics(SLData.get().getStatics().getStatics(point))) {
                    texture = TexturePool.getStaticTexture(sta.getStaticID());
                    if(texture == null) {
                        continue;
                    }
                    texture.setTextureUnit(0);
                    texture.bind();
                    model.reset();
                    model.translate(x + 0.5f, y + 0.5f, sta.getLocation().getZ());
                    model.rotate(0, 0, -45);
                    float gridLen = SLArt.TILE_DIAMETER / (float) Math.sqrt(2);
                    model.scale(texture.getWidth() / gridLen, texture.getHeight() / gridLen, 1f);
                    shader.setUniformMatrix("mat", model.combine(view).combine(projection));
                    glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, 0);
                }
            }
        }
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

        if(eboID != null) {
            glDeleteBuffers(eboID);
            eboID = null;
        }

        Display.destroy();
        mainController.onGameClosed();
    }

    private List<SLStatic> sortStatics(List<SLStatic> in) {
        // sort by view order
        Collections.sort(in, new Comparator<SLStatic>() {
            public int compare(SLStatic o1, SLStatic o2) {
                StaticTile tile1 = SLData.get().getTiles().getStaticTile(o1.getStaticID());
                StaticTile tile2 = SLData.get().getTiles().getStaticTile(o2.getStaticID());
                int z1 = o1.getLocation().getZ();
                int z2 = o2.getLocation().getZ();

                if((tile1.flags & StaticTile.FLAG_BACKGROUND) != 0) {
                    if((tile2.flags & StaticTile.FLAG_BACKGROUND) == 0) {
                        // draw background first so it will be overdrawn by statics
                        if(z1 > z2) {
                            // but only if there is nothing below it
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        return z1 - z2;
                    }
                }
                // default
                return z1 - z2;
            }
        });
        return in;
    }
}
