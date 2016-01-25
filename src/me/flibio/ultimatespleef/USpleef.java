package me.flibio.ultimatespleef;

import me.flibio.minigamecore.arena.ArenaData;
import me.flibio.minigamecore.file.FileManager;
import me.flibio.minigamecore.main.Minigame;
import me.flibio.ultimatespleef.UArena.ArenaShapeType;
import me.flibio.ultimatespleef.commands.CreateCommand;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

@Plugin(id = "UltimateSpleef", name = "Ultimate Spleef", version = "0.1.0")
public class USpleef {
	
	@Inject
	private Logger logger;
	
	@Inject
	private Game game;
	
	public String version = USpleef.class.getAnnotation(Plugin.class).version();
	
	public Minigame minigame;

	public static USpleef access;
	
	@Listener
	public void onServerInitialize(GameStartingServerEvent event) {
		logger.info("Ultimate Spleef v"+version+" is enabling!");
		access = this;
		
		minigame = new Minigame("Ultimate Spleef", game, logger);
		FileManager fileManager = minigame.getFileManager();
		fileManager.initializeFile("config");
		fileManager.initializeFile("arenas");
		
		registerCommands();
		
		for(ArenaData arenaData : fileManager.loadArenas("arenas")) {
			logger.info("Found arena: "+arenaData.getName());
			System.out.println(arenaData.getCustomLocations().keySet());
			UArena uarena = new UArena(arenaData.getName(),ArenaShapeType.valueOf(arenaData.getVariable("shape").get().toString()));
			uarena.overrideData(arenaData);
			uarena.initialize();
			//TODO - Check if the arena has all the necessary data
			minigame.getArenaManager().addArena(uarena);
		}
	}
	
	private void registerCommands() {
		CommandSpec createCommand = CommandSpec.builder()
			    .description(Text.of("Create a spleef arena"))
			    .executor(new CreateCommand(game))
			    .build();
		CommandSpec spleefCommand = CommandSpec.builder()
			    .description(Text.of("Control Ultimate Spleef"))
			    .permission("spleef.admin")
			    .child(createCommand, "create")
			    .build();
		game.getCommandManager().register(this, spleefCommand, "spleef");
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	public Game getGame() {
		return game;
	}
	
}
