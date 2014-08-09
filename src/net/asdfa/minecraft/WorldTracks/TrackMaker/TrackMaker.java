/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import net.asdfa.minecraft.WorldTracks.CommandHook;
import net.asdfa.minecraft.WorldTracks.Util;
//import org.apache.commons.collections.buffer.PriorityBuffer;
//import org.apache.commons.collections.comparators.ComparableComparator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class TrackMaker {

	public TrackMaker() {
	}
	static final int BOOST_INTERVAL = 5;
	public final HashMap<Player, Block> lastTrackBlockNearby = new HashMap<Player, Block>();

	static boolean canTurn(Block target, Block prev, Block next) {
		if (prev == null || next == null)
			return true;
		//implementation for now....
		if (target.getY() != prev.getY() || target.getY() != next.getY()){
			return false;
		}
		return true;
	}

	/**
	 * Generates track to the player's position
	 *
	 * @param player
	 * @return
	 */
	public void makeTrack(Player player) {
		makeTrack(player, false);
	}

	/**
	 * Generates track to the player's position
	 *
	 * @param player
	 * @return
	 */
	public void makeTrack(Player player, boolean isFromAuto) {
		Block targetBlock = null;
		if (isFromAuto) {
			targetBlock = lastTrackBlockNearby.get(player);
			if (!Util.isTrackBlock(targetBlock)){
				targetBlock = null;
			}
		}
		if (targetBlock == null) {
			List<Block> blocksLookedAt;
			blocksLookedAt = player.getLineOfSight(null, 100);
			ArrayList<Block> result = new ArrayList<Block>();
			if (Util.getTrackBlocksInList(blocksLookedAt, result).size() > 0) {
				targetBlock = result.get(0);
			}
		}
		if (targetBlock != null) {
			boolean routeFound = false;
			PathNode resultNode = null;
//	    PriorityBuffer<PathNode> openList = new PriorityBuffer(ComparableComparator.
//		    comparableComparator());
			int giveupCount;
			if (isFromAuto)
				giveupCount = 1 << 10;
			else
				giveupCount = 1 << 12;
			PriorityQueue<PathNode> openList = new PriorityQueue<>(200);
			ArrayList<PathNode> closedList = new ArrayList<>(20);
			Location start = player.getLocation().clone();
			PathNode startingNode = new PathNode();
			startingNode.setAssosiatedBlock(start.getBlock());
			if (Util.isTrackBlock(startingNode.getAssosiatedBlock())) {
				return; // allready done
			}
			startingNode.setValues(0, calculateH(start, targetBlock.
					getLocation()));
			if (CommandHook.debug) {
				player.sendMessage("H was " + startingNode.getH());
			}
			openList.add(startingNode);
			SearchMain:
			while (!routeFound) {
				PathNode current = openList.remove();
				closedList.add(current);
				List<PathNode> potentionals = current.getPotentionals(closedList, openList);
				for (PathNode node : potentionals) {
					if (node.getH() < 0) { // calculate H
						node.setH(calculateH(targetBlock.getLocation(), node.getAssosiatedBlock().getLocation()));
						if (node.getH() == 0) { // we have arrived!
							resultNode = node;
							routeFound = true;
							break SearchMain;
						}
					}
				}
				openList.addAll(potentionals);
				if (openList.size() > giveupCount)
					break;
			}
			if (routeFound) {
				buildTrack(resultNode);
				lastTrackBlockNearby.put(player, startingNode.getAssosiatedBlock());
			}
			else{
				player.sendMessage("Unable to construct track to you. Are you "
						+ "reachable?");
			}
		} else {
			if (isFromAuto) {
				player.sendMessage("Unable to retrieve last track block.  "
						+ "Update this by issuing the commmand \"/WT makeTrack\"");
			} else {
				player.sendMessage("There are no tracks where you are looking, "
						+ "please look towards the track you want to connect to");
			}
		}
	}

	int calculateH(Location start, Location end) {
		Location location = Util.getDistanceInbeetween(start, end);
		Vector vector = location.toVector();
		//return (int)Math.floor(vector.length() * 10);
		double height = vector.getY();
		vector.setY(0);
		double length = vector.length();
		return (int) (Math.floor(length * 10) + height * 50);
	}

	private void buildTrack(PathNode resultNode) {
		int boostCount = 5;
		Block prev = null;
		for (PathNode node : resultNode) {
			Block targetBlock = node.getAssosiatedBlock();
			if (node.getPenalty() != 0) { //we need to modify the surroundings...
				Block below = targetBlock.getRelative(BlockFace.DOWN);
				if (below.isLiquid() || Util.isAirEquivalent(below.getType())) {
					below.setType(Material.COBBLESTONE);
				}
				Block above = targetBlock.getRelative(BlockFace.UP);
				if (!Util.isAirEquivalent(above.getType())) {
					above.setType(Material.AIR);
				}
			}
			if (!Util.isTrackBlock(targetBlock)){
			// construct track
			// boost type?
				Material trackType = Material.RAILS;
				if (boostCount > 5){
					boolean leverPlaced = placeLever(node, prev);
					if (leverPlaced){
						trackType = Material.POWERED_RAIL;
						boostCount = 0;
					}
				}
				targetBlock.setType(trackType);
			}
			prev = targetBlock;
			boostCount++;
		}
	}

	private boolean placeLever(PathNode node, Block prev) {
		//final Material switchType = Material.LEVER;
		final Material switchType = Material.REDSTONE_TORCH_ON;
		// is this section straight?
		boolean straight;
		boolean isX = true;
		if (prev == null){
			// find it!
			List<Block> tracks = new ArrayList<>(PathNode.SURROUNDING_BLOCK_COUNT);
			Util.getTrackBlocksInList(node.getPotentionals(), tracks);
			if (!tracks.isEmpty()){
				if (tracks.size() == 1)
					prev = tracks.get(0);
				else{
					//grab the first for now
					prev = tracks.get(0);
				}
			}
		}
		if (prev == null || node.getParent() == null){
			straight = true;
		}
		else{
//			boolean canTurn = canTurn(node.getAssosiatedBlock(), prev,
//					node.getParent().getAssosiatedBlock());

			if (prev.getX() == node.getAssosiatedBlock().getX() &&
					prev.getX() == node.getParent().getAssosiatedBlock().getX()){
				straight = true;
				isX = true;
			}
			else if (prev.getZ() == node.getAssosiatedBlock().getZ() &&
					prev.getZ() == node.getParent().getAssosiatedBlock().getZ()){
				straight = true;
				isX = false;
			}
			else {
				straight = false;
			}
		}
		if (!straight)
			return false;
		// find block to place leaver on
		Block lever = null;
		Block curr = node.getAssosiatedBlock();
		if (curr.isBlockPowered() || curr.isBlockIndirectlyPowered()){
			//dont need leaver
			return true;
		}
		// note: z = North/south
		BlockFace a, b;
		if (isX){
			a = BlockFace.EAST;
			b = BlockFace.WEST;
		}
		else{
			a = BlockFace.NORTH;
			b = BlockFace.SOUTH;
		}
		List<Block> potentionals = new ArrayList<>(5);
		potentionals.add(curr.getRelative(a));
		potentionals.add(curr.getRelative(b));
		potentionals.add(curr.getRelative(BlockFace.UP));
//		potentionals.add(curr.getRelative(BlockFace.UP).getRelative(b));
//		potentionals.add(curr.getRelative(BlockFace.UP).getRelative(a));

		Outer:
		for (Block item : potentionals){
			if (!Util.isAirEquivalent(item.getType())){
				continue;
			}
			List<Block> directions = Arrays.asList(
					item.getRelative(BlockFace.UP),
					item.getRelative(BlockFace.DOWN),
					item.getRelative(BlockFace.NORTH),
					item.getRelative(BlockFace.EAST),
					item.getRelative(BlockFace.SOUTH),
					item.getRelative(BlockFace.WEST)
			);
			boolean hasMount = false;
			for (Block block : directions) {
				if (block != curr && !Util.isAirEquivalent(block.getType())
						&& !block.isLiquid() && !Util.isTrackBlock(block)){
					lever = item;
					break Outer;
				}
			}
		}
		if (lever == null){
			Block target = potentionals.get(0);
			Block below = target.getRelative(BlockFace.DOWN);
			if (below != null && (below.isLiquid() || Util.isAirEquivalent(
					below.getType()))){
				below.setType(Material.COBBLESTONE);
				lever = target;
			}
		}
		if (lever == null)
			return false;
		final Block result = lever;
		CommandHook.instance.getServer().getScheduler().
				scheduleSyncDelayedTask(CommandHook.instance, new Runnable() {
				@Override
				public void run() {
					result.setType(switchType);
					if (switchType == Material.LEVER)
						result.setData((byte) (result.getData() | 0x8));    // set ON bit
					if (result.getType() != switchType){
						// this happens some times
						result.setType(switchType);
					}
				}
			});
//		lever.setType(switchType);
		// Switch lever on.
		return true;
	}
}
