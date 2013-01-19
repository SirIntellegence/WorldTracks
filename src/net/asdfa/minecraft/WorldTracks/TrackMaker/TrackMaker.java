/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;


import org.apache.commons.collections.buffer.PriorityBuffer;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.bukkit.entity.Player;
/**
 *
 * @author austin
 */
public class TrackMaker {
    public TrackMaker(){}
    
    /**
     * Generates track to the player's position
     * @param player
     * @return
     */
    public void makeTrack(Player player){
	PriorityBuffer openList = new PriorityBuffer(ComparableComparator.getInstance());
	
    }
}
