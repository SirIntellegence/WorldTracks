/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;

/**
 *
 * @author austin
 */
public interface IPathFindingList {
    
    public boolean add(PathNode node);
    public boolean remove(Object node);
    
    public PathNode getFirst();
    public PathNode getLast();
    
    public PathNode getLowest();
    public PathNode getHighest();
    
}
