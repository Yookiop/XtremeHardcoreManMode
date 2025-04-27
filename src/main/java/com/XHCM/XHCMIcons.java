package com.XHCM;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum XHCMIcons
{
    ALIVE("Alive", "com/XHCM/icon_alive.png"),
    DEAD("Dead", "com/XHCM/icon_dead.png");

    private final String name;
    private final String imagePath;

    @Override
    public String toString()
    {
        return name;
    }
}
