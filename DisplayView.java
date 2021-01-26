import javax.swing.JComponent;
import java.util.Observer;
import java.util.Observable;
import java.awt.Color;

public class DisplayView extends JComponent implements Observer {
	private myView theApp;
	public static final long serialVersionUID = 1;
	static public String display_string[] = new String[201];

	public DisplayView(myView theApp) {
		this.theApp = theApp;
	}

	public void update(Observable o, Object rectangle) {

	}
	
	// Update the labels //
	public void updateLabels() {
		int a,b;
		Color labColor;
		//Graphics2D g2D = (Graphics2D) g;
		for (a=0;a<(200-1);a++)	{
					
			for (b=0;b<6;b++)	{
				if (a<theApp.orders.size())	{
					if (b==0) theApp.labels[a+1][b].setText(theApp.orders.get(a).creationDate);
					if (b==1) theApp.labels[a+1][b].setText(theApp.orders.get(a).shoppingCartName);
					if (b==2) theApp.labels[a+1][b].setText(theApp.orders.get(a).price);
					// If an item has arrived show its arrival date if not show its status
					if (b==3)	{
						if (theApp.orders.get(a).receiptDate.length()>1) theApp.labels[a+1][b].setText(theApp.orders.get(a).receiptDate);
					     else theApp.labels[a+1][b].setText(theApp.orders.get(a).status);
					}
					if (b==4) theApp.labels[a+1][b].setText(theApp.orders.get(a).order_number);
					if (b==5) theApp.labels[a+1][b].setText(theApp.orders.get(a).approval_note);		
					
					// Set the colour of the line depending on the orders status
					labColor=getLabelColor(theApp.orders.get(a).status,theApp.orders.get(a).receiptDate);
					theApp.labels[a+1][b].setBackground(labColor);
					
					}
				else	{
						theApp.labels[a+1][b].setText(" ");
				}
				theApp.labels[a][b].setOpaque(true);
				theApp.labels[a+1][b].repaint();
				
			}
		}
		

	}
	
	// Return a color depending on the status of the order //
	public Color getLabelColor (String stat,String receipt)	{
		Color labCol=Color.BLACK;
		
		if (stat.compareTo("Approved")==0) labCol=Color.ORANGE;
		if (stat.compareTo("Awaiting Approval")==0) labCol=Color.RED;
		if (receipt.length()>1) labCol=Color.GREEN;
			
	return labCol;
	}

}
