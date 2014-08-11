package controllers;

import geometry.Circle;
import geometry.Coordinate2D;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeListener;
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

import javax.swing.SwingWorker;

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
	
	private ImageType currentDisplay;
	private BufferedImage baseImg;
	private BufferedImage inputImg;	
	private BufferedImage houghSpaceImg;
	private BufferedImage detectedCirclesImg;
	
	@Override
	public void update(Observable o, Object updateObj) {
		if(o instanceof SharpeningController)
		{
			this.inputImg = (BufferedImage) updateObj;
		}
		else
		{
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
	
	public void setCircleDetectedImg(BufferedImage img)
	{
		this.detectedCirclesImg = img;
	}
	
	public void setHoughSpaceImg(BufferedImage img)
	{
		this.houghSpaceImg = img;
	}
	
	@Override
	public void detectCircles(int maxRadius, PropertyChangeListener listener)
	{
		setImage(null);
		CircleDetectorWorker worker = new CircleDetectorWorker(baseImg, inputImg, Constants.MIN_CIRCLE_RADIUS, maxRadius, listener);
		worker.execute();
	}
	
	// Important to set as used when displayed in the combo box
	@Override
	public String toString() {
		return "Circle Detection";
	}
	
	private class MutableFloat{
		float value; // note that we start at 1 since we're counting
		public MutableFloat(float initValue) { this.value = initValue; }
		public void increment (float amount) { value += amount;      }
		public float  get ()       { return value; }
	}
	
	private class CircleDetectorWorker extends SwingWorker<Boolean, Object>
	{
		private BufferedImage baseImg;
		private BufferedImage inputImg;
		
		private BufferedImage houghSpaceImg;
		private BufferedImage detectedCirclesImg;
		private int minR;
		private int maxR;
		
		public CircleDetectorWorker(BufferedImage baseImg, BufferedImage inputImg, int minR, int maxR, PropertyChangeListener listener)
		{
			this.baseImg = baseImg;
			this.inputImg = inputImg;
			this.minR = minR;
			this.maxR = maxR;
			addPropertyChangeListener(listener);
		}
		
		@Override
		protected Boolean doInBackground() throws Exception 
		{			
			int imgWidth = inputImg.getWidth();
			int imgHeight = inputImg.getHeight();
			double totalPixelsToProcess = imgWidth*imgHeight;
			
			HoughSpaceMonitor houghspaceMonitor = new HoughSpaceMonitor(minR, maxR); // Reinitialise the houghSpaceMonitor TODO: Rather have a reset method
			
			houghSpaceImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster houshSpaceImgRaster = houghSpaceImg.getRaster();
			
			detectedCirclesImg = ImageUtils.deepCopy(baseImg);
			
			int[] pixelIntensity = new int[1];
			Raster inputImgRaster = inputImg.getRaster();
			WritableRaster detectedCirclesImgRaster = detectedCirclesImg.getRaster();
			for(int y = 0; y < inputImg.getHeight(); y++)
			{
				setProgress((int) (70 * ((y * imgWidth)/totalPixelsToProcess)));
				for(int x = 0; x < inputImg.getWidth(); x++)
				{
					pixelIntensity = inputImgRaster.getPixel(x, y, pixelIntensity);
					if(pixelIntensity[0] > Constants.MIN_PIXEL_INTENSITY_THRESHOLD)
					{
						houghspaceMonitor.addNewCircleData(x, y, imgWidth, imgHeight);
					}
				}
			}	
			
			// Create the houghspace image
			{		
				Map<Coordinate2D, Map<Integer, MutableFloat>> rawData = houghspaceMonitor.getRawData();
				for(Map.Entry<Coordinate2D, Map<Integer, MutableFloat>> coordEntry : rawData.entrySet())
				{
					Coordinate2D c = coordEntry.getKey();
					float aggregatedIntensity = 0;
					float diviser = maxR/10.f;
					for(Map.Entry<Integer, MutableFloat> radiusEntry : coordEntry.getValue().entrySet())
					{
						aggregatedIntensity += radiusEntry.getValue().get() / diviser;
					}
					houshSpaceImgRaster.setPixel(c.getX(), c.getY(), new int[]{(int) aggregatedIntensity});
				}
			}
			
			setProgress(75);
			
			// Perform extra filtering on the sorted Data
			{
				CircleCounterList sortedData = houghspaceMonitor.getSortedData();
				
				// First take only values larger than one standard deviation
				{
					double mean = sortedData.getMean();
					double standardDeviation = sortedData.getStandardDeviation(mean);
					double threshold = mean + standardDeviation;
					
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
				}
				
				setProgress(80);
				
				// Now remove circle data within the radius of the top candidates
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
							float count = entry.getCount();
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
				}
				
				setProgress(90);
				
				// Again, take only values larger than one standard deviation
				{
					double mean = uniqueCoordinates.getMean();
					double standardDeviation = uniqueCoordinates.getStandardDeviation(mean);
					double threshold = mean + standardDeviation;
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
				}
				
				setProgress(95);
				
				// Filter out non-circles (i.e shapes which look similar to circles but are in fact not circles
				{
					Iterator<CircleCounter> uniqueCoordinatesIt = uniqueCoordinates.iterator();
					while(uniqueCoordinatesIt.hasNext())
					{
						CircleCounter entry = uniqueCoordinatesIt.next();
						Circle circle = entry.getCircle();
						Coordinate2D origin = circle.getOrigin();
						int radius = (int) (circle.getRadius() - (0.01f*circle.getRadius())); // 1% buffer
												
						double pointsOnCircle = circle.getPerimeter();
						double angleIncrement = Constants.TWO_PI/pointsOnCircle;
						pixelIntensity[0] = 0;
						for(double angle = 0.; angle < Constants.TWO_PI; angle += angleIncrement)
						{
							int x = (int) (origin.getX() + (radius * Math.cos(angle)));
							int y = (int) (origin.getY() + (radius * Math.sin(angle)));

							if(x >= 0 && x < imgWidth && y >= 0 && y < imgHeight)
							{
								pixelIntensity = detectedCirclesImgRaster.getPixel(x, y, pixelIntensity);
								if(pixelIntensity[0] < Constants.MIN_PIXEL_INTENSITY_THRESHOLD)
								{
									uniqueCoordinatesIt.remove();
									break;
								}
							}
						}
					}
				}
					
				for(CircleCounter cc : uniqueCoordinates)
				{
					ImageUtils.drawCircle(cc.getCircle(), detectedCirclesImgRaster);
				}
				System.out.println(uniqueCoordinates.size() + " circles detected...");
			}					
			return true;
		}
		
		@Override
		protected void done()
		{
			setProgress(100);
			setCircleDetectedImg(detectedCirclesImg);
			setHoughSpaceImg(houghSpaceImg);
			updateDisplay();
		}
	}
	
	private class HoughSpaceMonitor
	{
		Map<Coordinate2D, Map<Integer, MutableFloat>> data;
		CircleCounterList sortedData;
		int minR, maxR;
		boolean sortRequired;
		
		public HoughSpaceMonitor(int minR, int maxR)
		{
			data = new HashMap<Coordinate2D, Map<Integer, MutableFloat>>();
			this.minR = minR;
			this.maxR = maxR;
			sortRequired = false;
		}
		
		private void increment(int x, int y, int r)
		{
			Coordinate2D c = new Coordinate2D(x,y);
			Map<Integer, MutableFloat> coordinateData = data.get(c);
			
			float increment = ((float) maxR) / r;
			
			if(coordinateData == null)
			{
				data.put(c, new HashMap<Integer, MutableFloat>());
				coordinateData = data.get(c);
			}

			MutableFloat count = coordinateData.get(r);
				
			if(count == null)
				coordinateData.put(r, new MutableFloat(increment));
			else
				count.increment(increment);
		}
		
		public void addNewCircleData(int origX, int origY, int imgWidth, int imgHeight)
		{
			sortRequired = true;
			double pointsToPlot, angleIncrement;
			for(int radius = minR; radius <= maxR; radius++)
			{
				Circle c = new Circle(new Coordinate2D(origX, origY), radius);
				pointsToPlot = c.getPerimeter();
				angleIncrement = Constants.TWO_PI/pointsToPlot;
				
				for(double angle = 0.; angle < Constants.TWO_PI; angle += angleIncrement)
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
			Map<Coordinate2D, Map.Entry<Integer, MutableFloat>> topRadiusPerCoord = new HashMap<Coordinate2D, Map.Entry<Integer, MutableFloat>>();
			{
				for(Map.Entry<Coordinate2D, Map<Integer, MutableFloat>> entry : data.entrySet())
				{
					List<Map.Entry<Integer, MutableFloat>> sortedEntry = new ArrayList<Map.Entry<Integer, MutableFloat>>(entry.getValue().entrySet());
					
					Collections.sort(sortedEntry, new Comparator<Map.Entry<Integer, MutableFloat>>() {
						@Override
						public int compare(Entry<Integer, MutableFloat> o1,
								Entry<Integer, MutableFloat> o2) {
							float sign = (o1.getValue().get() - o2.getValue().get());
							if(sign > 0 )
								return 1;
							else if(sign == .0f)
								return 0;
							else
								return -1;
						}
					});
					Collections.reverse(sortedEntry);				
					topRadiusPerCoord.put(entry.getKey(), sortedEntry.get(0));
				}
			}
			
			// Now sort the coordinates
			List<Map.Entry<Coordinate2D, Map.Entry<Integer, MutableFloat>>> sortedCoord = new ArrayList<Map.Entry<Coordinate2D, Map.Entry<Integer, MutableFloat>>>(topRadiusPerCoord.entrySet());
			{
				Collections.sort(sortedCoord, new Comparator<Map.Entry<Coordinate2D, Map.Entry<Integer, MutableFloat>>>() {

					@Override
					public int compare(
							Entry<Coordinate2D, Entry<Integer, MutableFloat>> o1,
							Entry<Coordinate2D, Entry<Integer, MutableFloat>> o2) {
						float sign =  (o1.getValue().getValue().get() - o2.getValue().getValue().get());
						if(sign > 0 )
							return 1;
						else if(sign == .0f)
							return 0;
						else
							return -1;
					}
				});
				Collections.reverse(sortedCoord);		
			}

			
			CircleCounterList formattedSorted = new CircleCounterList();		
			for(Map.Entry<Coordinate2D, Map.Entry<Integer, MutableFloat>> e : sortedCoord)
			{
				Coordinate2D origin = e.getKey();
				int radius = e.getValue().getKey();
				float count = e.getValue().getValue().get();
				
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
		
		public Map<Coordinate2D, Map<Integer, MutableFloat>> getRawData()
		{
			return data;
		}
		
		public void writeRawDataToFile(String filename)
		{	
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), false));
				bw.write("X,Y,R,Count");
				bw.newLine();
				for(Map.Entry<Coordinate2D, Map<Integer, MutableFloat>> coordEntry : data.entrySet())
				{
					Coordinate2D c = coordEntry.getKey();
					for(Map.Entry<Integer, MutableFloat> radiusEntry : coordEntry.getValue().entrySet())
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
	
	private class CircleCounter extends Tuple<Circle, Float>
	{		
		CircleCounter(Circle c, Float counter)
		{
			super(c, counter);
		}
		
		public Circle getCircle()
		{
			return getKey();
		}
		
		public Float getCount()
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
					float count = cc.getCount();
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