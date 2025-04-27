package com.XHCM;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class XHCMOverlay extends OverlayPanel
{
    private final Client client;
    private final XHCMPlugin plugin;
    private final XHCMConfig config;

    @Inject
    private XHCMOverlay(Client client, XHCMPlugin plugin, XHCMConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showTimeAlive() || plugin.isPlayerDead() || !plugin.isPluginEnabled())
        {
            return null;
        }

        int timeAliveHours = config.timeAliveHours();
        String hourText = timeAliveHours + " hour" + (timeAliveHours != 1 ? "s" : "");

        // Build overlay components
        panelComponent.getChildren().clear();

        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Time Alive")
                .color(Color.GREEN)
                .build());

        // Add time counter
        panelComponent.getChildren().add(LineComponent.builder()
                .left("")
                .right(hourText)
                .build());

        return super.render(graphics);
    }
}