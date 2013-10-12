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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLTiles;
import org.solhost.folko.uosl.data.SLArt.ArtEntry;
import org.solhost.folko.uosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;

public class ArtView extends JPanel {
    private static final long serialVersionUID = 4594116320550647856L;
    private ImagePanel imagePanel;
    private JList<String> artList;
    private StaticInfoPanel staticPanel;
    private LandInfoPanel landPanel;
    private SLArt art;
    private SLTiles tiles;
    private boolean landOrStatic;

    private static final int NUM_ENTRIES = 0x4000;

    public ArtView(boolean landOrStatic, SLArt art, SLTiles tiles) {
        this.landOrStatic = landOrStatic;
        this.tiles = tiles;
        this.art = art;
        this.imagePanel = new ImagePanel(200, 200);
        this.staticPanel = new StaticInfoPanel();
        this.landPanel = new LandInfoPanel();
        
        setLayout(new BorderLayout());
        
        DefaultListModel<String> model = new DefaultListModel<String>();
        artList = new JList<String>(model);
        for(int i = 0; i < NUM_ENTRIES; i++)  {
            String name = null;
            ArtEntry entry;
            long flags = 0;
            if(landOrStatic) {
                LandTile tile = tiles.getLandTile(i);
                name = tile.name;
                flags = tile.flags;
                entry = art.getLandArt(i);
            } else {
                StaticTile tile = tiles.getStaticTile(i);
                name = tile.name;
                flags = tile.flags;
                entry = art.getStaticArt(i, (flags & StaticTile.FLAG_TRANSLUCENT) != 0);
            }
            String info = String.format("0x%04X: %s", i, name);
            if(entry != null || name.length() > 0 || flags != 0) {
                model.addElement(info);
            }
        }
        
        JPanel artInfoPanel = new JPanel();
        artInfoPanel.setLayout(new GridLayout(2, 1));
        if(landOrStatic) {
            artInfoPanel.add(landPanel);
        } else {
            artInfoPanel.add(staticPanel);
        }
        artInfoPanel.add(imagePanel);
       
        artList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        artList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(e.getValueIsAdjusting()) return;
                selectArt(artList.getSelectedIndex());
            }
        });
        artList.setSelectedIndex(0);
        
        add(new JScrollPane(artList), BorderLayout.WEST);
        add(artInfoPanel, BorderLayout.CENTER);
    }

    private void selectArt(int idx) {
        ArtEntry entry;
        String name = "";
        int id = Integer.parseInt(artList.getSelectedValue().substring(2, 6), 16);
        if(landOrStatic) {
            LandTile tile = tiles.getLandTile(id);
            landPanel.setTile(tile);
            name = tile.name;
            entry = art.getLandArt(id);
            if(entry != null) {
                String info = String.format("0x%04X: %s", entry.id, name);
                landPanel.setBorder(BorderFactory.createTitledBorder(info));
            }
        } else {
            StaticTile tile = tiles.getStaticTile(id);
            staticPanel.setTile(tile);
            name = tile.name;
            entry = art.getStaticArt(id, false);
            if(entry != null) {
                String info = String.format("0x%04X: %s", entry.id, name);
                staticPanel.setBorder(BorderFactory.createTitledBorder(info));
            }
        }
        if(entry != null) {
            int width = entry.image.getWidth();
            int height = entry.image.getHeight();
            imagePanel.setImage(entry.image);
            imagePanel.setBorder(BorderFactory.createTitledBorder("Dimensions: " + width + "x" + height));
        } else {
            imagePanel.setImage(null);
            imagePanel.setBorder(BorderFactory.createTitledBorder("No image"));
        }
    }
}

class StaticInfoPanel extends JPanel {
    private static final long serialVersionUID = -6607340640593973648L;
    private JLabel height, layer, price, weight, animation, unk1, unk2;
    private JLabel[] flagLabels = new JLabel[32];
    private static final String[] staticFlags = {
        //  1               2               4             8
        "Background",   "Weapon",       "Transparent",  "Translucent",
        "Wall",         "Damaging",     "Impassable",   "Wet",
        "Ignored",      "Surface",      "Stairs",       "Stackable",
        "Window",       "NoShoot",      "ArticleA",     "ArticleAn",
        "Generator",    "Foliage",      "LightSource",  "Animation",
        "NoDiagonal",   "Container",    "Wearable",     "Light",
        "Animation",    "Unknown 3",    "Unknown 4",    "Armor",
        "Roof",         "Door",         "Unknown 8",    "Unknown 9" };
    
    
    public StaticInfoPanel() {
        height = new JLabel();
        layer = new JLabel();
        price = new JLabel();
        weight = new JLabel();
        animation = new JLabel();
        unk1 = new JLabel();
        unk2 = new JLabel();
        
        JPanel flagPanel = new JPanel();
        setLayout(new GridLayout(10, 4));
        for(int i = 0; i < 32; i++) {
            flagLabels[i] = new JLabel(staticFlags[i]);
            add(flagLabels[i]);
        }
        add(height);
        add(layer);
        add(price);
        add(weight);
        add(animation);
        add(unk1);
        add(unk2);
        
        add(flagPanel);
    }
    
    public void setTile(StaticTile tile) {
        for(int i = 0; i < 32; i++) {
            if((tile.flags & (1 << i)) != 0) {
                flagLabels[i].setEnabled(true);
                flagLabels[i].setForeground(Color.blue);
            } else {
                flagLabels[i].setEnabled(false);
                flagLabels[i].setForeground(Color.black);
            }
        }
        height.setText(String.format("Height: %d", tile.height));
        layer.setText(String.format("Layer: %d", tile.layer));
        price.setText(String.format("Price: %d", tile.price));
        weight.setText(String.format("Weight: %d", tile.weight));
        animation.setText(String.format("Animation: 0x%04X", tile.animationID));
        unk1.setText(String.format("Unknown 1: 0x%04X", tile.unknown1));
        unk2.setText(String.format("Unknown 2: 0x%04X", tile.unknown2));
    }
}

class LandInfoPanel extends JPanel {
    private static final long serialVersionUID = -6607340640593973648L;
    private JLabel texture;
    private JLabel[] flagLabels = new JLabel[32];
    private static final String[] staticFlags = {
        //  1               2               4             8
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown",
        "Unknown",      "Unknown",      "Unknown",      "Unknown" };
    
    
    public LandInfoPanel() {
        texture = new JLabel();
        
        JPanel flagPanel = new JPanel();
        setLayout(new GridLayout(9, 4));
        for(int i = 0; i < 32; i++) {
            flagLabels[i] = new JLabel(staticFlags[i]);
            add(flagLabels[i]);
        }
        add(texture);
        add(flagPanel);
    }
    
    public void setTile(LandTile tile) {
        for(int i = 0; i < 32; i++) {
            if((tile.flags & (1 << i)) != 0) {
                flagLabels[i].setEnabled(true);
                flagLabels[i].setForeground(Color.blue);
            } else {
                flagLabels[i].setEnabled(false);
                flagLabels[i].setForeground(Color.black);
            }
        }
        texture.setText(String.format("Texture: 0x%04X", tile.textureID));
    }
}
