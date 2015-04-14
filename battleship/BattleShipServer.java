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
	public static final int CONNECTING = 0;		//you connect, server sends id
	public static final int WAITING = 1;		//you send name, server sends opponent
	public static final int PLACING = 2;		//you send ship placement, server sends who goes first
	public static final int READY = 3;			//you say ready
	public static final int YOURTURN = 4;		//you say ready, server sends oppboard
	public static final int YOURANSWER = 5;		//you send shots, server sends updated oppboard
	public static final int THEIRTURN = 6;		//you say ready, server sends yourboard
	public static final int THEIRANSWER = 7;	//you say ready, server sends updated board
	public static final int YOUWON = 8;
	public static final int THEYWON = 9;
	public static final int GAMEOVER = 10;
	public static final int UNTESTED = 0;
	public static final int AIMED = 1;
	public static final int HIT = 2;
	public static final int MISS = 3;
	public static final String OK = "OK";
	public static final int[] SHIPLENGTHS = {5, 4, 3, 3, 2};
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
	PlayerThread[] players = new PlayerThread[2];
	int gameID;
	int updateFlag = -1;
	boolean[] readyToPlay = {false, false};
	boolean active;
	
	public BattleShipGame(BattleShipServer server, int gameNumber)
    {
    	this.server = server;
    	gameID = gameNumber;
    	active = true;
    	server.getLog().debug("BattleShipGame: constructing game " + gameID + " on server " + server);
    	try
    	{
    		for (int i = 0; i < 2; i++)
    		{	
    			players[i] = new PlayerThread(server.getListener().accept(), this, i);
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
	/*
	 * updateFlag will be set whenever shots are evaluated, then turned immediately back off
	 */
	public boolean retrieveAndFlipFlag(int playerID)
	{
		if (updateFlag == playerID)
		{
			updateFlag = -1;
    		server.getLog().debug("Retrieved updateFlag and reset to -1");
			return true;
		}
		return false;
	}
	public void setUpdateFlag(int playerID)
	{
		server.getLog().debug("update flag set for pickup by player" + playerID);
		this.updateFlag = playerID;
	}
	public boolean isActive()
	{
		return active;
	}
	public void setActive(boolean active)
	{
		this.active = active;
	}
    public int getID()
    {
    	return gameID;
    }
    public boolean notReadyYet(int playerId)
    {
    	readyToPlay[playerId] = true;
    	if (readyToPlay[0] && readyToPlay[1])
    	{
    		return false;
    	}
    	return true;
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
    class PlayerThread extends Thread 
    {
    	Player playerInfo = new Player();
    	int gameStage = 0;
		OceanSquare[][] board = new OceanSquare[BattleShipServer.SIZE][BattleShipServer.SIZE];
    	Ship[] ships = new Ship[BattleShipServer.TOTALSHIPS];
		BattleShipGame game;
        PlayerThread opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

		public String toString()
        {
        	return "" + playerInfo.getId() + playerInfo.getShipsLeft() + playerInfo.getName();
        }
        public PlayerThread(Socket socket, BattleShipGame game, int playerID) 
        {
            this.socket = socket;
            this.game = game;
            this.playerInfo.setId(playerID);
            try 
            {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                /*
                 * gameStage is CONNECTING
                 *   send a playerID to the client
                 *   client responds with their name
                 */
                sendToClient("" + gameStage + playerID);
                playerInfo.setName(getClientResponse());
                /*
                 * gameStage set to WAITING
                 * 		when an opponent signs on, setOpponent will update stage to PLACING
                 * 		when ships are placed, stage will be either YOURTURN or THEIRTURN
                 * 		all activity then shifts to run method		
                 */
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
            		board[i][j] = new OceanSquare(i, j);
            	}	
            }
            for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
            {
            	ships[i] = new Ship(i);
            }
        }
        public void sendToClient(String s)
        {
    		server.getLog().debug("Game " + game.getID() + " Stage " + gameStage + " Player " + playerInfo.getId() + " Sending to Client: +" + s + "+");
            output.println(s);
        }
        public String getClientResponse() throws Exception
        {
        	try
        	{
        		String temp = input.readLine();
        		server.getLog().debug("Game " + game.getID() + " Stage " + gameStage + " Player " + playerInfo.getId() + " Client responded: +" + temp + "+");
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
            			sb.append("" + board[i][j].getStatus() + board[i][j].getContents());
            		}
            		else
            		{	
            			sb.append("" + board[i][j].getStatus());
            		}
            	}	
            }
        	return sb.toString();
        }
        /**
         * Accepts notification of who the opponent is.
         */
        public void setOpponent(PlayerThread opponent) 
        {
            if (gameStage == BattleShipServer.WAITING)
            {
                this.opponent = opponent;
            	// send the opponent's data to the client, then change status
                sendToClient("" + gameStage + opponent.toString());
                gameStage = BattleShipServer.PLACING;
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
         * The run method of this Player's thread.
         */
        public void run() 
        {
        	// this method is launched after all players are connected
        	// only coding for gameStage > PLACING (ships are hard-coded for now)
        	/*
        	 * You say 3, you get sent oppboard (server is now waiting for 4)
        	 * You say 4, you get sent updated oppboard (server is now waiting for 5)
        	 * You say 5, when opp has said 6, you see your board
        	 * You say 6, when opp has said 4, you see your updated board
        	 * You say 3...
        	 */
            try 
            {
        		while (true) 
        		{
/*        			if (!game.isActive())
        			{
                    	gameStage = BattleShipServer.GAMEOVER;
	            		sendToClient("" + gameStage);
	            		break;
        			}
*/	            	String resp = getClientResponse();
	            	if (gameStage == BattleShipServer.PLACING)
	            	{
	            		// process ship placement
	            		if (placeShips(resp))
	            		{	
	            			// respond with playerID of player who goes first
	            			int firstPlayer = game.getID() % 2;
	            			// at this point you want to wait until both players have placed their ships
	            			while (game.notReadyYet(playerInfo.getId()))
	            			{
	            			}
		            		sendToClient("" + BattleShipServer.READY + firstPlayer);
	            			gameStage = (playerInfo.getId() == firstPlayer) ? BattleShipServer.YOURTURN : BattleShipServer.THEIRTURN;			// in future, set to PLACING
	            		}
	            		else
	            		{
		            		sendToClient("" + gameStage + BattleShipServer.READY);
	            		}
	            	}
	            	else if (gameStage == BattleShipServer.THEIRTURN)
	            	{
	            		if (BattleShipServer.OK.equalsIgnoreCase(resp))
		            	{
		            		sendToClient("" + gameStage + opponent.playerInfo.getShipsLeft() + playerInfo.getShipsLeft() + boardToString(true));
		            		gameStage = BattleShipServer.THEIRANSWER;
		            	}
		            	else
		            	{
		            		game.getServer().getLog().error("ERROR 101");
		            	}
	            	}	
		            else if (gameStage == BattleShipServer.THEIRANSWER)
		            {
		            	if (BattleShipServer.OK.equalsIgnoreCase(resp))
		            	{
			            	/*
			            	 *  stage is THEIRANSWER
			            	 *  you want to send them an updated board as soon as flag is set
			            	 */
		            		while(!game.retrieveAndFlipFlag(playerInfo.getId()))
		            		{
		            		}
		            		sendToClient("" + gameStage + opponent.playerInfo.getShipsLeft() + playerInfo.getShipsLeft() + boardToString(true));
		            		gameStage = (playerInfo.getShipsLeft() == 0) ? BattleShipServer.THEYWON : BattleShipServer.YOURTURN;
		            	}
		            	else
		            	{
		            		game.getServer().getLog().error("ERROR 102");
		            	}
		            }	
	            	else if (gameStage == BattleShipServer.YOURTURN)
	            	{	
		            	if (BattleShipServer.OK.equalsIgnoreCase(resp))
		            	{
			            	/*
			            	 *  stage is YOURTURN
			            	 *  	send opponent's board as soon as ships are placed
			            	 */
		            		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + opponent.boardToString(false));
		            		gameStage = BattleShipServer.YOURANSWER;
		            	}
		            	else
		            	{
		            		game.getServer().getLog().error("ERROR 103");
		            	}
	            	}
	            	else if (gameStage == BattleShipServer.YOURANSWER)
	            	{	
		            	/*
		            	 *  stage is YOURANSWER
		            	 *  	process shots
		            	 *  	send updated board
		            	 *  	next response will relinquish turn
		            	 */
		            	processShots(resp);
		            	game.setUpdateFlag(opponent.playerInfo.getId());
//		            	game.getServer().getLog().debug("shipsLeft is " + shipsLeft + ", opp is " + opponent.shipsLeft);
	            		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + opponent.boardToString(false));
	            		gameStage = (opponent.playerInfo.getShipsLeft() == 0) ? BattleShipServer.YOUWON : BattleShipServer.THEIRTURN;
	            	}
            	}
            } 
            catch (Exception e) 
            {
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerInfo.getId() + " died: " + e);
            } 
            finally 
            {
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerInfo.getId() + " closing socket.");
            	try {socket.close();} catch (IOException e) {}
            	game.setActive(false);
            }
        }
        public boolean placeShips(String s)
        {
        	int ctr = 0;
        	try
        	{
        		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
        		{
        			int row = Integer.parseInt(s.substring(ctr, ++ctr));
        			int col = Integer.parseInt(s.substring(ctr, ++ctr));
        			int temp = Integer.parseInt(s.substring(ctr, ++ctr));
        			boolean hor = (temp == 0) ? false : true;
        			if (!ships[i].place(board, row, col, hor))
        			{
        				return false;
        			}
        		}
        	}
        	catch(Exception e)
        	{
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerInfo.getId() + " place ship " + s + ": " + e);
        	}
        	return true;
        }
        public void processShots(String s)
        {
        	try
        	{
        		int sh = Integer.parseInt(s.substring(0,1));
        		int ctr = 1;
        		for (int i = 0; i < sh; i++)
        		{
        			int row = Integer.parseInt(s.substring(ctr, ++ctr));
        			int col = Integer.parseInt(s.substring(ctr, ++ctr));
        			if (applyShot(row, col))
        			{
        				// you hit something, see if you sank a ship
        				int counter = 0;
        				for (int j = 0; j < BattleShipServer.TOTALSHIPS; j++)
        				{
       						if (opponent.ships[j].isStillAlive())
       						{
       							counter += 1;
        					}
        				}
        				opponent.playerInfo.setShipsLeft(counter);
        			}
        		}
        	}
        	catch(Exception e)
        	{
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerInfo.getId() + " process shots from " + s + ": " + e);
        	}
        }
        public boolean applyShot(int r, int c)
        {
        	if (opponent.board[r][c].getStatus() == BattleShipServer.UNTESTED)
        	{
        		int cc = opponent.board[r][c].getContents();
        		if (cc == 0)			// nothing there
        		{
        			opponent.board[r][c].setStatus(BattleShipServer.MISS);
        			return false;
        		}
        		else
        		{
        			opponent.board[r][c].setStatus(BattleShipServer.HIT);
        			return true;
        		}
        	}
        	else
        	{
            	game.getServer().getLog().error("Game " + game.getID() + ", Player " + playerInfo.getId() + " apply shot " + r + ", " + c + ", status is not UNTESTED");
        	}
        	return false;
        }
    }
}		
