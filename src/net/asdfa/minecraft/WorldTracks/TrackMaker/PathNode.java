/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import net.asdfa.minecraft.WorldTracks.CommandHook;
import net.asdfa.minecraft.WorldTracks.Util;
//import org.apache.commons.collections.buffer.PriorityBuffer;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


public class PathNode implements Comparable<PathNode>, Iterable<PathNode> {

	private double _cachedF = -1;
	private static final double AIR_PENALTY = 75d;
	private static final double LIQUID_UNDER_PENALTY = 10d;
	private static final double LIQUID_THROUGH_PENALTY = Double.NaN; //not yet implemented
	private static final double TUNNEL_PENALTY = 25d;
	public static final int SURROUNDING_BLOCK_COUNT = 12;

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
		if (value == null) {
			throw new NullArgumentException("value");
		}
		_cachedF = -1;
		assosiatedBlock = value;
		if (assosiatedBlock.getType() == Material.BEDROCK) {
			// can't go through bedrock
			valid = false;
		}
		if (assosiatedBlock.isLiquid()) {
	    // they might be swimming, check to see if the block above is air
			// and, if so, set assosiatedBlock to it
			Block above = assosiatedBlock.getRelative(BlockFace.UP);
			if (above != null && above.getType() == Material.AIR) {
				assosiatedBlock = above;
				//CommandHook.printDebugMessage("Bumped associated block since player may be swimming");
			}
		}

		penalty = 0;
		Block blockUnder = assosiatedBlock.getRelative(BlockFace.DOWN);
		if (blockUnder == null) {
			// there is no block under
			valid = false;
			return;
		}
		//check to see if any parent nodes are under
		for (PathNode node : this){
			if (Util.blocksEquivalent(blockUnder, node.getAssosiatedBlock())
					|| Util.blocksEquivalent(blockUnder.getRelative(BlockFace.DOWN, 2),
							node.getAssosiatedBlock())){
				valid = false;
				return;
			}
		}
		Block blockOver = assosiatedBlock.getRelative(BlockFace.UP);
		Block[] blockCheck = new Block[]{blockUnder, assosiatedBlock, blockOver};
		BlockLoop:
		for (int i = 0; i < blockCheck.length && valid; i++) {
			// note: i == 0 is a check to see if it is the bottom block
			Block item = blockCheck[i];
			penalty += getBlockPenalty(item, i == 0);
		}
		if (penalty != penalty) {// if penalty is NaN
			valid = false;
		}
	}
	private PathNode _parent;

	public PathNode() {
	}

	public PathNode(PathNode parent) {
		_parent = parent;
		g = parent.g + 1;// default assumsion
	}

	public double getG() {
		return g;
	}

	public void setG(double g) {
		_cachedF = -1;
		this.g = g;
	}

	public double getPenalty() {
		return penalty;
	}

	public boolean isValid() {
		return valid;
	}

	public PathNode getParent() {
		return _parent;
	}

	public void setParent(PathNode value) {
		_cachedF = -1;
		this._parent = value;
		g = value.g + 1;
	}

	public void setValues(double g, double h) {
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

	public double getF() {
		if (_cachedF < 0) { //calculate F
			_cachedF = h + g + penalty;
		}
		return _cachedF;
	}

	@Override
	public int compareTo(PathNode o) {
		return Math.round(Math.round(getF() - o.getF()));
	}

	private static boolean typeHasTileEntity(Material type) {
		switch (type) {
			case SIGN:
			case SIGN_POST:
			case CHEST:
			case DISPENSER:
			case FURNACE:
			case BREWING_STAND:
	    //case HOPPER:
			//case DROPPER:
			case BEACON:
			case MOB_SPAWNER:
			case NOTE_BLOCK:
			case JUKEBOX:
			case ENCHANTMENT_TABLE:
			case ENCHANTED_BOOK:
			case ENDER_PORTAL:
			case ENDER_CHEST:
			case SKULL:
			case COMMAND:
				return true;
			default:
				return false;
		}
	}

	private static double getBlockPenalty(Block block, boolean isUnderTrack) {
		Material type = block.getType();
		if (!isUnderTrack) { //not undertrack means it will be removed
			if (typeHasTileEntity(type) || type == Material.BEDROCK) {
				return Double.NaN; // is forbidden
			}
		}
		if (Util.isAirEquivalent(type)) {
			if (isUnderTrack) {
				return AIR_PENALTY;
			} else if (!typeHasTileEntity(type)) {
				return 0; // is optimal
			}
		} else { // not air
			if (block.isLiquid()) {
				if (isUnderTrack) {
					return LIQUID_UNDER_PENALTY;
				} else {
					return LIQUID_THROUGH_PENALTY;
				}
			}

			if (isUnderTrack) {
				return 0; // is ok
			} else {
				return TUNNEL_PENALTY;
			}
		}

		return Double.NaN;// not yet implemented or forbidden, therefore avoid
	}

	List<PathNode> getPotentionals(ArrayList<PathNode> closedList,
			PriorityQueue<PathNode> openList) {
		List<Block> surrounding = getPotentionals();
		boolean checkStraight = false;
		boolean isX = false;
		int coordVal = 0;
		Block prev;
		if (getParent() == null){
			// try to find a previous track
			List<Block> trackBlocks = new ArrayList<>(SURROUNDING_BLOCK_COUNT);
			Util.getTrackBlocksInList(surrounding, trackBlocks);
			if (trackBlocks.isEmpty())
				prev = null;
			else if (trackBlocks.size() == 1){
				prev = trackBlocks.get(0);
			}
			else{
				// choose one of them...
				//for now, grab the first
				prev = trackBlocks.get(0);
			}
		}
		else
			prev = getParent().assosiatedBlock;

		if (prev != null){
			isX = prev.getX() == assosiatedBlock.getX();
			if (isX){
				coordVal = assosiatedBlock.getX();
			}
			else{
				coordVal = assosiatedBlock.getZ();
			}
			checkStraight = prev.getY() != assosiatedBlock.getY();
			for(Iterator<Block> it = surrounding.iterator(); it.hasNext();){
				Block item = it.next();
				boolean sameLevel = item.getY() == assosiatedBlock.getY();
				// must be a straight line
				boolean passes;
				if (isX){
					passes = item.getX() == coordVal;
				}
				else{
					passes = item.getZ() == coordVal;
				}
				if (!passes && (checkStraight || !sameLevel))
					it.remove();
			}
		}
		List<PathNode> itemsInOpenList = Util.getExistingPathNodesFromBlockList(
				surrounding, openList);
		List<PathNode> itemsInClosedList = Util.getExistingPathNodesFromBlockList(
				surrounding, closedList);
		List<PathNode> refreshList = new ArrayList<>(SURROUNDING_BLOCK_COUNT);
		for (PathNode openNode : itemsInOpenList) {
			if (checkStraight){
				Block item = openNode.assosiatedBlock;
				// must be a straight line
				boolean sameLevel = item.getY() == assosiatedBlock.getY();
				// must be a straight line
				boolean passes;
				if (isX){
					passes = item.getX() == coordVal;
				}
				else{
					passes = item.getZ() == coordVal;
				}
				if (!passes && (checkStraight || !sameLevel))
					continue;
			}
			if (g + 1 < openNode.getG()) { // if it takes less effort to get there from here, set this as it's parent
				openNode.setParent(this);
				refreshList.add(openNode);
			}
		}
		if (refreshList.size() > 0) {
			openList.removeAll(refreshList);
			//openList.addAll(refreshList); they will get added back later
		}
		List<PathNode> returnList = new ArrayList<>(surrounding.size());
		for (Block block : surrounding) {
			PathNode item = new PathNode(this);
			item.setAssosiatedBlock(block);
			returnList.add(item);
		}
		returnList.addAll(refreshList);
		return returnList;
	}

	List<Block> getPotentionals() {
		// obtain the raw blocks
		List<Block> surrounding = new ArrayList<>(SURROUNDING_BLOCK_COUNT);
		Block[] checkBlock = new Block[]{
			assosiatedBlock.getRelative(BlockFace.DOWN),
			assosiatedBlock,
			assosiatedBlock.getRelative(BlockFace.UP)
		};
		for (Block block : checkBlock) {
			surrounding.add(block.getRelative(BlockFace.NORTH));
			surrounding.add(block.getRelative(BlockFace.EAST));
			surrounding.add(block.getRelative(BlockFace.SOUTH));
			surrounding.add(block.getRelative(BlockFace.WEST));
		}
		return surrounding;
	}
	

	@Override
	/**
	 * Returns an iterator that goes from here to the start
	 */
	public Iterator<PathNode> iterator() {
		// note: the first item is always this
		return new Iterator<PathNode>() {
			final PathNode start = PathNode.this;
			PathNode current = null;// PathNode.this;

			@Override
			public boolean hasNext() {
				if (current == null) {
					return true;
				} else {
					return current.getParent() != null;
				}
			}

			@Override
			public PathNode next() {
				if (current == null) {
					return current = start;
				} else {
					return current = current.getParent();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Cannot remove from this collection");
			}
		};
	}

	@Override
	public String toString() {
		return "PathNode{g=" + g + ",h=" + h + ",penalty=" + penalty
				+ ",f=" + getF() + ",assosiatedBlock=" + assosiatedBlock + "}";
	}

}
