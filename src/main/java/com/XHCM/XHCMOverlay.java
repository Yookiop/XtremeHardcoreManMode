package com.XHCM;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

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

        // Clear previous components
        panelComponent.getChildren().clear();

        // Add single line with both label and value
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time Alive:")
                .right(hourText)
                .leftColor(Color.GREEN)
                .build());

        return super.render(graphics);
    }
}