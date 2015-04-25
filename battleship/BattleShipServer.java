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
	
 	public BattleShipServer(boolean human)
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
                BattleShipGame game = new BattleShipGame(this, gameCounter, human);
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
    	BattleShipServer battleShipServer = new BattleShipServer(false);
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
	
	public BattleShipGame(BattleShipServer server, int gameNumber, boolean human)
    {
    	this.server = server;
    	gameID = gameNumber;
    	active = true;
    	server.getLog().debug("BattleShipGame: constructing game " + gameID + " on server " + server + ", human is " + human);
    	try
    	{
   			players[0] = new HumanPlayer(server.getListener().accept(), this, 0);
   			players[1] = (human) ? new HumanPlayer(server.getListener().accept(), this, 1) : new AutoPlayer(this, 1);
    	}
    	catch (Exception e)
    	{
    		server.getLog().error("BattleShipServer.BattleShipGame constructor: " + e);
    	}
    	players[0].setOpponent((PlayerThread) players[1]);
    	players[1].setOpponent((PlayerThread) players[0]);
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
    class HumanPlayer extends PlayerThread 
    {
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public HumanPlayer(Socket socket, BattleShipGame game, int playerID)  
        {
        	super(game, playerID);
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
        }
        public void sendToClient(String s)
        {
    		server.getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " HumanPlayer.sendToClient: +" + s + "+");
            output.println(s);
        }
        public String getClientResponse()
        {
        	try
        	{
        		String temp = input.readLine();
        		server.getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " HumanPlayer.clientResponse: +" + temp + "+");
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
    }    
}		
