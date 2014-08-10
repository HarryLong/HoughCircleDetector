package controllers;
import java.awt.image.BufferedImage;
import java.util.Observable;


public class BaseController extends Observable{
	private BufferedImage img;
	
	public void setImage(BufferedImage img)
	{
		this.img = img;
		setChanged();
		notifyObservers(img);
	}
	
	public BufferedImage getImage()
	{
		return img;
	}
	
	@Override
	public String toString()
	{
		return "Original";
	}
}
