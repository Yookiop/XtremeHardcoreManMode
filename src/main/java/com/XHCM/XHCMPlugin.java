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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;

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
    private XHCMConfig config;

	@Inject
	private XHCMOverlay overlay;

    private int aliveIconOffset;
    private int deadIconOffset;
    private boolean playerIsDead = false;
    private boolean firstRun = true;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private static final int REST_IN_PEACE_VARBIT_ID = 4394;
    private static final String DEATHS_DOMAIN_ENTRY = "Enter Death's Domain";

    private BufferedImage aliveIcon;
    private BufferedImage deadIcon;

    @Override
    protected void startUp() throws Exception
    {
		log.info("XHCM Plugin started!");
		aliveIcon = loadIcon("/icon_alive.png");
		deadIcon = loadIcon("/icon_dead.png");

        // Start de periodiciteit voor het controleren van de dood
        if (firstRun)
        {
            executorService.scheduleAtFixedRate(this::checkForDeath, 10, 1, TimeUnit.SECONDS);
            firstRun = false;
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("XHCM Plugin stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            // Controleer bij login of de speler dood is
            checkForDeath();
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (DEATHS_DOMAIN_ENTRY.equalsIgnoreCase(event.getOption()))
        {
            menuManager.addPlayerMenuItem(DEATHS_DOMAIN_ENTRY); // Voegt een menu-item toe
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        // Je kunt hier MenuOpened gebruiken om menu-entries te filteren voordat ze getoond worden
        MenuEntry[] entries = event.getMenuEntries();

        for (MenuEntry entry : entries)
        {
            if (DEATHS_DOMAIN_ENTRY.equalsIgnoreCase(entry.getOption()))
            {
                // Maak de menu-optie onbruikbaar door de optie leeg te maken
                entry.setOption(""); // Dit maakt de entry effectief onzichtbaar
            }
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
				log.error("Image not found: " + imageName);
				return null;
			}
			
			// Read the image and return it
			return ImageIO.read(inputStream);
		}
		catch (IOException e)
		{
			log.error("Failed to load image: " + imageName, e);
			return null;
		}
	}	

    private void checkForDeath()
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            boolean isRestInPeaceUnlocked = client.getVarbitValue(REST_IN_PEACE_VARBIT_ID) == 1;
            int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);

            // Controleer de doodstatus van de speler
            boolean currentlyDead = isRestInPeaceUnlocked || currentHP <= 0;

            if (currentlyDead && !playerIsDead)
            {
                playerIsDead = true;
                log.info("Player is dead! Xtreme Hardcore Mode failed.");
                updateOverlay(); // Update de overlay bij dood
            }
            else if (!currentlyDead && playerIsDead)
            {
                playerIsDead = false;
                log.info("Player is alive!");
                updateOverlay(); // Update de overlay bij levend
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
            log.error("Failed to load icon: " + iconPath, e);
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
