package battleship;

public class PlayerThread extends Thread
{
	Player playerInfo = new Player();
	int gameStage = 0;
	private OceanSquare[][] board = new OceanSquare[BattleShipServer.SIZE][BattleShipServer.SIZE];
	Ship[] ships = new Ship[BattleShipServer.TOTALSHIPS];
	BattleShipGame game;
    PlayerThread opponent;
    
    public PlayerThread(BattleShipGame game, int playerID)
    {
        this.game = game;
        this.playerInfo.setId(playerID);
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
    public void processTurn(String resp)
    {
    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.processTurn, gameStage is " + gameStage + " Response is " + resp);
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
    				if (idleCounter == 1000000000)
    				{
    			    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.processTurn, waiting");
    					idleCounter = 0;
    				}
    			}
        		sendToClient("" + BattleShipServer.READY + firstPlayer);
    			gameStage = (playerInfo.getId() == firstPlayer) ? BattleShipServer.YOURTURN : BattleShipServer.THEIRTURN;			// in future, set to PLACING
    		}
    	}
    	else if (opponent.playerInfo.getShipsLeft() == 0)
    	{
    		sendToClient("" + BattleShipServer.YOUWON + boardToString(true));
    		abort("You won - game over");
    	}
    	else if (this.playerInfo.getShipsLeft() == 0)
    	{
    		sendToClient("" + BattleShipServer.THEYWON + opponent.boardToString(true));
    		abort("You lost - game over");
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
        		sendToClient("" + gameStage + opponent.playerInfo.getShipsLeft() + playerInfo.getShipsLeft() + boardToString(true));
        		gameStage = (playerInfo.getShipsLeft() == 0) ? BattleShipServer.THEYWON : BattleShipServer.YOURTURN;
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
            	 */
        		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + opponent.boardToString(false));
        		gameStage = BattleShipServer.YOURANSWER;
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
//        	game.getServer().getLog().debug("shipsLeft is " + shipsLeft + ", opp is " + opponent.shipsLeft);
    		sendToClient("" + gameStage + playerInfo.getShipsLeft() + opponent.playerInfo.getShipsLeft() + opponent.boardToString(false));
    		gameStage = (opponent.playerInfo.getShipsLeft() == 0) ? BattleShipServer.YOUWON : BattleShipServer.THEIRTURN;
    	}
    	else
    	{
    		abort("Game already over.");
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
    	// this method is launched after all players are connected -- is overridden in HumanPlayer
   		while (gameStage < BattleShipServer.GAMEOVER) 
   		{
   	    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread.run");
        	processTurn(getClientResponse());
   		}	
    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " PlayerThread ended peacefully");
    }
    public String getClientResponse()
    {
    	// this method must be overridden in the AutoPlayer extension
    	return "";
    }
    public void sendToClient(String s)			//overridden in HumanPlayer
    {
    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " PlayerThread.sendtoClient: " + s);
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
    			if (!ships[i].place(board, row, col, hor))
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
