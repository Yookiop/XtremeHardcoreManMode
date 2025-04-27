package com.XHCM;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("xhcm")
public interface XHCMConfig extends Config
{
    // This config value will be hidden from users but still accessible programmatically
    @ConfigItem(
            keyName = "permanentDeath",
            name = "Permanent Death Status",
            description = "Sets the permanent death status (DO NOT MODIFY)",
            hidden = false  // This hides it from the configuration UI
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
}