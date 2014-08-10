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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

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
//		houghspaceMonitor.writeDataToFile("/home/harry/tmp/out_raw.csv");
		
		// Create the houghspace image
		{		
			Map<Coordinate, Map<Integer, MutableInt>> rawData = houghspaceMonitor.getRawData();
			for(Map.Entry<Coordinate, Map<Integer, MutableInt>> coordEntry : rawData.entrySet())
			{
				Coordinate c = coordEntry.getKey();
				int aggregatedIntensity = 0;
				for(Map.Entry<Integer, MutableInt> radiusEntry : coordEntry.getValue().entrySet())
				{
					aggregatedIntensity += radiusEntry.getValue().get();
				}
				houshSpaceImgRaster.setPixel(c.x, c.y, new int[]{aggregatedIntensity});
			}
		}
		
		// Perform extra filtering on the sorted Data
		{
			List<Tuple<CircleSpecs, Integer>> sortedData = houghspaceMonitor.getSortedData();
			houghspaceMonitor.writeSortedDataToFile("/home/harry/tmp/out_sorted_all.csv");
			
			// First take only values larger than one standard deviation
			{
				double mean = calculateMean(sortedData);
				double standardDeviation = calculateStandardDeviation(sortedData, mean);
				double threshold = mean + standardDeviation;
				
				System.out.println("First statistical filter threshold: " + threshold);
				// Perform deletion
				System.out.println("Pre statistical deletion size: " + sortedData.size());
				{
					int i = 0;
					while(true)
					{
						if(sortedData.get(i).el2 < threshold)
						{
							sortedData = sortedData.subList(0, i);
							break;
						}
						i++;
					}
				}
				System.out.println("Post statistical deletion size: " + sortedData.size());
				CircleDetectorController.writeToFile("/home/harry/tmp/out_sorted_stat_filtered.csv", sortedData);	
			}
			
			// Now remove similar circles
			List<Tuple<CircleSpecs, Integer>> uniqueCoordinates = new ArrayList<Tuple<CircleSpecs, Integer>>();
			{
				Set<Coordinate> coordinatesToRemove = new HashSet<Coordinate>();
				Iterator<Tuple<CircleSpecs, Integer>> sortedCoordinatesIt = sortedData.iterator();
				while(sortedCoordinatesIt.hasNext())
				{
					Tuple<CircleSpecs, Integer> entry = sortedCoordinatesIt.next();
					Coordinate coord = entry.el1.origin;
					
					if(!coordinatesToRemove.contains(coord))
					{
						int radius = entry.el1.radius;
						int count = entry.el2;
						uniqueCoordinates.add(new Tuple<CircleSpecs, Integer>(entry.el1, count));
						for(int y = Math.max(0, coord.y - radius); y < Math.min(imgHeight, coord.y + radius); y++)
						{
							for(int x = Math.max(0, coord.x - radius); x < Math.min(imgWidth, coord.x + radius); x++)
							{
								Coordinate coordToSkip = new Coordinate(x, y);
								if(coordToSkip != coord)
								{
									coordinatesToRemove.add(coordToSkip);
								}
							}
						}
					}
				}
				System.out.println("Post similar coordinate deletion size: " + uniqueCoordinates.size());
				CircleDetectorController.writeToFile("/home/harry/tmp/out_sorted_similarities_filtered.csv", uniqueCoordinates);	
			}
			
			// Again, take only values larger than one standard deviation
			{
				double mean = calculateMean(uniqueCoordinates);
				double standardDeviation = calculateStandardDeviation(sortedData, mean);
				double threshold = mean + standardDeviation;
				System.out.println("Second statistical filter threshold: " + threshold);
				// Perform deletion
				System.out.println("Pre second statistical deletion size: " + uniqueCoordinates.size());
				{
					int i = 0;
					while(true)
					{
						if(uniqueCoordinates.get(i).el2 < threshold)
						{
							uniqueCoordinates = uniqueCoordinates.subList(0, i);
							break;
						}
						i++;
					}
				}
				System.out.println("Post second statistical deletion size: " + uniqueCoordinates.size());
				CircleDetectorController.writeToFile("/home/harry/tmp/out_sorted_stat2_filtered.csv", sortedData);	
			}
				
			for(Tuple<CircleSpecs, Integer> circle : uniqueCoordinates)
			{
				System.out.println(circle.el1 + " --> " + circle.el2);
				drawCircle(circle.el1, detectedCirclesImgRaster);
			}
		}

		updateDisplay();
	}
	
	private void drawCircle(CircleSpecs circleSpecs, WritableRaster imgRaster)
	{
		double pointsToPlot = TWO_PI * circleSpecs.radius;
		double angleIncrement = TWO_PI/pointsToPlot;
		int width = imgRaster.getWidth();
		int height = imgRaster.getHeight();
			
		for(double angle = 0.; angle < TWO_PI; angle += angleIncrement)
		{
			int x = (int) (circleSpecs.origin.x + (circleSpecs.radius * Math.cos(angle)));
			int y = (int) (circleSpecs.origin.y + (circleSpecs.radius * Math.sin(angle)));

			if(x >= 0 && x < width && y >= 0 && y < height)
				imgRaster.setPixel(x, y, PIXEL_MAX_INTENSITY);
		}
	}
	
	public double calculateMean(List<Tuple<CircleSpecs, Integer>> data)
	{
		int totalCoordinates = data.size();
		double mean = 0.;
		
		// Mean
		{
			for(Tuple<CircleSpecs, Integer> entry  : data)
				mean += entry.el2;
		}
		return (mean /= totalCoordinates);
	}
	
	public double calculateStandardDeviation(List<Tuple<CircleSpecs, Integer>> data, double mean)
	{
		int totalCoordinates = data.size();
		double variance = 0.;		
		// Variance
		{
			for(Tuple<CircleSpecs, Integer> entry  : data)
				variance += Math.pow(entry.el2 - mean, 2);
		}
		variance /= totalCoordinates;
		return Math.sqrt(variance);
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
		Map<Coordinate, Map<Integer, MutableInt>> data;
		List<Tuple<CircleSpecs, Integer>> sortedData;
		boolean sortRequired;
		
		public HoughSpaceMonitor()
		{
			data = new HashMap<Coordinate, Map<Integer, MutableInt>>();
			sortRequired = false;
		}
		
		private void increment(int x, int y, int r)
		{
			Coordinate c = new Coordinate(x,y);
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
			Map<Coordinate, Map.Entry<Integer, MutableInt>> topRadiusPerCoord = new HashMap<Coordinate, Map.Entry<Integer, MutableInt>>();
			{
				for(Map.Entry<Coordinate, Map<Integer, MutableInt>> entry : data.entrySet())
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
			List<Map.Entry<Coordinate, Map.Entry<Integer, MutableInt>>> sortedCoord = new ArrayList<Map.Entry<Coordinate, Map.Entry<Integer, MutableInt>>>(topRadiusPerCoord.entrySet());
			{
				Collections.sort(sortedCoord, new Comparator<Map.Entry<Coordinate, Map.Entry<Integer, MutableInt>>>() {

					@Override
					public int compare(
							Entry<Coordinate, Entry<Integer, MutableInt>> o1,
							Entry<Coordinate, Entry<Integer, MutableInt>> o2) {
						return (o1.getValue().getValue().get() - o2.getValue().getValue().get());
					}
				});
				Collections.reverse(sortedCoord);		
			}

			
			List<Tuple<CircleSpecs, Integer>> formattedSorted = new ArrayList<Tuple<CircleSpecs, Integer>>();			
			for(Map.Entry<Coordinate, Map.Entry<Integer, MutableInt>> e : sortedCoord)
			{
				Coordinate origin = e.getKey();
				int radius = e.getValue().getKey();
				int count = e.getValue().getValue().get();
				
				formattedSorted.add(new Tuple<CircleSpecs, Integer>(new CircleSpecs(origin, radius), count));
			}

			
			sortedData = formattedSorted;
			
			sortRequired = false;
		}
		
		public List<Tuple<CircleSpecs, Integer>> getSortedData()
		{
			if(sortedData == null || sortRequired)
				filterAndSort();
			
			return sortedData;
		}
		
//		public List<Map.Entry<PlottedCircleData, MutableInt>> getSortedData()
//		{
//			List<Map.Entry<PlottedCircleData, MutableInt>> sortedData = new ArrayList<Map.Entry<PlottedCircleData, MutableInt>>(data.entrySet());
//			Collections.sort(sortedData, new Comparator<Map.Entry<PlottedCircleData, MutableInt>>() {
//				@Override
//				public int compare(Entry<PlottedCircleData, MutableInt> o1,
//						Entry<PlottedCircleData, MutableInt> o2) {
//					return (o1.getValue().get() - o2.getValue().get());
//				}
//			});
//			Collections.reverse(sortedData);
//			return sortedData;
//		}
		
		public Map<Coordinate, Map<Integer, MutableInt>> getRawData()
		{
			return data;
		}
		
		public void writeRawDataToFile(String filename)
		{	
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), false));
				bw.write("X,Y,R,Count");
				bw.newLine();
				for(Map.Entry<Coordinate, Map<Integer, MutableInt>> coordEntry : data.entrySet())
				{
					Coordinate c = coordEntry.getKey();
					for(Map.Entry<Integer, MutableInt> radiusEntry : coordEntry.getValue().entrySet())
					{
						bw.write(c.x + "," + c.y + "," + radiusEntry.getKey() + "," + radiusEntry.getValue().get());
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
			CircleDetectorController.writeToFile(filename, getSortedData());
		}
	}
	
	public class Tuple<T1,T2>
	{
		T1 el1;
		T2 el2;
		
		Tuple(T1 el1, T2 el2)
		{
			this.el1 = el1;
			this.el2 = el2;
		}
	}
	
	public class Coordinate
	{
		private int x, y;
		
		private Coordinate(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
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
			Coordinate other = (Coordinate) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}

		@Override
		public String toString()
		{
			return ("[" + x + "," + y + "]" );
		}

		private CircleDetectorController getOuterType() {
			return CircleDetectorController.this;
		}
	}
	
	public class CircleSpecs
	{
		Coordinate origin;
		int radius;
		
		public CircleSpecs(Coordinate origin, int radius) {
			this.origin = origin;
			this.radius = radius;
		}
		
		@Override
		public String toString()
		{
			return ("[" + origin.toString() + "," + radius + "]" );
		}
	}
	
	public static void writeToFile(String filename, List<Tuple<CircleSpecs, Integer>> data)
	{
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), false));
			bw.write("X,Y,R,Count");
			bw.newLine();
			for(Tuple<CircleSpecs, Integer> entry : data)
			{
				Coordinate c = entry.el1.origin;
				Integer radius = entry.el1.radius;
				Integer count = entry.el2;
				bw.write(c.x + "," + c.y + "," + radius + "," + count);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
