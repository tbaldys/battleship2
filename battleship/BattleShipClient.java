package battleship;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.ToMar.Utils.tmColors;
import org.ToMar.Utils.tmFonts;
import org.ToMar.Utils.tmLog;

public class BattleShipClient 
{
	int gameStage = 0;
	Player you;
	Player opp;
    private Shot[] shots = new Shot[BattleShipServer.TOTALSHIPS];
	private tmLog log = new tmLog(tmLog.TRACE);
    private int shotCounter = 0;
    private int currentShip = -1;
    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
	private JFrame frame = new JFrame("BattleShip");
	private JPanel mainPanel = new JPanel();    // holds turnPanel and boardPanel
	private JPanel bottom = new JPanel();		// holds youPanel and opponentPanel	
    private JPanel turnPanel = new JPanel();	// holds actionPanel, shipPanel, messageLabel, and bottom
    private JPanel actionPanel = new JPanel();
    private JPanel shipPanel = new JPanel();
    private JPanel actionP1 = new JPanel();  // tells you whose board is displayed
    private JPanel actionP2 = new JPanel();
    private bLabel p1Label = new bLabel(actionP1, "");
    private bLabel p2Label = new bLabel(actionP2, "Board");
    private bButton actionButton = new bButton(actionPanel, "");
    private ShipButton[] shipButtons = new ShipButton[BattleShipServer.TOTALSHIPS];
    private HorizButton horizButton;
    private bLabel messageLabel = new bLabel(turnPanel, "test message");
    private JPanel boardPanel = new JPanel();		//displays active board
    private ClientSquare[][] board = new ClientSquare[10][10];
    private JPanel youPanel = new JPanel();
    private bLabel youLabel = new bLabel(youPanel, "");
    private bLabel youShotText = new bLabel(youPanel, "Shots: ");
    private bLabel youShotLabel = new bLabel(youPanel, "");
    private JPanel oppPanel = new JPanel();
    private bLabel oppLabel = new bLabel(oppPanel, "");
    private bLabel oppShotText = new bLabel(oppPanel, "Shots: ");
    private bLabel oppShotLabel = new bLabel(oppPanel, "");
    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public BattleShipClient() throws Exception 
    {
        you = new Player();
        opp = new Player();
        // Set up networking
        try
        {
            socket = new Socket(getServerAddress(), PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            // stage is CONNECTING, read from server to get your own ID
           	you.setId(Integer.parseInt(getServerResponse()));
           	// respond with your player name
            you.setName(getPlayerName());
            frame.setTitle("BattleShip: " + you.getName());
            sendToServer("" + gameStage + you.getName());
            // set stage to WAITING until you get the opponent's information
            gameStage = BattleShipServer.WAITING;
        }
        catch (Exception e)
        {
            log.error(you.getName() + ": ERROR in BattleShipClient: " + e);
        	throw new Exception(e);
        }
        mainPanel.setBackground(tmColors.BLACK);
        boardPanel.setLayout(new GridLayout(10, 10, 1, 1));
        for (int i = 0; i < BattleShipServer.SIZE; i++)
        {
        	for (int j = 0; j < BattleShipServer.SIZE; j++)
        	{
        		final int I = i;
        		final int J = j;
        		board[i][j] = new ClientSquare(i, j);
        		board[i][j].panel.addMouseListener(new MouseAdapter() 
                {
                    public void mousePressed(MouseEvent e) 
                    {
                    	processSquare(board[I][J]); 
                    }    
                });
        		boardPanel.add(board[i][j].panel);
        	}	
        }
        turnPanel.setBackground(tmColors.LIGHTBLUE);
        actionPanel.setBackground(tmColors.LIGHTBLUE);
        bottom.setBackground(tmColors.BLACK);
        youPanel.setBackground(tmColors.LIGHTGREEN);
        oppPanel.setBackground(tmColors.LIGHTCYAN);
        actionP1.setBackground(tmColors.LIGHTBLUE);
        actionP2.setBackground(tmColors.LIGHTBLUE);
        shipPanel.setBackground(tmColors.LIGHTBLUE);
        shipPanel.setLayout(new GridLayout(1, 6, 15, 15));
        for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
        {
        	final int I = i;
        	shipButtons[i] = new ShipButton(shipPanel, i);
            shipButtons[i].addMouseListener(new MouseAdapter()
            {
                public void mousePressed(MouseEvent e) 
                {
                	if (shipButtons[I].isEnabled())
                	{	
                		if (shipButtons[I].isSelected())
                		{
                			shipButtons[I].setSelected(false);
                			currentShip = -1;
                			messageLabel.setText("Select a ship to place.");
                		}
                		else 
                		{
                			if (currentShip > -1)
                			{
                				shipButtons[currentShip].setSelected(false);
                			}
                			shipButtons[I].setSelected(true);
                			currentShip = I;
                			messageLabel.setText("Placing ship " + (I + 1) + ".");
                		}
                	}	
                }	
            });
        }
        horizButton = new HorizButton(shipPanel, "H");
        horizButton.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e) 
            {
            	if (horizButton.isEnabled())
            	{
            		if (flipCurrentShip())
            		{
            			messageLabel.setText("");
            		}
            		else
            		{
            			messageLabel.setText("Ship " + (currentShip + 1) + " is not safe.");
            		}
            	}	
            }	
        });
        messageLabel.setText("Waiting for an opponent to sign on.");
        // you will communicate your name/ship data, opponent's will be returned
        youLabel.setText(you.getName());
        // Layout GUI
        frame.getContentPane().add(mainPanel);
        mainPanel.setLayout(new GridLayout(1, 2, 10, 10));
        mainPanel.add(turnPanel);
        turnPanel.setLayout(new GridLayout(4, 1, 10, 10));
        actionPanel.setLayout(new GridLayout(1, 3, 10, 10));
        actionPanel.setBackground(tmColors.LIGHTBLUE);
        actionButton.setFont(tmFonts.PLAIN32);
        actionButton.setBackground(tmColors.RED);
        actionButton.addMouseListener(new MouseAdapter() 
        {
            public void mousePressed(MouseEvent e) 
            {
            	/*
            	 * actionButton gets pressed in the following:
            	 * 1. When done placing boats (PLACING)
            	 * 2. When done placing shots (YOURTURN)
            	 * 3. When done looking at the answer, to end your turn (YOURANSWER)
            	 * 4. After you've seen the answer of your opponent's turn, to begin yourturn (THEIRANSWER)
            	 */
            	if (actionButton.isEnabled())
            	{
            		if (gameStage == BattleShipServer.PLACING)
            		{
	            		StringBuilder sb = new StringBuilder("" + gameStage);
	            		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
	            		{
	            			sb.append(shipButtons[i].ship.toString());
	            			shipButtons[i].deActivate();
	            		}
	            		sendToServer(sb.toString());
	            		actionButton.deActivate();
	            		horizButton.deActivate();
	            		messageLabel.setText("Waiting for " + opp.getName() + ".");
	            		gameStage = BattleShipServer.READY;
            		}
            		else if (gameStage == BattleShipServer.YOURANSWER)
	            	{
	            		gameStage = BattleShipServer.THEIRTURN;
	            		sendToServer("" + gameStage + BattleShipServer.OK);
	            		actionButton.deActivate();
	            	}
	            	else if (gameStage == BattleShipServer.YOURTURN && shotCounter == you.getShipsLeft())
	            	{
	            		gameStage = BattleShipServer.YOURANSWER;
	            		StringBuilder sb = new StringBuilder("" + gameStage + you.getShipsLeft());
	            		for (int i = 0; i < you.getShipsLeft(); i++)
	            		{
	            			sb.append("" + shots[i].getRow() + shots[i].getColumn());
	            		}
	            		sendToServer(sb.toString());
	            		messageLabel.setText("Press END to end your turn.");
	            		actionButton.activate("END");
	            	}
	            	else if (gameStage == BattleShipServer.THEIRANSWER)
	            	{
	            		gameStage = BattleShipServer.YOURTURN;
	            		sendToServer("" + gameStage + BattleShipServer.OK);
	            	}
	            	else if (gameStage > BattleShipServer.THEIRANSWER)
	            	{
	            		sendToServer("" + gameStage + BattleShipServer.OK);
	            	}
            	}	
            }    
        });
        actionPanel.add(actionP1);
        actionPanel.add(actionP2);
        turnPanel.add(actionPanel);
        turnPanel.add(shipPanel);
        turnPanel.add(messageLabel);      
        bottom.setLayout(new GridLayout(1, 2, 10, 10));
        bottom.add(youPanel);
        bottom.add(oppPanel);
        turnPanel.add(bottom);
        mainPanel.add(boardPanel);
    }
    public void sendToServer(String s)
    {
		log.debug(you.getName() + ": Sending to Server: +" + s + "+");
        out.println(s);
    }
    public String getServerResponse() throws Exception
    {
    	try
    	{
    		String temp = in.readLine();
    		log.debug(you.getName() + ": Server responded: +" + temp + "+");
    		if (gameStage == Integer.parseInt(temp.substring(0, 1)))
    		{
    			return temp.substring(1);
    		}
    		else
    		{
    			throw new Exception("BattleShipClient.getServerResponse: gameStage not correct.");
    		}
    	}
        catch (Exception e)
        {
        	log.error(you.getName() + ": ERROR getting server response: " + e);
        	throw new Exception(e);
        }
    }
    public void play() throws Exception 
    {
    	try
    	{
    		while (true)
    		{
    			String temp = getServerResponse();
	    		if (gameStage == BattleShipServer.WAITING)
	    		{
	    			opp.setId(Integer.parseInt(temp.substring(0, 1)));
	    			opp.setName(temp.substring(2));
	    			oppLabel.setText(opp.getName());
	    			gameStage = BattleShipServer.PLACING;
	    			currentShip = -1;
	    			messageLabel.setText("Select a ship to place.");
	    			while (gameStage == BattleShipServer.PLACING)
	    			{
	    				// this code does nothing, but seems to need to be here
	    				int counter = 1;
	    			}
	    		}
	    		else if (gameStage == BattleShipServer.READY)
	    		{
	    			int firstPlayer = Integer.parseInt(temp.substring(0, 1));
	    			gameStage = (firstPlayer == you.getId()) ? BattleShipServer.YOURTURN : BattleShipServer.THEIRTURN;
	    			sendToServer("" + gameStage + BattleShipServer.OK);
	    		}
	    		else
	    		{	
	    			Player cur = null;
	    			Player other = null;
	    			if (gameStage > BattleShipServer.YOURANSWER && gameStage < BattleShipServer.YOUWON)
	    			{
	    				//IT IS NOT YOUR TURN
	    				cur = opp;
	    				other = you;
	    				if (gameStage == BattleShipServer.THEIRTURN)
	    				{
	        	    		messageLabel.setText(cur.getName() + " is shooting.");
	    	    			actionButton.deActivate();
	    					gameStage = BattleShipServer.THEIRANSWER;
	    					sendToServer("" + gameStage + BattleShipServer.OK);
	    				}
	    				else
	    				{
	    					messageLabel.setText("Press GO for your turn.");
	    					actionButton.activate("GO");
	    				}
	    			}
	    			else if (gameStage < BattleShipServer.THEIRTURN)
	    			{
	    				cur = you;
	    				other = opp;
	    			}	
	    			//column 0		current player's ships
	    			//column 1		other player's ships
	    			cur.setShipsLeft(Integer.parseInt(temp.substring(0, 1)));
	    			other.setShipsLeft(Integer.parseInt(temp.substring(1, 2)));
	    			youShotLabel.setText("" + you.getShipsLeft());
	    			oppShotLabel.setText("" + opp.getShipsLeft());
	    			p1Label.setText(other.getName() + "'s");
	    			displayBoard(temp.substring(2));
	    			if (you.getShipsLeft() == 0 || opp.getShipsLeft() == 0)
	    			{	
	    				if (you.getShipsLeft() == 0)
	    				{
	    					messageLabel.setText(opp.getName() + " has won!");
	    					gameStage = BattleShipServer.THEYWON;
	    				}
	    				else if (opp.getShipsLeft() == 0)
	    				{
	    					messageLabel.setText(you.getName() + " has won!");
	    					gameStage = BattleShipServer.YOUWON;
	    				}
	    				actionButton.activate("AGAIN");
	    			}
	    			else
	    			{	
		    			shotCounter = 0;
		    			while (gameStage == BattleShipServer.YOURTURN)
		    			{
		    				if (shotCounter == you.getShipsLeft())
		    				{
		    					actionButton.activate("FIRE");
		    					messageLabel.setText("All shots are set. Fire when ready.");
		    				}
		    				else
		    				{
		    					actionButton.deActivate();
		    					messageLabel.setText(you.getName() + " has set " + shotCounter + " of " + you.getShipsLeft() + " shots.");
		    				}
		    			}
	    			}	
	    		}
    		}	
        }
        catch (Exception e)
        {
        	throw new Exception("BattleShipClient.play: " + e);
        }
        finally 
        {
        	log.error(you.getName() + ": Closing socket.");
            socket.close();
        }
    }
    private boolean flipCurrentShip()
    {
    	if (currentShip < 0)
    	{
    		return false;
    	}
    	if ("V".equalsIgnoreCase(horizButton.getText()))
    	{
    		// flip ship if you can 
    		horizButton.setText("H");
    		shipButtons[currentShip].ship.setHorizontal(false);
    	}
    	else
    	{
    		horizButton.setText("V");
    		shipButtons[currentShip].ship.setHorizontal(true);
    	}
		return moveCurrentShip(shipButtons[currentShip], shipButtons[currentShip].ship.getStartRow(),shipButtons[currentShip].ship.getStartCol());
    }
    private void displayBoard(String bStr)
    {
    	int incr = (bStr.length() == 100) ? 1 : 2;
    	int ctr = 0;
   		for (int i = 0; i < BattleShipServer.SIZE; i++)
   		{
   			for (int j = 0; j < BattleShipServer.SIZE; j++)
   			{
   				try
				{
					board[i][j].setStatus(Integer.parseInt(bStr.substring(ctr, ctr + 1)));
	  	    		if (incr == 2)
	   	    		{
	   	   				board[i][j].setContents(Integer.parseInt(bStr.substring(ctr + 1, ctr + 2)));
	   	    		}
	 			}
   				catch (Exception e)
				{
   					log.error(you.getName() + ": ERROR updating row " + i + " col " + j + ": " + e); 
				}
   				ctr += incr;
   			}
   		}
    }
    private String getPlayerName() 
    {
    	String[] myStrings = {"marie", "Silly"};
        return (String) JOptionPane.showInputDialog(frame, "Name: ",
            "Welcome to BattleShip!",
            JOptionPane.QUESTION_MESSAGE, null, myStrings, "marie");
    }
    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() 
    {
    	return "localhost";
//    	String[] myStrings = { "Tom-HP", "MomMobile", "Localhost" };
//        return (String) JOptionPane.showInputDialog(
//            frame,
//            "Enter IP Address of Server:",
//            "Welcome to BattleShip!",
//            JOptionPane.QUESTION_MESSAGE, null, myStrings, "localhost");
    }
    private boolean wantsToPlayAgain() 
    {
//        int response = JOptionPane.showConfirmDialog(frame,
//            "Want to play again?",
//            "Tic Tac Toe is Fun Fun Fun",
//            JOptionPane.YES_NO_OPTION);
//        frame.dispose();
//        return response == JOptionPane.YES_OPTION;
    	return true;	
    }
    public void processSquare(ClientSquare square)
    {
    	//this can only happen if it's your turn and you click on this square
    	if (gameStage == BattleShipServer.YOURTURN)
    	{
    		if (square.getStatus() == BattleShipServer.AIMED)   
    		{	// need to remove this shot from the shot queue
    			ArrayList<Shot> sh = new ArrayList<>();
    			int sCtr = 0;
    			for (int i = 0; i < shotCounter; i++)
    			{
    				if (shots[i].getRow() == square.getRow() & shots[i].getColumn() == square.getColumn())
    				{
            			square.setStatus(BattleShipServer.UNTESTED);
    				}
    				else
    				{
    					sh.add(shots[i]);
    					sCtr += 1;
    				}
    			}
    			if (sCtr != (shotCounter - 1))   // should not happen
    			{
    				log.error("OceanSquare.process: Error 1 processing shot " + square.getRow() + ", " + square.getColumn());
    			}
    			for (shotCounter = 0; shotCounter < sCtr; shotCounter++)
    			{
    				shots[shotCounter] = sh.get(shotCounter);
    			}
    		}
    		else
    		{
    			if (shotCounter <  you.getShipsLeft())
    			{	
        			shots[shotCounter] = new Shot(square.getRow(), square.getColumn());
        			square.setStatus(BattleShipServer.AIMED);
        			shotCounter += 1;
    			}
    		}
    	}
    	else if (gameStage == BattleShipServer.PLACING)
    	{
    		if (currentShip < 0)
    		{
    			messageLabel.setText("Select a ship, then click on the grid.");
    		}
    		else
    		{
    			if (moveCurrentShip(shipButtons[currentShip], square.getRow(), square.getColumn()))
    			{
    				messageLabel.setText("");
    			}
    			else
        		{
        			messageLabel.setText("Ship " + (currentShip + 1) + " is not safe.");
        		}
    		}
    	}
    }
    private boolean moveCurrentShip(ShipButton curShip, int row, int col)
    {
    	boolean safe = false; 
		curShip.setSelected(false); 			// deselect the current squares
		curShip.setSafe(safe = curShip.ship.place(board, row, col, curShip.ship.isHorizontal()));
		curShip.setSelected(true);				// select the new squares
		shotCounter = 0;
		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
		{
			shotCounter += (shipButtons[i].isSafe()) ? 1 : 0;
		}
		if (shotCounter == BattleShipServer.TOTALSHIPS)
		{
			actionButton.activate("GO");
			messageLabel.setText("Press GO to begin play.");
		}
		else
		{
			actionButton.deActivate();
		}
		return safe;
    }
    
    private class bButton extends JButton
    {
    	public bButton(String text)
    	{
    		super(text);
    		this.setFont(tmFonts.PLAIN24);
    		this.deActivate();
    	}
    	public bButton(JPanel panel, String text)
    	{
    		super(text);
    		this.setFont(tmFonts.PLAIN24);
    		panel.add(this);
    	}
    	public void activate(String text)
    	{
    		this.setText(text);
    		this.setEnabled(true);
    		this.setVisible(true);
    	}
    	public void deActivate()
    	{
    		this.setEnabled(false);
    		this.setVisible(false);
    	}
    }
    private class ShipButton extends bButton
    {
    	private Ship ship = null;
    	private boolean selected = false;
    	private boolean safe = false;
    	
    	public ShipButton(JPanel panel, int id)
    	{
    		super(panel, "" + (id + 1));
			ship = new Ship(id);
			this.setSafe(ship.place(board, 1, id + 1, false));
    	}
    	public boolean isSelected()
		{
			return selected;
		}
    	public void deActivate()
    	{
    		this.setSelected(false);
    		super.deActivate();
    	}
		public void setSelected(boolean selected)
		{
			this.selected = selected;
			this.setBackground((selected) ? tmColors.YELLOW : tmColors.PALEYELLOW);
			for (int i = 0; i < ship.size(); i++)
			{
				((ClientSquare) ship.get(i)).setSelected(selected);
			}
			if (selected)
			{
				horizButton.setText((ship.isHorizontal()) ? "V" : "H");
			}
		}
    	public boolean isSafe()
    	{
    		return safe;
    	}
    	public void setSafe(boolean safe)
    	{
    		this.safe = safe;
    	}
    }
    private class HorizButton extends bButton
    {
    	public HorizButton(JPanel panel, String text)
    	{
    		super(panel, text);
    		this.setBackground(tmColors.DARKGREEN);
    		this.setForeground(tmColors.WHITE);
    	}
    }
    private class bLabel extends JLabel
    {
    	public bLabel(String text)
    	{
    		super(text);
    		this.setFont(tmFonts.PLAIN24);
    	}
    	public bLabel(JPanel panel, String text)
    	{
    		super(text);
    		this.setFont(tmFonts.PLAIN24);
    		panel.add(this);
    	}
    }
    private class ClientSquare extends OceanSquare
    {
    	JPanel panel = new JPanel();
        JLabel label = new JLabel("");
        boolean selected = false;

		public ClientSquare(int row, int column)
    	{
    		super(row, column);
           	panel.setBackground(tmColors.CHARTREUSE);
           	label.setForeground(tmColors.WHITE);
           	label.setFont(tmFonts.PLAIN16);
            panel.add(label);
    	}
    	public void setStatus(int status)
    	{
    		this.status = status;
       		panel.setBackground((status < BattleShipServer.HIT) ? tmColors.CHARTREUSE : tmColors.DARKBLUE);
       		label.setText((status == BattleShipServer.AIMED) ? "X" : (status == BattleShipServer.HIT) ? "+" : ""); 
    	}
    	public void setContents(int contents)
    	{
    		this.contents = contents;
        	label.setText((contents == 0) ? "" : "" + contents);
    	}
    	public boolean isSelected()
		{
			return selected;
		}
		public void setSelected(boolean selected)
		{
			this.selected = selected;
    		label.setForeground((selected) ? tmColors.BLUE : tmColors.WHITE);
		}
    }
    /**
     * Runs the client as an application.
     */
    public static void main(String[] args)  
    {
    	try
    	{
	        BattleShipClient client = new BattleShipClient();
	        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        client.frame.setSize(800, 400);
	        client.frame.setVisible(true);
	        client.frame.setResizable(true);
	        while (true) 
	        {
	 //           String serverAddress = (args.length == 0) ? "new-host-5.home" : args[1];
	//        	String serverAddress = (args.length == 0) ? "192.168.1.11" : args[1];
	//            String serverAddress = (args.length == 0) ? "localhost" : args[1];
	            client.play();
//	            if (!client.wantsToPlayAgain()) 
//	            {
//	                break;
//	            }
	        }      
        }
    	catch (Exception e)
    	{
    		System.out.println("Client bailing due to error, ending program. " + e);
    	}
    }
}
