package com.XHCM;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
        name = "XtremeHardcoreMan",
        description = "No ironman restrictions, but you have 1 life and there are no safe deaths (account won't be deleted)."
)
public class XHCMPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private MenuManager menuManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private XHCMConfig config;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private XHCMOverlay overlay;

    private HashMap<XHCMIcons, Integer> iconIds = new HashMap<>();
    private boolean firstRun = true;
    private BufferedImage cachedAliveIcon = null;
    private BufferedImage cachedDeadIcon = null;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private XHCMIcons currentIcon = XHCMIcons.ALIVE;
    private int tickCounter = 0;
    private static final int TICKS_PER_HOUR = 6000; // 6000 ticks = 1 hour (100 ticks/min * 60 min)

    @Override
    protected void startUp() throws Exception
    {
        log.info("XHCM plugin starting up...");

        // Register the overlay
        overlayManager.add(overlay);

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            // Add a slight delay to ensure client is fully loaded
            clientThread.invokeLater(() -> {
                log.info("Client is logged in, loading icons...");
                loadIcons();
            });
        }

        if (firstRun)
        {
            log.info("Scheduling checkForDeath...");
            executorService.scheduleAtFixedRate(this::checkForDeath, 10, 1, TimeUnit.SECONDS);
            firstRun = false;
        }

        clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("XHCM plugin shutting down...");
        overlayManager.remove(overlay);
        executorService.shutdown();
        iconIds.clear();
        clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
    }

    @Provides
    XHCMConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(XHCMConfig.class);
    }

    private boolean areIconsLoaded() {
        return iconIds.containsKey(XHCMIcons.ALIVE) && iconIds.containsKey(XHCMIcons.DEAD);
    }

    private void loadIcons()
    {
        log.info("Loading icons");

        // Check if we can access the mod icons
        if (client.getModIcons() == null) {
            log.error("ModIcons is null, cannot load icons");
            return;
        }

        // Try loading the images right away to check resource paths
        BufferedImage testAliveImage = ImageUtil.loadImageResource(XHCMPlugin.class, "/com/XHCM/icon_alive.png");
        BufferedImage testDeadImage = ImageUtil.loadImageResource(XHCMPlugin.class, "/com/XHCM/icon_dead.png");

        log.info("Resource test - Alive icon null? {}", testAliveImage == null);
        log.info("Resource test - Dead icon null? {}", testDeadImage == null);

        clientThread.invoke(() -> {
            try {
                // Load images
                BufferedImage aliveIconImage = ImageUtil.loadImageResource(XHCMPlugin.class, "/com/XHCM/icon_alive.png");
                BufferedImage deadIconImage = ImageUtil.loadImageResource(XHCMPlugin.class, "/com/XHCM/icon_dead.png");

                if (aliveIconImage == null) {
                    log.error("Failed to load alive icon");
                    return;
                } else {
                    log.info("Successfully loaded alive icon, size: {}x{}",
                            aliveIconImage.getWidth(), aliveIconImage.getHeight());
                    cachedAliveIcon = aliveIconImage;
                }

                if (deadIconImage == null) {
                    log.error("Failed to load dead icon");
                    return;
                } else {
                    log.info("Successfully loaded dead icon, size: {}x{}",
                            deadIconImage.getWidth(), deadIconImage.getHeight());
                    cachedDeadIcon = deadIconImage;
                }

                // Get existing mod icons
                IndexedSprite[] modIcons = client.getModIcons();
                log.info("Current mod icons length: {}", modIcons.length);

                // Create new indexed sprites
                IndexedSprite aliveSprite = ImageUtil.getImageIndexedSprite(aliveIconImage, client);
                IndexedSprite deadSprite = ImageUtil.getImageIndexedSprite(deadIconImage, client);

                // Create a new array including our new icons
                IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 2);

                // Add our icons to the end of the array
                int aliveIconId = modIcons.length;
                int deadIconId = modIcons.length + 1;

                newModIcons[aliveIconId] = aliveSprite;
                newModIcons[deadIconId] = deadSprite;

                // Store the icon IDs
                iconIds.put(XHCMIcons.ALIVE, aliveIconId);
                iconIds.put(XHCMIcons.DEAD, deadIconId);

                // Set the new mod icons array
                client.setModIcons(newModIcons);

                log.info("Icons loaded - Alive ID: {}, Dead ID: {}", aliveIconId, deadIconId);

                // Immediately update the chatbox to test
                client.runScript(ScriptID.CHAT_PROMPT_INIT);
            } catch (Exception e) {
                log.error("Error loading icons", e);
                e.printStackTrace();
            }
        });
    }

    public boolean isPlayerDead()
    {
        return config.permanentDeath();
    }

    /**
     * Checks if the plugin should be enabled for the current player
     * @return true if the plugin should be enabled, false otherwise
     */
    public boolean isPluginEnabled()
    {
        if (client.getLocalPlayer() == null) {
            return false;
        }

        String configUsername = config.username();
        String playerUsername = client.getLocalPlayer().getName();

        // If no username is set in config, the plugin is enabled for all accounts
        if (configUsername == null || configUsername.trim().isEmpty()) {
            return true;
        }

        // Otherwise, the plugin is only enabled if the username matches
        return Text.standardize(configUsername).equalsIgnoreCase(Text.standardize(playerUsername));
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            // Add a slight delay to ensure the client is fully loaded
            clientThread.invokeLater(() -> {
                log.info("Game logged in, loading icons...");
                loadIcons();

                if (firstRun) {
                    log.info("First run actions...");
                    firstRun = false;
                }

                // Initialize or update the last time updated if needed
                if (config.lastTimeUpdated() == 0) {
                    config.lastTimeUpdated(System.currentTimeMillis());
                }
            });
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        checkForDeath();

        // If icons aren't loaded yet, try loading them
        if (!areIconsLoaded() && client.getGameState() == GameState.LOGGED_IN) {
            log.info("Icons not loaded yet, trying to load at game tick");
            loadIcons();
        }

        // Increment tick counter if player is alive
        if (!isPlayerDead() && client.getGameState() == GameState.LOGGED_IN) {
            tickCounter++;

            // If we've reached an hour's worth of ticks
            if (tickCounter >= TICKS_PER_HOUR) {
                int currentHours = config.timeAliveHours();
                config.timeAliveHours(currentHours + 1);
                tickCounter = 0;
                log.debug("Time alive incremented to {} hours", config.timeAliveHours());
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        if (event.getType() != ChatMessageType.PUBLICCHAT &&
                event.getType() != ChatMessageType.PRIVATECHAT &&
                event.getType() != ChatMessageType.MODCHAT &&
                event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        if (event.getName() == null || client.getLocalPlayer() == null ||
                client.getLocalPlayer().getName() == null) {
            return;
        }

        boolean isLocalPlayer = Text.standardize(event.getName()).equalsIgnoreCase(
                Text.standardize(client.getLocalPlayer().getName()));

        if (isLocalPlayer) {
            // Debug info
            log.debug("Processing chat message for local player");

            currentIcon = isPlayerDead() ? XHCMIcons.DEAD : XHCMIcons.ALIVE;
            int iconToUse = iconIds.getOrDefault(currentIcon, -1);

            log.debug("Current icon: {}, ID: {}", currentIcon.getName(), iconToUse);

            if (iconToUse != -1) {
                String imgTag = getImgTag(iconToUse);
                log.debug("Setting chat name with icon tag: {}", imgTag);

                event.getMessageNode().setName(
                        imgTag + Text.removeTags(event.getName()));

                // Force a refresh of the chat
                client.refreshChat();
            } else {
                log.warn("Icon ID is -1, icons might not be loaded yet");
                if (!areIconsLoaded()) {
                    loadIcons();
                }
            }
        }
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        if (!event.getEventName().equals("setChatboxInput"))
        {
            return;
        }

        updateChatbox();
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event)
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        updateChatbox(); // this stops flickering when typing
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        if (event.getMenuOption().equalsIgnoreCase("Enter Death's Domain"))
        {
            event.consume();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Xtreme Hardcore: You may not enter Death's Office!", null);
        }
    }

    private void updateChatbox()
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        Widget chatboxTypedText = client.getWidget(WidgetInfo.CHATBOX_INPUT);

        if (chatboxTypedText == null || chatboxTypedText.isHidden())
        {
            return;
        }

        String[] chatbox = chatboxTypedText.getText().split(":", 2);
        if (chatbox.length < 2) {
            log.debug("Chatbox text doesn't contain expected format: {}", chatboxTypedText.getText());
            return;
        }

        String rsn = Objects.requireNonNull(client.getLocalPlayer()).getName();

        currentIcon = isPlayerDead() ? XHCMIcons.DEAD : XHCMIcons.ALIVE;
        int iconToUse = iconIds.getOrDefault(currentIcon, -1);

        if (iconToUse != -1) {
            String imgTag = getImgTag(iconToUse);
            chatboxTypedText.setText(imgTag + Text.removeTags(rsn) + ":" + chatbox[1]);
        } else {
            log.debug("Icon not available for chatbox update");
            if (!areIconsLoaded() && client.getGameState() == GameState.LOGGED_IN) {
                loadIcons();
            }
        }
    }

    private String getImgTag(int iconIndex)
    {
        return "<img=" + iconIndex + ">";
    }

    private void checkForDeath()
    {
        // Check if plugin should be enabled for this player
        if (!isPluginEnabled()) {
            return;
        }

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
            boolean currentlyDead = currentHP <= 0;
            // If player is dead and this hasn't been saved as permanent yet
            if (currentlyDead && !config.permanentDeath())
            {
                log.info("Player is permanently dead!");
                // Set permanent death in config
                config.permanentDeath(true);

                // Notify the player
                ChatMessageBuilder message = new ChatMessageBuilder()
                        .append(Color.RED, "Xtreme Hardcore mode: You have permanently died. No second chances!");
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message.build(), null);

                // Update the chatbox to show the death icon
                clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
            }
            else if (!currentlyDead && config.permanentDeath())
            {
                log.info("Player is permanently dead, cannot reset to alive.");

                // Update the chatbox to show the death icon
                clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
            }
            else if (!currentlyDead && !config.permanentDeath())
            {
            }
        }
    }
}