package battleship;

import java.util.ArrayList;

import org.ToMar.Utils.Functions;

public class AutoPlayer extends PlayerThread
{
	OceanSquare[][] grid = new OceanSquare[BattleShipServer.SIZE][BattleShipServer.SIZE];
	ArrayList<Shot> hits = new ArrayList<>();

	public AutoPlayer(BattleShipGame game, int playerID)
	{
		super(game, playerID);
		this.playerInfo.setName("Silly");
        gameStage = BattleShipServer.WAITING;
        //initialize grid for player use
		for (int i = 0; i < BattleShipServer.SIZE; i++)
		{
    		for (int j = 0; j < BattleShipServer.SIZE; j++)
    		{
    			grid[i][j] = new OceanSquare(i, j); 
    		}
		}
	}
	public void resetGrid()
	{
		for (int i = 0; i < BattleShipServer.SIZE; i++)
		{
    		for (int j = 0; j < BattleShipServer.SIZE; j++)
    		{
    			grid[i][j].setStatus(BattleShipServer.UNTESTED); 
    		}
		}
	}
	public String getClientResponse()
	{
		StringBuilder sb = new StringBuilder("");
		if (BattleShipServer.PLACING == this.getGameStage())
		{
			sb.append(placeShips());
		}
		else if (BattleShipServer.YOURANSWER == this.getGameStage())
		{
			sb.append(shoot());
		}
		else if (this.getGameStage() > BattleShipServer.READY)
		{
			sb.append(BattleShipServer.OK);
		}
    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " AutoPlayer.getPlayerResponse: " + sb);
		return sb.toString();
	}
    public void sendToClient(String s)			//overridden in HumanPlayer
    {
    	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " AutoPlayer.sendtoClient: " + s);
    	gameStage = Integer.parseInt(s.substring(0, 1));
    	if (BattleShipServer.YOURTURN == gameStage)
    	{
    		//refresh grid and hits with what the server sent
    		int pointer = 3;				// where the boardToString from the opponent starts
    		hits.clear();
    		for (int i = 0; i < BattleShipServer.SIZE; i++)
    		{
        		for (int j = 0; j < BattleShipServer.SIZE; j++)
        		{
        			int stat = Integer.parseInt(s.substring(pointer, ++pointer));
        			grid[i][j].setStatus(stat);
        			if (stat == BattleShipServer.HIT)
        			{
        				hits.add(new Shot(i, j));
        			}
        		}
    		}
        	game.getServer().getLog().debug("Game " + game.getID() + " Player " + playerInfo.getId() + " Stage " + this.getGameStage() + " AutoPlayer.sendtoClient number of hits is " + hits.size());
    	}
    }
	
	public String shoot()
	{
		/*
		 * this method should return a string of however many shots you're allowed to have
		 * you're operating on a 10 x 10 grid of integers representing the status of each square
		 * 0 is UNTESTED
		 * 2 is HIT
		 * 3 is MISS
		 * AutoPlayer shoots N, S, E, and W of every element of hits, then randomly
		 * This method should be overridden if AutoPlayer is extended
		 */
		StringBuilder sb = new StringBuilder("" + playerInfo.getShipsLeft());
		int sh = 0;
		while (sh < playerInfo.getShipsLeft())			// you get as many shots as you have ships left
		{
			// in each execution of this loop, one shot will be created
			// for each element of hits, if N, S, E, W don't produce a shot, go random
			Shot s = processHits();
			if (s != null)
			{
				sb.append("" + s.getRow() + s.getColumn());
				grid[s.getRow()][s.getColumn()].setStatus(BattleShipServer.AIMED);
				sh += 1;
			}
			else
			{	
				int r = Functions.getRnd(BattleShipServer.SIZE);
				int c = Functions.getRnd(BattleShipServer.SIZE);
				if (grid[r][c].getStatus() == BattleShipServer.UNTESTED)
				{
					sb.append("" + r + c);
					grid[r][c].setStatus(BattleShipServer.AIMED);
					sh += 1;
				}
			}	
		}
		return sb.toString();
	}
	public Shot processHits()
	{
		Shot s = null;
		for (int i = 0; i < hits.size(); i++)
		{
			int r = hits.get(i).getRow();
			int c = hits.get(i).getColumn();
			if ((grid[r][c].shootNorth(grid)) != null)
			{
				return grid[r][c].shootNorth(grid);
			}	
			else if ((grid[r][c].shootSouth(grid)) != null)
			{
				return grid[r][c].shootSouth(grid);
			}	
			else if ((grid[r][c].shootEast(grid)) != null)
			{
				return grid[r][c].shootEast(grid);
			}			
			else if ((grid[r][c].shootWest(grid)) != null)
			{
				return grid[r][c].shootWest(grid);
			}	
		}
		return s;
	}
	public String placeShips()
	{
		/*
		 * This method uses brute force to place ships randomly -- just keeps doing it until it works, no strategy
		 * This method can be overridden when extending AutoPlayer
		 */
		StringBuilder sb = new StringBuilder();
		Ship[] ships = new Ship[BattleShipServer.TOTALSHIPS];
		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
		{
			ships[i] = new Ship(i);
			boolean safe = false;
			do
			{
				int r = Functions.getRnd(BattleShipServer.SIZE);
				int c = Functions.getRnd(BattleShipServer.SIZE);
				boolean hor = (Functions.getRnd(2) == 0) ? false : true;
				safe = ships[i].place(grid, r, c, hor);
			} while (!safe);
		}
		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
		{
			sb.append(ships[i].toString());
		}
		return sb.toString();
	}
}
