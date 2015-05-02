package battleship;

public class Player
{
	protected int id;
	protected String name;
	protected int shipsLeft = 5;
	protected OceanSquare[][] board = new OceanSquare[BattleShipServer.SIZE][BattleShipServer.SIZE];
	
	public Player()
	{
	}
	public OceanSquare[][] getBoard()
	{
		return board;
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
	public String placeShips()
	{
		// this is a stub, overridden in ComputerPlayer
		return BattleShipServer.OK;
	}
	public String shoot()
	{
		// this is a stub, overridden in ComputerPlayer
		return BattleShipServer.OK;
	}
	public void populateBoard(String s)
	{
		// this is a stub, overridden in ComputerPlayer
	}
}
