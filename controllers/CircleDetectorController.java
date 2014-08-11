package controllers;

import geometry.Circle;
import geometry.Coordinate2D;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import utils.Constants;
import utils.Tuple;
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
//		houghspaceMonitor.writeDataToFile("/home/harry/tmp/out_raw.csv");
		
		// Create the houghspace image
		{		
			Map<Coordinate2D, Map<Integer, MutableInt>> rawData = houghspaceMonitor.getRawData();
			for(Map.Entry<Coordinate2D, Map<Integer, MutableInt>> coordEntry : rawData.entrySet())
			{
				Coordinate2D c = coordEntry.getKey();
				int aggregatedIntensity = 0;
				for(Map.Entry<Integer, MutableInt> radiusEntry : coordEntry.getValue().entrySet())
				{
					aggregatedIntensity += radiusEntry.getValue().get();
				}
				houshSpaceImgRaster.setPixel(c.getX(), c.getY(), new int[]{aggregatedIntensity});
			}
		}
		
		// Perform extra filtering on the sorted Data
		{
			CircleCounterList sortedData = houghspaceMonitor.getSortedData();
			houghspaceMonitor.writeSortedDataToFile("/home/harry/tmp/out_sorted_all.csv");
			
			// First take only values larger than one standard deviation
			{
				double mean = sortedData.getMean();
				double standardDeviation = sortedData.getStandardDeviation(mean);
				double threshold = mean + standardDeviation;
				
				System.out.println("First statistical filter threshold: " + threshold);
				// Perform deletion
				System.out.println("Pre statistical deletion size: " + sortedData.size());
				{
					int i = 0;
					while(true)
					{
						if(sortedData.get(i).getCount() < threshold)
						{
							sortedData = new CircleCounterList(sortedData.subList(0, i));
							break;
						}
						i++;
					}
				}
				System.out.println("Post statistical deletion size: " + sortedData.size());
				sortedData.writeToFile("/home/harry/tmp/out_sorted_stat_filtered.csv");
			}
			
			// Now remove similar circles
			CircleCounterList uniqueCoordinates = new CircleCounterList();
			{
				Set<Coordinate2D> coordinatesToRemove = new HashSet<Coordinate2D>();
				Iterator<CircleCounter> sortedCoordinatesIt = sortedData.iterator();
				while(sortedCoordinatesIt.hasNext())
				{
					CircleCounter entry = sortedCoordinatesIt.next();
					Circle circle = entry.getCircle();
					Coordinate2D origin = circle.getOrigin();
					
					if(!coordinatesToRemove.contains(circle.getOrigin()))
					{
						int radius = circle.getRadius();
						int count = entry.getCount();
						uniqueCoordinates.add(new CircleCounter(circle, count));
						for(int y = Math.max(0, origin.getY() - radius); y < Math.min(imgHeight, origin.getY() + radius); y++)
						{
							for(int x = Math.max(0, origin.getX() - radius); x < Math.min(imgWidth, origin.getX() + radius); x++)
							{
								Coordinate2D coordToSkip = new Coordinate2D(x, y);
								if(coordToSkip != origin)
									coordinatesToRemove.add(coordToSkip);
							}
						}
					}
				}
				System.out.println("Post similar coordinate deletion size: " + uniqueCoordinates.size());
				uniqueCoordinates.writeToFile("/home/harry/tmp/out_sorted_similarities_filtered.csv");
			}
			
			// Again, take only values larger than one standard deviation
			{
				double mean = uniqueCoordinates.getMean();
				double standardDeviation = uniqueCoordinates.getStandardDeviation(mean);
				double threshold = mean + standardDeviation;
				System.out.println("Second statistical filter threshold: " + threshold);
				// Perform deletion
				System.out.println("Pre second statistical deletion size: " + uniqueCoordinates.size());
				{
					int i = 0;
					while(true)
					{
						if(uniqueCoordinates.get(i).getCount() < threshold)
						{
							uniqueCoordinates = new CircleCounterList(uniqueCoordinates.subList(0, i));
							break;
						}
						i++;
					}
				}
				System.out.println("Post second statistical deletion size: " + uniqueCoordinates.size());
				uniqueCoordinates.writeToFile("/home/harry/tmp/out_sorted_stat2_filtered.csv");	
			}
				
			for(CircleCounter cc : uniqueCoordinates)
			{
				System.out.println(cc);
				drawCircle(cc.getCircle(), detectedCirclesImgRaster);
			}
		}

		updateDisplay();
	}
	
	private void drawCircle(Circle circle, WritableRaster imgRaster)
	{
		double pointsToPlot = TWO_PI * circle.getRadius();
		double angleIncrement = TWO_PI/pointsToPlot;
		int width = imgRaster.getWidth();
		int height = imgRaster.getHeight();
			
		for(double angle = 0.; angle < TWO_PI; angle += angleIncrement)
		{
			int x = (int) (circle.getOrigin().getX() + (circle.getRadius() * Math.cos(angle)));
			int y = (int) (circle.getOrigin().getY() + (circle.getRadius() * Math.sin(angle)));

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
		Map<Coordinate2D, Map<Integer, MutableInt>> data;
		CircleCounterList sortedData;
		boolean sortRequired;
		
		public HoughSpaceMonitor()
		{
			data = new HashMap<Coordinate2D, Map<Integer, MutableInt>>();
			sortRequired = false;
		}
		
		private void increment(int x, int y, int r)
		{
			Coordinate2D c = new Coordinate2D(x,y);
			Map<Integer, MutableInt> coordinateData = data.get(c);
			
			if(coordinateData == null)
			{
				data.put(c, new HashMap<Integer, MutableInt>());
				coordinateData = data.get(c);
			}

			MutableInt count = coordinateData.get(r);
				
			if(count == null)
				coordinateData.put(r, new MutableInt());
			else
				count.increment();
		}
		
		public void addNewCircleData(int origX, int origY, int imgWidth, int imgHeight, int fromRadius, int toRadius)
		{
			sortRequired = true;
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
						increment(x, y, radius);
				}
			}
		}
		
		private void filterAndSort()
		{
			// First for each coordinate, get the most probable circle			
			Map<Coordinate2D, Map.Entry<Integer, MutableInt>> topRadiusPerCoord = new HashMap<Coordinate2D, Map.Entry<Integer, MutableInt>>();
			{
				for(Map.Entry<Coordinate2D, Map<Integer, MutableInt>> entry : data.entrySet())
				{
					List<Map.Entry<Integer, MutableInt>> sortedEntry = new ArrayList<Map.Entry<Integer, MutableInt>>(entry.getValue().entrySet());
					
					Collections.sort(sortedEntry, new Comparator<Map.Entry<Integer, MutableInt>>() {
						@Override
						public int compare(Entry<Integer, MutableInt> o1,
								Entry<Integer, MutableInt> o2) {
							return (o1.getValue().get() - o2.getValue().get());
						}
					});
					Collections.reverse(sortedEntry);				
					topRadiusPerCoord.put(entry.getKey(), sortedEntry.get(0));
				}
			}
			
			// Now sort the coordinates
			List<Map.Entry<Coordinate2D, Map.Entry<Integer, MutableInt>>> sortedCoord = new ArrayList<Map.Entry<Coordinate2D, Map.Entry<Integer, MutableInt>>>(topRadiusPerCoord.entrySet());
			{
				Collections.sort(sortedCoord, new Comparator<Map.Entry<Coordinate2D, Map.Entry<Integer, MutableInt>>>() {

					@Override
					public int compare(
							Entry<Coordinate2D, Entry<Integer, MutableInt>> o1,
							Entry<Coordinate2D, Entry<Integer, MutableInt>> o2) {
						return (o1.getValue().getValue().get() - o2.getValue().getValue().get());
					}
				});
				Collections.reverse(sortedCoord);		
			}

			
			CircleCounterList formattedSorted = new CircleCounterList();		
			for(Map.Entry<Coordinate2D, Map.Entry<Integer, MutableInt>> e : sortedCoord)
			{
				Coordinate2D origin = e.getKey();
				int radius = e.getValue().getKey();
				int count = e.getValue().getValue().get();
				
				formattedSorted.add(new CircleCounter(new Circle(origin, radius), count));
			}

			
			sortedData = formattedSorted;
			
			sortRequired = false;
		}
		
		public CircleCounterList getSortedData()
		{
			if(sortedData == null || sortRequired)
				filterAndSort();
			
			return sortedData;
		}
		
		public Map<Coordinate2D, Map<Integer, MutableInt>> getRawData()
		{
			return data;
		}
		
		public void writeRawDataToFile(String filename)
		{	
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), false));
				bw.write("X,Y,R,Count");
				bw.newLine();
				for(Map.Entry<Coordinate2D, Map<Integer, MutableInt>> coordEntry : data.entrySet())
				{
					Coordinate2D c = coordEntry.getKey();
					for(Map.Entry<Integer, MutableInt> radiusEntry : coordEntry.getValue().entrySet())
					{
						bw.write(c.getX() + "," + c.getY() + "," + radiusEntry.getKey() + "," + radiusEntry.getValue().get());
						bw.newLine();
					}
				}
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void writeSortedDataToFile(String filename)
		{	
			getSortedData().writeToFile(filename);
		}
	}
	
	private class CircleCounter extends Tuple<Circle, Integer>
	{		
		CircleCounter(Circle c, Integer counter)
		{
			super(c, counter);
		}
		
		public Circle getCircle()
		{
			return getKey();
		}
		
		public Integer getCount()
		{
			return getValue();
		}
		
		@Override
		public String toString()
		{
			return (getKey() + " --> " + getCount());
		}
	}
	
	private class CircleCounterList extends ArrayList<CircleCounter>
	{	
		public CircleCounterList()
		{
			super();
		}
		
		public CircleCounterList(List<CircleCounter> initList)
		{
			super(initList);
		}
		
		public double getMean()
		{
			int totalCoordinates = size();
			double mean = 0.;
			
			for(CircleCounter cc : this)
					mean += cc.getCount();

			return (mean /= totalCoordinates);
		}
		
		public double getStandardDeviation()
		{
			return getStandardDeviation(getMean());
		}
		
		public double getStandardDeviation(double mean)
		{
			int totalCoordinates = size();
			double variance = 0.;		
			// Variance
			for(CircleCounter cc : this)
				variance += Math.pow(cc.getCount() - mean, 2);
			variance /= totalCoordinates;
			return Math.sqrt(variance);
		}
		
		public void writeToFile(String filename)
		{
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), false));
				bw.write("X,Y,R,Count");
				bw.newLine();
				for(CircleCounter cc : this)
				{
					Coordinate2D coord = cc.getCircle().getOrigin();
					Integer radius = cc.getCircle().getRadius();
					Integer count = cc.getCount();
					bw.write(coord.getX() + "," + coord.getY() + "," + radius + "," + count);
					bw.newLine();
				}
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}