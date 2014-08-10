package controllers;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import utils.image.ImageUtils;


public class GreyscaleController extends BaseController implements Observer{
	
	@Override
	public void update(Observable o, Object updateObj) {
		BufferedImage img = (BufferedImage) updateObj;
		BufferedImage greyScaleImg = ImageUtils.convertToGreyscale(img);
		setImage(greyScaleImg);
	}
	
	// Important to set as used when displayed in the combo box
	@Override
	public String toString()
	{
		return "Greyscale";
	}
}
