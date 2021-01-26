import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

// A status bar class //
public class JStatusBar extends JPanel {
	public static final long serialVersionUID = 1;
	private BorderLayout borderLayoutStatusBar = new BorderLayout();
	private BorderLayout borderLayoutStatusBarLeft = new BorderLayout();
	private BorderLayout borderLayoutStatusBarRight = new BorderLayout();

	private JPanel jStatusBar1 = new JPanel();
	private JPanel jStatusBarLeftPanel = new JPanel();
	private JPanel jStatusBarRightPanel = new JPanel();
	private JLabel statusBar = new JLabel();

	// Border style definitions
//	private Border raisedbevel = BorderFactory.createRaisedBevelBorder();
	private Border loweredbevel = BorderFactory.createLoweredBevelBorder();
//	private Border compound = BorderFactory.createCompoundBorder(raisedbevel, loweredbevel);

	public JStatusBar() {
		jStatusBar1.setLayout(borderLayoutStatusBar);
		jStatusBarLeftPanel.setLayout(borderLayoutStatusBarLeft);
		jStatusBarLeftPanel.setBorder(loweredbevel);
		jStatusBarRightPanel.setLayout(borderLayoutStatusBarRight);
		jStatusBar1.add(jStatusBarLeftPanel, BorderLayout.CENTER);
		jStatusBar1.add(jStatusBarRightPanel, BorderLayout.EAST);
		jStatusBarLeftPanel.add(statusBar, BorderLayout.CENTER);
		jStatusBar1.updateUI();
		statusBar.updateUI();
		// Ensure the elements of the status bar are displayed from the left
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.add(jStatusBar1, BorderLayout.CENTER);
	}

	public void setText(String text) {
		statusBar.setText(text);
	}

}
