/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.asdfa.minecraft.WorldTracks.TrackMaker;


/**
 *
 * @author austin
 */
public class PathNode implements Comparable<PathNode> {
    private int _cachedF = -1;
    /*
     * Movement cost from the starting point
     */
    int g = -1;
    /*
     * estimated movement cost to goal
     */
    int h = -1;
    int penalty;
    private PathNode _parent;

    public PathNode() {
    }
    public PathNode(PathNode parent){
	_parent = parent;
    }
    
    public int getG() {
	return g;
    }
    public void setG(int g) {
	_cachedF = -1;
	this.g = g;
    }

    public int getPenalty() {
	return penalty;
    }
    public void setPenalty(int penalty) {
	_cachedF = -1;
	this.penalty = penalty;
    }

    public PathNode getParent() {
	return _parent;
    }

    public void setParent(PathNode value) {
	_cachedF = -1;
	this._parent = value;
    }
    

    public int getH() {
	return h;
    }
    public void setH(int h) {
	_cachedF = -1;
	this.h = h;
    }
    
    public int getF(){
	if (_cachedF < 0){ //calculate F
	    _cachedF = h + g + penalty;
	}
	return _cachedF;
    }

    @Override
    public int compareTo(PathNode o) {
	return getF() - o.getF();
    }
    
    
}
