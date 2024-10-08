/* Copyright (c) 2007-2017 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Board test suite
 */
public class BoardTest {
    
    // TODO: Testing strategy
    
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }
    
    @Test
    public void testBoard() {
    	/**
    	 *  * - - * - - -
    	 *  - * * * * - -
    	 *  - * - * - - -
    	 *  - * * * - * -
    	 *  - - - - - - -
    	 */
    	List<List<Boolean>> mines = new ArrayList<List<Boolean>>();
    	mines.add(Arrays.asList(true, false, false, true, false, false, false));
    	mines.add(Arrays.asList(false, true, true, true, true, false, false));
    	mines.add(Arrays.asList(false, true, false, true, false, false, false));
    	mines.add(Arrays.asList(false, true, true, true, false, true, false));
    	mines.add(Arrays.asList(false, false, false, false, false, false, false));
    	
    	Board board = new Board(mines, 7, 5);
    	board.dig(2, 0);
    	List<String> ans = Arrays.asList("- - 4 - - - -", 
    			"- - - - - - -",
    			"- - - - - - -",
    			"- - - - - - -",
    			"- - - - - - -");
    	
    	assert(ans.equals(board.toRep()));
    	
    	board.flag(0, 0);
    	ans = Arrays.asList("F - 4 - - - -", 
    			"- - - - - - -",
    			"- - - - - - -",
    			"- - - - - - -",
    			"- - - - - - -");
    	
    	assert(ans.equals(board.toRep()));
    	
    	board.dig(0, 0);
    	assert(ans.equals(board.toRep()));
    	
    	assert(board.dig(5, 3) == true);
    	ans = Arrays.asList("F - 4 - - 1  ",
    			"- - - - - 1  ",
    			"- - - - 4 1  ",
    			"- - - - 2    ",
    			"- - - - 1    ");
    	
    	assert(ans.equals(board.toRep()));
    	
    	board.deflag(0, 0);
    	ans = Arrays.asList("- - 4 - - 1  ",
    			"- - - - - 1  ",
    			"- - - - 4 1  ",
    			"- - - - 2    ",
    			"- - - - 1    ");
    	assert(ans.equals(board.toRep()));
    	
    	assert(board.dig(0, 0) == true);
    	ans = Arrays.asList("1 - 4 - - 1  ",
    			"- - - - - 1  ",
    			"- - - - 4 1  ",
    			"- - - - 2    ",
    			"- - - - 1    ");
    	assert(ans.equals(board.toRep()));
    	
    	board.dig(2, 2);
    	ans = Arrays.asList("1 - 4 - - 1  ",
    			"- - - - - 1  ",
    			"- - 8 - 4 1  ",
    			"- - - - 2    ",
    			"- - - - 1    ");
    	assert(ans.equals(board.toRep()));
    	
    }
    
}
