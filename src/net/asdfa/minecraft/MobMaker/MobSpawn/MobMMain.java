/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.MobMaker.MobSpawn;

import java.util.logging.Logger;
import java.util.Arrays;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.entity.CreatureType;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;



/**
 *
 * @author austin
 */
public class MobMMain extends JavaPlugin {
    Logger log = Logger.getLogger("minecraft");
    
    
    public void onEnable(){
	PluginManager pm = this.getServer().getPluginManager();
	PluginDescriptionFile pdfFile = this.getDescription();
	log.info("MobMaker version " + pdfFile.getVersion() + " is enabled!");
    }
    
    public void onDisable(){
	log.info("MobMaker disabled.");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
	Player player = sender instanceof Player ? (Player)sender : null;
        Player player2 = null;
        int argpos = 0;
        if(args.length > argpos && player == null) {
            player = getServer().getPlayer(args[argpos]);
            ++argpos;
        }
        if(args.length > argpos) {
            player2 = getServer().getPlayer(args[argpos]);
            ++argpos;
        }
	
	if(command.getName().equalsIgnoreCase("spawnwolf") && player.isOp()){
	    if(player == null) {sender.sendMessage("Please specify a player"); return false;}
	    oneMobSpawn(CreatureType.WOLF, player);
	    return true;
	}
	if(command.getName().equalsIgnoreCase("spawntamewolf") && player.isOp()){
	    if(player == null) {sender.sendMessage("Please specify a player"); return false;}
	    Wolf wolf = wolfSpawn(player);
	    wolf.setOwner(player);
	    // one health = one half heart
	    wolf.setHealth(20);
	    sender.sendMessage("Succesfully spawned a wolf belonging to " + player.getName() +
		    " with " + wolf.getHealth()/2 + " hearts of health.");
	    return true;
	    
	}
	if(command.getName().equalsIgnoreCase("spawntamewolves") && player.isOp()){
	    try{
		if(player == null) {sender.sendMessage("Please specify a player"); return false;}
		int number = 2; //todo: allow player passed number

		Wolf[] wolves = new Wolf[number];
		// spawn and set the wolves owner and their health
		for (int i=0; i<number;i++){
		    wolves[i] = wolfSpawn(player);
		    wolves[i].setOwner(player);
		    wolves[i].setHealth(20);
		}
		sender.sendMessage("Spawned " + number + " wolves belonging to " + player.getDisplayName()+
			" with " + wolves[1].getHealth()/2 + " hearts of health each.");
	    }

	    catch(Exception e){
		log.severe(e.toString());
		
	    }
	    return true;
	}
	
	return false;
    }
    
    public LivingEntity oneMobSpawn(CreatureType creature, Player target){
	World w = target instanceof Player ? ((Player)target).getWorld() : getServer().getWorlds().get(0);
	return w.spawnCreature(target.getLocation(), creature);
    }
    
    public Wolf wolfSpawn(Player target){
	Wolf wolf= (Wolf)oneMobSpawn(CreatureType.WOLF, target);
	return wolf;
    }
}
