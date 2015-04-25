package battleship;

import org.ToMar.Utils.tmLog;

public class OceanSquare 
{
	protected tmLog log = new tmLog(tmLog.TRACE);
	protected int row;
	protected int column;
	protected int status = BattleShipServer.UNTESTED;
	protected int contents = 0;		// will hold boatID if one is there
	
	public OceanSquare(int row, int col)
	{
		this.row = row;
		this.column = col;
	}
	public Shot shootNorth(OceanSquare[][] grid)
	{
		// if there is an untested square directly north of this square, return a shot at it
		if (row > 0)
		{
			if (BattleShipServer.UNTESTED == grid[row - 1][column].getStatus())
			{
				return new Shot(row - 1, column);
			}
		}
		return null;
	}
	public Shot shootSouth(OceanSquare[][] grid)
	{
		// if there is an untested square directly south of this square, return a shot at it
		if (row < BattleShipServer.SIZE - 1)
		{
			if (BattleShipServer.UNTESTED == grid[row + 1][column].getStatus())
			{
				return new Shot(row + 1, column);
			}
		}
		return null;
	}
	public Shot shootEast(OceanSquare[][] grid)
	{
		// if there is an untested square directly east of this square, return a shot at it
		if (column < BattleShipServer.SIZE - 1)
		{
			if (BattleShipServer.UNTESTED == grid[row][column + 1].getStatus())
			{
				return new Shot(row, column + 1);
			}
		}
		return null;
	}
	public Shot shootWest(OceanSquare[][] grid)
	{
		// if there is an untested square directly west of this square, return a shot at it
		if (column > 0)
		{
			if (BattleShipServer.UNTESTED == grid[row][column - 1].getStatus())
			{
				return new Shot(row, column - 1);
			}
		}
		return null;
	}
	public String toString()
	{
		return " Square " + row + ", " + column + ": " + contents;
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
