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
	cPlayer you;
	cPlayer opp;
    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
	private JFrame frame = new JFrame("BattleShip");
	private JPanel mainPanel = new JPanel();    // holds turnPanel and boardPanel
	private JPanel bottom = new JPanel();		// holds youPanel and opponentPanel	
    private JPanel turnPanel = new JPanel();		//tells whose turn it is and holds action and message panels
    private JPanel actionPanel = new JPanel();
    private JPanel actionP1 = new JPanel();  // tells you whose board is displayed
    private JPanel actionP2 = new JPanel();
    private bLabel p1Label = new bLabel(actionP1, "");
    private bLabel p2Label = new bLabel(actionP2, "Board");
    private JButton actionButton = new JButton("");
    private bLabel messageLabel = new bLabel(turnPanel, "test message");
    private JPanel boardPanel = new JPanel();		//displays active board
    private OceanSquare[][] board = new OceanSquare[10][10];
    private JPanel youPanel = new JPanel();
    private bLabel youLabel = new bLabel(youPanel, "");
    private bLabel youShotText = new bLabel(youPanel, "Shots: ");
    private bLabel youShotLabel = new bLabel(youPanel, "");
    private JPanel oppPanel = new JPanel();
    private bLabel oppLabel = new bLabel(oppPanel, "");
    private bLabel oppShotText = new bLabel(oppPanel, "Shots: ");
    private bLabel oppShotLabel = new bLabel(oppPanel, "");
    private Shot[] shots = new Shot[BattleShipServer.TOTALSHIPS];
	private tmLog log = new tmLog(tmLog.TRACE);
    private int shotCounter = 0;
    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public BattleShipClient() throws Exception 
    {
        you = new cPlayer();
        opp = new cPlayer();
        // Setup networking
        try
        {
            socket = new Socket(getServerAddress(), PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            // stage is CONNECTING, read from server to get your own ID
           	you.setId(Integer.parseInt(getServerResponse()));
           	// respond with your player name
            you.setName(getPlayerName());
            actionButton.setEnabled(false);
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
        turnPanel.setBackground(tmColors.LIGHTBLUE);
        actionPanel.setBackground(tmColors.LIGHTBLUE);
        bottom.setBackground(tmColors.BLACK);
        youPanel.setBackground(tmColors.LIGHTGREEN);
        oppPanel.setBackground(tmColors.LIGHTCYAN);
        actionP1.setBackground(tmColors.LIGHTBLUE);
        actionP2.setBackground(tmColors.LIGHTBLUE);
        messageLabel.setText("Waiting for an opponent to sign on.");
        // you will communicate your name/ship data, opponent's will be returned
        youLabel.setText(you.getName());
        // Layout GUI
        frame.getContentPane().add(mainPanel);
        mainPanel.setLayout(new GridLayout(1, 2, 10, 10));
        mainPanel.add(turnPanel);
        turnPanel.setLayout(new GridLayout(3, 1, 10, 10));
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
	            	if (gameStage == BattleShipServer.YOURANSWER)
	            	{
	            		gameStage = BattleShipServer.THEIRTURN;
	            		sendToServer("" + gameStage + BattleShipServer.READY);
	            		actionButton.setText("");
	            		actionButton.setEnabled(false);
	            	}
	            	else if (gameStage == BattleShipServer.YOURTURN && shotCounter == you.getShips())
	            	{
	            		gameStage = BattleShipServer.YOURANSWER;
	            		StringBuilder sb = new StringBuilder("" + gameStage + you.getShips());
	            		for (int i = 0; i < you.getShips(); i++)
	            		{
	            			sb.append("" + shots[i].getR() + shots[i].getC());
	            		}
	            		sendToServer(sb.toString());
	            		messageLabel.setText("Press END to end your turn.");
	            		actionButton.setText("END");
	            		actionButton.setEnabled(true);
	            	}
	            	else if (gameStage == BattleShipServer.THEIRANSWER)
	            	{
	            		gameStage = BattleShipServer.YOURTURN;
	            		sendToServer("" + gameStage + BattleShipServer.READY);
	            	}
	            	else if (gameStage > BattleShipServer.THEIRANSWER)
	            	{
	            		sendToServer("" + gameStage + BattleShipServer.READY);
	            	}
            	}	
            }    
        });
        actionPanel.add(actionButton);
        actionPanel.add(actionP1);
        actionPanel.add(actionP2);
        turnPanel.add(actionPanel);
        turnPanel.add(messageLabel);
        bottom.setLayout(new GridLayout(1, 2, 10, 10));
        bottom.add(youPanel);
        bottom.add(oppPanel);
        turnPanel.add(bottom);
        mainPanel.add(boardPanel);
        boardPanel.setLayout(new GridLayout(10, 10, 1, 1));
        for (int i = 0; i < BattleShipServer.SIZE; i++)
        {
        	for (int j = 0; j < BattleShipServer.SIZE; j++)
        	{
        		final int I = i;
        		final int J = j;
        		board[i][j] = new OceanSquare();
        		board[i][j].addMouseListener(new MouseAdapter() 
                {
                    public void mousePressed(MouseEvent e) 
                    {
                    	(board[I][J]).process(I, J); 
                    }    
                });
        		boardPanel.add(board[i][j]);
        	}	
        }
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
	    			gameStage = (opp.getId() == 1) ? BattleShipServer.YOURTURN : BattleShipServer.THEIRTURN;
	    			sendToServer("" + gameStage + BattleShipServer.READY);
	    		}
	    		else if (gameStage == BattleShipServer.PLACING)
	    		{
	    			// placeHolder for placing ships
	    		}
	    		else
	    		{	
	    			cPlayer cur = null;
	    			cPlayer other = null;
	    			if (gameStage > BattleShipServer.YOURANSWER && gameStage < BattleShipServer.YOUWON)
	    			{
	    				//IT IS NOT YOUR TURN
	    				cur = opp;
	    				other = you;
	    				if (gameStage == BattleShipServer.THEIRTURN)
	    				{
	        	    		messageLabel.setText(cur.getName() + " is shooting.");
	    	    			actionButton.setEnabled(false);
	    	    			actionButton.setText("");
	    					gameStage = BattleShipServer.THEIRANSWER;
	    					sendToServer("" + gameStage + BattleShipServer.READY);
	    				}
	    				else
	    				{
	    					messageLabel.setText("Press GO for your turn.");
	    					actionButton.setText("GO");
	    					actionButton.setEnabled(true);
	    				}
	    			}
	    			else if (gameStage < BattleShipServer.THEIRTURN)
	    			{
	    				cur = you;
	    				other = opp;
	    			}	
	    			//column 0		current player's ships
	    			//column 1		other player's ships
	    			cur.setShips(Integer.parseInt(temp.substring(0, 1)));
	    			other.setShips(Integer.parseInt(temp.substring(1, 2)));
	    			youShotLabel.setText("" + you.getShips());
	    			oppShotLabel.setText("" + opp.getShips());
	    			p1Label.setText(other.getName() + "'s");
	    			displayBoard(temp.substring(2));
	    			if (you.getShips() == 0 || opp.getShips() == 0)
	    			{	
	    				if (you.getShips() == 0)
	    				{
	    					messageLabel.setText(opp.getName() + " has won!");
	    					gameStage = BattleShipServer.THEYWON;
	    				}
	    				else if (opp.getShips() == 0)
	    				{
	    					messageLabel.setText(you.getName() + " has won!");
	    					gameStage = BattleShipServer.YOUWON;
	    				}
	    				actionButton.setText("AGAIN");
	    				actionButton.setEnabled(true);
	    			}
	    			else
	    			{	
		    			shotCounter = 0;
		    			while (gameStage == BattleShipServer.YOURTURN)
		    			{
		    				if (shotCounter == you.getShips())
		    				{
		    					actionButton.setEnabled(true);
		    					actionButton.setText("FIRE");
		    					messageLabel.setText("All shots are set. Fire when ready.");
		    				}
		    				else
		    				{
		    					actionButton.setText("");
		    					actionButton.setEnabled(false);
		    					messageLabel.setText(you.getName() + " has set " + shotCounter + " of " + you.getShips() + " shots.");
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
    private class cPlayer
    {
    	private int id;
		private String name;
    	private int ships = 5;
    	
    	public cPlayer()
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
		public int getShips()
		{
			return ships;
		}
		public void setShips(int ships)
		{
			this.ships = ships;
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
    private class Shot
    {
    	int r;
    	int c;
		public Shot(int r, int c)
    	{
    		this.r = r;
    		this.c = c;
    	}
		public String toString()
		{
			return "Row " + r + ", Column " + c;
		}
    	public int getR()
		{
			return r;
		}
		public void setR(int r)
		{
			this.r = r;
		}
		public int getC()
		{
			return c;
		}
		public void setC(int c)
		{
			this.c = c;
		}
    }
    private class OceanSquare extends JPanel 
    {
        JLabel label = new JLabel("");
        private int status = BattleShipServer.UNTESTED;
        private int contents = 0;
        
        public OceanSquare() 
        {
        	this.setBackground(tmColors.CHARTREUSE);
        	label.setForeground(tmColors.WHITE);
        	label.setFont(tmFonts.PLAIN16);
            add(label);
        }
      
        public void setStatus(int status)
        {
        	this.status = status;
       		this.setBackground((status < BattleShipServer.HIT) ? tmColors.CHARTREUSE : tmColors.DARKBLUE);
       		label.setText((status == BattleShipServer.AIMED) ? "X" : (status == BattleShipServer.HIT) ? "+" : ""); 
        }
        public void setContents(int contents)
        {
        	this.contents = contents;
        	label.setText((contents == 0) ? "" : "" + contents);
        }
        public void process(int r, int c)
        {
        	//this can only happen if it's your turn and you click on this square
        	if (gameStage == BattleShipServer.YOURTURN)
        	{
        		if (status == BattleShipServer.AIMED)   
        		{	// need to remove this shot from the shot queue
        			ArrayList<Shot> sh = new ArrayList<>();
        			int sCtr = 0;
        			for (int i = 0; i < shotCounter; i++)
        			{
        				if (shots[i].getR() == r & shots[i].getC() == c)
        				{
                			setStatus(BattleShipServer.UNTESTED);
        				}
        				else
        				{
        					sh.add(shots[i]);
        					sCtr += 1;
        				}
        			}
        			if (sCtr != (shotCounter - 1))   // should not happen
        			{
        				log.error(you.getName() + ": Error 1 processing shot " + r + ", " + c);
        			}
        			for (shotCounter = 0; shotCounter < sCtr; shotCounter++)
        			{
        				shots[shotCounter] = sh.get(shotCounter);
        			}
        		}
        		else
        		{
        			if (shotCounter <  you.getShips())
        			{	
	        			shots[shotCounter] = new Shot(r, c);
	        			setStatus(BattleShipServer.AIMED);
	        			shotCounter += 1;
        			}
        		}	
        	}
        	else
        	{
        		messageLabel.setText("Not your turn.");
        	}
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
