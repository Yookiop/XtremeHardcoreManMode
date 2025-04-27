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

    private HashMap<XHCMIcons, Integer> iconIds = new HashMap<>();
    private boolean firstRun = true;
    private BufferedImage cachedAliveIcon = null;
    private BufferedImage cachedDeadIcon = null;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private XHCMIcons currentIcon = XHCMIcons.ALIVE;

    @Override
    protected void startUp() throws Exception
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            loadIcons();
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
        executorService.shutdown();
        iconIds.clear();
        clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
    }

    @Provides
    XHCMConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(XHCMConfig.class);
    }

    private void loadIcons()
    {
        log.info("Loading icons");
        if (client.getModIcons() == null)
        {
            log.warn("ModIcons is null, cannot load icons");
            return;
        }

        clientThread.invoke(() -> {
            try {
                // Load images
                BufferedImage aliveIconImage = ImageUtil.loadImageResource(XHCMPlugin.class, "/com/XHCM/icon_alive.png");
                BufferedImage deadIconImage = ImageUtil.loadImageResource(XHCMPlugin.class, "/com/XHCM/icon_dead.png");

                if (aliveIconImage == null) {
                    log.error("Failed to load alive icon");
                    return;
                } else {
                    log.info("Successfully loaded alive icon");
                    cachedAliveIcon = aliveIconImage;
                }

                if (deadIconImage == null) {
                    log.error("Failed to load dead icon");
                    return;
                } else {
                    log.info("Successfully loaded dead icon");
                    cachedDeadIcon = deadIconImage;
                }

                // Get existing mod icons
                IndexedSprite[] modIcons = client.getModIcons();

                // Create new indexed sprites for our icons
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
            } catch (Exception e) {
                log.error("Error loading icons", e);

            }
        });
    }

    public boolean isPlayerDead()
    {
        return config.permanentDeath();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (firstRun && gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            log.info("Scheduling checkForDeath...");
            executorService.schedule(() -> {
                int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
                //config.permanentDeath(false);
                firstRun = false;
            }, 10, TimeUnit.SECONDS);
        } else {
            log.info("Waiting for game to be logged in...");

            // Plan een taak om na 5 seconden opnieuw te controleren
            executorService.schedule(() -> {
                // Je kunt hier de actie uitvoeren die je wilt na 5 seconden
                // config.permanentDeath(false);  // Als je dat wilt
            }, 5, TimeUnit.SECONDS);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        checkForDeath();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getName() == null || client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
        {
            return;
        }

        boolean isLocalPlayer = Text.standardize(event.getName()).equalsIgnoreCase(
                Text.standardize(client.getLocalPlayer().getName()));

        if (isLocalPlayer)
        {
            currentIcon = isPlayerDead() ? XHCMIcons.DEAD : XHCMIcons.ALIVE;
            int iconToUse = iconIds.getOrDefault(currentIcon, -1);

            if (iconToUse != -1) {
                log.debug("Setting chat name with icon {} for player {}", iconToUse, event.getName());
                event.getMessageNode().setName(
                        getImgTag(iconToUse) + Text.removeTags(event.getName()));
            }
        }
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        if (!event.getEventName().equals("setChatboxInput"))
        {
            return;
        }

        updateChatbox();
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event)
    {
        updateChatbox(); // this stops flickering when typing
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event.getMenuOption().equalsIgnoreCase("Enter Death's Domain"))
        {
            event.consume();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Xtreme Hardcore: You may not enter Death's Office!", null);
        }
    }

    private void updateChatbox()
    {
        Widget chatboxTypedText = client.getWidget(WidgetInfo.CHATBOX_INPUT);

        if (chatboxTypedText == null || chatboxTypedText.isHidden())
        {
            return;
        }

        String[] chatbox = chatboxTypedText.getText().split(":", 2);
        String rsn = Objects.requireNonNull(client.getLocalPlayer()).getName();

        int iconToUse = iconIds.getOrDefault(isPlayerDead() ? XHCMIcons.DEAD : XHCMIcons.ALIVE, -1);
        if (iconToUse != -1) {
            chatboxTypedText.setText(getImgTag(iconToUse) + Text.removeTags(rsn) + ":" + chatbox[1]);
        }
    }

    private String getImgTag(int iconIndex)
    {
        return "<img=" + iconIndex + ">";
    }

    private void checkForDeath()
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
        int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
        boolean currentlyDead = currentHP <= 0;

        log.info("currentHP: {}", client.getBoostedSkillLevel(Skill.HITPOINTS));
        log.info("PermanentDeath: {}", config.permanentDeath());

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
            log.info("Player is alive.");
            log.info("currentHP: {}", currentHP);
        }}
    }
}