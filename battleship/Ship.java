package battleship;

import java.util.ArrayList;

public class Ship extends ArrayList<OceanSquare>
{
	/**
	 * an ArrayList of OceanSquares, used by both BattleShipServer and BattleShipClient 
	 */
	private static final long serialVersionUID = -6950031419767134919L;
	private int id;
	private boolean horizontal= false;
	private int startRow = 0;
	private int startCol = 0;
	private int length = 0;
	
	public Ship(int idx)
	{
		super(BattleShipServer.SHIPLENGTHS[idx]);
		length = BattleShipServer.SHIPLENGTHS[idx];
		this.id = idx + 1;
	}
	public String toString()
	{
		return "" + startRow + startCol + ((horizontal) ? 1 : 0);
	}
	public boolean isStillAlive()
	{
		// if any square in the ship is not a hit, the ship is still alive
		for (int i = 0; i < this.size(); i++)
		{
			if (this.get(i).getStatus() != BattleShipServer.HIT)
			{
				return true;
			}
		}
		return false;
	}
    public boolean place(OceanSquare[][] board, int row, int col, boolean horizontal)
    {
    	// first look to see if you need to erase where it already is -- client version only
    	for (int i = 0; i < this.size(); i++)
    	{
   			this.get(i).setContents(BattleShipServer.UNTESTED);
    	}
    	// if there's anything in any of the places, return false, but place others
    	this.clear();
    	boolean safe = true;
    	this.setStartRow(row);
    	this.setStartCol(col);
    	this.setHorizontal(horizontal);
       	for (int i = 0; i < length; i++)
       	{
       		if (row >= BattleShipServer.SIZE || col >= BattleShipServer.SIZE)
       		{
       			safe = false;
       		}
       		else if (board[row][col].getContents() == BattleShipServer.UNTESTED)
       		{	
       			board[row][col].setContents(this.getId());
       			this.add(board[row][col]);
       		}	
       		else
       		{
       			safe = false;
       		}
  			if (horizontal)
   			{
  				col += 1;
   			}
  			else
       		{
  				row += 1;
       		}
    	}
    	return safe;
    }
	public OceanSquare getSquare(int idx)
	{
		return this.get(idx);
	}
	public void setSquare(OceanSquare s, int idx)
	{
		this.set(idx, s);
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
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
	public int getLength()
	{
		return length;
	}
	public void setLength(int length)
	{
		this.length = length;
	}
}
