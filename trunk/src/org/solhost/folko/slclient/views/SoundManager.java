package org.solhost.folko.slclient.views;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

import org.solhost.folko.slclient.models.GameState;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLSound;
import org.solhost.folko.uosl.data.SLSound.SoundEntry;

public class SoundManager {
    private static final Logger log = Logger.getLogger("slclient.sound");
    private final SLSound sound;
    private final GameState game;
    private final Sequencer sequencer;
    private final Sequence songs[];

    public SoundManager(GameState gameState) {
        this.game = gameState;
        this.sound = SLData.get().getSound();

        Sequencer midiSeq;
        try {
            log.fine("Initializing MIDI system");
            midiSeq = MidiSystem.getSequencer();
            midiSeq.open();
        } catch (MidiUnavailableException e) {
            log.warning("No MIDI support -> no music");
            midiSeq = null;
        }
        sequencer = midiSeq;
        songs = new Sequence[25];
        loadSongs();
    }

    public void update(long elapsedMillis) {
        if(sequencer != null) {
            updateSong();
        }
    }

    private void updateSong() {
        // this is implemented exactly like in the client, including unreachable entries

        // Strategy: Search nearest location center in the following table.
        //           Since centers can overlap, all centers have to be scanned.
        //           The closest location index is then used as index into the songTable.

        // @434528h in client
        final int[][] locationTable = {
                // centerx, centery, size
                {320, 592, 75},
                {464, 592, 16},
                {536, 568, 0},
                {480, 680, 16},
                {387, 868, 50},
                {64,  560, 40},
                {48,  704, 20},
                {80,  832, 40},
                {512, 640, 128},
                {0,     0,  0},
                {0,     0,  0},
        };

        // @434580h in client
        final int[] songTable = {
                10, 16, 16, 16, 2, 24, 24, 24, 7, 4, 6, 7, 8, 9, 20, 1
        };


        if(game.getPlayer().isInWarMode()) {
            // in war mode, always play war song immediately
            playSongNow(songTable[9]);
        } else if(!sequencer.isRunning()) {
            // otherwise, only choose new song when required
            int index = 10;
            int x = game.getPlayer().getLocation().getX();
            int y = game.getPlayer().getLocation().getY();
            int minDist = Integer.MAX_VALUE;
            for(int i = 0; i < locationTable.length; i++) {
                int distX = Math.abs(x - locationTable[i][0]);
                int distY = Math.abs(y - locationTable[i][1]);
                int dist = Math.max(distX, distY);
                if(dist <= locationTable[i][2] && dist <= minDist) {
                    minDist = dist;
                    index = i;
                }
            }
            if(index == 8) {
                // in town, use random music
                index = ThreadLocalRandom.current().nextInt(11, 15);
            }
            int songId = songTable[index];
            playSongNow(songId);
        }
    }

    private void playSongNow(int id) {
        if(sequencer == null) {
            return;
        }
        if(id < 0 || id >= songs.length || songs[id] == null) {
            // TODO: logging a warning would spam the log, think of something else
            return;
        }
        try {
            sequencer.stop();
            sequencer.setSequence(songs[id]);
            sequencer.start();
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't play song " + id + ": " + e.getMessage(), e);
        }
    }

    public void playSound(int id) {
        try {
            SoundEntry sfx = sound.getEntry(id);
            AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, sfx.pcmData, 0, sfx.pcmData.length);
            clip.start();
        } catch(Exception e) {
            log.log(Level.WARNING, "Couldn't play sound " + id + ": " + e.getMessage(), e);
        }
    }

    private Sequence[] loadSongs() {
        if(sequencer == null) {
            return null;
        }

        for(int i = 0; i < songs.length; i++) {
            try {
                Path path = Paths.get(SLData.get().getDataPath(), "MUSIC", String.format("ULTIMA%02d.MID", i));
                songs[i] = MidiSystem.getSequence(path.toFile());
            } catch (Exception e) {
                continue;
            }
        }

        return new Sequence[0];
    }

    public void dispose() {
        if(sequencer != null) {
            sequencer.stop();
            sequencer.close();
        }
    }
}
