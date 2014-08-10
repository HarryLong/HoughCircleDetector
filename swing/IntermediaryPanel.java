package swing;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import controllers.BaseController;


public class IntermediaryPanel extends JPanel implements ActionListener {
		
	private static final long serialVersionUID = -5982293564636458941L;
	private static final float HEADING_RATIO = .05f;
	private static final float COMBOBOX_RATIO = .05f;
	private static final float IMAGEPANEL_RATIO = .9f;
	private static final String HEADING = "Select intermediary image: ";
	
	JTextField headingTF;
	ImagePanel imgPanel;
	JComboBox<BaseController> selectionBox;
	BaseController currentController;
	
	public IntermediaryPanel(BaseController[] intermediaryControllers)
	{
		imgPanel = new ImagePanel();
		headingTF = new HeadingTF(HEADING);
		
		selectionBox = new JComboBox<BaseController>();
		for(BaseController c : intermediaryControllers)
		{
			selectionBox.addItem(c);
		}
		
		selectionBox.addActionListener(this);
		
		initLayout();
		
		setController(selectionBox.getSelectedItem()); // Set the first controller
	}
	
	public void initLayout()
	{
		setLayout(new GridBagLayout());		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.weightx = 1;

		// Add the heading
		c.gridy = 0;
		c.weighty = HEADING_RATIO;
		add(headingTF, c);
		
		// Add the combo-box
		c.gridy = 1;
		c.weighty = COMBOBOX_RATIO;
		add(selectionBox, c);
		
		// Add the image panel
		c.gridy = 3;
		c.weighty = IMAGEPANEL_RATIO;
		add(imgPanel, c);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		setController(((JComboBox<BaseController>) e.getSource()).getSelectedItem());
	}
	
	private void setController(Object selectedItem)
	{
		BaseController selectedController = (BaseController) selectedItem;

		if(currentController != null)
			currentController.deleteObserver(imgPanel);
		
		currentController = selectedController;
		
		currentController.addObserver(imgPanel);
		imgPanel.update(currentController, currentController.getImage());
	}
	
	private class HeadingTF extends JTextField
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1672473597744349515L;

		public HeadingTF(String heading)
		{
			super(heading);
			setEditable(false);
			setHorizontalAlignment(JTextField.CENTER);
			setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
			setBorder(null);
		}
	}
}
