package battleship;

public class Player
{
	private int id;
	private String name;
	private int shipsLeft = 5;
	private int moves = 0;
	boolean human = false;
	
	public Player()
	{
	}
	public boolean isHuman()
	{
		return human;
	}
	public void setHuman(boolean human)
	{
		this.human = human;
	}
	public int getMoves()
	{
		return moves;
	}
	public void setMoves(int moves)
	{
		this.moves = moves;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public int getShipsLeft()
	{
		return shipsLeft;
	}
	public void setShipsLeft(int ships)
	{
		this.shipsLeft = ships;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
}
