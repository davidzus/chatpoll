package com.zusworks;

import com.zusworks.command.Command;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chatpoll implements ModInitializer {
	public static final String MOD_ID = "chatpoll";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Command.registerCommands();
		LOGGER.info("Chatpoll has been initialized.");

		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) ->
				Command.notifyUnvotedPolls(listener.getPlayer()));
	}
}
