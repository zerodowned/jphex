package org.solhost.folko.uosl.slclient.views;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.solhost.folko.uosl.libuosl.data.SLArt;
import org.solhost.folko.uosl.libuosl.data.SLData;
import org.solhost.folko.uosl.libuosl.data.SLMap;
import org.solhost.folko.uosl.libuosl.data.SLStatic;
import org.solhost.folko.uosl.libuosl.data.SLStatics;
import org.solhost.folko.uosl.libuosl.data.SLTiles;
import org.solhost.folko.uosl.libuosl.data.SLArt.MobileAnimation;
import org.solhost.folko.uosl.libuosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.libuosl.data.SLTiles.StaticTile;
import org.solhost.folko.uosl.libuosl.types.Direction;
import org.solhost.folko.uosl.libuosl.types.Point2D;
import org.solhost.folko.uosl.libuosl.types.Point3D;
import org.solhost.folko.uosl.slclient.controllers.MainController;
import org.solhost.folko.uosl.slclient.models.GameState;
import org.solhost.folko.uosl.slclient.models.SLItem;
import org.solhost.folko.uosl.slclient.models.SLMobile;
import org.solhost.folko.uosl.slclient.models.TexturePool;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class GameView {
    private static final Logger log = Logger.getLogger("slclient.gameview");
    private static final int DEFAULT_WIDTH  = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final float GRID_DIAMETER = 42.0f;
    private static final float GRID_EDGE     = GRID_DIAMETER / (float) Math.sqrt(2);
    private static final float PROJECTION_CONSTANT = 4.0f;
    private static final int FPS = 20;

    private final SLMap map;
    private final SLTiles tiles;
    private final SLStatics statics;
    private final SLArt art;

    private final MainController mainController;
    private final GameState game;

    private Transform projection, view, model;
    private ShaderProgram shader;
    private Integer vaoID, vboID, eboID;
    private int texLocation, zOffsetLocation, matLocation, texTypeLocation;

    private boolean gridOnly;
    private float zoom = 1.0f;

    private int animFrameCounter;
    private final int animDelay = 100;
    private long nextAnimFrameIncrease = animDelay;

    private long fpsCounter, lastFPSReport;

    public GameView(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();
        this.map = SLData.get().getMap();
        this.art = SLData.get().getArt();
        this.tiles = SLData.get().getTiles();
        this.statics = SLData.get().getStatics();

        try {
            Display.setDisplayMode(new DisplayMode(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        } catch (LWJGLException e) {
            log.log(Level.SEVERE, "Couldn't set display mode: " + e.getMessage(), e);
            mainController.onGameError("Couldn't set display mode: " + e.getMessage());
        }

        projection = new Transform();
        gridOnly = false;
    }

    public void createWindow() {
        try {
            PixelFormat pixFormat = new PixelFormat();
            ContextAttribs contextAttribs = new ContextAttribs(3, 2)
                .withForwardCompatible(true)
                .withProfileCore(true);
            Display.setTitle("Ultima Online: Shattered Legacy");
            Display.setResizable(true);
            Display.create(pixFormat, contextAttribs);
            initGL();
        } catch (LWJGLException e) {
            log.log(Level.SEVERE, "Couldn't create display: " + e.getMessage(), e);
            mainController.onGameError("Couldn't create display: " + e.getMessage());
            return;
        }

        lastFPSReport = game.getTimeMillis();
    }

    public void render() {
        if(Display.isCloseRequested()) {
            mainController.onGameWindowClosed();
            return;
        }

        if(Display.wasResized()) {
            onResize();
        }

        renderGameScene();
        updateFPS();
        Display.update();
    }

    public void pause() {
        Display.sync(FPS);
    }

    private void updateFPS() {
        // update FPS stats each second
        if(game.getTimeMillis() - lastFPSReport > 1000) {
            mainController.onReportFPS(fpsCounter);
            fpsCounter = 0;
            lastFPSReport += 1000;
        }
        fpsCounter++;
    }

    private void handleInput() {
        handleAsyncInput();
        handleSyncInput();
    }

    public void update(long elapsedMillis) {
        nextAnimFrameIncrease -= elapsedMillis;
        if(nextAnimFrameIncrease < 0) {
            animFrameCounter++;
            nextAnimFrameIncrease = animDelay;
        }

        handleInput();
    }

    private void handleAsyncInput() {
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
        while(Mouse.next()) {
            int dz = Mouse.getEventDWheel();
            if(dz > 0) {
                onZoom(1.05f);
            } else if(dz < 0) {
                onZoom(0.95f);
            }
        }
    }

    private void handleSyncInput() {
        if(Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            mainController.onRequestMove(Direction.SOUTH_EAST);
        } else if(Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            mainController.onRequestMove(Direction.NORTH_WEST);
        } else if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            mainController.onRequestMove(Direction.SOUTH_WEST);
        } else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            mainController.onRequestMove(Direction.NORTH_EAST);
        }

        if(Keyboard.isKeyDown(Keyboard.KEY_RBRACKET)) {
            onZoom(1.05f);
        } else if(Keyboard.isKeyDown(Keyboard.KEY_SLASH)) {
            onZoom(0.95f);
        }

        if(Mouse.isButtonDown(1)) {
            double midX = Display.getWidth() / 2.0;
            double midY = Display.getHeight() / 2.0;

            double angle = Math.toDegrees(Math.atan2(Mouse.getX() - midX, Mouse.getY() - midY));
            if(angle < 0) {
                angle = 360 + angle;
            }

            mainController.onRequestMove(Direction.fromAngle(angle));
        }
    }

    private void initGL() {
        shader = new ShaderProgram();
        try {
            shader.setVertexShader(Paths.get("shaders", "tile.vert"));
            shader.setFragmentShader(Paths.get("shaders", "tile.frag"));
            shader.link();
            matLocation = shader.getUniformLocation("mat");
            zOffsetLocation = shader.getUniformLocation("zOffsets");
            texLocation = shader.getUniformLocation("tex");
            texTypeLocation = shader.getUniformLocation("textureType");
        } catch (Exception e) {
            shader.dispose();
            log.log(Level.SEVERE, "Couldn't load shader: " + e.getMessage(), e);
            mainController.onGameError("Couldn't load shader: " + e.getMessage());
            return;
        }

        log.fine("Loading textures into GPU...");
        TexturePool.load();
        log.fine("Done loading textures");

        // glDepthFunc(GL_LEQUAL);
        // glEnable(GL_DEPTH_TEST);

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);

        model = new Transform();

        onResize();

        FloatBuffer vertices = BufferUtils.createFloatBuffer(8);
        vertices.put(new float[] {
                0, 0, // left bottom
                0, 1, // left top
                1, 1, // right top
                1, 0, // right bottom
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
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);

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
        projection.scale(zoom, zoom, 1);
        onZoom(1.0f);
    }

    private void onZoom(float f) {
        int width = Display.getWidth();
        int height = Display.getHeight();

        projection.scale(f, f, 1);
        zoom *= f;
        float radiusX = (width / zoom / GRID_DIAMETER);
        float radiusY = (height / zoom / GRID_DIAMETER);
        int radius = (int) (Math.max(radiusX, radiusY) + 0.5);
        mainController.onUpdateRangeChange(radius);
    }

    private int getZ(int x, int y) {
        return map.getTileElevation(x, y);
    }

    private void renderGameScene() {
        int centerX = game.getPlayer().getLocation().getX();
        int centerY = game.getPlayer().getLocation().getY();
        int centerZ = game.getPlayer().getLocation().getZ();
        int radius = game.getUpdateRange();

        glClear(GL_COLOR_BUFFER_BIT /* | GL_DEPTH_BUFFER_BIT */);

        shader.bind();
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);

        view = Transform.UO(GRID_DIAMETER, PROJECTION_CONSTANT);
        view.translate(-centerX, -centerY, -centerZ);

        shader.setUniformFloat(texLocation, 0);

        for(int x = centerX - radius; x < centerX + radius; x++) {
            for(int y = centerY - radius; y < centerY + radius; y++) {
                // draw land even at invalid locations: will draw void like real client
                drawLand(x, y);

                // but don't attempt to draw anything else at invalid locations
                if(x < 0 || x >= SLMap.MAP_WIDTH || y < 0 || y >= SLMap.MAP_HEIGHT) {
                    continue;
                }

                // now draw items and mobiles so that they cover the land
                Point2D point = new Point2D(x, y);
                if(point.equals(game.getPlayer().getLocation())) {
                    drawMobile(game.getPlayer());
                }

                game.forEachObjectAt(point, (obj) -> {
                    if(obj instanceof SLMobile) {
                        drawMobile((SLMobile) obj);
                    } else if(obj instanceof SLItem) {
                        drawItem((SLItem) obj);
                    }
                 });

                // draw statics last so they cover mobiles
                for(SLStatic sta : sortStatics(statics.getStatics(point))) {
                    SLItem staItm = SLItem.fromStatic(sta);
                    drawItem(staItm);
                }
            }
        }
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
        shader.unbind();
    }

    private void drawLand(int x, int y) {
        Point3D point;
        boolean shouldProject = false, canProject = false;
        int selfZ = 0, eastZ = 0, southZ = 0, southEastZ = 0;
        Texture texture;

        if(x < 0 || x >= SLMap.MAP_WIDTH || y < 0 || y >= SLMap.MAP_HEIGHT) {
            point = null;
            texture = TexturePool.getLandTexture(1); // VOID texture like in real client
        } else {
            point = new Point3D(x, y, map.getTileElevation(x, y));
            int landID = map.getTextureID(point);
            LandTile landTile = tiles.getLandTile(landID);
            selfZ = point.getZ();
            eastZ = getZ(x + 1, y);
            southZ = getZ(x, y + 1);
            southEastZ = getZ(x + 1, y + 1);
            shouldProject = (selfZ != eastZ) || (selfZ != southZ) || (selfZ != southEastZ);
            canProject = (landTile != null && landTile.textureID != 0);
            if(shouldProject && canProject) {
                texture = TexturePool.getStaticTexture(landTile.textureID);
                shader.setUniformInt(texTypeLocation, 1);
            } else {
                texture = TexturePool.getLandTexture(landID);
                shader.setUniformInt(texTypeLocation, 0);
            }
            if(texture == null) {
                texture = TexturePool.getLandTexture(0);
            }
        }
        texture.setTextureUnit(0);
        texture.bind();
        shader.setUniformFloat(zOffsetLocation, selfZ, southZ, southEastZ, eastZ);

        model.reset();
        model.translate(x, y, 0);
        shader.setUniformMatrix(matLocation, model.combine(view).combine(projection));
        glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, 0);
    }

    private void drawItem(SLItem item) {
        int x = item.getLocation().getX();
        int y = item.getLocation().getY();
        int z = item.getLocation().getZ();

        shader.setUniformInt(texTypeLocation, 1);
        shader.setUniformFloat(zOffsetLocation, 0, 0, 0, 0);
        Texture texture = TexturePool.getStaticTexture(item.getGraphic());
        if(texture == null) {
            log.warning("No texture for item with graphic: " + item.getGraphic());
            return;
        }
        texture.setTextureUnit(0);
        texture.bind();

        Transform textureProjection = new Transform(projection);
        textureProjection.translate(-texture.getWidth() / 2.0f, GRID_DIAMETER - texture.getHeight(), 0);

        model.reset();
        model.translate(x, y, z);
        model.rotate(0, 0, -45);
        model.scale(texture.getWidth() / GRID_EDGE, texture.getHeight() / GRID_EDGE, 1f);
        shader.setUniformMatrix(matLocation, model.combine(view).combine(textureProjection));
        glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, 0);
    }

    private void drawMobile(SLMobile mobile) {
        boolean fighting = false;
        int x = mobile.getLocation().getX();
        int y = mobile.getLocation().getY();
        int z = mobile.getLocation().getZ();
        int graphic = mobile.getGraphic();
        Direction facing = mobile.getFacing();

        // first character
        MobileAnimation animation = art.getAnimationEntry(graphic, facing, fighting);
        if(animation == null) {
            log.warning("No animation for mobile " + graphic + " with facing " + facing + ", fight: " + fighting);
            return;
        }
        drawAnimationFrame(animation, x, y, z);

        // then equipment
        for(SLItem equipItem : mobile.getEquipment()) {
            int id = equipItem.getTileInfo().animationID;
            animation = art.getAnimationEntry(id, facing, fighting);
            if(animation == null) {
                log.warning("No animation for equipment " + graphic + " with facing " + facing + ", fight: " + fighting);
                continue;
            }
            drawAnimationFrame(animation, x, y, z);
        }
    }

    private void drawAnimationFrame(MobileAnimation animation, int x, int y, int z) {
        int numFrames = animation.frames.size();
        Texture texture = TexturePool.getAnimationFrame(animation.frames.get(animFrameCounter % numFrames));
        texture.setTextureUnit(0);
        texture.bind();
        shader.setUniformInt(texTypeLocation, 1);
        shader.setUniformFloat(zOffsetLocation, 0, 0, 0, 0);
        shader.setUniformInt(texTypeLocation, 2);
        Transform textureProjection = new Transform(projection);
        textureProjection.translate(-texture.getWidth() / 2.0f, GRID_DIAMETER - texture.getHeight(), 0);
        model.reset();
        model.translate(x, y, z);
        model.rotate(0, 0, -45);
        if(animation.needMirror) {
            model.translate(texture.getWidth() / GRID_EDGE / 2.0f, 0, 0);
            model.rotate(0, 180, 0);
            model.translate(-texture.getWidth() / GRID_EDGE / 2.0f, 0, 0);
        }
        model.scale(texture.getWidth() / GRID_EDGE, texture.getHeight() / GRID_EDGE, 1f);
        shader.setUniformMatrix(matLocation, model.combine(view).combine(textureProjection));
        glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, 0);
    }

    public synchronized void close() {
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
    }

    private List<SLStatic> sortStatics(List<SLStatic> in) {
        // sort by view order (painter's algorithm)
        Collections.sort(in, (o1, o2) -> {
                StaticTile tile1 = tiles.getStaticTile(o1.getStaticID());
                StaticTile tile2 = tiles.getStaticTile(o2.getStaticID());
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
            });
        return in;
    }
}
