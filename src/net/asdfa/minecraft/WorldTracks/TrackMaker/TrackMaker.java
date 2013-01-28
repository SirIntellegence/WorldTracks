/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.asdfa.minecraft.WorldTracks.CommandHook;
import net.asdfa.minecraft.WorldTracks.Util;
import org.apache.commons.collections.buffer.PriorityBuffer;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
/**
 *
 * @author austin
 */
public class TrackMaker {
    public TrackMaker(){}
    
    public final HashMap<Player, Block> lastTrackBlockNearby = new HashMap<Player, Block>();
    
    /**
     * Generates track to the player's position
     * @param player
     * @return
     */
    public void makeTrack(Player player){
	makeTrack(player, false);
    }

    /**
     * Generates track to the player's position
     * @param player
     * @return
     */
    public void makeTrack(Player player, boolean isFromAuto) {
	Block targetBlock = null;
	if (isFromAuto)
	    targetBlock = lastTrackBlockNearby.get(player);
	if (targetBlock == null){
	    List<Block> blocksLookedAt;
	    blocksLookedAt = player.getLineOfSight(null, 100);
	    ArrayList<Block> result = new ArrayList<Block>();
	    if (Util.getTrackBlocksInList(blocksLookedAt, result).size() > 0){
		targetBlock = result.get(0);
	    }
	}
	if (targetBlock != null){
	    PriorityBuffer openList = new PriorityBuffer(ComparableComparator.
		    comparableComparator());
	    ArrayList<PathNode> closedList = new ArrayList<PathNode>();
	    Location start = player.getLocation().clone();
	    PathNode startingNode = new PathNode();
	    startingNode.setAssosiatedBlock(start.getBlock());
	    if (Util.isTrackBlock(startingNode.getAssosiatedBlock()))
		return; // allready done
	    startingNode.setValues(0, calculateH(start, targetBlock.
		    getLocation()));
	    if (CommandHook.debug)
		player.sendMessage("H was " + startingNode.getH());
	    openList.add(startingNode);
	}
	else{
	    if (isFromAuto)
		player.sendMessage("Unable to retrieve last track block.  "
			+ "Update this by issuing the commmand \"/WT makeTrack\"");
	    else
		player.sendMessage("There are no tracks where you are looking, "
			+ "please look towards the track you want to connect to");
	}
    }
    
    int calculateH(Location start, Location end){
	Location location = Util.getDistanceInbeetween(start, end);
	Vector vector = location.toVector();
	double hight = vector.getY();
	vector.setY(0);
	double length = vector.length();
	return (int)(Math.floor(length * 10) + hight * 50);
    }
}
