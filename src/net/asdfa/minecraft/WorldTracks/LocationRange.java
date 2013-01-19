/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 *
 * @author austin
 */
public class LocationRange {
    Location _location;
    int _range;

    public LocationRange(Location spot, int range) {
	_location = spot;
	_range = range;
    }
    
    public <T extends Entity> List<T> getThoseInRange(List<T> canidates){
	List<T> outList = new ArrayList<T>(canidates.size());
	for (Iterator<T> it = canidates.iterator(); it.hasNext();) {
	    T item = it.next();
	    Location location = item.getLocation();
	    if ((_location.getX() + _range > location.getX() && //check X
		    _location.getX() - _range < location.getX()) && 
		    (_location.getY() + _range > location.getY() && //check Y
		    _location.getY() - _range < location.getY()) &&
		    (_location.getZ() + _range > location.getZ() && //check Z
		    _location.getZ() - _range < location.getZ())){
		outList.add(item);
	    }
	}
	return outList;
    }
    
}
