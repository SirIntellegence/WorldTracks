/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.asdfa.minecraft.WorldTracks.TrackMaker.TrackMaker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 *
 * @author austin
 */
public class CommandHook extends JavaPlugin implements Listener {

	static final Logger log = Logger.getLogger("minecraft");
	public static final boolean debug = true;
	static ConsoleCommandSender console;// = getServer().getConsoleSender();
	public static CommandHook instance;

	private FileConfiguration customConfig = null;
	private File customConfigFile = null;

	TrackMaker trackMaker;
	//Note: if the bool is false, they are out of range
	HashMap<Player, Boolean> playerProximity;
	int hurtTaskID;

	@Override
	public void onEnable() {
		instance = this;
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(this, this);
		PluginDescriptionFile pdfFile = this.getDescription();
		log.log(Level.INFO, "TrackMaker version {0} is enabled!", pdfFile.getVersion());
		trackMaker = new TrackMaker();
		playerProximity = new HashMap<Player, Boolean>();
		hurtTaskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new Runnable() {
					public void run() {
						hurtPlayers();
					}
				}, 100L, getConfig().getLong("proximity.checkTicks"));
		console = getServer().getConsoleSender();
		for (Player player : getServer().getOnlinePlayers()) {
			TrackDetect(player);
		}
	}

	void hurtPlayers() {
		double hurtAmount = getConfig().getDouble("proximity.hurtAmount", 1);
		double healAmount = getConfig().getDouble("proximity.healAmount", 1);

		for (Player p : playerProximity.keySet()) {
			if (!playerProximity.get(p)) {
				if (hurtAmount != 0) {
					p.damage(hurtAmount);
				}
			} else {
				if (healAmount != 0 && !getConfig().
						getStringList("proximity.ignoredWorlds").
						contains(p.getWorld().getName())) {
					if (p.getHealth() < 20) {
						double amount = p.getHealth() + healAmount;
						if (amount > 20) {
							amount = 20;
						}
						p.setHealth(amount);
					}
				}
			}
		}
	}

	@Override
	public void onDisable() {
		log.info("TrackMaker disabled.");
		this.saveConfig();
		this.getServer().getScheduler().cancelTask(hurtTaskID);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().startsWith("WT")) {
			Player player = sender instanceof Player ? (Player) sender : null;

			if (args.length < 1) {
				showHelp(sender);
			} else if (args[0].equals("makeTrack")) {
				if (player != null) {
					trackMaker.makeTrack(player);
				} else {
					sender.sendMessage("Only Players can make track!");
				}
			} else if (args[0].equals("set")) {
				if (sender.isOp()) {
					if (args.length > 2) {
						if (args[1].equals("hurtAmount")) {
							getConfig().set("proximity.hurtAmount", Integer.parseInt(args[2]));
						} else if (args[1].equals("healAmount")) {
							getConfig().set("proximity.healAmount", Integer.parseInt(args[2]));
						} else if (args[1].equals("checkTicks")) {
							getConfig().set("proximity.checkTicks", Long.parseLong(args[2]));
							sender.sendMessage("You will need to reload the plugin for this to take affect");
						} else {
							Object value;
							if (getConfig().isBoolean(args[1])) {
								value = Boolean.parseBoolean(args[2]);
							} else if (getConfig().isLong(args[1])) {
								value = Long.parseLong(args[2]);
							} else if (getConfig().isInt(args[2])) {
								value = Integer.parseInt(args[2]);
							} else {
								value = args[2];
							}
							getConfig().set(args[1], value);
						}
					} else {
						sender.sendMessage("Usage: /WT set <var> <value>");
					}
				} else {
					sender.sendMessage("You must be op to do this");
				}
			} else if (args[0].equals("listVars")) {
				sender.sendMessage(getConfig().saveToString());
			}

			return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerMove(PlayerMoveEvent movement) {
		Player player = movement.getPlayer();
		World world = player.getWorld();
		Vector to = Util.roundLocation(movement.getTo());
		Vector from = Util.roundLocation(movement.getFrom());

		Vector result = to.clone();
		result.subtract(from);
		if (!(result.getBlockX() == 0 && result.getBlockY() == 0 && result.getBlockZ() == 0)) {
			if (false) {
				player.getServer().getConsoleSender().sendMessage(
						String.format("Location diff (x = %d, y = %d, z = %d)", result.getBlockX(),
								result.getBlockY(),
								result.getBlockZ()));
			}
			TrackDetect(player, to);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void playerJoin(PlayerJoinEvent args) {
		Player player = args.getPlayer();
		TrackDetect(player, player.getLocation().toVector());
		enforcePlayerHasTrack(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void playerQuit(PlayerQuitEvent args) {
		if (playerProximity.containsKey(args.getPlayer())) {
			playerProximity.remove(args.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void blockCheck(BlockBreakEvent args) {
		if (Util.isBlockTypeTrack(args.getBlock().getType())) {
			if (playerProximity.get(args.getPlayer())) {
				final Player player = args.getPlayer();
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run() {
						TrackDetect(player);
					}
				});
			}
		}
	}

	//@EventHandler(priority= EventPriority.MONITOR)
	void blockCheck(BlockEvent args) {
		if (Util.isTrackBlock(args.getBlock())) {
			log.info("Track block was modified!");
		}
		if (args instanceof BlockBreakEvent) {
			blockCheck((BlockBreakEvent) args);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void asplosion(EntityExplodeEvent args) {
		List<Block> blocks = args.blockList();
		List<Block> trackBlocks = new ArrayList<>();
		Util.getTrackBlocksInList(blocks, trackBlocks);
		if (trackBlocks.size() > 0) {
			Location location = args.getLocation();
			World world = location.getWorld();
			// find the furthest block
			LocationRange range = new LocationRange(location, getConfig().
					getInt("proximity.checkDistance")
					+ (int) Math.floor(Util.getFurthest(location, trackBlocks)));
			List<Player> worldPlayers = world.getPlayers();
			final List<Player> affectedPlayers = range.getThoseInRange(worldPlayers);
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					for (Player player : affectedPlayers) {
						TrackDetect(player);
					}
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	void perpetualTrackStack(BlockPlaceEvent args) {
		if (getConfig().getBoolean("perpetualTrackStack") && Util.isProvidedTrack(args.getBlockPlaced())) {
			Player player = args.getPlayer();
			Material type = args.getBlockPlaced().getType();
			PlayerInventory inventory = player.getInventory();
			ItemStack trackStack;
			if (!inventory.contains(type)) {
				trackStack = inventory.getItemInHand();
			} else {
				trackStack = inventory.getItem(inventory.first(type));
			}
			if (trackStack.getType() != type) {
				trackStack.setType(type);
			}
			trackStack.setAmount(64);
		}
		if (Util.isTrackBlock(args.getBlock())) {
			if (!playerProximity.get(args.getPlayer())) {
				TrackDetect(args.getPlayer());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void perpetualTrackStack(PlayerPickupItemEvent args) {
		if (Util.isProvidedTrack(args.getItem().getItemStack())) {
			Player player = args.getPlayer();
			Item item = args.getItem();
			Material trackType = item.getItemStack().getType();
			int inventorySlot = -1;
			ItemStack trackStack = null;
			for (ItemStack stack : Util.getHotBarItems(player.getInventory())) {
				if (stack != null && stack.getType() == trackType) {
					trackStack = stack;
					break;
				}
			}
			if (trackStack == null && player.getInventory().contains(trackType)) {
				inventorySlot = player.getInventory().first(trackType);
				trackStack = player.getInventory().getItem(inventorySlot);
			}

			if (trackStack != null) {
				player.getInventory().remove(trackStack);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	void perpetualTrackStack(InventoryClickEvent args) {
		if (Util.isProvidedTrack(args.getCurrentItem())) {
			Player player = (Player) args.getWhoClicked();
			ItemStack stack = args.getCurrentItem();
			if (stack.getAmount() != 64) {
				stack.setAmount(64);
				args.setCurrentItem(stack);
			}

		} else if (Util.isProvidedTrack(args.getCursor())) {
			if (args.isLeftClick()) {
				ItemStack cursor = args.getCursor();
				Player player = (Player) args.getWhoClicked();
				player.getInventory().remove(cursor.getType());
				if (cursor.getAmount() < 64) {
					cursor.setAmount(64);
					args.setCurrentItem(cursor);
				}
			}
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	void perpetualTrackStack(PlayerRespawnEvent args) {
		Util.givePlayerTrack(args.getPlayer());
	}

	private void showHelp(CommandSender sender) {
		sender.sendMessage("Hi");
	}

	void TrackDetect(Player player) {
		TrackDetect(player, Util.roundLocation(player.getLocation()));
	}

	private void TrackDetect(Player player, Vector to) {
		// check all nearby blocks
		World world = player.getWorld();
		if (getConfig().getStringList("proximity.ignoredWorlds").contains(world.getName())) {
			playerProximity.put(player, true);
			return;
		}
		boolean lastValue;// = playerProximity.get(player);
		boolean hadValue = true;
		if (playerProximity.containsKey(player)) {
			lastValue = playerProximity.get(player);
		} else {
			lastValue = hadValue = false;
		}

		final int range = getConfig().getInt("proximity.checkDistance");
		int subrange = range;
		boolean trackNearby = false;
		trackSearch:
		for (int i = -subrange; i <= subrange; i++) {
			for (int j = -subrange; j <= subrange; j++) {
				for (int k = -subrange; k <= subrange; k++) {
					Block checkBlock = world.getBlockAt(i + to.getBlockX(),
							j + to.getBlockY(),
							k + to.getBlockZ());
					trackNearby = Util.isTrackBlock(checkBlock);
					if (trackNearby) {
						trackMaker.lastTrackBlockNearby.put(player, checkBlock);
						///log.info("Track detected at " + checkBlock.getLocation().toString());
						break trackSearch;
					}
				}
			}
		}
		playerProximity.put(player, trackNearby);
		if (lastValue != trackNearby && debug && false) {
			if (trackNearby) {
				console.sendMessage(player.getDisplayName() + " stepped into safe zone");
			} else {
				console.sendMessage(player.getDisplayName() + " stepped out of safe zone");
			}
		}
		if (lastValue != trackNearby || !hadValue) {
			if (trackNearby) {
				player.sendMessage("You have entered the safety of the tracks");
			} else {
				if (true) {
					if (debug) {
						player.sendMessage("You have left the safety of the tracks, and it pains you!");
					}
					trackMaker.makeTrack(player, true);
				} else {
					player.sendMessage("You have left the safety of the tracks, and it pains you!");
				}
			}
		}
	}

	void enforcePlayerHasTrack(Player player) {
		enforcePlayerHasTrack(player, Util.checkPlayerInventory(player));
	}

	void enforcePlayerHasTrack(Player player, InventoryQueryResult args) {
		PlayerInventory inventory = player.getInventory();
		if (!args.playerHasItems) {
			Util.givePlayerTrack(player);
		} else {
			if (args.boosterTrackSlotNum < 0) {
				Util.givePlayerBoosterTrack(player);
			} else {
				ItemStack boosterStack = inventory.getItem(args.boosterTrackSlotNum);
				for (Map.Entry<Integer, ? extends ItemStack> stack
						: inventory.all(Material.POWERED_RAIL).entrySet()) {
					if (stack.getValue() != boosterStack) {
						inventory.remove(stack.getValue());
					}
				}
				boosterStack.setAmount(64);
			}
			if (args.normalTrackSlotNum < 0) {
				Util.givePlayerStandardTrack(player);
			} else {
				ItemStack trackStack = inventory.getItem(args.normalTrackSlotNum);
				for (Map.Entry<Integer, ? extends ItemStack> stack
						: inventory.all(Material.RAILS).entrySet()) {
					if (stack.getValue() != trackStack) {
						inventory.remove(stack.getValue());
					}
				}
				trackStack.setAmount(64);
			}
		}

	}

	public static void printDebugMessage(String message) {
		if (debug) {
			console.sendMessage(message);
		}
	}
}
