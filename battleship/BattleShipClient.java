package battleship;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.ToMar.Utils.tmColors;
import org.ToMar.Utils.tmFonts;

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
    private bLabel currentPlayerLabel = new bLabel(actionPanel, "");
    private JButton actionButton = new JButton("FIRE");
    private bLabel messageLabel = new bLabel(turnPanel, "test message");
    private JPanel boardPanel = new JPanel();		//displays active board
    private OceanSquare[][] board = new OceanSquare[10][10];
    private JPanel youPanel = new JPanel();
    private bLabel youLabel = new bLabel(youPanel, "");
    private bLabel youShotText = new bLabel(youPanel, "Shots: ");
    private bLabel youShotLabel = new bLabel(youPanel, "5");
    private JPanel oppPanel = new JPanel();
    private bLabel oppLabel = new bLabel(oppPanel, "");
    private bLabel oppShotText = new bLabel(oppPanel, "Shots: ");
    private bLabel oppShotLabel = new bLabel(oppPanel, "");
    
    private ImageIcon[] icons = new ImageIcon[4];
    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public BattleShipClient() throws Exception 
    {
        you = new cPlayer();
        opp = new cPlayer();
        try
        {
        	icons[0] = new ImageIcon("src/images/UNTESTED.png");
        	icons[1] = new ImageIcon("src/images/AIMED.png");
        	icons[2] = new ImageIcon("src/images/HIT.png");
        	icons[3] = new ImageIcon("src/images/MISS.png");
        }
        catch (Exception e)
        {
        	System.out.println("ERROR: " + e);
        }

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
            frame.setTitle("BattleShip: " + you.getName());
            sendToServer("" + gameStage + you.getName());
            // set stage to WAITING until you get the opponent's information
            gameStage = BattleShipServer.WAITING;
        }
        catch (Exception e)
        {
            System.out.println("ERROR in BattleShipClient: " + e);
        	throw new Exception(e);
        }
        messageLabel.setBackground(tmColors.LIGHTYELLOW);
        messageLabel.setText("Waiting for an opponent to sign on.");
        // you will communicate your name/ship data, opponent's will be returned
        youLabel.setText(you.getName());
        // Layout GUI
        frame.getContentPane().add(mainPanel);
        mainPanel.setLayout(new GridLayout(1, 2, 10, 10));
        mainPanel.add(turnPanel);
        turnPanel.setLayout(new GridLayout(3, 1, 10, 10));
        actionPanel.setLayout(new GridLayout(1, 3, 10, 10));
        actionPanel.add(new JPanel());
        actionButton.setFont(tmFonts.PLAIN32);
        actionPanel.add(actionButton);
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
        		OceanSquare o = new OceanSquare();
                o.addMouseListener(new MouseAdapter() 
                {
                    public void mousePressed(MouseEvent e) 
                    {
//                        System.out.println("pressed " + i + " " + j);
                    }    
                });
        		boardPanel.add(o);
        	}	
        }
    }
    public void sendToServer(String s)
    {
		System.out.println("Sending to Server: +" + s + "+");
        out.println(s);
    }
    public String getServerResponse() throws Exception
    {
    	try
    	{
    		String temp = in.readLine();
    		System.out.println("Server responded: +" + temp + "+");
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
        	System.out.println("BattleShipClient: ERROR getting server response: " + e);
        	throw new Exception(e);
        }
    }
    public void play() throws Exception 
    {
    	try
    	{
    		while (true)
    		{	
	    		if (gameStage > BattleShipServer.PLACING)
	    		{
	    			// during this stage, the message from the server will be
	    			// column 1: 3 (gameStage) --> will be stripped by getServerResponse()
	    			// column 2:   (id of currentPlayer)
	    			// column 3:   (shipsLeft of currentPlayer)
	    			// column 4:   (boardToString of currentPlayer's opponent, 100 chars if it's their board, 200 if it's yours)
	    			String temp = getServerResponse();
	    			// whose turn is it? -- update whoever's information it is
	    			int currentPlayer = Integer.parseInt(temp.substring(0, 1));
	    			int currentShots = Integer.parseInt(temp.substring(1, 2));
	    			if (currentPlayer == you.getId())
	    			{
	    				you.setShips(currentShots);
	    				this.currentPlayerLabel.setText(you.getName());
	    			}
	    			else
	    			{
	    				opp.setShips(currentShots);
	    				this.currentPlayerLabel.setText(opp.getName());
	    			}
	    			displayBoard(temp.substring(3));
	    		}
	    		else if (gameStage == BattleShipServer.WAITING)
	    		{
	    			String temp = getServerResponse();
	    			opp.setId(Integer.parseInt(temp.substring(0, 1)));
	    			opp.setName(temp.substring(2));
	    			oppLabel.setText(opp.getName());
	    			gameStage = BattleShipServer.YOURTURN;
	    			sendToServer("" + gameStage + BattleShipServer.READY);
	    		}
    		}	
        }
        catch (Exception e)
        {
        	throw new Exception("BattleShipClient.play: " + e);
        }
        finally 
        {
        	System.out.println("Closing socket.");
            socket.close();
        }
    }
    private void displayBoard(String bStr)
    {
    	if (bStr.length() == 100)
    	{
    		int ctr = 0;
    		for (int i = 0; i < BattleShipServer.SIZE; i++)
    		{
    			for (int j = 0; j < BattleShipServer.SIZE; j++)
    			{
    	    		int temp = Integer.parseInt(bStr.substring(ctr, ctr + 1));
    				board[i][j].setStatus(temp);
    				ctr += 1;
    			}
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
    private class OceanSquare extends JPanel 
    {
        JLabel label = new JLabel(icons[0]);
        private int status = BattleShipServer.UNTESTED;
        private int contents;
        
        public OceanSquare() 
        {
        	this.setBackground(tmColors.CHARTREUSE);
            add(label);
            setIcon(icons[status]);
        }
        public void setStatus(int status)
        {
        	this.status = status;
            setIcon(icons[status]);
        }
        public void setIcon(Icon icon) 
        {
            label.setIcon(icon);
        }
        public void process()
        {
        	//this is a stub
        	System.out.println(".");
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
    		System.out.println("Client bailing due to error, ending program.");
    	}
    }
}
