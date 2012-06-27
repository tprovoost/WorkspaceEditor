package plugins.tprovoost.workspaceeditor;

import icy.gui.component.IcyTextField;
import icy.resource.ResourceUtil;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

public class TextFieldFilter extends IcyTextField {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Image img = ResourceUtil.getAlphaIconAsImage("zoom");

	public TextFieldFilter() {
	}

	public TextFieldFilter(String name) {
		super(name);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		int w = getWidth();
		int h = getHeight();
 
		// set rendering presets
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
		
		// draw images
		g2.drawImage(img, w - h, 5, h - 10, h - 10, null);
		g2.dispose();
	}
}
