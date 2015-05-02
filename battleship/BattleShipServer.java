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
	public static final String AUTOPLAYER = "Silly";
	public static final int[] SHIPLENGTHS = {5, 4, 3, 3, 2};
	private tmLog log = new tmLog(tmLog.TRACE);
	private ServerSocket listener;
	
 	public BattleShipServer()
 	{
        try 
        {
            this.setListener(new ServerSocket(8901));
            this.getLog().display("BattleShipServer: running");
        	int gameCounter = 0;
            while (true) 
            {
            	gameCounter += 1;
            	// set to true for game between two humans
                BattleShipGame game = new BattleShipGame(this, gameCounter);
            }
        }
        catch (Exception e)
        {
        	this.getLog().error("Error in BattleShipServer: " + e);
        }
        finally
        {
        	try
			{
				this.getListener().close();
			} 
        	catch (IOException e1)
			{
				e1.printStackTrace();
			}
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
    public static void main(String[] args) throws Exception 
    {
    	// parameter is if you want your opponent to be human
    	BattleShipServer battleShipServer = new BattleShipServer();
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
	int numberOfMoves = 0;
	boolean[] readyToPlay = {false, false};
	boolean[] moved = {false, false};
	boolean active;
	
	public BattleShipGame(BattleShipServer server, int gameNumber)
    {
    	this.server = server;
    	gameID = gameNumber;
    	active = true;
    	server.getLog().debug("BattleShipGame: constructing game " + gameID + " on server " + server);
    	try
    	{
   			players[0] = new PlayerThread(server.getListener().accept(), this, 0);
   			players[1] = new PlayerThread(server.getListener().accept(), this, 1);
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
	public int getNumberOfMoves()
	{
		return numberOfMoves;
	}
	public void moved(int playerID)
	{
    	moved[playerID] = true;
    	if (moved[0] && moved[1])
    	{
    		numberOfMoves += 1;
    		moved[0] = moved[1] = false;
    	}
	}
	/*
	 * updateFlag will be set whenever shots are evaluated, then turned immediately back off
	 */
	public boolean retrieveAndFlipFlag(int playerID)
	{
		if (updateFlag == playerID)
		{
			updateFlag = -1;
    		server.getLog().debug("Game " + this.getID() + " Player " + playerID + " BattleShipGame.retrieveAndFlipFlag");
			return true;
		}
		return false;
	}
	public void setUpdateFlag(int playerID)
	{
		server.getLog().debug("Game " + this.getID() + " Player " + playerID + " BattleShipGame.setUpdateFlag for pickup by this player");
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
    	Ship[] ships = new Ship[BattleShipServer.TOTALSHIPS];
    	BattleShipGame game;
        PlayerThread opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        
        public PlayerThread(Socket socket, BattleShipGame game, int playerID)
        {
            this.game = game;
            this.playerInfo.setId(playerID);
            this.socket = socket;
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
            	abort("Game " + game.getID() + " Player " + playerID + " died: " + e);
            }
            for (int i = 0; i < BattleShipServer.SIZE; i++)
            {
            	for (int j = 0; j < BattleShipServer.SIZE; j++)
            	{	
            		playerInfo.getBoard()[i][j] = new OceanSquare(i, j);
            	}	
            }
            for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
            {
            	ships[i] = new Ship(i);
            }
        }
        public void processTurn(String resp)
        {
        	if (gameStage == BattleShipServer.PLACING)
        	{
        		// process ship placement
        		if (placeShips(resp))
        		{	
        			// respond with playerID of player who goes first
        			int firstPlayer = game.getID() % 2;
        			// at this point you want to wait until both players have placed their ships
        			int idleCounter = 0;
        			while (game.notReadyYet(playerInfo.getId()))
        			{
        				// this code does nothing, but seems to need to be here
        				idleCounter += 1;
        				if (idleCounter == 2100000000)
        				{
        			    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.processTurn, waiting");
        					idleCounter = 0;
        				}
        			}
            		sendToClient("" + BattleShipServer.READY + firstPlayer);
        			gameStage = (playerInfo.getId() == firstPlayer) ? BattleShipServer.YOURTURN : BattleShipServer.THEIRTURN;			// in future, set to PLACING
        		}
        	}
        	else if (gameStage == BattleShipServer.THEIRTURN)
        	{
        		if (BattleShipServer.OK.equalsIgnoreCase(resp))
            	{
            		sendToClient("" + gameStage + opponent.playerInfo.getShipsLeft() + playerInfo.getShipsLeft() + Functions.formatNumber(game.getNumberOfMoves(), 2) + boardToString(true));
            		gameStage = (playerInfo.getShipsLeft() == 0) ? BattleShipServer.THEYWON : (opponent.playerInfo.getShipsLeft() == 0) ? BattleShipServer.YOUWON : BattleShipServer.THEIRANSWER;  
            	}
            	else
            	{
            		abort("Expecting 6OK");
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
            		sendToClient("" + gameStage + opponent.playerInfo.getShipsLeft() + playerInfo.getShipsLeft() + Functions.formatNumber(game.getNumberOfMoves(), 2) + boardToString(true));
            		gameStage = (playerInfo.getShipsLeft() == 0) ? BattleShipServer.THEYWON : (opponent.playerInfo.getShipsLeft() == 0) ? BattleShipServer.YOUWON : BattleShipServer.YOURTURN;  
            	}
            	else
            	{
            		abort("Expecting 5OK");
            	}
            }	
        	else if (gameStage == BattleShipServer.YOURTURN)
        	{	
            	if (BattleShipServer.OK.equalsIgnoreCase(resp))
            	{
                	/*
                	 *  stage is YOURTURN
                	 *  	send opponent's board as soon as ships are placed
                	 *      increment move number
                	 */
            		game.moved(playerInfo.getId());
            		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + Functions.formatNumber(game.getNumberOfMoves(), 2) + opponent.boardToString(false));
            		gameStage = (playerInfo.getShipsLeft() == 0) ? BattleShipServer.THEYWON : (opponent.playerInfo.getShipsLeft() == 0) ? BattleShipServer.YOUWON : BattleShipServer.YOURANSWER;  
            	}
            	else
            	{
            		abort("Expecting 4OK");
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
        		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + Functions.formatNumber(game.getNumberOfMoves(), 2) + opponent.boardToString(false));
        		gameStage = (this.playerInfo.getShipsLeft() == 0) ? BattleShipServer.THEYWON : (opponent.playerInfo.getShipsLeft() == 0) ? BattleShipServer.YOUWON : BattleShipServer.THEIRTURN;  
        	}
        	else 
        	{
        		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + opponent.boardToString(true));
        	}
        }
        public void abort(String message)
        {
        	game.getServer().getLog().error("Aborting. Message: ");
        	gameStage = BattleShipServer.GAMEOVER;
        }
        /**
         * The run method of this Player's thread.
         */
        public void run() 
        {
        	// this method is launched after all players are connected 
       		while (gameStage < BattleShipServer.GAMEOVER) 
       		{
       	    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.run");
            	processTurn(getClientResponse());
       		}	
        	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread ended peacefully");
        }
        public String getClientResponse()
        {
        	try
        	{
        		String temp = input.readLine();
        		game.server.getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.clientResponse: +" + temp + "+");
        		if (gameStage == Integer.parseInt(temp.substring(0, 1)))
        		{
        			return temp.substring(1);
        		}
        		else
        		{
                	abort("BattleShipServer.Player: gameStage should be " + gameStage + ", instead received " + temp);
        		}
        	}
            catch (Exception e)
            {
            	abort("BattleShipServer.Player: ERROR getting client response: " + e);
            }
        	return "";
        }
        public void sendToClient(String s)			
        {
        	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " PlayerThread.sendtoClient: " + s);
            output.println(s);
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
            			sb.append("" + playerInfo.getBoard()[i][j].getStatus() + playerInfo.getBoard()[i][j].getContents());
            		}
            		else
            		{	
            			sb.append("" + playerInfo.getBoard()[i][j].getStatus());
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
        	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " PlayerThread.setOpponent to " + opponent.playerInfo.getName());
            if (gameStage == BattleShipServer.WAITING)
            {
                this.opponent = opponent;
                sendToClient("" + gameStage + opponent.toString());
                gameStage = BattleShipServer.PLACING;
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
        			if (!ships[i].place(playerInfo.getBoard(), row, col, hor))
        			{
        				return false;
        			}
        		}
        	}
        	catch(Exception e)
        	{
            	abort("PlayerThread.placeShips for string " + s + ": " + e);
        	}
        	return true;
        }
        public void processShots(String s)
        {
        	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.processShots from " + s);
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
            	game.getServer().getLog().error("Game " + game.getID() + " Player " + playerInfo.getId() + " PlayerThread.processShots from " + s + ": " + e);
        	}
        }
        public boolean applyShot(int r, int c)
        {
        	if (opponent.playerInfo.getBoard()[r][c].getStatus() == BattleShipServer.UNTESTED)
        	{
        		int cc = opponent.playerInfo.getBoard()[r][c].getContents();
        		if (cc == 0)			// nothing there
        		{
        			opponent.playerInfo.getBoard()[r][c].setStatus(BattleShipServer.MISS);
        			return false;
        		}
        		else
        		{
        			opponent.playerInfo.getBoard()[r][c].setStatus(BattleShipServer.HIT);
        			return true;
        		}
        	}
        	else
        	{
            	abort("Game " + game.getID() + " Player " + playerInfo.getId() + " apply shot " + r + ", " + c + ", status is not UNTESTED");
        	}
        	return false;
        }
        public int getGameStage()
    	{
    		return gameStage;
    	}
    	public void setGameStage(int gameStage)
    	{
    		this.gameStage = gameStage;
    	}
    	public String toString()
        {
        	return "" + playerInfo.getId() + playerInfo.getShipsLeft() + playerInfo.getName();
        }
    }
}		
