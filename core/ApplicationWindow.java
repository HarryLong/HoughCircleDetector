package core;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import swing.CircleDetectionPanel;
import swing.ImageFileChooser;
import swing.ImagePanel;
import swing.IntermediaryPanel;
import utils.Constants;
import controllers.ControllerManager;

public class ApplicationWindow extends JFrame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6340818753464515771L;
	ImageFileChooser imgFileChooser;
	
	ControllerManager cntrlManager;
	
	ImagePanel origImgPanel;
	IntermediaryPanel intermediaryImagesPanel;
	CircleDetectionPanel circleDetectionPanel;
	
	public ApplicationWindow()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle(Constants.TITLE);
		setSize(new Dimension(900, 900));
		setLocationRelativeTo(null);
	
		initMenu();
		
		imgFileChooser = new ImageFileChooser();
		cntrlManager = new ControllerManager();
		
		// Panels
		origImgPanel = new ImagePanel("Original Image");
		intermediaryImagesPanel = new IntermediaryPanel(cntrlManager.getIntermediaryControllers());
		circleDetectionPanel = new CircleDetectionPanel(cntrlManager.getCircleDetectionController());

		// Attach panels to controllers
		cntrlManager.attachToEntry(origImgPanel);
		cntrlManager.attachToExit(circleDetectionPanel.getImgPanel());
		
		initLayout();
		
//		pack();
		setVisible(true);
	}
	
	private void initLayout()
	{
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 0;
		c.weightx = 0.33;
		c.weighty = 0.75;
		
		// Base image
		c.gridx = 0;
		add(origImgPanel, c);
		
		c.gridx = 1;
		add(intermediaryImagesPanel, c);
		
		c.gridx = 2;
		add(circleDetectionPanel, c);
	}
	
	private void initMenu()
	{
		// Main menu
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		
		JMenuItem loadImgMI = new JMenuItem("Load...");
		loadImgMI.setActionCommand(ActionCommands.LOAD_IMAGE);
		loadImgMI.addActionListener(this);
		
		JMenuItem exitMI = new JMenuItem("Exit");
		exitMI.setActionCommand(ActionCommands.EXIT);
		exitMI.addActionListener(this);
		
		fileMenu.add(loadImgMI);
		fileMenu.addSeparator();
		fileMenu.add(exitMI);
		
		menuBar.add(fileMenu);
		
		setJMenuBar(menuBar);
	}
	
	public static void main(String[] args)
	{
		new ApplicationWindow();
	}
	
	private class ActionCommands{
		public static final String LOAD_IMAGE = "LOAD_IMAGE";
		public static final String EXIT = "EXIT";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch(e.getActionCommand())
		{
		case ActionCommands.LOAD_IMAGE:
			loadImg();
			break;
		case ActionCommands.EXIT:
			exit();
			break;
		}
	}
	
	public void exit()
	{
		int ret = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?", null, JOptionPane.YES_NO_OPTION);
		
		if(ret == JOptionPane.YES_OPTION)
			dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));			
	}
	
	public void loadImg()
	{
		int returnVal = imgFileChooser.showOpenDialog(this);
		if(returnVal == ImageFileChooser.APPROVE_OPTION)
		{
			try {
				cntrlManager.setOriginalImage(ImageIO.read(imgFileChooser.getSelectedFile()));
			} catch (IOException e) {
				System.err.println("Unable to load image file " + imgFileChooser.getSelectedFile().getName());
			}
		}
	}
}
