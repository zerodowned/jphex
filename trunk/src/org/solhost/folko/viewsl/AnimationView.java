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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLArt.AnimationEntry;

public class AnimationView extends JPanel {
    private static final long serialVersionUID = -225672106193952019L;
    private ImagePanel imagePanel;
    private JList<String> artList;
    private JLabel artLabel;
    private JButton animationButton;
    private SLArt art;
    private Timer timer;
    private int currentFrame;
    private AnimationEntry currentAnimation;

    private static final int MAX_FRAMES = 32768;
    private static final int ANIMATION_DELAY = 80;

    public AnimationView(SLArt art) {
        this.art = art;
        this.imagePanel = new ImagePanel(200, 200);

        setLayout(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<String>();
        artList = new JList<String>(model);
        for(int i = 0; i < MAX_FRAMES; i++)  {
            AnimationEntry entry = art.getAnimationEntry(i);
            if(entry != null) {
                model.addElement(String.format("0x%04X", entry.id));
                i += entry.frames.size() - 1; // -1 because loop will do ++
            }
        }

        JPanel artInfoPanel = new JPanel();
        artInfoPanel.setLayout(new FlowLayout());
        artLabel = new JLabel("0x0000");
        animationButton = new JButton("Start");
        artInfoPanel.add(artLabel);
        artInfoPanel.add(animationButton);
        artInfoPanel.add(imagePanel);

        animationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(timer == null) {
                    animationButton.setText("Stop");
                    timer = new Timer(ANIMATION_DELAY, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            updateFrame();
                        }
                    });
                    timer.start();
                } else {
                    animationButton.setText("Start");
                    timer.stop();
                    timer = null;
                    currentFrame = 0;
                    updateFrame();
                }
            }
        });

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
        if(timer != null) {
            animationButton.setText("Start");
            timer.stop();
            timer = null;
        }
        int id = Integer.parseInt(artList.getSelectedValue().substring(2, 6), 16);
        currentAnimation = art.getAnimationEntry(id);
        currentFrame = 0;
        artLabel.setText(String.format("0x%04X: %d frames", currentAnimation.id, currentAnimation.frames.size()));
        updateFrame();
    }

    private void updateFrame() {
        imagePanel.setImage(currentAnimation.frames.get(currentFrame).image);
        currentFrame++;
        if(currentFrame == currentAnimation.frames.size()) {
            currentFrame = 0;
        }
    }
}
