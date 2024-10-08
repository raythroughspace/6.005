/* Copyright (c) 2007-2017 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper;
import java.util.*;

/**
 * Multiplayer Minesweeper board.
 */
public class Board {
    
	private final List<List<Boolean>> mines;
	private final List<List<State>> state;
	private final int sizeX;
	private final int sizeY;
	private static enum State {DUG, FLAGGED, UNTOUCHED};
	
	/**
	 * Abstraction function:
	 * Represents a Board of size (sizeX, sizeY)
	 * where mines[i][j] indicates whether there is a mine 
	 * and state[i][j] indicates the state of the square
	 * 
	 * Rep invariant:
	 * # of columns in mines and state = sizeX
	 * # of rows in mines and state = sizeY
	 * sizeX,sizeY > 0
	 * 
	 * Rep exposure:
	 * All variables private and final
	 * Mutable variables mines and state are never returned and are initialized with deep copies
	 * 
	 * Thread safety:
	 * monitor pattern, only one thread allowed in an instance of Board
	 * No rep exposure
	 */
	
	/**
	 * Initialize a Board of size (sizeX, sizeY) with mines located according to mines.
	 * @param mines, 2d list where mines[i][j] = true indicates a mine at (i,j)
	 * @param sizeX, number of rows on the board
	 * @param sizeY, number of columns on the board
	 */
	public Board(List<List<Boolean>> mines, int sizeX, int sizeY) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.mines = new ArrayList<List<Boolean>>();
		for (int i=0; i<sizeY; ++i) {
			List<Boolean> row = new ArrayList<Boolean>();
			for (Boolean b: mines.get(i)) {
				row.add(b);
			}
			this.mines.add(row);
		}
		this.state = new ArrayList<List<State>>();
		for (int i=0; i<sizeY; ++i) {
		    List<State> row = new ArrayList<State>();
			for (int j=0; j<sizeX; ++j) {
				row.add(State.UNTOUCHED);
			}
			this.state.add(row);
		}
		checkRep();
	}
	/**
	 * Checks rep invariant
	 */
	private synchronized void checkRep() {
		assert(sizeX > 0);
		assert(sizeY > 0);
		assert(mines.size() == sizeY);
		assert(state.size() == sizeY);
		for (int i=0; i<sizeY; ++i) {
			assert (mines.get(i).size() == sizeX);
			assert(state.get(i).size() == sizeX);
		}
	}
	
	/**
	 * Returns a list of strings representation of board
	 * @return board representation
	 */
	public synchronized List<String> toRep() {
		List<String> rep = new ArrayList<String>();
		for (int j =0; j<sizeY; ++j) {
			String row = "";
			for (int i=0; i<sizeX; ++i) {
				if (state.get(j).get(i) == State.UNTOUCHED) {
					row += "-";
				}
				else if (state.get(j).get(i) == State.FLAGGED) {
					row += "F";
				}
				else { 
					if (nAdjBombs(i, j) == 0) {
						row += " ";
					}
					else {
						row += nAdjBombs(i,j);
					}
				}
				if (i != sizeX - 1) {
					row += " ";
				}
			}
			rep.add(row);
			
		}
		checkRep();
		return rep;
	}
	
	
	/**
	 * Returns the number of adjacent squares to (x,y) with bombs
	 * @param x, x coordinate of square
	 * @param y, y coordinate of square
	 * @return number of adjacent bombs to (x,y)
	 */
	private synchronized int nAdjBombs(int x, int y) {
		int bombs = 0;
		for (int i=x-1; i<=x+1; ++i) {
			for (int j=y-1; j<=y+1; ++j) {
				if (i == x && j == y) {
					continue;
				}
				if (checkBounds(i,j) && mines.get(j).get(i)) {
					++bombs;
				}
			}
		}
		checkRep();
		return bombs;
	}
	
	/**
	 * Check whether (x,y) is inside board
	 * @param x, x coordinate
	 * @param y, y coordinate
	 * @return true iff (x,y) is inside board
	 */
	private synchronized boolean checkBounds(int x, int y) {
		return (x >= 0 && x < sizeX && y >= 0 && y < sizeY);
	}
	
	/**
	 * Attempts to dig the square at (x,y). 
	 * Does nothing if (x,y) is outside board or not untouched and return false.
	 * If (x,y) is untouched and contains bomb, dig square, remove bomb and returns true.
	 * Otherwise, (x,y) is untouched and contains no bomb, return false and dig square.
	 * If (x,y) is untouched and no neighbours contain bombs, propagate 
	 * @param x, x coordinate
	 * @param y, y coordinate
	 * @return true if (x,y) is untouched and contains bomb, false otherwise
	 */
	public synchronized boolean dig(int x, int y) {
		boolean isBomb = false;
		if (!checkBounds(x,y) || state.get(y).get(x) != State.UNTOUCHED) {
			return false;
		}
		else {
			state.get(y).set(x, State.DUG);
			if (mines.get(y).get(x)) {
				mines.get(y).set(x, false);
				isBomb = true;
			}
			if (nAdjBombs(x,y) == 0) {
				propagate(x,y);
			}
			checkRep();
			return isBomb;
		}
	}
	/**
	 * For each (x,y)'s untouched neighbours, dig square and 
	 * if neighbour has no bomb, recursively propagate.
	 * (x,y) must contain no bomb
	 * @param x, x coordinate
	 * @param y, y coordinate
	 */
	private synchronized void propagate(int x, int y) {
		for (int i=x-1; i<=x+1; ++i) {
			for (int j=y-1; j<=y+1; ++j) {
				if (i == x && j== y) {
					continue;
				}
				else if (checkBounds(i,j) && (state.get(j).get(i) == State.UNTOUCHED)) {
					state.get(j).set(i, State.DUG);
					if (nAdjBombs(i,j) == 0) {
						propagate(i,j);
					}
				}
			}
		}
		checkRep();
	}
	
	/**
	 * Attempts to flag square at (x,y)
	 * Does nothing if (x,y) is not (inside board and untouched)
	 * @param x, x coordinate
	 * @param y, y coordinate
	 */
	public synchronized void flag(int x, int y) {
		if (checkBounds(x,y) && state.get(y).get(x) == State.UNTOUCHED) {
			state.get(y).set(x, State.FLAGGED);
		}
		checkRep();
	}
	
	/**
	 * Attempts to deflag square at (x,y)
	 * Does nothing if (x,y) is not (inside board and flagged)
	 * @param x, x coordinate
	 * @param y, y coordinate
	 */
	public synchronized void deflag(int x, int y) {
		if (checkBounds(x,y) && state.get(y).get(x) == State.FLAGGED) {
			state.get(y).set(x, State.UNTOUCHED);
		}
		checkRep();
	}
	/**
	 * Get sizeY of board
	 * @return sizeY
	 */
	public synchronized int getSizeY() {
		return sizeY;
	}
	
	/**
	 * Get sizeX of board
	 * @return sizeX
	 */
	public synchronized int getSizeX() {
		return sizeX;
	}
}
