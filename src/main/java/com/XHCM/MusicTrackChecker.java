package com.XHCM;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;

/**
 * Class to check if the "Rest in Peace" music track is unlocked
 * Implementation based on CS2 script: music_isunlocked
 */
@Slf4j
@Singleton
public class MusicTrackChecker {

    // The track ID for "Rest in Peace" is 87
    private static final int REST_IN_PEACE_TRACK_ID = 87;

    // Check every 10 game ticks (6 seconds)
    private static final int CHECK_INTERVAL = 10;

    // VarPlayer IDs for music tracks (from DeVco's VarbitViewer)
    private static final int MUSIC_TRACKS_UNLOCKED_1 = 20;
    private static final int MUSIC_TRACKS_UNLOCKED_2 = 21;
    private static final int MUSIC_TRACKS_UNLOCKED_3 = 22;
    private static final int MUSIC_TRACKS_UNLOCKED_4 = 23;
    private static final int MUSIC_TRACKS_UNLOCKED_5 = 24;
    private static final int MUSIC_TRACKS_UNLOCKED_6 = 25;
    private static final int MUSIC_TRACKS_UNLOCKED_7 = 298;
    private static final int MUSIC_TRACKS_UNLOCKED_8 = 311;
    private static final int MUSIC_TRACKS_UNLOCKED_9 = 346;
    private static final int MUSIC_TRACKS_UNLOCKED_10 = 414;
    private static final int MUSIC_TRACKS_UNLOCKED_11 = 464;
    private static final int MUSIC_TRACKS_UNLOCKED_12 = 598;
    private static final int MUSIC_TRACKS_UNLOCKED_13 = 662;
    private static final int MUSIC_TRACKS_UNLOCKED_14 = 721;
    private static final int MUSIC_TRACKS_UNLOCKED_15 = 906;
    private static final int MUSIC_TRACKS_UNLOCKED_16 = 1009;
    private static final int MUSIC_TRACKS_UNLOCKED_17 = 1338;
    private static final int MUSIC_TRACKS_UNLOCKED_18 = 1681;
    private static final int MUSIC_TRACKS_UNLOCKED_19 = 2065;
    private static final int MUSIC_TRACKS_UNLOCKED_20 = 2237;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private XHCMConfig config;

    private int tickCounter = 0;
    private boolean previousUnlockState = false;
    private boolean isInitialized = false;

    /**
     * Initialize the checker
     */
    public void initialize() {
        log.info("MusicTrackChecker initialized");
        isInitialized = true;
        tickCounter = 0;

        // Check music track status on startup if enabled
        if (config.checkMusicTrack()) {
            clientThread.invoke(() -> {
                log.info("Initial music track check");
                checkMusicTrack();
            });
        }
    }

    /**
     * Checks if a music track is unlocked based on the CS2 script implementation
     * Based on: https://github.com/runelite/cs2-scripts/blob/37c606916c7213cefdb47f930ceb1c9dad79400e/scripts/%5Bproc%2Cmusic_isunlocked%5D.cs2
     *
     * @param trackId the ID of the music track to check
     * @return true if the track is unlocked, false otherwise
     */
    public boolean isMusicTrackUnlocked(int trackId) {
        // Based on how music tracks are stored in the game
        // Each track uses a bit in the music unlock varbits
        log.info("public boolean isMusicTrackUnlocked");
        // Calculate which varbit contains this track
        int varbitIndex = trackId / 32;

        // Calculate which bit in the varbit is used for this track
        int bitPosition = trackId % 32;

        // Get the value of the appropriate music varbit
        int musicVarbitValue = 0;

        // The music unlocks are stored in VarPlayer 20 through 40
        // These correspond to the ranges of music tracks
        switch (varbitIndex) {
            case 0:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_1);
                break;
            case 1:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_2);
                break;
            case 2:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_3);
                break;
            case 3:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_4);
                break;
            case 4:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_5);
                break;
            case 5:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_6);
                break;
            case 6:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_7);
                break;
            case 7:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_8);
                break;
            case 8:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_9);
                break;
            case 9:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_10);
                break;
            case 10:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_11);
                break;
            case 11:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_12);
                break;
            case 12:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_13);
                break;
            case 13:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_14);
                break;
            case 14:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_15);
                break;
            case 15:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_16);
                break;
            case 16:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_17);
                break;
            case 17:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_18);
                break;
            case 18:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_19);
                break;
            case 19:
                musicVarbitValue = client.getVarpValue(MUSIC_TRACKS_UNLOCKED_20);
                break;
            default:
                log.warn("Track ID {} is outside of the expected range", trackId);
                return false;
        }

        // Check if the specific bit is set
        return (musicVarbitValue & (1 << bitPosition)) != 0;
    }

    /**
     * Check the music track and update state
     */
    private void checkMusicTrack() {
        boolean isUnlocked = isMusicTrackUnlocked(REST_IN_PEACE_TRACK_ID);
        log.info("'Rest in Peace' track unlocked: {}", isUnlocked);

        // Track unlock state change
        boolean stateChanged = isUnlocked != previousUnlockState;
        if (stateChanged) {
            log.info("Music track unlock state changed from {} to {}", previousUnlockState, isUnlocked);
        }

        previousUnlockState = isUnlocked;
    }

    // @Subscribe
//    public void onGameTick(GameTick tick) {
//        // Skip if not logged in
//        if (client.getGameState() != GameState.LOGGED_IN) {
//            return;
//        }
//
//        // Skip if music track checking is disabled in config
//        if (!config.checkMusicTrack()) {
//            return;
//        }
//
//        // Initialize if needed
//        if (!isInitialized) {
//            initialize();
//        }
//
//        tickCounter++;
//
//
//            clientThread.invoke(this::checkMusicTrack);
//
//    }
}