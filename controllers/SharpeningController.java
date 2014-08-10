package controllers;

import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import utils.image.Filter;
import utils.image.FilterFactory;

public class SharpeningController extends BaseController implements Observer {

	@Override
	public void update(Observable o, Object updateObj) {
		BufferedImage img = (BufferedImage) updateObj;
		Filter filter = FilterFactory.instance().getSharpeningFilter(
				FilterFactory.EDGE_NO_OP);
		setImage(filter.processImage(img));
	}
	
	@Override
	public String toString()
	{
		return "Sharpened";
	}
}
