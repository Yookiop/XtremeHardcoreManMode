package com.XHCM;

import com.google.inject.Provides;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
//import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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
	private XHCMConfig config;
	private Boolean firstRun = true;
	private Boolean playerIsDead = false;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			if(firstRun) {
				executorService.scheduleAtFixedRate(this::checkForDeath, 10, 1, TimeUnit.SECONDS);
				firstRun = false;
			}
		}

		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
			firstRun = true;
		}
	}


	private void checkForDeath() {
		if (client.getGameState() == GameState.LOGGED_IN) {
			int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);

			if (currentHP <= 0) {
				if(!playerIsDead) {
					playerIsDead = true;
					//Text command replacement you died
				}
			} else {
				playerIsDead = false;
			}
		}
	}

	@Provides
	XHCMConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(XHCMConfig.class);
	}
}
