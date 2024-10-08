/* Copyright (c) 2007-2017 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper.server;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import org.junit.Test;

import minesweeper.server.MinesweeperServer;

/**
 * MinesweeperServerTest test suite
 */
public class MinesweeperServerTest {
    
	 	private static final String LOCALHOST = "127.0.0.1";
	 	
	    private static final int PORT = 4000 + new Random().nextInt(1 << 15);

	    private static final int MAX_CONNECTION_ATTEMPTS = 10;

	    /**
	     * Start a MinesweeperServer in debug mode with a board file from BOARDS_PKG.
	     * 
	     * @param boardFile board to load
	     * @return thread running the server
	     * @throws IOException if the board file cannot be found
	     */
	    private static Thread startMinesweeperServer(String boardFile) throws IOException {
	    	final String boardPath = new File("test/minesweeper/server/board1").getAbsolutePath();
	        final String[] args = new String[] {
	                "--debug",
	                "--port", Integer.toString(PORT),
	                "--file", boardPath
	        };
	        Thread serverThread = new Thread(() -> MinesweeperServer.main(args));
	        serverThread.start();
	        return serverThread;
	    }

	    /**
	     * Connect to a MinesweeperServer and return the connected socket.
	     * 
	     * @param server abort connection attempts if the server thread dies
	     * @return socket connected to the server
	     * @throws IOException if the connection fails
	     */
	    private static Socket connectToMinesweeperServer(Thread server) throws IOException {
	        int attempts = 0;
	        while (true) {
	            try {
	                Socket socket = new Socket(LOCALHOST, PORT);
	                socket.setSoTimeout(3000);
	                return socket;
	            } catch (ConnectException ce) {
	                if ( ! server.isAlive()) {
	                    throw new IOException("Server thread not running");
	                }
	                if (++attempts > MAX_CONNECTION_ATTEMPTS) {
	                    throw new IOException("Exceeded max connection attempts", ce);
	                }
	                try { Thread.sleep(attempts * 10); } catch (InterruptedException ie) { }
	            }
	        }
	    }
	    
	    /**
	     * Test strategy for MinesweeperServer
	     * 
	     * Number of clients: 1, >1
	     * Number of clients disconnected: 0, 1, >1
	     * 
	     * Commands test:
	     * Look: correct rep even when other clients modify the board
	     * 
	     * Dig: 
	     * (x,y) state: outside board/not untouched, untouched, bomb
	     * (x,y) dig chain reaction # of squares: 0, 1, >1, >10
	     * 
	     * Flag: 
	     * (x,y): inside board and untouched, not first cond
	     * 
	     * Deflag: 
	     * (x,y): inside board and in flagged state, not first cond
	     * Bye:
	     * returns nothing
	     * 
	     * Invalid command:
	     * returns a help message
	     * 
	     * Help_req:
	     * matches regex
	     * 
	     * Special cases:
	     * after one user digs a bomb, update square so it contains no bomb and update adjacent squares count
	     */
	    
	    @Test
	    public void testMultipleClients() throws IOException{
	    	/**
	    	 *  * - - * - - -
	    	 *  - * * * * - -
	    	 *  - * - * - - -
	    	 *  - * * * - * -
	    	 *  - - - - - - -
	    	 */
	    	Thread thread = startMinesweeperServer("board1");
	    	
	    	Socket socket1 = connectToMinesweeperServer(thread);
	    	Socket socket2 = connectToMinesweeperServer(thread);
	    	
	    	BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
	    	PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);
	    	BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
	    	PrintWriter out2 = new PrintWriter(socket2.getOutputStream(), true);
	    	
	    	assertTrue("expected HELLO message", in1.readLine().startsWith("Welcome"));
	    	assertTrue("expected HELLO message", in2.readLine().startsWith("Welcome"));
	    	
	    	out1.println("dig 2 0");
	    	assertEquals("- - 4 - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	
	    	out2.println("look");
	    	assertEquals("- - 4 - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	
	    	out2.println("flag 0 0");
	    	assertEquals("F - 4 - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	assertEquals("- - - - - - -", in2.readLine());
	    	
	    	out1.println("dig 0 0");
	    	assertEquals("F - 4 - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	assertEquals("- - - - - - -", in1.readLine());
	    	
	    	out2.println("dig 5 3");
	    	assertEquals("BOOM!", in2.readLine());
	    	
	    	out1.println("look");
	    	assertEquals("F - 4 - - 1  ", in1.readLine());
	    	assertEquals("- - - - - 1  ", in1.readLine());
	    	assertEquals("- - - - 4 1  ", in1.readLine());
	    	assertEquals("- - - - 2    ", in1.readLine());
	    	assertEquals("- - - - 1    ", in1.readLine());
	    	
	    	out1.println("deflag 0 0");
	    	assertEquals("- - 4 - - 1  ", in1.readLine());
	    	assertEquals("- - - - - 1  ", in1.readLine());
	    	assertEquals("- - - - 4 1  ", in1.readLine());
	    	assertEquals("- - - - 2    ", in1.readLine());
	    	assertEquals("- - - - 1    ", in1.readLine());
	    	
	    	out1.println("dig 0 0");
	    	assertEquals("BOOM!", in1.readLine());
	    	
	    	out1.println("dig 0 0");
	    	assertEquals("1 - 4 - - 1  ", in1.readLine());
	    	assertEquals("- - - - - 1  ", in1.readLine());
	    	assertEquals("- - - - 4 1  ", in1.readLine());
	    	assertEquals("- - - - 2    ", in1.readLine());
	    	assertEquals("- - - - 1    ", in1.readLine());
	    	
	    	Socket socket3 = connectToMinesweeperServer(thread);
	    	
	    	BufferedReader in3 = new BufferedReader(new InputStreamReader(socket3.getInputStream()));
	    	PrintWriter out3 = new PrintWriter(socket3.getOutputStream(), true);
	    	assertTrue("expected HELLO message", in3.readLine().startsWith("Welcome"));
	    	
	    	out3.println("dig 2 2");
	    	assertEquals("1 - 4 - - 1  ", in3.readLine());
	    	assertEquals("- - - - - 1  ", in3.readLine());
	    	assertEquals("- - 8 - 4 1  ", in3.readLine());
	    	assertEquals("- - - - 2    ", in3.readLine());
	    	assertEquals("- - - - 1    ", in3.readLine());
	    	
	    	out3.println("dig -1 -1");
	    	assertEquals("1 - 4 - - 1  ", in3.readLine());
	    	assertEquals("- - - - - 1  ", in3.readLine());
	    	assertEquals("- - 8 - 4 1  ", in3.readLine());
	    	assertEquals("- - - - 2    ", in3.readLine());
	    	assertEquals("- - - - 1    ", in3.readLine());
	    	
	    	out3.println("deflag 0 0");
	    	assertEquals("1 - 4 - - 1  ", in3.readLine());
	    	assertEquals("- - - - - 1  ", in3.readLine());
	    	assertEquals("- - 8 - 4 1  ", in3.readLine());
	    	assertEquals("- - - - 2    ", in3.readLine());
	    	assertEquals("- - - - 1    ", in3.readLine());
	    	
	    	out3.println("flag 0 0");
	    	assertEquals("1 - 4 - - 1  ", in3.readLine());
	    	assertEquals("- - - - - 1  ", in3.readLine());
	    	assertEquals("- - 8 - 4 1  ", in3.readLine());
	    	assertEquals("- - - - 2    ", in3.readLine());
	    	assertEquals("- - - - 1    ", in3.readLine());
	    	
	    	socket1.close();
	    	socket2.close();
	    	socket3.close();
	    }
	    
	    @Test
	    public void testOneClient() throws IOException{
	    	/**
	    	 *  * - - * - - -
	    	 *  - * * * * - -
	    	 *  - * - * - - -
	    	 *  - * * * - * -
	    	 *  - - - - - - -
	    	 */
	    	Thread thread = startMinesweeperServer("board1");
	    	
	    	Socket socket = connectToMinesweeperServer(thread);
	    	
	    	BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	    	
	    	assertTrue("expected HELLO message", in.readLine().startsWith("Welcome"));
	    	
	    	out.println("dig 2 0");
	    	assertEquals("- - 4 - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	
	    	out.println("look");
	    	assertEquals("- - 4 - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	
	    	out.println("flag 0 0");
	    	assertEquals("F - 4 - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	
	    	out.println("dig 0 0");
	    	assertEquals("F - 4 - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	assertEquals("- - - - - - -", in.readLine());
	    	
	    	out.println("dig 5 3");
	    	assertEquals("BOOM!", in.readLine());
	    	
	    	out.println("look");
	    	assertEquals("F - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - - - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	out.println("deflag 0 0");
	    	assertEquals("- - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - - - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	out.println("dig 0 0");
	    	assertEquals("BOOM!", in.readLine());
	    	
	    	out.println("dig 0 0");
	    	assertEquals("1 - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - - - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	out.println("dig 2 2");
	    	assertEquals("1 - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - 8 - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	out.println("dig -1 -1");
	    	assertEquals("1 - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - 8 - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	out.println("deflag 0 0");
	    	assertEquals("1 - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - 8 - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	out.println("flag 0 0");
	    	assertEquals("1 - 4 - - 1  ", in.readLine());
	    	assertEquals("- - - - - 1  ", in.readLine());
	    	assertEquals("- - 8 - 4 1  ", in.readLine());
	    	assertEquals("- - - - 2    ", in.readLine());
	    	assertEquals("- - - - 1    ", in.readLine());
	    	
	    	socket.close();
	    }
	    /**
	    @Test(timeout = 10000)
	    public void publishedTest() throws IOException {

	        Thread thread = startMinesweeperServer("board_file_5");

	        Socket socket = connectToMinesweeperServer(thread);

	        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

	        assertTrue("expected HELLO message", in.readLine().startsWith("Welcome"));

	        out.println("look");
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());

	        out.println("dig 3 1");
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - 1 - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());
	        assertEquals("- - - - - - -", in.readLine());

	        out.println("dig 4 1");
	        assertEquals("BOOM!", in.readLine());

	        out.println("look"); // debug mode is on
	        assertEquals("             ", in.readLine());
	        assertEquals("             ", in.readLine());
	        assertEquals("             ", in.readLine());
	        assertEquals("             ", in.readLine());
	        assertEquals("             ", in.readLine());
	        assertEquals("1 1          ", in.readLine());
	        assertEquals("- 1          ", in.readLine());

	        out.println("bye");
	        socket.close();
	    }*/
}
