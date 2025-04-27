package com.XHCM;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("xhcm")
public interface XHCMConfig extends Config
{
    @ConfigItem(
            keyName = "permanentDeath",
            name = "Permanent Death",
            description = "Sets the permanent death status (based on whether the player is dead or not)"
    )
    default boolean permanentDeath()
    {
        return false; // Initial default, you may modify this according to your death check logic
    }

    @ConfigItem(
            keyName = "permanentDeath",
            name = "Permanent Death",
            description = "Sets the permanent death status"
    )
    void permanentDeath(boolean permanentDeath);
}
