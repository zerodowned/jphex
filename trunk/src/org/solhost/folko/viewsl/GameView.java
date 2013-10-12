/*******************************************************************************
 * Copyright (c) 2013 Folke Will <folke.will@gmail.com>
 * 
 * This file is part of JPhex.
 * 
 * JPhex is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JPhex is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.solhost.folko.viewsl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLMap;
import org.solhost.folko.uosl.data.SLStatic;
import org.solhost.folko.uosl.data.SLStatics;
import org.solhost.folko.uosl.data.SLTiles;
import org.solhost.folko.uosl.data.SLArt.ArtEntry;
import org.solhost.folko.uosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Point2D;
import org.solhost.folko.uosl.types.Point3D;
import org.solhost.folko.uosl.util.Pathfinder;

public class GameView extends JPanel {
    private static final long serialVersionUID = 1070070853406868736L;
    private final Map<Integer, Image> mapTileCache;
    private final Map<Integer, Image> staticTileCache;
    private Point3D sceneCenter;
    private final SLData data;
    private final SLMap map;
    private final SLArt art;
    private final SLTiles tiles;
    private final SLStatics statics;
    private final Timer redrawTimer;
    private long lastRedraw, drawDuration, lastFPSUpdate;
    private int redrawDelay;
    private double lastFPS;
    private Pathfinder finder;

    private static final int TILE_SIZE = 44;
    private static final double TARGET_FPS = 25.0;

    public GameView(final SLData data) {
        this.data = data;
        this.map = data.getMap();
        this.art = data.getArt();
        this.statics = data.getStatics();
        this.tiles = data.getTiles();
        this.mapTileCache = new HashMap<Integer, Image>();
        this.staticTileCache = new HashMap<Integer, Image>();
        this.sceneCenter = new Point3D(379, 607, 0); // 600, 700 has interesting Z
        this.lastRedraw = System.currentTimeMillis();
        this.redrawDelay = (int) (1000.0 / TARGET_FPS);
        this.redrawTimer = new Timer(redrawDelay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });

        SLData.DEBUG_MOVE = false;

        setMinimumSize(new Dimension(800, 600));
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);

        addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                redrawTimer.stop();
            }

            public void focusGained(FocusEvent e) {
                redrawTimer.start();
            }
        });

        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) { }
            public void keyReleased(KeyEvent e) { }

            public void keyPressed(KeyEvent e) {
                Direction dir = null;
                switch(e.getKeyCode()) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    dir = Direction.NORTH_WEST;
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    dir = Direction.SOUTH_EAST;
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    dir = Direction.SOUTH_WEST;
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    dir = Direction.NORTH_EAST;
                    break;
                case KeyEvent.VK_Q:
                case KeyEvent.VK_HOME:
                    dir = Direction.WEST;
                    break;
                case KeyEvent.VK_Y:
                case KeyEvent.VK_END:
                    dir = Direction.SOUTH;
                    break;
                case KeyEvent.VK_E:
                case KeyEvent.VK_PAGE_UP:
                    dir = Direction.NORTH;
                    break;
                case KeyEvent.VK_C:
                case KeyEvent.VK_PAGE_DOWN:
                    dir = Direction.EAST;
                    break;
                case KeyEvent.VK_I:
                    showStatics();
                    break;
                }
                if(dir != null) {
                    // shift forces location
                    moveCenter(dir, e.isShiftDown());
                }
            }
        });

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Direction dir;
                double rot = e.getPreciseWheelRotation();
                int num = (int) Math.max(1, Math.abs(rot * 0.4));
                if(e.getModifiers() == 0) {
                    if(rot < 0) {
                        dir = Direction.NORTH_WEST;
                    } else {
                        dir = Direction.SOUTH_EAST;
                    }
                } else {
                    if(rot < 0) {
                        dir = Direction.SOUTH_WEST;
                    } else {
                        dir = Direction.NORTH_EAST;
                    }
                }
                for(int i = 0; i < num; i++) {
                    moveCenter(dir, e.isMetaDown());
                }
            }
        });

        redrawTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        int drawDist = Math.max(getWidth() / TILE_SIZE - 1, getHeight() / TILE_SIZE - 1);

        Point3D point = new Point3D(0, 0, 0);
        for(int y = sceneCenter.getY() - drawDist; y <= sceneCenter.getY() + drawDist; y++) {
            for(int x = sceneCenter.getX() - drawDist; x <= sceneCenter.getX() + drawDist; x++) {
                if(x >= 0 && x < 1024 && y >= 0 && y < 1024) {
                    point.setX(x);
                    point.setY(y);
                    point.setZ(map.getElevation(point));
                    drawMapTile(g, point);
                    drawStatics(g, point);
                }
            }
        }

        markPoint(g, sceneCenter);
        drawInfo(g);
        drawPath(g);
        // drawSlopes(g);
        controlFPS();
    }

    private void controlFPS() {
        this.drawDuration = System.currentTimeMillis() - lastRedraw;
        this.lastRedraw = System.currentTimeMillis();
        if(System.currentTimeMillis() - lastFPSUpdate > 500) {
            lastFPSUpdate = System.currentTimeMillis();
            lastFPS = 1000.0 / drawDuration;
            double off = lastFPS - TARGET_FPS;
            if(off > 1) {
                redrawDelay += 5;
            } else if(off < -1 && redrawDelay >= 5) {
                redrawDelay -= 5;
            }
            redrawTimer.setDelay(redrawDelay);
        }
    }

    private void markPoint(Graphics g, Point3D p) {
        Point center = transform(p);
        Point bottom = new Point(center.x, center.y + TILE_SIZE / 2);
        Point left = new Point(center.x - TILE_SIZE / 2, center.y);
        Point top = new Point(center.x,                 center.y - TILE_SIZE / 2);
        Point right = new Point(center.x + TILE_SIZE / 2, center.y);

        g.setColor(Color.pink);
        g.drawLine(top.x, top.y, left.x, left.y);
        g.drawLine(left.x, left.y, bottom.x, bottom.y);
        g.drawLine(bottom.x, bottom.y, right.x, right.y);
        g.drawLine(right.x, right.y, top.x, top.y);

        g.setColor(Color.red);
        g.fillOval(center.x - 5, center.y - 5, 10, 10);
    }

    private void drawPath(Graphics g) {
        if(finder != null && finder.hasPath()) {
            List<Direction> path = finder.getPath();
            Point3D curLoc = finder.getStart();
            Point lastPoint = transform(curLoc), curPoint;
            for(Direction dir : path) {
                Point3D nextLoc = data.getElevatedPoint(curLoc, dir, statics);
                if(nextLoc == null) {
                    System.err.println("invalid path move: " + dir);
                    break;
                }
                curPoint = transform(nextLoc);
                g.drawLine(lastPoint.x, lastPoint.y, curPoint.x, curPoint.y);
                lastPoint = curPoint;
                curLoc = nextLoc;
            }
        }
    }

    private void drawInfo(Graphics g) {
        String info = String.format("Standing at %4d, %4d, %4d with %.2f FPS",
                sceneCenter.getX(), sceneCenter.getY(), sceneCenter.getZ(), lastFPS);
        g.setColor(Color.red);
        g.drawString(info, 10, 20);

        Point mousePoint = getMousePosition();
        if(mousePoint != null) {
            Point2D igp = transform(mousePoint);
            info = String.format("Mouse at %4d, %4d on screen -> %4d, %4d in game",
                    mousePoint.x, mousePoint.y, igp.getX(), igp.getY());
            g.drawString(info, 10, 35);
        }
    }

    private void showStatics() {
        System.out.println("Statics at " + sceneCenter.getX() + ", " + sceneCenter.getY());
        for(SLStatic stat : sortStatics(statics.getStatics(sceneCenter))) {
            StaticTile tile = tiles.getStaticTile(stat.getStaticID());
            String info = String.format("Z = %3d, height = %3d -> 0x%04X 0x%08X (%s)", stat.getLocation().getZ(), tile.height, stat.getStaticID(), tile.flags, tile.name);
            System.out.println(info);
        }
        int landID = map.getTextureID(sceneCenter);
        System.out.println(String.format("Land: %04X", landID));
        System.out.println("====");
    }

    private void moveCenter(Direction dir, boolean force) {
        Point3D dest = data.getElevatedPoint(sceneCenter, dir, statics);
        if(dest != null) {
            sceneCenter = dest;
        } else if(force) {
            // hack mover
            Point2D hackDest = sceneCenter.getTranslated(dir);
            sceneCenter.setX(hackDest.getX());
            sceneCenter.setY(hackDest.getY());
            // keep old Z
        }
        if(finder == null) {
            finder = new Pathfinder(sceneCenter, new Point3D(409,678, 0), statics);
            finder.findPath(2000);
            if(finder.hasPath()) {
                System.out.println(finder.getPathInfo());
            }
        }
    }

    private void drawMapTile(Graphics g, Point3D pos) {
        Point center = transform(pos);
        int landID = map.getTextureID(pos);
        Image image = getMapTileImage(landID);

        Polygon poly = new Polygon();
        Double theta = getPointPolygon(poly, pos);
        if(theta == null) {
            g.drawImage(image, center.x - TILE_SIZE / 2, center.y - TILE_SIZE / 2, null);
        } else {
            LandTile tile = tiles.getLandTile(landID);
            Image texture = image;
            if(tile.textureID != 0) {
                texture = getStaticTileImage(tile.textureID);
            }
            drawTexture(g, poly, texture, theta);
        }
    }

    private List<SLStatic> sortStatics(List<SLStatic> in) {
        // sort by view order
        Collections.sort(in, new Comparator<SLStatic>() {
            public int compare(SLStatic o1, SLStatic o2) {
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
            }
        });
        return in;
    }

    private void drawStatics(Graphics g, Point3D pos) {
        pos.setZ(0);
        Point center = transform(pos);
        for(SLStatic s : sortStatics(statics.getStatics(pos))) {
            if(s.getLocation().getZ() - sceneCenter.getZ() > 10) {
                continue;
            }

            Image image = getStaticTileImage(s.getStaticID());
            if(image != null) {
                int xOff = 0, yOff = 0;
                int z = s.getLocation().getZ() * 4;

                xOff = -(TILE_SIZE / 2);
                yOff = TILE_SIZE / 2 - image.getHeight(null) - z;
                if(image.getWidth(null) > TILE_SIZE) {
                    xOff -= (image.getWidth(null) - TILE_SIZE) / 2;
                }
                g.drawImage(image, center.x + xOff, center.y + yOff, null);
            }
        }
    }

    private Image getMapTileImage(int id) {
        Image image;
        if(!mapTileCache.containsKey(id)) {
            ArtEntry entry = art.getLandArt(id);
            if(entry != null) {
                image = entry.image;
                mapTileCache.put(id, image);
            } else {
                image = null;
            }
        } else {
            image = mapTileCache.get(id);
        }
        return image;
    }

    private Image getStaticTileImage(int id) {
        Image image;
        if(!staticTileCache.containsKey(id)) {
            StaticTile tile = tiles.getStaticTile(id);
            ArtEntry entry = art.getStaticArt(id, (tile.flags & StaticTile.FLAG_TRANSLUCENT) != 0);
            if(entry != null) {
                image = entry.image;
                // TODO: I don't think this is how it's supposed to work
                if((tile.flags & StaticTile.FLAG_TRANSLUCENT) != 0 || ((tile.flags & StaticTile.FLAG_ROOF) != 0)) {
                    int width = image.getWidth(null);
                    int height = image.getHeight(null);
                    image = image.getScaledInstance(width + 5, height + 5, Image.SCALE_DEFAULT);
                }
                staticTileCache.put(id, image);
            } else {
                image = null;
            }
        } else {
            image = staticTileCache.get(id);
        }
        return image;
    }

    private void drawTexture(Graphics g, Polygon poly, Image image, double angle) {
        for(int i = 0; i < poly.npoints; i++) {
            if(poly.xpoints[i] < 0) poly.xpoints[i] = 0;
            if(poly.ypoints[i] < 0) poly.ypoints[i] = 0;
            if(poly.xpoints[i] >= getWidth() - 1) poly.xpoints[i] = getWidth() - 1;
            if(poly.ypoints[i] >= getHeight() - 1) poly.ypoints[i] = getHeight() - 1;
        }
        Shape before = g.getClip();
        g.setClip(poly);
        Rectangle box = poly.getBounds();
        if(box.width > 0 && box.height > 0) {
            if(angle != 0.0) {
                AffineTransform rot = AffineTransform.getRotateInstance(angle, image.getWidth(null) / 2.0, image.getHeight(null) / 2.0);
                AffineTransformOp op = new AffineTransformOp(rot, AffineTransformOp.TYPE_BILINEAR);
                image = op.filter((BufferedImage) image, null);
            }
            g.drawImage(image, box.x, box.y, null);
        }
        g.setClip(before);
    }

    private int getZ(int x, int y) {
        if(x < 0 || x >= 1024 || y < 0 || y >= 1024) {
            return 0;
        }
        Point2D point = new Point3D(x, y);
        return map.getElevation(point);
    }

    private Double getPointPolygon(Polygon dest, Point3D point) {
        int east = getZ(point.getX() + 1, point.getY());
        int south = getZ(point.getX(), point.getY() + 1);
        int southEast = getZ(point.getX() + 1, point.getY() + 1);
        int our = point.getZ();

        Point tCenter = transform(point);
        tCenter.y -= TILE_SIZE / 2;

        int y2 = TILE_SIZE / 2 + (our - east) * 4;
        int y3 = TILE_SIZE     + (our - southEast) * 4;
        int y4 = TILE_SIZE / 2 + (our - south) * 4;

        if(y2 == TILE_SIZE / 2 && y3 == TILE_SIZE  && y4 == TILE_SIZE / 2) {
            // TODO: Check what real client does
            return null;
        }

        dest.reset();
        dest.addPoint(tCenter.x, tCenter.y);
        dest.addPoint(tCenter.x + TILE_SIZE / 2, tCenter.y + y2);
        dest.addPoint(tCenter.x, tCenter.y + y3);
        dest.addPoint(tCenter.x - TILE_SIZE / 2, tCenter.y + y4);
        return 0.0;
    }

    protected void drawSlopes(Graphics g) {
        Polygon poly = new Polygon();
        Double theta = getPointPolygon(poly, sceneCenter);
        if(theta != null) {
            g.setColor(Color.yellow);
            g.drawPolygon(poly);
        }
    }

    // game -> screen, returns center of tile
    private Point transform(Point3D src) {
        int width = getWidth();
        int height = getHeight();

        // direction vector in game
        int dxG = src.getX() - sceneCenter.getX();
        int dyG = src.getY() - sceneCenter.getY();
        int dzG = src.getZ() - sceneCenter.getZ();

        // direction vector on screen
        int dxS = (dxG - dyG) * TILE_SIZE / 2;
        int dyS = (dxG + dyG) * TILE_SIZE / 2 - dzG * 4;

        return new Point(width / 2 + dxS, height / 2 + dyS);
    }

    private Point3D transform(Point src) {
        int width = getWidth();
        int height = getHeight();

        // direction vector on screen
        int dxS = src.x - width / 2;
        int dyS = src.y - height / 2;

        // direction vector in game
        int dxG_ = dxS * 2 / TILE_SIZE;
        int dyG_ = dyS * 2 / TILE_SIZE;
        int dxG = (dxG_ + dyG_) / 2;
        int dyG = (dyG_ - dxG_) / 2;

        int resX = sceneCenter.getX() + dxG;
        int resY = sceneCenter.getY() + dyG;
        if(resX < 0) resX = 0;
        if(resY < 0) resY = 0;
        if(resX >= 1024) resX = 1023;
        if(resY >= 1024) resY = 1023;

        Point3D res = new Point3D(resX, resY, 0);
        res.setZ(map.getElevation(res));
        return res;
    }
}
