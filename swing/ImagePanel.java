package swing;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.image.ImageUtils;


public class ImagePanel extends JPanel implements Observer{
		
	private static final long serialVersionUID = -5982293564636458941L;
	private static final float HEADING_RATIO = 0.05f;
	private static final float IMAGE_RATIO = 0.95f;
	
	JTextField headingTF;
	ImageHolder imgHolder;
	
	public ImagePanel(String heading)
	{
		this.imgHolder = new ImageHolder();
		headingTF = new HeadingTF(heading);		
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		initLayout(true);

		setVisible(true);
	}
	
	public ImagePanel()
	{
		this.imgHolder = new ImageHolder();
		initLayout(false);
	}
	
	public void initLayout(boolean heading)
	{	
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;		
		c.gridy = 0;

		if(heading)
		{
			// Heading 
			c.weighty = HEADING_RATIO;
			add(headingTF, c);
		}

		// Image
		if(heading)
		{
			c.gridy = 1;
			c.weighty = IMAGE_RATIO;
		}
		else
		{
			c.weighty = 1;
		}
		
		add(imgHolder, c);

		setVisible(true);
	}

	@Override
	public void update(Observable arg0, Object updateObj) {
		imgHolder.setImage((Image) updateObj);
	}
	
	private class ImageHolder extends JPanel 
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -2646935184560753570L;
		private Image img;

	    @Override
	    public void paintComponent(Graphics g) {
	        super.paintComponent(g);
	        
	        if(img != null)
	        {
	    		int width = getWidth() - 10;
	    		int height = getHeight() - 10;
	    		Image scaledImg = ImageUtils.scaleImage(img, width, height);
	        	int xOffset = Math.max((width- scaledImg.getWidth(null) ) / 2,0);
	        	int yOffset = Math.max((height - scaledImg.getHeight(null)) / 2,0);
	            g.drawImage(scaledImg, xOffset, yOffset, null);
	            
	            // TEST SIZE
//	            g.setColor(Color.red);
//	            g.fillRect(0, 0, width, height);
	        }
	    }

		public void setImage(Image img) {
			this.img = img;
			repaint();		
		}
	}
	
	private class HeadingTF extends JTextField
	{
		public HeadingTF(String heading)
		{
			super(heading);
			setEditable(false);
			setHorizontalAlignment(JTextField.CENTER);
			setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
		}
	}
}
