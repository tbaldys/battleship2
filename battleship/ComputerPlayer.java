package battleship;

import java.util.ArrayList;
import org.ToMar.Utils.Functions;

public class ComputerPlayer extends Player
{
	ArrayList<Shot> hits = new ArrayList<>();

	public ComputerPlayer()
	{
   		for (int i = 0; i < BattleShipServer.SIZE; i++)
   		{
   			for (int j = 0; j < BattleShipServer.SIZE; j++)
   			{
   				board[i][j] = new OceanSquare(i, j);
   			}
   		}
   		this.setName("Silly");
	}	
	public void populateBoard(String bStr)
	{
		// this will fill the hits arraylist<Shot> with any hits on the board
    	int ctr = 0;
    	hits.clear();
   		for (int i = 0; i < BattleShipServer.SIZE; i++)
   		{
   			for (int j = 0; j < BattleShipServer.SIZE; j++)
   			{
				int stat = Integer.parseInt(bStr.substring(ctr, ctr + 1));
				board[i][j].setStatus(stat);
        		if (stat == BattleShipServer.HIT)
        		{
        			hits.add(new Shot(i, j));
        		}
   				ctr += 1;
	   		}
	    }
	}
    // these methods are for computer players
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
		StringBuilder sb = new StringBuilder("");
		int sh = 0;
		while (sh < this.getShipsLeft())			// you get as many shots as you have ships left
		{
			// in each execution of this loop, one shot will be created
			// for each element of hits, if N, S, E, W don't produce a shot, go random
			Shot s = processHits();
			if (s != null)
			{
				sb.append("" + s.getRow() + s.getColumn());
				board[s.getRow()][s.getColumn()].setStatus(BattleShipServer.AIMED);
				sh += 1;
			}
			else
			{	
				int r = Functions.getRnd(BattleShipServer.SIZE);
				int c = Functions.getRnd(BattleShipServer.SIZE);
				if (board[r][c].getStatus() == BattleShipServer.UNTESTED)
				{
					sb.append("" + r + c);
					board[r][c].setStatus(BattleShipServer.AIMED);
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
			if ((board[r][c].shootNorth(board)) != null)
			{
				return board[r][c].shootNorth(board);
			}	
			else if ((board[r][c].shootSouth(board)) != null)
			{
				return board[r][c].shootSouth(board);
			}	
			else if ((board[r][c].shootEast(board)) != null)
			{
				return board[r][c].shootEast(board);
			}			
			else if ((board[r][c].shootWest(board)) != null)
			{
				return board[r][c].shootWest(board);
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
				safe = ships[i].place(board, r, c, hor);
			} while (!safe);
		}
		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
		{
			sb.append(ships[i].toString());
		}
		return sb.toString();
	}
}
