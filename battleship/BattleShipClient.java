package battleship;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.ToMar.Utils.Functions;
import org.ToMar.Utils.tmColors;
import org.ToMar.Utils.tmFonts;
import org.ToMar.Utils.tmLog;

public class BattleShipClient 
{
	int gameStage = 0;
	boolean human = false;
	boolean test = false;				// set to true to test a computer player			
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
    private JPanel actionPanel = new JPanel();  // holds big red button, p1, and p2
    private JPanel shipPanel = new JPanel();
    private JPanel actionP1 = new JPanel();  	// number of moves
    private JPanel actionP2 = new JPanel();  	// whose board it is
    private bLabel p1Label1 = new bLabel("Move");
    private bLabel p1Label2 = new bLabel("");
    private bLabel p2Label1 = new bLabel("");
    private bLabel p2Label2 = new bLabel("Board");
    private bButton actionButton = new bButton(actionPanel, "");
    private ShipButton[] shipButtons = new ShipButton[BattleShipServer.TOTALSHIPS];
    private HorizButton horizButton;
    private bLabel messageLabel = new bLabel(turnPanel, "test message");
    private JPanel boardPanel = new JPanel();		//displays active board
    private ClientSquare[][] displayBoard = new ClientSquare[10][10];
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
    	int n = JOptionPane.showConfirmDialog(frame,
    		    								"Is this player human?",
    		    								"ToMarBattleShipClient",
    		    								JOptionPane.YES_NO_OPTION);
    	human = (n == JOptionPane.NO_OPTION) ? false : true;
    	if (human)
    	{	
    		you = new Player();
    		you.setName((String) JOptionPane.showInputDialog(frame, "Player name: ", "Human Player", JOptionPane.QUESTION_MESSAGE, null, null, null));
    	}
    	else
    	{
    		you = new ComputerPlayer();
    	}
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
        p1Label1.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        p1Label2.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        mainPanel.setBackground(tmColors.BLACK);
        boardPanel.setLayout(new GridLayout(10, 10, 1, 1));
        for (int i = 0; i < BattleShipServer.SIZE; i++)
        {
        	for (int j = 0; j < BattleShipServer.SIZE; j++)
        	{
        		final int I = i;
        		final int J = j;
        		displayBoard[i][j] = new ClientSquare(i, j);
        		if (human)
        		{	
        			displayBoard[i][j].panel.addMouseListener(new MouseAdapter() 
        			{
        				public void mousePressed(MouseEvent e) 
        				{
        					processSquare(displayBoard[I][J]); 
        				}    
        			});
        		}	
        		boardPanel.add(displayBoard[i][j].panel);
        	}	
        }
        turnPanel.setBackground(tmColors.LIGHTBLUE);
        actionPanel.setBackground(tmColors.LIGHTBLUE);
        bottom.setBackground(tmColors.BLACK);
        youPanel.setBackground(tmColors.LIGHTGREEN);
        oppPanel.setBackground(tmColors.LIGHTCYAN);
        actionP1.setBackground(tmColors.LIGHTBLUE);
        actionP2.setBackground(tmColors.LIGHTBLUE);
        actionP1.setLayout(new GridLayout(2, 1, 1, 1));
        actionP1.add(p1Label1);
        actionP1.add(p1Label2);
        actionP2.setLayout(new GridLayout(2, 1, 1, 1));
        actionP2.add(p2Label1);
        actionP2.add(p2Label2);
        shipPanel.setBackground(tmColors.LIGHTBLUE);
        shipPanel.setLayout(new GridLayout(1, 6, 15, 15));
    	if (human)
    	{	
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
    	}	
        messageLabel.setText("Waiting for an opponent to sign on.");
        // you will communicate your name/ship data, opponent's will be returned
        youLabel.setText("Player " + (you.getId() + 1) + ": " + you.getName());
        // Layout GUI
        frame.getContentPane().add(mainPanel);
        mainPanel.setLayout(new GridLayout(1, 2, 10, 10));
        mainPanel.add(turnPanel);
        turnPanel.setLayout(new GridLayout(4, 1, 10, 10));
        actionPanel.setLayout(new GridLayout(1, 3, 10, 10));
        actionPanel.setBackground(tmColors.LIGHTBLUE);
        if (human)
        {
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
	            			// communicates the placement of your ships to the server
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
	            			// signals the end of your turn, you will now see your board being fired upon
		            		gameStage = BattleShipServer.THEIRTURN;
		            		sendToServer("" + gameStage + BattleShipServer.OK);
		            		actionButton.deActivate();
		            	}
		            	else if (gameStage == BattleShipServer.YOURTURN && shotCounter == you.getShipsLeft())
		            	{
		            		// communicates your shots to the server
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
		            		// ready to start your turn, board switches to opponent's for you to fire upon
		            		gameStage = BattleShipServer.YOURTURN;
		            		sendToServer("" + gameStage + BattleShipServer.OK);
		            	}
		            	else if (gameStage > BattleShipServer.THEIRANSWER)
		            	{
		            		// game is over
		            		sendToServer("" + gameStage + BattleShipServer.OK);
		            	}
	            	}	
	            }    
	        });
        }    
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
    public String getServerResponse()
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
    			abort("BattleShipClient.getServerResponse: gameStage not correct.");
    		}
    	}
        catch (Exception e)
        {
        	abort(you.getName() + ": ERROR getting server response: " + e);
        }
    	return "E44OR";
    }
    public void abort(String message)
    {
    	log.error("Aborting. Message: ");
    	gameStage = BattleShipServer.GAMEOVER;
    }
    public void play() throws Exception 
    {
    	try
    	{
/*
 * 	public static final int CONNECTING = 0;		//you connect, server sends id
	public static final int WAITING = 1;		//you send name, server sends opponent
	public static final int PLACING = 2;		//you send ship placement, server sends who goes first
	public static final int READY = 3;			//you say ready
	public static final int YOURTURN = 4;		//you say ready, server sends oppboard
	public static final int YOURANSWER = 5;		//you send shots, server sends updated oppboard
	public static final int THEIRTURN = 6;		//you say ready, server sends yourboard
	public static final int THEIRANSWER = 7;	//you say ready, server sends updated board
	public static final int YOUWON = 8;
	public static final int THEYWON = 9;
	public static final int GAMEOVER = 10;
	Server responded: +00+						// server responds when you connect, stage 0, playerID 0 (or 1)
	Sending to Server: +0marie+					// you respond with your player name (stage 0)
	Server responded: +115Silly+				// when another player connects, server your opponent info
												// stage 1, playerID 1, shipsLeft 5, name Silly
	Sending to Server: +2230841480710151+	    // stage 2, (row, column, horiz) for 5 ships to be placed
	Server responded: +31+						// stage 3, server says playerID 1 goes first
	********* the cycle below repeats until someone wins
	Sending to Server: +4OK+
	Server responded: +4550000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000+
	Sending to Server: +552233445566+
	Server responded: +5550000000000000000000000200000000002000000000030000000000300000000003000000000000000000000000000000000+
	Sending to Server: +6OK+
	Server responded: +65500000000000000000000000102000405000000000001020004050000000000010200040000000000000102000000000000000001000000030303000000000000000000000000000000000000000000000000000000000000000000000000000000000000+
	Sending to Server: +7OK+
	Server responded: +75500000000000000000000000102000405000000000001023004050000000000010200240000000000000102000030000000000001000000032303000000000000000000300000000000000000000000000000000000000000000000000000000000000000+
	Sending to Server: +4OK+
	*****************
 */
    		while (gameStage < BattleShipServer.GAMEOVER)
    		{
    			String temp = getServerResponse();
	    		if (gameStage == BattleShipServer.WAITING)
	    		{
	    			opp.setId(Integer.parseInt(temp.substring(0, 1)));
	    			opp.setName(temp.substring(2));
	    			oppLabel.setText("Player " + (opp.getId() + 1) + ": " + opp.getName());
	    			gameStage = BattleShipServer.PLACING;
/*
 *  Placing ships. The string should be "2" (gameStage == PLACING) + 5 sets of (row, column, direction)
 */
	    			if (human)
	    			{
		    			currentShip = -1;
		    			messageLabel.setText("Select a ship to place.");
		    			actionButton.deActivate();
		    			// this code will spin until the actionButton is clicked, then send to server
		    			int idleCounter = 0;
		    			while (gameStage == BattleShipServer.PLACING)
		    			{
		    				// this code does nothing, but seems to need to be here
		    				idleCounter += 1;
		    				if (idleCounter == 1000000000)
		    				{
		    					log.debug("waiting for ship placement from player " + you.getName());
		    					idleCounter = 0;
		    				}
		    			}
	    			}
	    			else
	    			{
	    				// computer player will call method placeShips for ship placement information and send it
	            		sendToServer("" + gameStage + you.placeShips());				
	            		messageLabel.setText("Waiting for " + opp.getName() + ".");
	            		gameStage = BattleShipServer.READY;
	    			}
	    		}
	    		else if (gameStage == BattleShipServer.READY)
	    		{
	    			int firstPlayer = Integer.parseInt(temp.substring(0, 1));
	    			// unhighlight all the ocean squares
	    	        for (int i = 0; i < BattleShipServer.SIZE; i++)
	    	        {
	    	        	for (int j = 0; j < BattleShipServer.SIZE; j++)
	    	        	{
	    	        		displayBoard[i][j].setSelected(false);
	    	        	}
	    	        }
	    	        // you send back either 4OK if it's your turn or 6OK if it's not
	    			gameStage = (firstPlayer == you.getId()) ? BattleShipServer.YOURTURN : BattleShipServer.THEIRTURN;
	    			sendToServer("" + gameStage + BattleShipServer.OK);
	    		}
	    		else if (gameStage > BattleShipServer.THEIRANSWER)
	    		{
	    			// someone won -- either 8 you won, or 9 they won
	    			// the rest of the message is the board of ????? need to decide this
	    			gameOver(temp);
	    		}
	    		else
	    		{	
	    			// the game is actively being played; stage is 4, 5, 6, or 7
	    			Player cur = null;
	    			Player other = null;
	    			// first set cur and other, and display board
	    			if (gameStage > BattleShipServer.YOURANSWER)
	    			{
	    				//IT IS NOT YOUR TURN
	    				cur = opp;
	    				other = you;
	    			}
	    			else
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
	    			p2Label1.setText(other.getName() + "'s");
	    			p1Label2.setText(temp.substring(2, 4));
	    			displayBoard(temp.substring(4));
	    			if (you.getShipsLeft() == 0)
	    			{
	    				gameStage = BattleShipServer.THEYWON;
    					sendToServer("" + gameStage + BattleShipServer.OK);
	    			}
	    			else if (opp.getShipsLeft() == 0)
	    			{
	    				gameStage = BattleShipServer.YOUWON;
    					sendToServer("" + gameStage + BattleShipServer.OK);
	    			}
	    			else if (gameStage == BattleShipServer.THEIRTURN)
    				{
        	    		messageLabel.setText(cur.getName() + " is shooting.");
    					if (human)
    					{	
    						actionButton.deActivate();
    					}	
    					gameStage = BattleShipServer.THEIRANSWER;
    					sendToServer("" + gameStage + BattleShipServer.OK);
    				}
    				else if (gameStage == BattleShipServer.THEIRANSWER)
	    			{
	    				if (human)
	    				{	
	    					messageLabel.setText("Press GO for your turn.");
	    					actionButton.activate("GO");
	    				}
	    				else
	    				{
			          		// ready to start your turn, board switches to opponent's for you to fire upon
			           		gameStage = BattleShipServer.YOURTURN;
			          		sendToServer("" + gameStage + BattleShipServer.OK);
	    				}
	    			}
	    			else if (gameStage == BattleShipServer.YOURTURN)
	    			{
	    				// human will wait until they've placed all the shots and clicked the actionButton
	    				if (human)
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
	    				else 
	    				{
	    					// this will send the results of your shoot method to the server
	    					// communicates your shots to the server
	    					you.populateBoard(temp.substring(4));
	    					gameStage = BattleShipServer.YOURANSWER;
	    					sendToServer("" + gameStage + you.getShipsLeft() + you.shoot());
	    				}
	    			}	
	    			else
	    			{	 
	    				if (human)
	    				{	
	    					messageLabel.setText("Press END to end your turn.");
	    					actionButton.activate("END");
	    				}
	    				else
	    				{
	    					// count down 10 seconds
	    					if (test)
	    					{	
	    						waitXseconds(10, messageLabel);
	    					}	
	    					// signals the end of your turn, you will now see your board being fired upon
	    					gameStage = BattleShipServer.THEIRTURN;
	    					sendToServer("" + gameStage + BattleShipServer.OK);
	    				}	
	    			}
    			}
    		}
   		}	
        catch (Exception e)
        {
        	throw new Exception("BattleShipClient.play: " + e);
        }
       	log.debug(you.getName() + ": Closing socket and ending.");
        socket.close();
    	throw new Exception("BattleShipClient.play ending game");
    }
    public void gameOver(String display)
    {
    	// need work here -- for now, setting gameStage to GAMEOVER
    	log.debug("Game is over, stage is " + gameStage + " " + display);
		messageLabel.setText(gameStage == BattleShipServer.YOUWON ? you.getName() + " has won!" : opp.getName() + " has won!");
		gameStage = BattleShipServer.GAMEOVER; 
    }
    public void waitXseconds(int x, JLabel m)
    {
    	long startSecs = new Date().getTime();
    	while (true)
    	{	
    		long elapsed = Functions.getElapsedSeconds(startSecs);
	    	if (elapsed > x)
	    	{
	    		break;
	   		}
	   		else
	   		{
	   			m.setText("Results... " + (x - elapsed));
	    	}
    	}
    	return;
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
/*
 * displayBoard gets a string of either 100 or 200 characters 
 * 		if it's your board, you get the contents as well as the status (200 chars)
 * 		if it's the one you're shooting at, you only see the status (100 chars)  
 * this will display it on the board visually, and any hits will be added to the hits arraylist  
 */
    private void displayBoard(String bStr)
    {
    	int incr = (bStr.length() == 100) ? 1 : 2;
    	// if this is a computer player, don't show its ship placement if test is false
    	if (!human && !test && incr == 2)
    	{
    		return;
    	}
    	int ctr = 0;
   		for (int i = 0; i < BattleShipServer.SIZE; i++)
   		{
   			for (int j = 0; j < BattleShipServer.SIZE; j++)
   			{
   				try
				{
   					int stat = Integer.parseInt(bStr.substring(ctr, ctr + 1));
					displayBoard[i][j].setStatus(stat);
	  	    		if (incr == 2)
	   	    		{
	   	   				displayBoard[i][j].setContents(Integer.parseInt(bStr.substring(ctr + 1, ctr + 2)));
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
    	String[] myStrings = {"marie", "Tom", "Silly"};
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
    		else if (square.getStatus() == BattleShipServer.UNTESTED)
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
		curShip.setSafe(safe = curShip.ship.place(displayBoard, row, col, curShip.ship.isHorizontal()));
		curShip.setSelected(true);				// select the new squares
		countShips();
		return safe;
    }
    public void countShips()
    {
		int sCounter = 0;
		for (int i = 0; i < BattleShipServer.TOTALSHIPS; i++)
		{
			sCounter += (shipButtons[i].isSafe()) ? 1 : 0;
		}
//		log.debug("Ship count is " + shotCounter);
		if (sCounter == BattleShipServer.TOTALSHIPS)
		{
			actionButton.activate("GO");
			messageLabel.setText("Press GO to begin play.");
		}
		else
		{
			actionButton.deActivate();
		}
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
			this.setSafe(ship.place(displayBoard, 1, id + 1, false));
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
			// intended to "refresh" a ship that wasn't completely placed originally because it wasn't safe
			if (!safe)
			{
				this.setSafe(this.ship.place(displayBoard, this.ship.getStartRow(), this.ship.getStartCol(), this.ship.isHorizontal()));
			}
			for (int i = 0; i < ship.size(); i++)
			{
				((ClientSquare) ship.get(i)).setSelected(selected);
			}
			if (selected)
			{
				horizButton.setText((ship.isHorizontal()) ? "V" : "H");
			}
			countShips();
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
