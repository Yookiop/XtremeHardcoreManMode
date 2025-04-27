package com.XHCM;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum XHCMIcons
{
    ALIVE("Alive", "icon_alive.png"),
    DEAD("Dead", "icon_dead.png");

    private final String name;
    private final String imagePath;

    @Override
    public String toString()
    {
        return name;
    }
}
