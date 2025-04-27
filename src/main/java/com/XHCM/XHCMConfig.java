package com.XHCM;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup("xhcm")
public interface XHCMConfig extends Config
{
    // This config value will be hidden from users but still accessible programmatically
    @ConfigItem(
            keyName = "permanentDeath",
            name = "Permanent Death Status",
            description = "Sets the permanent death status (DO NOT MODIFY)",
            hidden = false  // TODO: Make hidden = true
    )
    default boolean permanentDeath()
    {
        return false;
    }

    @ConfigItem(
            keyName = "permanentDeath",
            name = "Permanent Death Status",
            description = "Sets the permanent death status"
    )
    void permanentDeath(boolean permanentDeath);

    @ConfigItem(
            keyName = "showTimeAlive",
            name = "Show Time Alive Counter",
            description = "Displays a counter showing how long your character has been alive in hours"
    )
    default boolean showTimeAlive()
    {
        return true;
    }

    @ConfigItem(
            keyName = "timeAliveHours",
            name = "Time Alive Hours",
            description = "Tracks how many hours your character has been alive",
            hidden = true
    )
    default int timeAliveHours()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "timeAliveHours",
            name = "Time Alive Hours",
            description = "Tracks how many hours your character has been alive"
    )
    void timeAliveHours(int hours);

    @ConfigItem(
            keyName = "lastTimeUpdated",
            name = "Last Time Updated",
            description = "Timestamp of when the time alive was last updated",
            hidden = true
    )
    default long lastTimeUpdated()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "lastTimeUpdated",
            name = "Last Time Updated",
            description = "Timestamp of when the time alive was last updated"
    )
    void lastTimeUpdated(long timestamp);

    @ConfigItem(
            keyName = "username",
            name = "Fill in your in-game username below:",
            description = "Fill in your in-game username to make this plugin work for that account specifically (leave empty to work for any account)",
            position = 0
    )
    default String username()
    {
        return "";
    }
}