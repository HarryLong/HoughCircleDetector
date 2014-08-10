package controllers;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import utils.Constants;
import utils.image.ImageUtils;

public class CircleDetectorController extends BaseController implements Observer, CircleDetector{
	public static enum ImageType {
		HOUGH_SPACE("Hough Space"),
		CIRCLE_DETECTED("Detected Circles");
		
		String name;
		ImageType(String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
	}
	
	private BufferedImage baseImg;
	private BufferedImage inputImg;
	private BufferedImage houghSpaceImg;
	private BufferedImage detectedCirclesImg;
	private ImageType currentDisplay;

	private static final double TWO_PI = 2 * Math.PI;
	private static final int[] PIXEL_MAX_INTENSITY = new int[]{255};
	
	private HoughSpaceMonitor houghspaceMonitor;
	
	@Override
	public void update(Observable o, Object updateObj) {
		if(o instanceof SharpeningController)
		{
			System.out.println("Edge detected image set");
			this.inputImg = (BufferedImage) updateObj;
		}
		else
		{
			System.out.println("Base image set");
			this.baseImg = (BufferedImage) updateObj;
		}
	}

	@Override
	public void showHoughSpaceImg() {
		display(ImageType.HOUGH_SPACE);		
	}

	@Override
	public void showDetectedCirclesImg() {
		display(ImageType.CIRCLE_DETECTED);		
	}

	private void display(ImageType imageType)
	{
		currentDisplay = imageType;
		if(imageType == ImageType.CIRCLE_DETECTED)
		{
			setImage(detectedCirclesImg);		
		}
		else
		{
			setImage(houghSpaceImg);		
		}	
	}
	
	private void updateDisplay()
	{
		display(currentDisplay);
	}
	
	@Override
	public void detectCircles(int maxRadius)
	{
		if(baseImg == null || inputImg == null)
		{
			System.err.println("Unable to detect circles. Image missing...");
			return;
		}
		int imgWidth = inputImg.getWidth();
		int imgHeight = inputImg.getHeight();
		
		houghspaceMonitor = new HoughSpaceMonitor(); // Reinitialise the houghSpaceMonitor TODO: Rather have a reset method
		
		houghSpaceImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster houshSpaceImgRaster = houghSpaceImg.getRaster();
		
		detectedCirclesImg = ImageUtils.deepCopy(baseImg);
		
		int[] pixelIntensity = new int[1];
//		System.out.println("Image: [X: " + inputImg.getHeight() + ", Y:" + inputImg.getWidth() + "]");
		Raster inputImgRaster = inputImg.getRaster();
		WritableRaster detectedCirclesImgRaster = detectedCirclesImg.getRaster();
		for(int y = 0; y < inputImg.getHeight(); y++)
		{
			for(int x = 0; x < inputImg.getWidth(); x++)
			{
				pixelIntensity = inputImgRaster.getPixel(x, y, pixelIntensity);
				if(pixelIntensity[0] > Constants.MIN_PIXEL_INTENSITY_THRESHOLD)
				{
					System.out.println("X: " + x + " | Y: " + y);
					houghspaceMonitor.addNewCircleData(x, y, imgWidth, imgHeight, Constants.MIN_CIRCLE_RADIUS, maxRadius);
				}
			}
		}	
		houghspaceMonitor.writeDataToFile("/home/harry/tmp/out.csv");
		
		// Now create the houghspace image TODO: Improve
		List<Map.Entry<PlottedCircleData, MutableInt>> sortedData = houghspaceMonitor.getSortedData();
//		Map<Integer, PlottedCircleData> rawData = houghspaceMonitor.getRawData();
//		
//		for(Map.Entry<Integer, PlottedCircleData> entry : rawData.entrySet())
//		{
//			PlottedCircleData pcd = entry.getValue();
//			houshSpaceImgRaster.setPixel(pcd.x, pcd.y, PIXEL_MAX_INTENSITY);
//		}
		
		int[] pIntensity = new int[1];
		for(Map.Entry<PlottedCircleData, MutableInt> entry : sortedData)
		{
			PlottedCircleData pcd = entry.getKey();
			pIntensity = houshSpaceImgRaster.getPixel(pcd.x, pcd.y, pIntensity);
			pIntensity[0] = Math.min(pIntensity[0] + 2, 255);
			houshSpaceImgRaster.setPixel(pcd.x, pcd.y, pIntensity);
		}
		
		for(int i = 0; i < 100; i++)
		{
			System.out.println(i + " : " + sortedData.get(i).getValue().get());
			drawCircle(sortedData.get(i).getKey(), detectedCirclesImgRaster);
		}

		updateDisplay();
	}
	
	private void drawCircle(PlottedCircleData circleSpecs, WritableRaster imgRaster)
	{
		double pointsToPlot = TWO_PI * circleSpecs.r;
		double angleIncrement = TWO_PI/pointsToPlot;
		int width = imgRaster.getWidth();
		int height = imgRaster.getHeight();
			
		for(double angle = 0.; angle < TWO_PI; angle += angleIncrement)
		{
			int x = (int) (circleSpecs.x + (circleSpecs.r * Math.cos(angle)));
			int y = (int) (circleSpecs.y + (circleSpecs.r * Math.sin(angle)));

			if(x >= 0 && x < width && y >= 0 && y < height)
				imgRaster.setPixel(x, y, PIXEL_MAX_INTENSITY);
		}
	}
	
	// Important to set as used when displayed in the combo box
	@Override
	public String toString() {
		return "Circle Detection";
	}
	
	private class MutableInt {
		int value = 1; // note that we start at 1 since we're counting
		public void increment () { ++value;      }
		public int  get ()       { return value; }
	}
	
	private class HoughSpaceMonitor
	{
		Map<PlottedCircleData, MutableInt> data;
		
		public HoughSpaceMonitor()
		{
			data = new HashMap<PlottedCircleData, MutableInt>();
		}
		
		private void increment(PlottedCircleData circleSpecs)
		{
			MutableInt count = data.get(circleSpecs);
			
			if(count == null)
				data.put(circleSpecs, new MutableInt());
			else
				count.increment();
		}
		
		public void addNewCircleData(int origX, int origY, int imgWidth, int imgHeight, int fromRadius, int toRadius)
		{
			double pointsToPlot, angleIncrement;
			for(int radius = fromRadius; radius <= toRadius; radius++)
			{
				pointsToPlot = TWO_PI * radius;
				angleIncrement = TWO_PI/pointsToPlot;
				
				for(double angle = 0.; angle < TWO_PI; angle += angleIncrement)
				{
					int x = (int) (origX + (radius * Math.cos(angle)));
					int y = (int) (origY + (radius * Math.sin(angle)));

					if(x >= 0 && x < imgWidth && y >= 0 && y < imgHeight)
						increment(new PlottedCircleData(x, y, radius));
				}
			}
		}
		
		public void addNewCircleData(int origX, int origY, int imgWidth, int imgHeight, int fromRadius, int toRadius, WritableRaster raster)
		{
			double pointsToPlot, angleIncrement;
			for(int radius = fromRadius; radius <= toRadius; radius += 5)
			{
				pointsToPlot = TWO_PI * radius;
				angleIncrement = TWO_PI/pointsToPlot;
				
				for(double angle = 0.; angle < TWO_PI; angle += angleIncrement)
				{
					int x = (int) (origX + (radius * Math.cos(angle)));
					int y = (int) (origY + (radius * Math.sin(angle)));

					if(x >= 0 && x < imgWidth && y >= 0 && y < imgHeight)
					{
						increment(new PlottedCircleData(x, y, radius));
						raster.setPixel(x, y, new int[]{255});
					}
				}
			}
		}
		
		public List<Map.Entry<PlottedCircleData, MutableInt>> getSortedData()
		{
			List<Map.Entry<PlottedCircleData, MutableInt>> sortedData = new ArrayList<Map.Entry<PlottedCircleData, MutableInt>>(data.entrySet());
			Collections.sort(sortedData, new Comparator<Map.Entry<PlottedCircleData, MutableInt>>() {
				@Override
				public int compare(Entry<PlottedCircleData, MutableInt> o1,
						Entry<PlottedCircleData, MutableInt> o2) {
					return (o1.getValue().get() - o2.getValue().get());
				}
			});
			Collections.reverse(sortedData);
			return sortedData;
		}
		
		public Map<PlottedCircleData, MutableInt> getRawData()
		{
			return data;
		}
		
		public void writeDataToFile(String filename)
		{			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), false));
				bw.write("X,Y,R,Count");
				bw.newLine();
				for(Map.Entry<PlottedCircleData, MutableInt> entry : data.entrySet())
				{
					PlottedCircleData c = entry.getKey();
					bw.write(c.x + "," + c.y + "," + c.r + "," + entry.getValue().get());
					bw.newLine();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public class PlottedCircleData{
		int x, y, r;
		
		PlottedCircleData(int x, int y, int r)
		{
			this.x = x;
			this.y = y;
			this.r = r;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + r;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PlottedCircleData other = (PlottedCircleData) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (r != other.r)
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
		
		private CircleDetectorController getOuterType() {
			return CircleDetectorController.this;
		}
		
		@Override
		public String toString()
		{
			return ("[" + x + "," + y + "," + r + "]");
		} 
	}
}
