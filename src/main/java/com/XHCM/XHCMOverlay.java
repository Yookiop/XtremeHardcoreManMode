package com.XHCM;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;

@Slf4j
public class XHCMOverlay extends Overlay
{
	private final Client client;
	private final XHCMPlugin plugin;

	@Inject
	public XHCMOverlay(Client client, XHCMPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		Point playerLocation = player.getCanvasImageLocation(plugin.isPlayerDead() ?
				plugin.getDeadIcon() : plugin.getAliveIcon(), player.getLogicalHeight() / 2);

		if (playerLocation == null)
		{
			return null;
		}

		BufferedImage icon = plugin.isPlayerDead() ? plugin.getDeadIcon() : plugin.getAliveIcon();
		if (icon == null)
		{
			return null;
		}

		graphics.drawImage(icon, playerLocation.getX(), playerLocation.getY(), null);

		return null;
	}
}