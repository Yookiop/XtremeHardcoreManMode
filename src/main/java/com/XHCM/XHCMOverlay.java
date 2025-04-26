package com.XHCM;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.components.ImageComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageCapture;
import javax.inject.Inject;


@Slf4j
public class XHCMOverlay extends Overlay
{
    private final Client client;
    private final XHCMPlugin plugin;

    private final BufferedImage aliveIcon;
    private final BufferedImage deadIcon;

    @Inject
    public XHCMOverlay(Client client, XHCMPlugin plugin)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;

        // Laad de iconen voor levend en dood
        this.aliveIcon = plugin.loadImage("icon_alive.png");
        this.deadIcon = plugin.loadImage("icon_dead.png");

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
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
	
		LocalPoint localLocation = player.getLocalLocation();
		net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localLocation, client.getPlane());
	
		if (canvasPoint == null)
		{
			return null;
		}
	
		BufferedImage icon = plugin.isPlayerDead() ? plugin.getDeadIcon() : plugin.getAliveIcon();
		if (icon == null)
		{
			return null;
		}
	
		int x = canvasPoint.getX() - icon.getWidth() / 2;
		int y = canvasPoint.getY() - icon.getHeight() - 10;
	
		graphics.drawImage(icon, x, y, null);
	
		return null;
	}


    private boolean checkIfPlayerIsDead(Player player)
    {
        // Controleer of de speler dood is
        int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
        return currentHP <= 0;
    }
}
