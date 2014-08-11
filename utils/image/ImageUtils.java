package utils.image;
import geometry.Circle;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import utils.Constants;


public class ImageUtils {
	
	public static Image scaleImage(Image inputImg, int maxWidth, int maxHeight)
	{
		int imgWidth = inputImg.getWidth(null);
		int imgHeight = inputImg.getHeight(null);
		
    	if(imgWidth > maxWidth)
    	{
    		imgHeight = (imgHeight * maxWidth) / imgWidth;
    		imgWidth = maxWidth;
    	}
    	
    	if(imgHeight > maxHeight)
    	{
    		imgWidth = (imgWidth * maxHeight) / imgHeight;
    		imgHeight = maxHeight;
    	}        	
    	
    	if(imgWidth != inputImg.getWidth(null) || imgHeight != inputImg.getHeight(null))
    		return inputImg.getScaledInstance(imgWidth, imgHeight, Image.SCALE_SMOOTH);
    	else
    		return inputImg;
	}
	
	public static BufferedImage convertToGreyscale(BufferedImage inputImg)
	{		
		int imgWidth = inputImg.getWidth(null);
		int imgHeight = inputImg.getHeight(null);
		
		BufferedImage greyScaleImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
		
		WritableRaster raster=greyScaleImg.getRaster();
		int[] pixel = new int[1];
		for(int y = 0; y < imgHeight; y++)
		{
			for(int x = 0; x < imgWidth; x++)
			{
				int avgRGB = getAvgRGB(inputImg.getRGB(x, y));// + " | ");
				pixel[0] = avgRGB;
				raster.setPixel(x, y, pixel);
			}
		}
		return greyScaleImg;
	}
	
	public static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	public static void drawCircle(Circle circle, WritableRaster imgRaster)
	{
		double perimeter = circle.getPerimeter();
		double angleIncrement = Constants.TWO_PI/perimeter;
		int width = imgRaster.getWidth();
		int height = imgRaster.getHeight();
			
		for(double angle = 0.; angle < Constants.TWO_PI; angle += angleIncrement)
		{
			int x = (int) (circle.getOrigin().getX() + (circle.getRadius() * Math.cos(angle)));
			int y = (int) (circle.getOrigin().getY() + (circle.getRadius() * Math.sin(angle)));

			if(x >= 0 && x < width && y >= 0 && y < height)
				imgRaster.setPixel(x, y, Constants.PIXEL_MAX_INTENSITY);
		}
	}
	
	static int getAvgRGB(int intRGB)
	{
		RGB rgb = new RGB(intRGB);
		
		return (int) ((rgb.getRed()/3.f) + (rgb.getGreen()/3.f) + (rgb.getBlue()/3.f));
	}
	
	private static class RGB
	{
		private int a, r,g,b;
		
		RGB(int intRGB)
		{
			a = (intRGB >> 24) & 0xFF;	
			r = (intRGB >> 16) & 0xFF;	
			g = (intRGB >> 8) & 0xFF;	
			b = (intRGB >> 0) & 0xFF;
		}
		
		RGB(int r, int g, int b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		public int toIntRGB()
		{
			return ((r&0x0ff)<<16)|((g&0x0ff)<<8)|(b&0x0ff);
		}
		
		public int getRed()
		{
			return r;
		}
		
		public int getGreen()
		{
			return g;
		}
		
		public int getBlue()
		{
			return b;
		}
	}
}
