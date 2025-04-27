package com.XHCM;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;

import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.chat.ChatMessageBuilder;
import static net.runelite.api.ChatMessageType.GAMEMESSAGE;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.client.config.ConfigManager;
import javax.inject.Inject;


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

    @Provides
    XHCMConfig getConfig()
    {
        return configManager.getConfig(XHCMConfig.class);
    }

    private int aliveIconOffset;
    private int deadIconOffset;
    private boolean playerIsDead = false;
    private boolean firstRun = true;
    //private boolean isPlayerPermanentlyDead = false;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    //public static final int REST_IN_PEACE_VARBIT_ID = 4394;
    //public static final String DEATHS_DOMAIN_ENTRY = "Enter Death's Domain";

    private BufferedImage aliveIcon;
    private BufferedImage deadIcon;

    @Override
    protected void startUp() throws Exception
    {
        aliveIcon = loadIcon("/icon_alive.png");
        deadIcon = loadIcon("/icon_dead.png");
        overlayManager.add(xhcmOverlay);

        if (firstRun)
        {
            log.info("Scheduling checkForDeath...");
            executorService.scheduleAtFixedRate(this::checkForDeath, 10, 1, TimeUnit.SECONDS);
            firstRun = false;
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(xhcmOverlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            executorService.scheduleAtFixedRate(this::checkForDeath, 10, 1, TimeUnit.SECONDS);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // Bij elke tick controleren we of de speler dood is en slaan we dat op
        updatePermanentDeathStatus();
    }

    private void updatePermanentDeathStatus()
    {
        // Gebruik de originele check om te bepalen of de speler dood is
        boolean isDead = isPlayerDead();

        // Als de status verandert, sla het dan op in de configuratie
        if (config.permanentDeath() != isDead)
        {
            config.permanentDeath(isDead); // Zet permanentDeath status in config
        }
    }


    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        log.info("Clicked on option: {}", event.getMenuOption());

        if (event.getMenuOption().equalsIgnoreCase("Enter Death's Domain"))
        {
            event.consume();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Xtreme Hardcore: You may not enter Death's Office!", null);
        }
    }

    public BufferedImage loadImage(String imageName)
    {
        try
        {
            // Get the image from resources (e.g., inside resources folder or plugin directory)
            InputStream inputStream = getClass().getResourceAsStream("/" + imageName);
            if (inputStream == null)
            {
                return null;
            }

            // Read the image and return it
            return ImageIO.read(inputStream);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private void checkForDeath()
    {
        log.info("Executing checkForDeath...");
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            //log.info("Attempting to fetch Rest In Peace Varbit value...");
            // int restInPeaceValue = client.getVarbitValue(REST_IN_PEACE_VARBIT_ID);
            // log.info("Fetched Rest In Peace Varbit value: {}", restInPeaceValue);

            // boolean isRestInPeaceUnlocked = client.getVarbitValue(REST_IN_PEACE_VARBIT_ID) == 1;
            client.getBoostedSkillLevel(Skill.HITPOINTS);
            try {
                Thread.sleep(500);  // extra refresh HP
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            client.getBoostedSkillLevel(Skill.HITPOINTS);
            int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);

            //boolean currentlyDead = isRestInPeaceUnlocked || currentHP <= 0;
            boolean currentlyDead = currentHP <= 0;
            // log.info("isRestInPeaceUnlocked: {}", client.getVarbitValue(REST_IN_PEACE_VARBIT_ID) == 1);
            log.info("currentHP: {}", client.getBoostedSkillLevel(Skill.HITPOINTS));

            if (currentlyDead && !XHCMConfig.permanentDeath()) {
                XHCMConfig.setPermanentDeath(true);
                log.info("Player is permanently dead!");
                ChatMessageBuilder message = new ChatMessageBuilder()
                        .append(Color.RED, "Xtreme Hardcore mode: You have permanently died. No second chances!");
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message.build(), null);
            } else if (!currentlyDead && XHCMConfig.permanentDeath()) {
                log.info("Player is permanently dead, cannot reset to alive.");
            } else if (!currentlyDead && !XHCMConfig.permanentDeath()) {
                log.info("Player is alive.");
                log.info("currentHP: {}", currentHP);
            }
        }
    }

    private BufferedImage loadIcon(String iconPath)
    {
        try
        {
            return ImageIO.read(getClass().getResourceAsStream(iconPath));
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public boolean isPlayerDead()
    {
        return playerIsDead;
    }

    public BufferedImage getAliveIcon()
    {
        return aliveIcon;
    }

    public BufferedImage getDeadIcon()
    {
        return deadIcon;
    }


    @Provides
    XHCMConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(XHCMConfig.class);
    }


}
