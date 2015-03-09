package battleship;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.ToMar.Utils.*;

/**
 * A server for a network multi-player game of BattleShip  
 */
public class BattleShipServer
{
	public static final int SIZE = 10;
	public static final int TOTALSHIPS = 5;
	public static final int CONNECTING = 0;
	public static final int WAITING = 1;
	public static final int PLACING = 2;
	public static final int YOURTURN = 3;
	public static final int THEIRTURN = 4;
	public static final String[] MESSAGES = {"Waiting for an opponent.", "Placing your ships.", "It's your turn.", "It's your opponents's turn."};
	public static final int UNTESTED = 0;
	public static final int AIMED = 1;
	public static final int HIT = 2;
	public static final int MISS = 3;
	public static final String READY = "READY";
	private tmLog log = new tmLog(tmLog.TRACE);
	private ServerSocket listener;
	
 	/**
     * Runs the application. Pairs up clients that connect.
     */
    public static void main(String[] args) throws Exception 
    {
    	BattleShipServer battleShipServer = new BattleShipServer();
        battleShipServer.setListener(new ServerSocket(8901));
        battleShipServer.getLog().display("BattleShipServer: running");
        try 
        {
        	int gameCounter = 0;
            while (true) 
            {
            	gameCounter += 1;
                BattleShipGame game = new BattleShipGame(battleShipServer, gameCounter);
            }
        }
        catch (Exception e)
        {
        	battleShipServer.getLog().error("Error in BattleShipServer.main: " + e);
        }
        finally 
        {
        	battleShipServer.getListener().close();
        }
    }
    public ServerSocket getListener()
    {
    	return listener;
    }
	public tmLog getLog()
	{
		return log;
	}
	public void setLog(tmLog log)
	{
		this.log = log;
	}
	public void setListener(ServerSocket listener)
	{
		this.listener = listener;
	}
}
/**
 * A two-player game.
 */
class BattleShipGame 
{
	BattleShipServer server;
	Player[] players = new Player[2];
	int gameID;
	int currentPlayer = 0;
	
	public BattleShipGame(BattleShipServer server, int gameNumber)
    {
    	this.server = server;
    	gameID = gameNumber;
    	server.getLog().debug("BattleShipGame: constructing game " + gameID + " on server " + server);
    	try
    	{
    		for (int i = 0; i < 2; i++)
    		{	
    			players[i] = new Player(server.getListener().accept(), this, i);
//    	    	server.getLog().debug("BattleShipGame: Player " + i + " is " + players[i].toString());
    		}	
    	}
    	catch (Exception e)
    	{
    		server.getLog().error("BattleShipServer.BattleShipGame constructor: " + e);
    	}
    	players[0].setOpponent(players[1]);
    	players[1].setOpponent(players[0]);
    	players[0].start();
    	players[1].start();
    }
    public int getCurrentPlayer()
	{
		return currentPlayer;
	}
	public void setCurrentPlayer(int currentPlayer)
	{
		this.currentPlayer = currentPlayer;
	}
    public int getID()
    {
    	return gameID;
    }
	public BattleShipServer getServer()
	{
		return server;
	}
    /**
     * The class for the helper threads in this multithreaded server
         * This thread is the handler for each player and owns the socket
         * through which each client communicates
     */
    class Player extends Thread 
    {
    	int gameStage = 0;
    	int playerID;
    	int shipsLeft = 5;
    	String playerName;
		GridSquare[][] board = new GridSquare[BattleShipServer.SIZE][BattleShipServer.SIZE];
    	Ship[] ships = new Ship[BattleShipServer.TOTALSHIPS];
		BattleShipGame game;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public String toString()
        {
        	return "" + playerID + shipsLeft + playerName;
        }
        public Player(Socket socket, BattleShipGame game, int playerID) 
        {
            this.socket = socket;
            this.game = game;
            this.playerID = playerID;
            try 
            {
//            	game.getServer().getLog().debug("Game " + game.getID() + ", Player " + playerID + " says hello.");
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                // gameStage is CONNECTING, send a playerID to the client
                sendToClient("" + gameStage + playerID);
                // gameStage is CONNECTING, client sends back player name
                setPlayerName(getClientResponse());
                // set stage to WAITING; will be updated when there's another player
                gameStage = BattleShipServer.WAITING;
            } 
            catch (Exception e)
            {
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerID + " died: " + e);
            }
            for (int i = 0; i < BattleShipServer.SIZE; i++)
            {
            	for (int j = 0; j < BattleShipServer.SIZE; j++)
            	{	
            		board[i][j] = new GridSquare(i, j);
            	}	
            }
            for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
            {
            	ships[i] = new Ship(i);
            	placeShip(ships[i], 0, i + 1, false);
            }
        }
        public void sendToClient(String s)
        {
    		server.getLog().debug("Game " + game.getID() + " Stage " + gameStage + " Player " + playerID + " Sending to Client: +" + s + "+");
            output.println(s);
        }
        public String getClientResponse() throws Exception
        {
        	try
        	{
        		String temp = input.readLine();
        		server.getLog().debug("Player " + playerID + " Client responded: +" + temp + "+");
        		if (gameStage == Integer.parseInt(temp.substring(0, 1)))
        		{
        			return temp.substring(1);
        		}
        		else
        		{
                	game.getServer().getLog().error("BattleShipServer.Player: gameStage should be " + gameStage + ", instead received " + temp);
        			throw new Exception("BattleShipServer.Player.getClientResponse: gameStage not correct.");
        		}
        	}
            catch (Exception e)
            {
            	game.getServer().getLog().error("BattleShipServer.Player: ERROR getting client response: " + e);
            	throw new Exception(e);
            }
        }
        public String boardToString(boolean me)
        {
        	StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < BattleShipServer.SIZE; i++)
            {
            	for (int j = 0; j < BattleShipServer.SIZE; j++)
            	{
            		if (me)
            		{	
            			sb.append("" + board[i][j].getContents() + board[i][j].getStatus());
            		}
            		else
            		{	
            			sb.append("" + board[i][j].getStatus());
            		}
            	}	
            }
        	return sb.toString();
        }
		public String getPlayerName()
		{
			return playerName;
		}
		public void setPlayerName(String pName)
		{
			this.playerName = pName;
		}
        public boolean placeShip(Ship ship, int row, int col, boolean horizontal)
        {
        	ship.setStartRow(row);
        	ship.setStartCol(col);
        	ship.setHorizontal(horizontal);
        	for (int i = 0; i < ship.getSize(); i++)
        	{	
        		board[row][col].setContents(ship.getId());
        		if (horizontal)
        		{
        			col += 1;
        		}
        		else
        		{
        			row += 1;
        		}
        	}	
        	return true;
        }
        /**
         * Accepts notification of who the opponent is.
         */
        public void setOpponent(Player opponent) 
        {
            if (gameStage == BattleShipServer.WAITING)
            {
                this.opponent = opponent;
            	// send the opponent's data to the client, then change status
                sendToClient("" + gameStage + opponent.toString());
            	gameStage = BattleShipServer.YOURTURN;			// in future, set to PLACING
            }
        }
        public int getGameStage()
		{
			return gameStage;
		}
		public void setGameStage(int gameStage)
		{
			this.gameStage = gameStage;
		}
        /**
         * The run method of this thread.
         */
        public void run() 
        {
        	// this method is launched after all players are connected
        	// only coding for gameStage == PLAYING (ships are hard-coded for now)
            try 
            {
            	String resp = getClientResponse();
            	if (BattleShipServer.READY.equalsIgnoreCase(resp))
            	{
        			// during this stage, the message from the server will be
        			// column 1: 3 (gameStage)
        			// column 2:   (id of currentPlayer)
        			// column 3:   (shipsLeft of currentPlayer)
        			// column 4:   (boardToString of currentPlayer's opponent, 100 chars if it's their board, 200 if it's yours)
            		StringBuilder sb = new StringBuilder("" + gameStage + game.getCurrentPlayer());
            		if (game.getCurrentPlayer() == playerID)	// it's your turn
            		{
            			sb.append("" + shipsLeft + opponent.boardToString(false));
            		}
            		else
            		{
            			sb.append("" + opponent.shipsLeft + boardToString(true));
            		}
            		sendToClient(sb.toString());
            		while (true) {}
            	}
            	
            } 
            catch (Exception e) 
            {
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerID + " died: " + e);
            } 
            finally 
            {
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerID + " closing socket.");
                
            	try {socket.close();} catch (IOException e) {}
            }
        }
        private class Ship
        {
        	private int[] shipLengths = {5, 4, 3, 3, 2};
        	private int id;
        	private int size;
        	private boolean horizontal;
        	private int startRow;
        	private int startCol;
        	
        	public Ship(int idx)
        	{
        		this.id = idx + 1;
        		this.size = shipLengths[idx];
        		this.horizontal = false;				// will be set later
        		this.startRow = 0;						// will be set later
        		this.startCol = idx;					// will be set later
        	}
			public int getId()
			{
				return id;
			}
			public void setId(int id)
			{
				this.id = id;
			}
			public int getSize()
			{
				return size;
			}
			public void setSize(int size)
			{
				this.size = size;
			}
			public boolean isHorizontal()
			{
				return horizontal;
			}
			public void setHorizontal(boolean horizontal)
			{
				this.horizontal = horizontal;
			}
			public int getStartRow()
			{
				return startRow;
			}
			public void setStartRow(int startRow)
			{
				this.startRow = startRow;
			}
			public int getStartCol()
			{
				return startCol;
			}
			public void setStartCol(int startCol)
			{
				this.startCol = startCol;
			}
        }
        private class GridSquare
        {
        	static final int UNTESTED = 0;
        	static final int SHOTAIMED = 1;
        	static final int NOSHIP = 2;
			static final int SHIP = 3;
			private int row;
        	private int column;
        	private int status= 0;			// corresponds to display icons
        	private int contents = 0;		// will hold boatID if one is there
        	
        	public GridSquare(int row, int col)
        	{
        		this.row = row;
        		this.column = col;
        	}
        	public int getRow()
			{
				return row;
			}
			public void setRow(int row)
			{
				this.row = row;
			}
			public int getColumn()
			{
				return column;
			}
			public void setColumn(int column)
			{
				this.column = column;
			}
			public int getStatus()
			{
				return status;
			}
			public void setStatus(int status)
			{
				this.status = status;
			}
			public int getContents()
			{
				return contents;
			}
			public void setContents(int contents)
			{
				this.contents = contents;
			}
        }
    }
}		
