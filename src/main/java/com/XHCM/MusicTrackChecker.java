package com.XHCM;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.awt.Color;

/**
 * Class to check if the "Rest in Peace" music track is unlocked
 * Implementation based on CS2 script: music_isunlocked
 */
@Slf4j
public class MusicTrackChecker {

    // The track ID for "Rest in Peace" is 87
    private static final int REST_IN_PEACE_TRACK_ID = 87;

    // Check every 100 game ticks (60 seconds)
    private static final int CHECK_INTERVAL = 100;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ChatMessageManager chatMessageManager;

    private int tickCounter = 0;

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

        // Calculate which varbit contains this track
        int varbitIndex = trackId / 32;

        // Calculate which bit in the varbit is used for this track
        int bitPosition = trackId % 32;

        // Get the value of the appropriate music varbit
        int musicVarbitValue = 0;

        // The music unlocks are stored in VarPlayer 20 through 40
        // These correspond to the ranges of music tracks
        if (varbitIndex == 0) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_1);
        } else if (varbitIndex == 1) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_2);
        } else if (varbitIndex == 2) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_3);
        } else if (varbitIndex == 3) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_4);
        } else if (varbitIndex == 4) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_5);
        } else if (varbitIndex == 5) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_6);
        } else if (varbitIndex == 6) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_7);
        } else if (varbitIndex == 7) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_8);
        } else if (varbitIndex == 8) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_9);
        } else if (varbitIndex == 9) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_10);
        } else if (varbitIndex == 10) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_11);
        } else if (varbitIndex == 11) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_12);
        } else if (varbitIndex == 12) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_13);
        } else if (varbitIndex == 13) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_14);
        } else if (varbitIndex == 14) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_15);
        } else if (varbitIndex == 15) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_16);
        } else if (varbitIndex == 16) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_17);
        } else if (varbitIndex == 17) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_18);
        } else if (varbitIndex == 18) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_19);
        } else if (varbitIndex == 19) {
            musicVarbitValue = client.getVarpValue(VarPlayer.MUSIC_TRACKS_UNLOCKED_20);
        } else {
            log.warn("Track ID {} is outside of the expected range", trackId);
            return false;
        }

        // Check if the specific bit is set
        return (musicVarbitValue & (1 << bitPosition)) != 0;
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        tickCounter++;

        // Check every CHECK_INTERVAL ticks
        if (tickCounter % CHECK_INTERVAL == 0) {
            clientThread.invoke(() -> {
                boolean isUnlocked = isMusicTrackUnlocked(REST_IN_PEACE_TRACK_ID);

                String message = isUnlocked ?
                        "Rest in Peace music track is unlocked!" :
                        "Rest in Peace music track is not unlocked yet.";

                // Log the result
                log.debug(message);

                // Send a chat message to the player
                final ChatMessageBuilder chatMessageBuilder = new ChatMessageBuilder()
                        .append(ChatColorType.HIGHLIGHT)
                        .append(message);

                chatMessageManager.queue(QueuedMessage.builder()
                        .type(net.runelite.api.ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(chatMessageBuilder.build())
                        .build());
            });
        }
    }
}