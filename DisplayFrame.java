import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.Dimension;
import javax.swing.JScrollPane;

public class DisplayFrame extends JFrame implements ActionListener {
	public static final long serialVersionUID = 1;
	private myView theApp;
	public JStatusBar status_bar = new JStatusBar();
	public JPanel dataPanel = new JPanel();
	private JPanel buttonPanel = new JPanel();
	private Button refreshButton = new Button();
	private Button exitButton = new Button();
	private Button aboutButton = new Button();
	JScrollPane srcScrollPaneImage = null;
	
	// Constructor
	public DisplayFrame(String title, myView theApp) {
		setTitle(title);
		this.theApp = theApp;
		Border border = LineBorder.createBlackLineBorder();
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.BLACK);
		// Add the status bar
		getContentPane().add(status_bar, java.awt.BorderLayout.SOUTH);
		// Setup the data panel
		dataPanel.setBackground(Color.WHITE);
		dataPanel.setPreferredSize(new Dimension(600,6000));
		dataPanel.setLayout(new GridLayout(200,6));
		dataPanel.setOpaque(false);
		dataPanel.setVisible(true);
		// Add the layered pane to the scroll pane
		srcScrollPaneImage=new JScrollPane(dataPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		// Add the scroll pane to the main content pane
		getContentPane().add(srcScrollPaneImage,BorderLayout.CENTER);
		this.setVisible(true); // show the frame.
		
		
		int a,b;
		for (a=0;a<200;a++)	{
			for (b=0;b<6;b++)	{
			theApp.labels[a][b]=new JLabel();
			if (a==0)	{
				theApp.labels[a][b].setBackground(Color.LIGHT_GRAY);
				theApp.labels[a][b].setOpaque(true);
			}
			theApp.labels[a][b].setBorder(border);
			theApp.labels[a][b].setHorizontalAlignment(JLabel.CENTER);
			theApp.labels[a][b].setVerticalAlignment(JLabel.CENTER);
			
			if ((a!=0) && (b==5)) theApp.labels[a][b].setFont(new Font("Default",Font.BOLD,10));
			
			dataPanel.add(theApp.labels[a][b]);	
			}
			
		}
		
		theApp.labels[0][0].setText("Date");
		theApp.labels[0][1].setText("Cart Name");
		theApp.labels[0][2].setText("Cost");
		theApp.labels[0][3].setText("Status");
		theApp.labels[0][4].setText("Order Number");
		theApp.labels[0][5].setText("Approval Note");
		
		// Add the button bar
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.setBackground(Color.GRAY);
		buttonPanel.setVisible(true);		
		getContentPane().add(buttonPanel, java.awt.BorderLayout.NORTH);
		
		// Refresh Button
		refreshButton.setLabel("Refresh");
		refreshButton.setPreferredSize(new Dimension(90,30));				
		refreshButton.addActionListener(this);
		buttonPanel.add(refreshButton, BorderLayout.EAST);
		
		// About Button
		aboutButton.setLabel("About");
		aboutButton.setPreferredSize(new Dimension(90,30));				
		aboutButton.addActionListener(this);
		buttonPanel.add(aboutButton);
		
		// Exit Button
		exitButton.setLabel("Exit");
		exitButton.setPreferredSize(new Dimension(90,30));				
		exitButton.addActionListener(this);
		buttonPanel.add(exitButton);
		
			
	}
	
	// Handle all events
	public void actionPerformed(ActionEvent event) {
		String event_name = event.getActionCommand();
		// Refresh button
		if (event_name=="Refresh")	{
			theApp.lastSapCheck=0;
		}
		
		// Exit button
		if (event_name=="Exit")	{
			System.exit(0);
		}		
		
		// About button
		if (event_name=="About")	{
			JOptionPane.showMessageDialog(null,"Ian Wraith","myView",  JOptionPane.INFORMATION_MESSAGE);
		}		
		
		
	}
	
	// Change the status bar contents
	public void statusBarUpdate (String text)	{
	status_bar.setText(text);
	}

}
