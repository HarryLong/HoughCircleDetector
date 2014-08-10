package swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import utils.Constants;
import controllers.CircleDetector;
import controllers.CircleDetectorController;

public class CircleDetectionPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -542708943729970740L;
	private static final String HEADING = "Detected Circles: ";
	
	ImagePanel imgPanel;
	CircleDetectorControls circleDetectionControls;
	
	private static final float IMG_PANEL_RATIO = .95f;
	private static final float CONTROLS_RATIO = .05f;
	
	public CircleDetectionPanel(CircleDetector detector)
	{
		imgPanel = new ImagePanel(HEADING);
		circleDetectionControls = new CircleDetectorControls(detector);		
		initLayout();
	}
	
	private void initLayout()
	{
		setLayout(new GridBagLayout());		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.weightx = 1;

		// Image Panel
		c.gridy = 0;
		c.weighty = IMG_PANEL_RATIO;
		add(imgPanel, c);
		
		// Controller
		c.gridy = 1;
		c.weighty = CONTROLS_RATIO;
		add(circleDetectionControls, c);
	}
	
	public ImagePanel getImgPanel()
	{
		return imgPanel;
	}
	
	private class CircleDetectorControls extends JPanel implements ActionListener
	{			
		LabeledSlider radiusSlider;
		JComboBox<String> displayTypeCB;
		JButton confirmBtn;
		CircleDetector detector;
		
		CircleDetectorControls(CircleDetector detector)
		{
			displayTypeCB = new JComboBox<String>(new String[] {CircleDetectorController.ImageType.HOUGH_SPACE.getName(), CircleDetectorController.ImageType.CIRCLE_DETECTED.getName()});
			displayTypeCB.addActionListener(this);
			
			this.detector = detector;
			radiusSlider = new RadiusSlider();			
			
			confirmBtn = new ConfirmBtn();
			confirmBtn.setActionCommand(ActionCommands.START);
			confirmBtn.addActionListener(this);
			
			initLayout();
		}
		
		public void initLayout()
		{	
			setLayout(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.weightx = 1;
			
			// Display combo box
			c.gridy = 0;
			c.weighty = .4f;
			add(displayTypeCB, c);
			
			// Radius slider
			c.gridy = 1;
			c.weighty = .4f;
			add(radiusSlider, c);
			
			// Confirm btn
			c.gridy = 2;
			c.weighty = .2f;
			add(confirmBtn, c);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals(ActionCommands.START)) // Comes from the Start button
			{
				detector.detectCircles(radiusSlider.getValue());
			}
			else // From combobox
			{
				String selectedItem = (String) ((JComboBox<String>) e.getSource()).getSelectedItem();
				if(selectedItem.equals( CircleDetectorController.ImageType.HOUGH_SPACE.getName()))
				{
					detector.showHoughSpaceImg();
				}
				else
					detector.showDetectedCirclesImg();
			}	
		}
		
		private class ActionCommands{
			public static final String START = "START";
		}
		
		
		private class ConfirmBtn extends JButton
		{
			private static final String CONFIRM_BTN_TEXT = "Start";

			public ConfirmBtn()
			{
				super(CONFIRM_BTN_TEXT);
				setHorizontalAlignment(JButton.CENTER);
			}
		}
		
		private class LabeledSlider extends JPanel {
			
			private JSlider slider;
			private JLabel lbl;
			
			LabeledSlider(String lblTxt, int min, int max, int minorSpace, int majorSpace)
			{
				slider = new JSlider(min, max, min + ((max-min)/2));
				slider.setMinorTickSpacing(minorSpace);
				slider.setMajorTickSpacing(majorSpace);
				slider.setPaintTicks(true);
				slider.setPaintLabels(true);
				
				lbl = new JLabel(lblTxt);
				
				initLayout();
			}
			
			private void initLayout()
			{
				setLayout(new GridBagLayout());
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.gridy = 0;
				
				// Lbl
				c.gridx = 0;
				c.weightx = .2f;
				add(lbl, c);
				
				// Slider
				c.gridx = 1;
				c.weightx = .8f;
				add(slider, c);
			}
			
			public int getValue()
			{
				return slider.getValue();
			}
		}
		
		private class RadiusSlider extends LabeledSlider {
			private static final int MIN = Constants.MIN_CIRCLE_RADIUS;
			private static final int MAX = 200;
			private static final int MINOR_SPACE = 1;
			private static final int MAJOR_SPACE = 20;
			private static final String LABEL = "Radius: ";
			
			public RadiusSlider() {
				super(LABEL, MIN, MAX, MINOR_SPACE, MAJOR_SPACE);
			}
		}
	}
}
