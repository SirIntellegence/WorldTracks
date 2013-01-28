/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;

import net.asdfa.minecraft.WorldTracks.Util;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


/**
 *
 * @author austin
 */
public class PathNode implements Comparable<PathNode> {
    private double _cachedF = -1;
    private static final double AIR_PENALTY = 25d;
    private static final double LIQUID_UNDER_PENALTY = 5d;
    private static final double LIQUID_THROUGH_PENALTY = Double.NaN; //not yet implemented
    private static final double TUNNEL_PENALTY = 25d;
    /*
     * Movement cost from the starting point
     */
    private double g = -1;
    /*
     * estimated movement cost to goal
     */
    private double h = -1;
    private double penalty;
    private Block assosiatedBlock;
    private boolean valid = true;

    public Block getAssosiatedBlock() {
	return assosiatedBlock;
    }
    public void setAssosiatedBlock(Block value) {
	if (value == null)
	    throw new NullArgumentException("value");
	_cachedF = -1;
	assosiatedBlock = value;
	if (assosiatedBlock.getType() == Material.BEDROCK){
	    // can't go through bedrock
	    valid = false; 
	}
	if (assosiatedBlock.getType() == Material.WATER ||
		assosiatedBlock.getType() == Material.STATIONARY_WATER){
	    // they might be swimming, check to see if the block above is air
	    // and, if so, set assosiatedBlock to it
	    Block above = assosiatedBlock.getRelative(BlockFace.UP);
	    if (above != null && above.getType() == Material.AIR)
		assosiatedBlock = above;
	}
	
	penalty = 0;
	Block blockUnder = assosiatedBlock.getRelative(BlockFace.DOWN);
	if (blockUnder == null){
	    // there is no block under
	    valid = false;
	    return;
	}
	Block blockOver = assosiatedBlock.getRelative(BlockFace.UP);
	Block[] blockCheck = new Block[]{blockUnder, assosiatedBlock, blockOver};
	for (int i = 0; i < blockCheck.length; i++) {
	    // note: i == 0 is a check to see if it is the bottom block
	    Block item = blockCheck[i];
	    switch (item.getType()){
		case AIR:
		    if (i == 0) // is bottom block
			penalty += AIR_PENALTY;
		    // otherwise this is optimal, next
		    continue;
		case WATER:
		case STATIONARY_WATER:
		case LAVA:
		case STATIONARY_LAVA:
		    if (i == 0){
			penalty += LIQUID_UNDER_PENALTY;
		    }
		    else
			penalty += LIQUID_THROUGH_PENALTY;
		    // next
		    continue;
		default:
		    Material type = item.getType();
		    if (type.isBlock()){
			if (Util.isNaturalType(type)){}
		    }
	    }
	}
	if (penalty != penalty){// if penalty is NaN
	    valid = false;
	}
    }
    private PathNode _parent;

    public PathNode() {
    }
    public PathNode(PathNode parent){
	_parent = parent;
    }
    
    public double getG() {
	return g;
    }
    public void setG(double g) {
	_cachedF = -1;
	this.g = g;
    }

    private void setPenalty(double penalty) {
	_cachedF = -1;
	this.penalty = penalty;
    }
    
    public boolean isValid(){
	return valid;
    }

    public PathNode getParent() {
	return _parent;
    }

    public void setParent(PathNode value) {
	_cachedF = -1;
	this._parent = value;
    }
    
    public void setValues (double g, double h){
	_cachedF = -1;
	this.g = g;
	this.h = h;
    }

    public double getH() {
	return h;
    }
    public void setH(double h) {
	_cachedF = -1;
	this.h = h;
    }
    
    public double getF(){
	if (_cachedF < 0){ //calculate F
	    _cachedF = h + g + penalty;
	}
	return _cachedF;
    }

    @Override
    public int compareTo(PathNode o) {
	return Math.round(Math.round(getF() - o.getF()));
    }
    
    
}
