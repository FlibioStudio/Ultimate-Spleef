package me.flibio.ultimatespleef.commands;

import me.flibio.minigamecore.arena.Arena;
import me.flibio.minigamecore.file.FileManager;
import me.flibio.ultimatespleef.USpleef;
import me.flibio.ultimatespleef.utils.TextUtils;

import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Optional;

public class CreateCommand implements CommandExecutor {
	
	public enum ArenaShape {
		CIRCLE,RECTANGLE,CUSTOM
	}
	
	enum CreationState {
		DEFAULT,ARENA_MODE,ARENA_NAME,JOIN_SIGN,LOBBY_LOCATION,ARENA_SHAPE,CIRCLE_CENTER,RCORNER1,RCORNER2,CUSTOM,RADIUS,FINALIZE,COMPLETE,SAVING
	}
	
	private CreationState creationState = CreationState.DEFAULT;
	private Player player;
	private Game game;
	
	//Arena Settings
	private boolean dedicatedMode;
	private String arenaName;
	private Location<World> joinSign;
	private Location<World> lobbySpawn;
	private ArenaShape arenaShape;
	private Location<World> circleCenter;
	private int radius;
	
	public CreateCommand(Game game) {
		this.game = game;
		game.getEventManager().registerListeners(USpleef.access, this);
	}
	
	@Override
	public CommandResult execute(CommandSource source, CommandContext args)
			throws CommandException {
		
		if(!(source instanceof Player)) {
			source.sendMessage(Text.builder("You must be a player to use /spleef!").color(TextColors.RED).build());
			return CommandResult.success();
		}
		
		Player player = (Player) source;
		this.player = player;
		player.sendMessage(TextUtils.message("Welcome to the setup wizard! Which mode would you like the new arena in?"));
		creationState = CreationState.ARENA_MODE;
		player.sendMessage(TextUtils.option(c -> {
			if(creationState.equals(CreationState.ARENA_MODE)) {
				creationState = CreationState.LOBBY_LOCATION;
				dedicatedMode = true;
				player.sendMessage(TextUtils.message(Text.of(TextColors.YELLOW,"Dedicated Mode ",TextColors.WHITE,
						"- Please look at the exact lobby location and type 'lobby'")));
			}
		}, TextColors.YELLOW, "Dedicated"));
		player.sendMessage(TextUtils.option(c -> {
			if(creationState.equals(CreationState.ARENA_MODE)) {
				creationState = CreationState.ARENA_NAME;
				dedicatedMode = false;
				player.sendMessage(TextUtils.message(Text.of(TextColors.LIGHT_PURPLE,"Multi-Arena Mode ",TextColors.WHITE,
						"- Please name your arena")));
			}
		}, TextColors.LIGHT_PURPLE, "Multi-Arena"));
		
		return CommandResult.success();
	}
	
	@Listener
	public void onChat(MessageChannelEvent.Chat event) {
		Optional<Player> playerOptional = event.getCause().first(Player.class);
		if(!playerOptional.isPresent()) return;
		Player player = playerOptional.get();
		if(this.player == player) {
			if(creationState.equals(CreationState.ARENA_NAME)) {
				creationState = CreationState.JOIN_SIGN;
				event.setCancelled(true);
				arenaName = event.getRawMessage().toPlain();
				player.sendMessage(TextUtils.message(arenaName+" - Please place a join sign and enter [USpleef]:"));
			} else if(creationState.equals((CreationState.LOBBY_LOCATION))) {
				if(event.getRawMessage().toPlain().equalsIgnoreCase("lobby")) {
					event.setCancelled(true);
					Optional<BlockRayHit<World>> rayOptional = BlockRay.from(player).filter(BlockRay.onlyAirFilter()).build().end();
					if(rayOptional.isPresent()) {
						creationState = CreationState.ARENA_SHAPE;
						lobbySpawn = rayOptional.get().getLocation();
						player.sendMessage(TextUtils.message(lobbySpawn.getBlockX()+":"+lobbySpawn.getBlockY()+":"+lobbySpawn.getBlockZ()+
								" - Please pick an arena shape"));
						player.sendMessage(TextUtils.option(c -> {
							if(creationState.equals(CreationState.ARENA_SHAPE)) {
								creationState = CreationState.CIRCLE_CENTER;
								arenaShape = ArenaShape.CIRCLE;
								player.sendMessage(TextUtils.message(Text.of(TextColors.GREEN,"Circle ",TextColors.WHITE,
										"- Please look at the center of the circle and type 'center'")));

							}
						}, TextColors.GREEN, "Circle"));
						//TODO - Implement the other arena shapes
						/*player.sendMessage(TextUtils.option(c -> {
							if(creationState.equals(CreationState.ARENA_SHAPE)) {
								creationState = CreationState.RCORNER1;
								arenaShape = ArenaShape.RECTANGLE;
								player.sendMessage(TextUtils.message(Text.of(TextColors.GOLD,"Rectangle ",TextColors.WHITE,
										"- Please look at a corner of the rectangle and type 'corner'")));
								//TODO
							}
						}, TextColors.GOLD, "Rectangle"));
						player.sendMessage(TextUtils.option(c -> {
							if(creationState.equals(CreationState.ARENA_SHAPE)) {
								creationState = CreationState.CUSTOM;
								arenaShape = ArenaShape.CUSTOM;
								player.sendMessage(TextUtils.message(Text.of(TextColors.BLUE,"Custom ",TextColors.WHITE,
										"- Please click on each block that is part of the arena")));
								//TODO
							}
						}, TextColors.BLUE, "Custom"));*/
					}
				}
			} else if(creationState.equals(CreationState.CIRCLE_CENTER)) {
				if(event.getRawMessage().toPlain().equalsIgnoreCase("center")) {
					event.setCancelled(true);
					Optional<BlockRayHit<World>> rayOptional = BlockRay.from(player).filter(BlockRay.onlyAirFilter()).build().end();
					if(rayOptional.isPresent()) {
						creationState = CreationState.RADIUS;
						circleCenter = rayOptional.get().getLocation();
						player.sendMessage(TextUtils.message(Text.of(TextColors.GREEN,"Saved Circle Center ",TextColors.WHITE,
								"- Please input the radius of the circle (Excludes center)")));
					}
				}
			} else if(creationState.equals(CreationState.RADIUS)) {
				event.setCancelled(true);
				String msg = event.getRawMessage().toPlain();
				int rad;
				try {
					rad = Integer.parseInt(msg);
				} catch (NumberFormatException e) {
					player.sendMessage(Text.builder("Invalid number!").color(TextColors.RED).build());
					return;
				}
				radius = rad;
				//TODO support for multiple arena layers
				//TODO support for custom block types
				creationState = CreationState.FINALIZE;
				player.sendMessage(TextUtils.message(Text.of(TextColors.GREEN,"Saved Radius ",TextColors.WHITE,
						"- Please type 'done' to complete setup")));
			} else if(creationState.equals(CreationState.FINALIZE)) {
				if(event.getRawMessage().toPlain().equalsIgnoreCase("done")) {
					event.setCancelled(true);
					creationState = CreationState.SAVING;
					for(Player pl : game.getServer().getOnlinePlayers()) {
						pl.kick(Text.of(TextColors.GREEN,"Finalizing arena setup..."));
					}
					saveArena();
				}
			}
		}
	}
	
	@Listener
	public void onSignChange(ChangeSignEvent event) {
		Optional<Player> playerOptional = event.getCause().first(Player.class);
		if(!playerOptional.isPresent()) return;
		Player player = playerOptional.get();
		if(this.player == player) {
			if(creationState.equals(CreationState.JOIN_SIGN)) {
				Sign signTile = event.getTargetTile();
				SignData data = event.getText();
				if(data.getValue(Keys.SIGN_LINES).get().get(0).toPlain().equals("[USpleef]")) {
					creationState = CreationState.LOBBY_LOCATION;
					joinSign = event.getTargetTile().getLocation();
					player.sendMessage(TextUtils.message("Sign Found - Please look at the exact lobby location and type 'lobby'"));
					signTile.offer(data.set(data.getValue(Keys.SIGN_LINES).get().set(1, Text.of("Success"))));
					//TODO prevent sign from breaking
				}
			}
		}
	}
	
	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join event) {
		if(creationState.equals(CreationState.SAVING)) {
			event.getTargetEntity().kick(Text.of(TextColors.GREEN,"Finalizing arena setup..."));
			event.setChannel(MessageChannel.TO_NONE);
		}
	}
	
	private void saveArena() {
		FileManager fileManager = USpleef.access.minigame.getFileManager();
		if(dedicatedMode) {
			Arena arena = new Arena("arena",game,USpleef.access);
			arena.getData().setVariable("dedicatedServer", true);
			arena.getData().setLocation("lobbySpawn", lobbySpawn);
			arena.getData().setLocation("lobby",circleCenter);
			arena.getData().setVariable("shape", arenaShape.toString());
			if(arenaShape.equals(ArenaShape.CIRCLE)) {
				arena.getData().setLocation("circlecenter",circleCenter);
				arena.getData().setVariable("circlerad",radius);
			}
			USpleef.access.minigame.getArenaManager().addArena(arena);
			fileManager.saveArena(arena, "arenas");
		} else {
			Arena arena = new Arena(arenaName,game,USpleef.access);
			arena.getData().setVariable("dedicatedServer", false);
			arena.getData().setLocation("lobbySpawn", lobbySpawn);
			arena.getData().setLocation("lobby",circleCenter);
			arena.getData().setVariable("shape", arenaShape.toString());
			if(arenaShape.equals(ArenaShape.CIRCLE)) {
				arena.getData().setLocation("circlecenter",circleCenter);
				arena.getData().setVariable("circlerad",radius);
			}
			USpleef.access.minigame.getArenaManager().addArena(arena);
			fileManager.saveArena(arena,"arenas");
		}
		for(Location<World> loc : getCircle(circleCenter,radius)) {
			loc.setBlockType(BlockTypes.QUARTZ_BLOCK);
		}
		creationState = CreationState.COMPLETE;
	}
	
	private ArrayList<Location<World>> getCircle(Location<World> center, int r) {
		ArrayList<Location<World>> locations = new ArrayList<Location<World>>();
		int cx = center.getBlockX();
		int cy = center.getBlockY();
		int cz = center.getBlockZ();
		World w = center.getExtent();
		int rSquared = r * r;
		for(int x = cx - r; x <= cx +r; x++) {
			for(int z = cz - r; z <= cz +r; z++) {
				if((cx - x) * (cx -x) + (cz - z) * (cz - z) <= rSquared) {
					locations.add(new Location<World>(w,x,cy,z));
				}
			}
		}
		return locations;
	}

}
