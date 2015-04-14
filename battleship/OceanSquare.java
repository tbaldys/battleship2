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
