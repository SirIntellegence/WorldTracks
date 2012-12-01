/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.asdfa.minecraft.WorldTracks.TrackMaker.TrackMaker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
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
public class CommandHook extends JavaPlugin implements Listener{
    static final Logger log = Logger.getLogger("minecraft");
    final boolean debug = true;
    ConsoleCommandSender console;// = getServer().getConsoleSender();
    
    private FileConfiguration customConfig = null;
    private File customConfigFile = null;
    
    TrackMaker trackMaker;
    //Note: if the bool is false, they are out of range
    HashMap<Player, Boolean> playerProximity;
    int hurtTaskID;
    
    @Override
    public void onEnable(){
	PluginManager pm = this.getServer().getPluginManager();
	pm.registerEvents(this, this);
	PluginDescriptionFile pdfFile = this.getDescription();
	log.log(Level.INFO, "TrackMaker version {0} is enabled!", pdfFile.getVersion());
	trackMaker = new TrackMaker();
	playerProximity = new HashMap<Player, Boolean>();
	hurtTaskID = this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, 
		new Runnable (){
		    public void run(){
			hurtPlayers();
		    }
		}, 100L, getConfig().getLong("proximity.checkTicks"));
	console = getServer().getConsoleSender();
	for(Player player : getServer().getOnlinePlayers())
	    TrackDetect(player);
    }
    
    void hurtPlayers(){
	int hurtAmount = getConfig().getInt("proximity.hurtAmount", 1);
	int healAmount = getConfig().getInt("proximity.healAmount", 1);
	
	for(Player p : playerProximity.keySet()){
	    if (!playerProximity.get(p))
	    {
		if(hurtAmount != 0)
		    p.damage(hurtAmount);
	    }
	    else{
		if (healAmount != 0)
		    if (p.getHealth() < 20){
			int amount = p.getHealth() + healAmount;
			if (amount > 20)
			    amount = 20;
			p.setHealth(amount);
		    }
	    }
	}
    }
    
    @Override
    public void onDisable(){
	log.info("TrackMaker disabled.");
	this.saveConfig();
	this.getServer().getScheduler().cancelTask(hurtTaskID);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
	if (command.getName().startsWith("WT")){
	    Player player = sender instanceof Player ? (Player)sender : null;
	    
	    if (args.length < 1)
		showHelp(sender);
	    else if (args[0].equals("makeTrack")){
		if (player != null)
		    trackMaker.makeTrack(player);
		else
		    sender.sendMessage("Only Players can make track!");
	    }
	    else if (args[0].equals("set")){
		if (args.length>2){
		    if (args[1].equals("hurtAmount"))
			getConfig().set("proximity.hurtAmount", Integer.parseInt(args[2]));
		    else if (args[1].equals("healAmount"))
			getConfig().set("proximity.healAmount", Integer.parseInt(args[2]));
		    else if (args[1].equals("checkTicks")){
			getConfig().set("proximity.checkTicks", Long.parseLong(args[2]));
			sender.sendMessage("You will need to reload the plugin for this to take affect");
		    }
		    else{
			Object value;
			if (getConfig().isBoolean(args[1]))
			    value = Boolean.parseBoolean(args[2]);
			else if (getConfig().isLong(args[1]))
			    value = Long.parseLong(args[2]);
			else if (getConfig().isInt(args[2]))
			    value = Integer.parseInt(args[2]);
			else
			    value = args[2];
			getConfig().set(args[1], value);
		    }
		}
		else
		    sender.sendMessage("Usage: /WT set <var> <value>");
	    }
	    
	    return true;
	}

	return false;
    }
    Vector roundLocation(Location location){
	return new Vector(location.getBlockX(),location.getBlockY(), location.getBlockZ());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerMove(PlayerMoveEvent movement){
	Player player = movement.getPlayer();
	World world = player.getWorld();
	Vector to = roundLocation(movement.getTo());
	Vector from = roundLocation(movement.getFrom());
	
	Vector result = to.clone();
	result.subtract(from);
	if(!(result.getBlockX() == 0 && result.getBlockY() == 0 && result.getBlockZ() == 0)){
	    if (false)
		player.getServer().getConsoleSender().sendMessage(
		    String.format("Location diff (x = %d, y = %d, z = %d)", result.getBlockX(),
									       result.getBlockY(),
									       result.getBlockZ()));
	    TrackDetect(player, to);
	}	
    }
    
    @EventHandler(priority= EventPriority.MONITOR)
    void playerJoin(PlayerJoinEvent args){
	Player player = args.getPlayer();
	TrackDetect(player, player.getLocation().toVector());
    }
    
    @EventHandler(priority= EventPriority.MONITOR)
    void playerQuit(PlayerQuitEvent args){
	if (playerProximity.containsKey(args.getPlayer()))
	    playerProximity.remove(args.getPlayer());	
    }
    
    @EventHandler(priority= EventPriority.MONITOR)
    void blockCheck(BlockBreakEvent args){
	if (isBlockTrack(args.getBlock())){
	    TrackDetect(args.getPlayer());
	}
    }
    
    @EventHandler(priority= EventPriority.MONITOR, ignoreCancelled=false)
    void perpetualTrackStack(BlockPlaceEvent args){
	if (isProvidedTrack(args.getBlockPlaced())){
	    Player player = args.getPlayer();
	    Material type = args.getBlockPlaced().getType();
	    PlayerInventory inventory = player.getInventory();
	    ItemStack trackStack;
	    if (!inventory.contains(type))
		trackStack = inventory.getItemInHand();
	    else
		trackStack = inventory.getItem(inventory.first(type));
	    if (trackStack.getType() != type)
		trackStack.setType(type);
	    trackStack.setAmount(64);
	}
	if (isBlockTrack(args.getBlock())){
	    if (!playerProximity.get(args.getPlayer()))
		TrackDetect(args.getPlayer());
	}
    }
    
    @EventHandler(priority= EventPriority.MONITOR, ignoreCancelled=false)
    void perpetualTrackStack(PlayerPickupItemEvent args){
	if (isProvidedTrack(args.getItem().getItemStack().getType())){
	    Player player = args.getPlayer();
	    HashMap<Integer, ? extends ItemStack> trackStacks =
		    player.getInventory().all(args.getItem().getItemStack().getType());
	    log.log(Level.INFO, trackStacks.toString());
	    ItemStack firstStack = null;
	    ArrayList<ItemStack> stacksToRemove = new ArrayList<ItemStack>();
	    for(ItemStack stack : trackStacks.values()){
		if (firstStack == null)
		    firstStack = stack;
		else
		    stacksToRemove.add(stack);
	    }
	    try{
		firstStack.setAmount(63);
		for(ItemStack stack : stacksToRemove)
		    player.getInventory().remove(stack);
	    }
	    catch (NullPointerException npe) { } // can't seem to fix this issue, so I'm eating the exception
	}
    }
    
//    @EventHandler(priority= EventPriority.LOW)
//    void perpetualTrackStack(InventoryClickEvent args){
//	if (isProvidedTrack(args.getCurrentItem().getType())){
//	    Player player = (Player)args.getWhoClicked();
//	    ItemStack stack = args.getCurrentItem();
//	    if (stack.getAmount()!=64)
//		stack.setAmount(64);
//	    
//	}
//	
//    }
    
    boolean isProvidedTrack(Block block){
	return isProvidedTrack(block.getType());
    }
    
    boolean isProvidedTrack(Material type){
	return type == Material.RAILS || type == Material.POWERED_RAIL;
    }
    
    boolean isBlockTrack(Block block){
	return isBlockTypeTrack(block.getType());
    }
    
    boolean isBlockTypeTrack(Material type){
	return type == Material.RAILS || type == Material.POWERED_RAIL || type == Material.DETECTOR_RAIL;
    }
    
    private void showHelp(CommandSender sender){
	sender.sendMessage("Hi");
    }
    
    void TrackDetect(Player player){
	TrackDetect(player, roundLocation(player.getLocation()));
    }

    private void TrackDetect(Player player, Vector to) {
	// check all nearby blocks
	World world = player.getWorld();
	boolean lastValue;// = playerProximity.get(player);
	boolean hadValue = true;
	if (playerProximity.containsKey(player))
	    lastValue = playerProximity.get(player);
	else
	    lastValue = hadValue = false;
	    
	final int range = 9; //9x9x9 cube to search
	int subrange = (int)Math.floor(range/2);
	boolean trackNearby = false;
	trackSearch:
	for(int i = -subrange; i <= subrange; i++)
	    for (int j = -subrange; j <= subrange; j++)
		for (int k = -subrange; k <= subrange; k++){
		    trackNearby = isBlockTrack(
			world.getBlockAt(i + to.getBlockX(),
					 j + to.getBlockY(),
					 k + to.getBlockZ()));
//			if (trackNearby && debug)
//			    player.sendMessage("Track Detected");
		    if (trackNearby)
			break trackSearch;
		}
	playerProximity.put(player, trackNearby);
	if (lastValue != trackNearby && debug && false)
	    if (trackNearby)
		console.sendMessage(player.getDisplayName() + " stepped into safe zone");
	    else
		console.sendMessage(player.getDisplayName() + " stepped out of safe zone");
	if (lastValue != trackNearby || !hadValue)
	    if (trackNearby)
		player.sendMessage("You have entered the safety of the tracks");
	    else
		player.sendMessage("You have left the safety of the tracks, and it pains you!");
    }
}