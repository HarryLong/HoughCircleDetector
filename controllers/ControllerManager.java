package controllers;

import java.awt.image.BufferedImage;
import java.util.Observer;


public class ControllerManager {
	BaseController baseImgeCntrl; // Base
	
	// Intermediary Images
	GreyscaleController greyScaleCntrl; 
	SmoothingController smooothenCntrl; 
	SharpeningController sharpenCntrl; 
	EdgeDetectionController edgeDetectionCntrl;
	CircleDetectorController circleDetectionCntrl;
		
	public ControllerManager()
	{
		// Original image controller
		baseImgeCntrl = new BaseController();
		
		// Intermediary controllers
		greyScaleCntrl = new GreyscaleController();
		smooothenCntrl = new SmoothingController(); 
		sharpenCntrl = new SharpeningController();
		edgeDetectionCntrl = new EdgeDetectionController();
		
		// Final image
		circleDetectionCntrl = new CircleDetectorController();
		
		// Attach observers
		baseImgeCntrl.addObserver(greyScaleCntrl);
		greyScaleCntrl.addObserver(smooothenCntrl);
		greyScaleCntrl.addObserver(circleDetectionCntrl);
		smooothenCntrl.addObserver(edgeDetectionCntrl);
		edgeDetectionCntrl.addObserver(sharpenCntrl);
		sharpenCntrl.addObserver(circleDetectionCntrl);
	}
	
	public void attachToEntry(Observer o)
	{
		baseImgeCntrl.addObserver(o);
	}
	
	public void attachToExit(Observer o)
	{
		circleDetectionCntrl.addObserver(o);
	}
	
	public void setOriginalImage(BufferedImage img)
	{
		baseImgeCntrl.setImage(img);
	}
	
	public BaseController[] getIntermediaryControllers()
	{
		// Order is important!
		return new BaseController[]{greyScaleCntrl, smooothenCntrl, edgeDetectionCntrl, sharpenCntrl};
	}
	
	public CircleDetectorController getCircleDetectionController()
	{
		return circleDetectionCntrl;
	}
}
