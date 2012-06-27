package plugins.tprovoost.workspaceeditor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPopupMenu;

public class TitledPopupMenu extends JPopupMenu {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5086816009462968371L;

	public TitledPopupMenu(String name) {
		super(name);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		super.paintComponent(g2);
	}
	
}
