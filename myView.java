import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

public class myView {
	private DisplayModel display_model;
	private DisplayView display_view;
	private static myView theApp;
	static DisplayFrame window;
	public WebClient webClient;
	public JLabel [][] labels  = new JLabel [201][6];
	public String program_version = "myView Build 8 - Ian Wraith 2010";
	private String sapUsername;
	private String sapPassword;
	public List<sapOrder> orders = new ArrayList<sapOrder>();
	long lastSapCheck=0;
	final int sapCheckPeriodinSeconds=1800;
	boolean sapConnectedState=false;
	String orderNumber;
	String approvalNote;
	String receiptInfo;
	public HtmlPage workingPage;
	public HtmlUnitCommandLineDebugger debugger;
	public int orderBase=0;
	public long lasttimeToNextCheck=-1;
	private boolean debug=false;
	
	
	public static void main(String[] args) {
		theApp = new myView();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				theApp.createGUI();
			}
		});		

	theApp.setup();	  
	
	while (true)	{
	theApp.loop();
	}
	
	}
	
	public void loop()	{
		long currentTime=(System.currentTimeMillis()/1000);
		long timeDiff=currentTime-lastSapCheck;
		
		if (timeDiff>=sapCheckPeriodinSeconds) { 
			getSapOrderStatus();
			lastSapCheck=(System.currentTimeMillis()/1000);
		}
		else	{
			
			if (sapConnectedState==false)	{
				// Work out the minutes remaining until the next check
				long timeToNextCheck=(sapCheckPeriodinSeconds/60)-(timeDiff/60);
				String timePeriod=Long.toString(timeToNextCheck);
				if (timeToNextCheck!=lasttimeToNextCheck)	{
					String timeDisplay="Disconnected from SAP (Next update in "+timePeriod+" minutes)";
					window.statusBarUpdate(timeDisplay);
					lasttimeToNextCheck=timeToNextCheck;
				}
			}	
		}	
	}
	
	// Setup the window //
	public void createGUI() {
		window = new DisplayFrame(program_version, this);
		Toolkit theKit = window.getToolkit();
		Dimension wndsize = theKit.getScreenSize();
		
		// Width was /6 /3
		
		window.setBounds(wndsize.width/15,wndsize.height/6,5*wndsize.width/6,2*wndsize.height/3);
		window.dataPanel.setBounds(wndsize.width/15,wndsize.height/6,5*wndsize.width/3,2*wndsize.height/3);
		window.addWindowListener(new WindowHandler());
		display_model = new DisplayModel();
		display_view = new DisplayView(this);
		display_model.addObserver(display_view);
		//window.getContentPane().add(display_view, BorderLayout.CENTER);
		window.setVisible(true);
	}
	
	public boolean sap_Connect () {
		
		HtmlForm form;
		HtmlTextInput users;
		HtmlPasswordInput password;
		HtmlPage page,logPage;
						
		// Attempt to connect to SAP
		try {
			page = webClient.getPage("https://xxx.xxx.com/sap/bc/gui/sap/its/bbpstart?sap-client=400&sap-language=EN");		
		}
		catch (Exception ex) {
			return false;
		}
		
		// Get the form
		form = page.getFormByName("loginForm");
		// Username
		try {
			users = form.getInputByName("sap-user");
			users.setValueAttribute(sapUsername);		
		}
		catch (Exception ex) {
			return false;
		}
		// Password
		try {
			password = form.getInputByName("sap-password");
			password.setValueAttribute(sapPassword);
		}
		catch (Exception ex) {
			return false;
		}	
				
		// Submit this //
		try {
			final HtmlAnchor anchor = page.getAnchorByName("Log on");
			anchor.click();
			form.submit(null);
			page = webClient.getPage("https://xxx.xxx.com/sap/bc/gui/sap/its/bbpstart?sap-client=400&sap-language=EN");		
		}
		catch (Exception ex) {
			return false;
			}	
		if (debug==true) writeDebugMessage("Logging onto SAP");
		// Get the returned pages title
		String title=page.getTitleText();
		// This will be "SRM - Enterprise Buyer" if all is OK //
		if (title.compareTo("SRM - Enterprise Buyer")==0) return true;
			else return false;
	}
	
	// Disconnect from SAP
	public boolean sap_Disconnect () {
		HtmlPage page;
		String title;
		if (debug==true) writeDebugMessage("Disconnecting from SAP");
		// Get frame 0 from the main page
		page=get_frame("https://xxx.xxx.com/sap/bc/gui/sap/its/bbpstart?sap-client=400&sap-language=EN",0);
		// Run a Javascript command to disconnect
		try	{
			ScriptResult scriptresult=page.executeJavaScript("headerMenuClickInFrame('LGOF')");
			boolean result=ScriptResult.isFalse(scriptresult);
			if (result==true) return false;
			page = webClient.getPage("https://xxx.xxx.com/sap/bc/gui/sap/its/bbpstart?sap-client=400&sap-language=EN");			
		}
		catch (Exception ex) {
			return false;
			}	
		// Get the returned pages title
		title=page.getTitleText();
		// This will be "SRM - Enterprise Buyer Welcome!" if all is OK and we are no longer connected //
		if (title.compareTo("SRM - Enterprise Buyer Welcome!")==0) return true;
			else return false;			
	}
	
	// Prepare assorted variables and objects
	public void setup () {
	CookieManager cookiemanager;
    // Pretend to be a Firefox 3 browser	
	webClient = new WebClient(BrowserVersion.FIREFOX_3);	
	
	try {
		webClient.setUseInsecureSSL(true);
		cookiemanager=webClient.getCookieManager();
		cookiemanager.setCookiesEnabled(true);
		webClient.setCookieManager(cookiemanager);
		// Log events
		//System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");
		//webClient.getJavaScriptEngine().getContextFactory().setDebugger(new DebuggerImpl());
		debugger = new HtmlUnitCommandLineDebugger();
		webClient.getJavaScriptEngine().getContextFactory().setDebugger(debugger);
		// Stop exceptions buggering everything up
		webClient.setThrowExceptionOnScriptError(false);
		
	}			
	catch (Exception ex) {
		JOptionPane.showMessageDialog(null, "Fatal error in setup()",
				"myView", JOptionPane.ERROR_MESSAGE);
		}	
	
	getUserDetails();
		
	}
	
	// Get the SAP order status //
	public void getSapOrderStatus ()	{
		boolean state;
		
		// Check the user isn't already connected to SAP before even trying
		if (sapConnectedState==true) return ;
		 else sapConnectedState=true;
	
		window.statusBarUpdate("Trying Connect to SAP");
		
		state=sap_Connect();
		
		if (state==true) window.statusBarUpdate("Connected to SAP");
		 else window.statusBarUpdate("Error unable to connect to SAP");
		
		if (state==true) {
			if (click_status()==true) window.statusBarUpdate("Status Obtained");
			 else window.statusBarUpdate("Unable to Obtain Status");
		// Remove any deleted orders 
	    removedDeletedFromStatus();	
		}
		
		if (state==true) {
			state=sap_Disconnect();	
			if (state==true) window.statusBarUpdate("Disconnected from SAP");
			else window.statusBarUpdate("Error unable to disconnect from SAP");		
		}	
		
		if (state==true) display_view.updateLabels();
		
		sapConnectedState=false;
		
	}
	
	// Get a specific frame from an URL 
	public HtmlPage get_frame (String url,int frame)
	{
		HtmlPage page;
		List<FrameWindow> framewindows = new ArrayList<FrameWindow>();

		try	{
			// Get the main frame 
			page = webClient.getPage(url);
		    // Get a list of frames
			framewindows=page.getFrames();
			// Get the requested frame as a HtmlPage
			page=(HtmlPage)framewindows.get(frame).getEnclosedPage();
		}
		catch (Exception ex) {
			return null;
			}					
		return page;
	}
	
	// View Status //
	public boolean click_status ()
	{
		HtmlPage mainPage;
			
		// Frames
		// 0 is framename HEADER
		// 1 is framename APPLICATION
	
		// Get frame 1 from the main page
		mainPage=get_frame("https://xxx.xxx.com/sap/bc/gui/sap/its/bbpstart?sap-client=400&sap-language=EN",1);
        // From this get the IAC page //
		workingPage=(HtmlPage)mainPage.getFrameByName("IACFrame").getEnclosedPage();
						
		// Run a Javascript command to go to the status page
		try	{
			// Get a list of Anchors on this page
			List<HtmlAnchor> anchors = new ArrayList<HtmlAnchor>();
			anchors=workingPage.getAnchors();
			// Click on the 2nd one which is check status //
			workingPage=(HtmlPage)anchors.get(2).click();
			// Check the title of this page is SAP
			String title=workingPage.getTitleText();
			if (title.compareTo("SAP")!=0) return false;
			// Now click on the "Extended Search" link
			anchors=workingPage.getAnchors();
			workingPage=(HtmlPage)anchors.get(1).click();
		}
		catch (Exception ex) {
			if (debug==true) savePage("error_click_status.html",workingPage.asXml());
			return false;
			}			
		// Handle the extended search
		handle_extended_search();
		if (workingPage==null) return false;
		
		// Extract the data
		if (extractData()==false) return false;
		
		return true;	
	}
	
	// Fill in the Extended Search page
	public boolean handle_extended_search ()
	{
	HtmlSelect select;
	HtmlOption option;
	HtmlCheckBoxInput checkbox;
		
		try	{
			if (debug==true) writeDebugMessage("Completing Extended Search Details");
            // Fill in the Timeframe drop down box
			List<HtmlElement> elements = new ArrayList<HtmlElement>();
			elements=workingPage.getElementsByName("GS_SEARCH_FIELD-SELECTION_DATE");
			select=(HtmlSelect)elements.get(0);
			// Set to "1 Year" //
			option=select.getOptionByValue("6");
			workingPage=(HtmlPage)select.setSelectedAttribute(option,true);
			// Select the "Including Completed Shopping Carts" tickbox //
			elements=workingPage.getElementsByName("GS_SEARCH_FIELD-WITH_CLOSED");
			// This is the 2nd element with this name
	        checkbox=(HtmlCheckBoxInput)elements.get(1);
	        workingPage=(HtmlPage)checkbox.setChecked(true);
			// Get a list of Anchors on this page
			List<HtmlAnchor> anchors = new ArrayList<HtmlAnchor>();
			anchors=workingPage.getAnchors();
			// Click on the 1st one which is "Start" //
			workingPage=anchors.get(0).click();
		}
		catch (Exception ex) {
			if (debug==true) savePage("error_handle_extended_search.html",workingPage.asXml());
			return false;
			}		
		return true;
	}
	
	// Save a page as a text file
	public boolean savePage(String filename,String contents)
	{
		FileWriter file;
		
		// Create a file with this name //
		File tfile = new File(filename);
			
		try {
			// Open the file
			file = new FileWriter(tfile);
			// Write the data 
			file.write (contents);
			// Flush & close
			file.flush();
			file.close();
		} catch (Exception e) {
			return false;
		}
		
		return true;	
	}
	
	// Parse the page showing the orders for info
	public boolean extractData ()
	{
	HtmlAnchor anchor;
    HtmlTable info_table;
    int row_count;
    // Clear the existing orders list
    orders.clear();
    orderBase=0;
    
    //savePage("c:\\temp\\extractData.html",workingPage.asXml());
    
    // An endless loop to view all the users pages of orders
    while (true) {
    	// Get the table containing the order info
    	info_table=(HtmlTable)workingPage.getFirstByXPath("/html/body/form/table[3]/tbody/tr/td/table");		
    	// Find the number of rows in the table
    	row_count=info_table.getRowCount();
    	// Extract the data from the table
    	if (extractDatafromTable(info_table)==true)	{
    		// All has gone well get the order numbers
    		if (fetchOrderNumbers(row_count)==false) return false;		
    		// Present partial info to the user as soon as possible
    		display_view.updateLabels();
    		
    	} else {
    		if (debug==true) savePage("error_extractData.html",workingPage.asXml());
    		return false;	
    		}	
    	// See if there are more pages of orders to get	
    	try	{
  			// See if the next page anchor exists
			anchor=workingPage.getFirstByXPath("/html/body/form/table[3]/tbody/tr/td/table[3]/tbody/tr/td[2]/table/tbody/tr/td[3]/a");
			// If it does then click on it 
			workingPage=anchor.click();		
			// Increase the order counter by the correct ammount
			orderBase=(orderBase+row_count)-1;
			
    	} catch (Exception e) {
    		// If this anchor doesn't exist then we are all done and there are no more pages to get
			return true;
			}
    	}  	
	}
	
	// Parse a table containing brief info on orders
	public boolean extractDatafromTable	(HtmlTable info_table) {
	    
		List<HtmlTableRow> rows = new ArrayList<HtmlTableRow>();
	    List<HtmlTableCell> cells = new ArrayList<HtmlTableCell>();     
	    int row_count,a,b,cell_count,oCount;
	  
	    try	{
		// Get the number of rows
		row_count = info_table.getRowCount();
		// Check if there are any rows returned
		if (row_count<2) return true;
		// Get each row
		rows = info_table.getRows();    

		// Run through each row
	    for (a=1;a<row_count;a++) {
		    sapOrder torder= new sapOrder();
	    	// Get the cells
	    	cells=rows.get(a).getCells();
	    	cell_count=cells.size();
	    	// Run through each cell
	    	for (b=0;b<cell_count;b++)	{
	    		// 1 is Number
	    		if (b==1) torder.number=cells.get(b).asText();
	    		// 2 is Name of shopping cart
	    		if (b==2) torder.shoppingCartName=cells.get(b).asText();
	    		// 3 is date
	    		if (b==3) torder.creationDate=cells.get(b).asText();
	    		// 4 is price
	    		if (b==4) torder.price=cells.get(b).asText();
	    		// 5 is status
	    		if (b==5) torder.status=cells.get(b).asText();
	    		// 6 is the further info link
	    	}	
	    
	    	oCount=orderBase+(a-1);	    	
	    	orders.add(oCount,torder);
	    }
   
	    return true;
	    
	    } catch (Exception e) {
			return false;
		}
	    
	}
	
	
	public boolean fetchOrderNumbers (int rcount)	{
				
		HtmlTable info_table;
		List<HtmlTableRow> rows = new ArrayList<HtmlTableRow>();
		List<HtmlTableCell> cells = new ArrayList<HtmlTableCell>();
		HtmlAnchor anchor;
		HtmlTableCell link;
		int a,oCount,tCount;
		String infoString;
				
		try	{ 
			for (a=1;a<rcount;a++)	{
				  				 			  			  
				  	//workingPage=(HtmlPage)workingPage.refresh();
					  				    				  	    
				  	info_table = (HtmlTable)workingPage.getFirstByXPath("/html/body/form/table[3]/tbody/tr/td/table");		
				  	rows = info_table.getRows();
					cells=rows.get(a).getCells();
					link=cells.get(6);	
					anchor=link.getFirstByXPath("a");  
					oCount=orderBase+(a-1);
					// Tell the user what the program is doing
					tCount=oCount+1;
			    	infoString="Fetching Details of Order "+Integer.toString(tCount);
			    	window.statusBarUpdate(infoString);
			    	
					if (extractOrderNumber(anchor)==false) {
						orders.get(oCount).order_number="";	
						orders.get(oCount).receiptDate="";
						orders.get(oCount).approval_note=approvalNote;	
					}
					else {
						orders.get(oCount).order_number=orderNumber;
						orders.get(oCount).approval_note=approvalNote;
						orders.get(oCount).receiptDate=receiptInfo;
					}	
			  }
		
		} catch (Exception e) {
			if (debug==true) savePage("error_fetchOrderNumbers.html",workingPage.asXml());
			return false;
		}
	
		return true;
	}
	
	
	public boolean extractOrderNumber(HtmlAnchor faAnchor)	{		
		if (faAnchor==null) return false;
		try	{	
			
			//workingPage=(HtmlPage)workingPage.getFrameByName("IACFrame").getEnclosedPage();;
			
			// Click on the further info link
			workingPage=faAnchor.click();	
			
		} catch (Exception e) {
				
			if (debug==true)	{
				// Save the page with the problem
				savePage("error_extractOrderNumber.html",workingPage.asXml());
				// and the link with the problem
				savePage("error_extractOrderNumberAnchor.html",faAnchor.asXml());
				// and the exception stack trace
				savePage("error_extractOrderNumber_stacktrace.txt",stack2string(e));
			}
			return false;
		}
		
	// Process the resulting page
	if (furtherInfoProcessing()==false) return false;		
	return true;
	}
	
	// Display a dialog box asking the user their SAP username and password
	public void getUserDetails()	{
		 //Create a panel that will be use to put
		 //one JTextField, one JPasswordField and two JLabel
		 JPanel panel=new JPanel();
		 //Set JPanel layout using GridLayout
		 panel.setLayout(new GridLayout(4,1));
		 //Create a label with text (Username)
		 JLabel username=new JLabel("SAP Username");
		 //Create a label with text (Password)
		 JLabel password=new JLabel("SAP Password");
		 //Create text field that will use to enter username
		 JTextField textField=new JTextField(15);
		 //Create password field that will be use to enter password
		 JPasswordField passwordField=new JPasswordField(15);
		 //Add label with text (username) into created panel
		 panel.add(username);
		 //Add text field into created panel
		 panel.add(textField);
		 //Add label with text (password) into created panel
		 panel.add(password);
		 //Add password field into created panel
		 panel.add(passwordField);
		 panel.setVisible(true);
		 //Show JOptionPane that will ask user for username and password
		 int a=JOptionPane.showConfirmDialog(window,panel,"Enter your SAP Details",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		 //Operation that will do when user click 'OK'
		 if(a==JOptionPane.OK_OPTION)
		 {
		 // Make sure the user name is upper case
		 sapUsername=textField.getText().toUpperCase();
		 String tpass=new String (passwordField.getPassword());
		 sapPassword=tpass;
		 }		
	}
	
	// Process the further info page
	boolean furtherInfoProcessing ()
	{
		HtmlAnchor actionAnchor;
		HtmlTableCell oNumber;
		HtmlTableCell appNote;
		String orderArrived;
		
		approvalNote="";
		orderNumber="";
		receiptInfo="";
				
		try	{
			actionAnchor = (HtmlAnchor)workingPage.getFirstByXPath("/html/body/form[6]/table[2]/tbody/tr[3]/td[2]/table[4]/tbody/tr[2]/td[10]/a");		
			workingPage = actionAnchor.click();
			
			//savePage("furtherInfoProcessing.html",workingPage.asXml());
			
	        // Get the order number
			oNumber=(HtmlTableCell)workingPage.getFirstByXPath("/html/body/form[6]/table[8]/tbody/tr/td/table/tbody/tr[2]/td/table[2]/tbody/tr/td/table/tbody/tr[6]/td[4]");
			//oNumber=(HtmlTableCell)workingPage.getFirstByXPath("/html/body/form[6]/table[2]/tbody/tr[3]/td[2]/table[7]/tbody/tr/td/table/tbody/tr[2]/td/table[7]/tbody/tr/td/table/tbody/tr[2]/td/table[2]/tbody/tr/td/table/tbody/tr[6]/td[4]");
			orderNumber=oNumber.asText();
			
			// Get the approval note
			appNote=(HtmlTableCell)workingPage.getFirstByXPath("/html/body/form[6]/table[9]/tbody/tr[3]/td[2]/table/tbody/tr[5]/td[2]");
			approvalNote=appNote.asText();
			// Find if this item has been receipted
			orderArrived=isOrderReceipted();
			if (orderArrived.length()>1)	{
				receiptInfo="Receipted "+orderArrived;
			}
			returnToStatus();
			}
		catch (Exception e) {
			if (debug==true) savePage("error_furtherInfoProcessing.html",workingPage.asXml());
			returnToStatus ();
			return false;
		}
	
		return true;
		}
	

	public boolean returnToStatus ()	{
		HtmlAnchor returnAnchor;
		try	{
			// Click on the return to status link
			returnAnchor = (HtmlAnchor)workingPage.getFirstByXPath("/html/body/form[6]/table/tbody/tr/td/table/tbody/tr/td/font/a");
			workingPage=returnAnchor.click();
			}
		catch (Exception e) {
			if (debug==true) savePage("error_returnToStatus.html",workingPage.asXml());		
			return false;
		}
		return true;
	}
		
	// Remove deleted orders from the orders list //
	public void removedDeletedFromStatus ()	{
		int a;
		for (a=0;a<orders.size();a++)	{
			if (orders.get(a).status.compareTo("Deleted")==0) orders.remove(a);	
		}	
	}
	
	// Convert printStackTrace() to a string
	public static String stack2string(Exception e) {
		  try {
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		    return "------\r\n" + sw.toString() + "------\r\n";
		  }
		  catch(Exception e2) {
		    return "bad stack2string";
		  }
		 }
	
	// Find if an order has been receipted
	public String isOrderReceipted ()	{
		HtmlTableCell confirmed;
		String contents;
		// See if the item has been receipted
		try	{
			confirmed=(HtmlTableCell)workingPage.getFirstByXPath("/html/body/form[6]/table[8]/tbody/tr/td/table/tbody/tr[2]/td/table[2]/tbody/tr/td/table/tbody/tr[8]/td[5]");
			contents=confirmed.asText();
		}
		 catch(Exception e2) {
			    return "N";
			  }
		return contents;
	}
		
	// Write messages to the file DEBUG.TXT
	public void writeDebugMessage (String message)	{
		FileWriter file;
		String line;
		try	{
			file=new FileWriter("debug.txt",true);
			// Get the current time & date //
			Date now=new Date();
			DateFormat tf=DateFormat.getTimeInstance(DateFormat.LONG);
			DateFormat df=DateFormat.getDateInstance(DateFormat.FULL);
			line=df.format(now)+" "+tf.format(now)+" "+message+"\n";
			file.write(line);
			file.flush();
			file.close();
		} catch (Exception e){
			System.out.println("\nError : Unable to create the file debug.txt");
			System.out.println(e.toString());
		}
		
	}
	
	
	class WindowHandler extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
		}
	}
	
}
