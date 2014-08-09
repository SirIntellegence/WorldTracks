/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.asdfa.minecraft.WorldTracks.TrackMaker.PathNode;
//import org.apache.commons.collections.buffer.PriorityBuffer;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

/**
 *
 * @author austin
 */
public final class Util {

	private Util() {
	}

	public static void givePlayerStandardTrack(Player player) {
		player.getInventory().addItem(new ItemStack(Material.RAILS, 64));
	}

	public static ArrayList<ItemStack> getHotBarItems(PlayerInventory inventory) {
		ArrayList<ItemStack> items = new ArrayList<ItemStack>(9);
		for (int i = 0; i < 9; i++) {
			items.add(inventory.getItem(i));
		}
		return items;
	}

	public static boolean isProvidedTrack(ItemStack stack) {
		if (stack == null) {
			return false;
		} else {
			return isProvidedTrack(stack.getType());
		}
	}

	public static boolean isProvidedTrack(Block block) {
		return isProvidedTrack(block.getType());
	}

	public static void givePlayerTrack(Player player) {
		player.getInventory().addItem(new ItemStack(Material.RAILS, 64), new ItemStack(Material.POWERED_RAIL, 64));
	}

	public static Vector roundLocation(Location location) {
		return new Vector(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public static void givePlayerBoosterTrack(Player player) {
		player.getInventory().addItem(new ItemStack(Material.POWERED_RAIL, 64));
	}

	public static Location absoluteValue(Location location) {
		if (location.getX() < 0) {
			location.setX(location.getX() * -1);
		}
		if (location.getY() < 0) {
			location.setY(location.getY() * -1);
		}
		if (location.getZ() < 0) {
			location.setZ(location.getZ() * -1);
		}
		return location;
	}

	public static Vector absoluteValue(Vector vector) {
		if (vector.getX() < 0) {
			vector.setX(vector.getX() * -1);
		}
		if (vector.getY() < 0) {
			vector.setY(vector.getY() * -1);
		}
		if (vector.getZ() < 0) {
			vector.setZ(vector.getZ() * -1);
		}
		return vector;
	}

	public static double getFurthest(Location target, List<? extends Block> blocks) {
		double largest = 0;
		Location absoluteTarget = target.clone();
		absoluteValue(absoluteTarget);
		for (Block block : blocks) {
			Location diff = absoluteValue(block.getLocation().clone());
			Location result = absoluteValue(absoluteTarget.clone().subtract(diff));
			if (result.getX() > largest) {
				largest = result.getX();
			}
			if (result.getY() > largest) {
				largest = result.getY();
			}
			if (result.getY() > largest) {
				largest = result.getY();
			}
		}
		return largest;
	}

	public static InventoryQueryResult checkPlayerInventory(Player player) {
		PlayerInventory inventory = player.getInventory();
		InventoryQueryResult result = new InventoryQueryResult();
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack != null) {
				if (isProvidedTrack(stack.getType())) {
					if (stack.getType() == Material.RAILS && result.normalTrackSlotNum < 0) {
						result.normalTrackSlotNum = i;
					} else if (stack.getType() == Material.POWERED_RAIL && result.boosterTrackSlotNum < 0) {
						result.boosterTrackSlotNum = i;
					}
				}
				if (stack.getAmount() > 0 && !result.playerHasItems) {
					result.playerHasItems = true;
				}
			}
		}
		return result;
	}

	public static List<Block> getTrackBlocksInList(List<Block> blocks, List<Block> outList) {
		if (outList == null) {
			outList = new ArrayList<Block>();
		}
		for (Block block : blocks) {
			if (isBlockTypeTrack(block.getType())) {
				outList.add(block);
			}
		}
		return outList;
	}

	public static boolean isProvidedTrack(Material type) {
		return type == Material.RAILS || type == Material.POWERED_RAIL;
	}

	public static boolean isTrackBlock(Block block) {
		if (block == null) {
			return false;
		}
		return isBlockTypeTrack(block.getType());
	}

	public static boolean isBlockTypeTrack(Material type) {
		return type == Material.RAILS || type == Material.POWERED_RAIL || type == Material.DETECTOR_RAIL;
	}

	public static Location getDistanceInbeetween(Location first, Location second) {
		if (first == null) {
			throw new NullArgumentException("first");
		}
		if (second == null) {
			throw new NullArgumentException("second");
		}
		if (first.getWorld() != second.getWorld()) {
			throw new IllegalArgumentException("Both of the provided locations "
					+ "must be in the same world!");
		}
		Location result = first.clone();
		result = result.subtract(second);
		result = absoluteValue(result);

		return result;
	}

	public static Vector getDistanceInbeetween(Vector first, Vector second) {
		if (first == null) {
			throw new NullArgumentException("first");
		}
		if (second == null) {
			throw new NullArgumentException("second");
		}
		return absoluteValue(first.clone().subtract(second));
	}

	public static List<PathNode> getExistingPathNodesFromBlockList(List<Block> blocks, Collection<PathNode> openList) {
		List<PathNode> nodes = new ArrayList<PathNode>();
		NodeLoop:
		for (PathNode node : openList) {
			for (Iterator<Block> blockIt = blocks.iterator(); blockIt.hasNext();) {
				Block block = blockIt.next();
				if (blocksEquivalent(block, node.getAssosiatedBlock())) {
					nodes.add(node);
					blockIt.remove();
					continue NodeLoop;
				}
			}
		}
		return nodes;
	}

	public static boolean blocksEquivalent(Block a, Block b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null)
			return false;
		Location locationA = a.getLocation();
		Location locationB = b.getLocation();
		if (locationA.getWorld().getName().equals(locationB.getWorld().getName())) {
			if (locationA.getBlockX() == locationB.getBlockX()
					&& locationA.getBlockY() == locationB.getBlockY()
					&& locationA.getBlockZ() == locationB.getBlockZ()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Determine if the Provided type, for all we care, is air
	 *
	 * @param type
	 * @return
	 */
	public static boolean isAirEquivalent(Material type) {
		if (isBlockTypeTrack(type)) {
			return true;
		}
		switch (type) {
			case AIR:
			case SNOW:
			case LONG_GRASS:
			case WEB:
			case LEVER:
			case STONE_BUTTON:
			case WOOD_BUTTON:
			case REDSTONE:
			case REDSTONE_TORCH_OFF:
			case REDSTONE_TORCH_ON:
			case REDSTONE_WIRE:
			case TRIPWIRE:
			case TRIPWIRE_HOOK:
			case CARROT:
			case CROPS:
			case COCOA:
			case RED_ROSE:
			case YELLOW_FLOWER:
			case MELON_STEM:
			case RED_MUSHROOM:
			case BROWN_MUSHROOM:
			case SUGAR_CANE_BLOCK:
			case SAPLING:
			case WHEAT:
			case VINE:
			case FIRE:
			case SIGN:
			case SIGN_POST:
			case TORCH:
			case NETHER_WARTS:
			case DRAGON_EGG:
				return true;
			default:
				return false;
		}
	}

}
