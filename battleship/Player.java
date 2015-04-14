package battleship;

public class Player
{
	private int id;
	private String name;
	private int shipsLeft = 5;
	
	public Player()
	{
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
