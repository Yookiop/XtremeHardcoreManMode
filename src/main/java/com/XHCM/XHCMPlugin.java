package com.XHCM;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
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
    private OverlayManager overlayManager;

    @Inject
    private XHCMOverlay xhcmOverlay;

    @Inject
    private ConfigManager configManager;

    @Inject
    private XHCMConfig config;

    @Inject
    private ClientThread clientThread;

    private int aliveIconId = -1;
    private int deadIconId = -1;
    private boolean firstRun = true;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(xhcmOverlay);
        loadIcons();

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
        overlayManager.remove(xhcmOverlay);
        executorService.shutdown();

        clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
    }

    @Provides
    XHCMConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(XHCMConfig.class);
    }

    private void loadIcons()
    {
        if (client.getModIcons() == null)
        {
            return;
        }

        BufferedImage aliveIconImage = ImageUtil.loadImageResource(getClass(), "/icon_alive.png");
        BufferedImage deadIconImage = ImageUtil.loadImageResource(getClass(), "/icon_dead.png");

        IndexedSprite[] modIcons = client.getModIcons();
        IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 2);

        aliveIconId = modIcons.length;
        deadIconId = modIcons.length + 1;

        newModIcons[aliveIconId] = ImageUtil.getImageIndexedSprite(aliveIconImage, client);
        newModIcons[deadIconId] = ImageUtil.getImageIndexedSprite(deadIconImage, client);

        client.setModIcons(newModIcons);
    }

    public BufferedImage getAliveIcon()
    {
        return ImageUtil.loadImageResource(getClass(), "/icon_alive.png");
    }

    public BufferedImage getDeadIcon()
    {
        return ImageUtil.loadImageResource(getClass(), "/icon_dead.png");
    }

    public boolean isPlayerDead()
    {
        return config.permanentDeath();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            if (client.getModIcons() != null && aliveIconId == -1)
            {
                loadIcons();
            }
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
            event.getMessageNode().setName(
                    getImgTag(isPlayerDead() ? deadIconId : aliveIconId) +
                            Text.removeTags(event.getName()));
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

        if (aliveIconId == -1 || deadIconId == -1)
        {
            return;
        }

        if (chatboxTypedText == null || chatboxTypedText.isHidden())
        {
            return;
        }

        String[] chatbox = chatboxTypedText.getText().split(":", 2);
        String rsn = Objects.requireNonNull(client.getLocalPlayer()).getName();

        chatboxTypedText.setText(getImgTag(isPlayerDead() ? deadIconId : aliveIconId)
                + Text.removeTags(rsn) + ":" + chatbox[1]);
    }

    private String getImgTag(int iconIndex)
    {
        return "<img=" + iconIndex + ">";
    }

    private void checkForDeath()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
        boolean currentlyDead = currentHP <= 0;

        // If player is dead and this hasn't been saved as permanent yet
        if (currentlyDead && !config.permanentDeath())
        {
            // Set permanent death in config
            config.permanentDeath(true);

            // Notify the player
            ChatMessageBuilder message = new ChatMessageBuilder()
                    .append(Color.RED, "Xtreme Hardcore mode: You have permanently died. No second chances!");
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message.build(), null);

            // Update the chatbox to show the death icon
            clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
        }
    }
}