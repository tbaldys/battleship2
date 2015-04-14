package battleship;

public class Shot
{
	int row;
	int column;
	public Shot(int r, int c)
	{
		this.row = r;
		this.column = c;
	}
	public String toString()
	{
		return "Row " + row + ", Column " + column;
	}
	public int getRow()
	{
		return row;
	}
	public void setRow(int r)
	{
		this.row = r;
	}
	public int getColumn()
	{
		return column;
	}
	public void setColumn(int c)
	{
		this.column = c;
	}
}	
