package com.XHCM;

import com.XHCM.XHCMPlugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class XHCMPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(XHCMPlugin.class);
		RuneLite.main(args);
	}
}