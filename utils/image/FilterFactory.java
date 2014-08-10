package utils.image;

import java.awt.image.ConvolveOp;

public class FilterFactory {

	public static final int EDGE_NO_OP = ConvolveOp.EDGE_NO_OP;
	public static final int EDGE_ZERO_FILL = ConvolveOp.EDGE_ZERO_FILL;
	
	static class SingletonHolder {
		  static FilterFactory instance = new FilterFactory();    
	}
	
	private static final float[] SHARPEN_FILTER = new float[] {
	       -.0f,  -.25f,  -.0f,
	       -.25f,   2.f,  -.25f,
	       -.0f,  -.25f,  -.0f
	};
	
	private static final float[] EDGE_DETECTOR_FILTER = new float[] {
	       -1.f,  -1.f,  -1.f,
	       -1.f,   8.f,  -1.f,
	       -1.f,  -1.f,  -1.f
	};
	
//	private static final float[] SMOOTH_FILTER = new float[] {
//			0.111f, 0.111f, 0.111f, 
//		    0.111f, 0.111f, 0.111f, 
//		    0.111f, 0.111f, 0.111f
//	};
	
	private static final float[] SMOOTH_FILTER = new float[] {
		0.111f, .111f, .111f, 
	    0.111f, .111f, .111f, 
	    0.111f, .111f, .111f
	};
	
	public static FilterFactory instance()
	{
		return SingletonHolder.instance;
	}

	private FilterFactory(){}

	public Filter getSharpeningFilter(int edgeCondition)
	{
		return new SharpenFilter(edgeCondition);
	}
	
	public Filter getSmoothingFilter(int edgeCondition)
	{
		return new SmoothFilter(edgeCondition);
	}
	
	public Filter getEdgeDetectionFilter(int edgeCondition)
	{
		return new EdgeDetectorFilter(edgeCondition);
	}

	private class SharpenFilter extends Filter
	{		
		SharpenFilter(int edgeCondition)
		{
			super(3, 3, SHARPEN_FILTER, edgeCondition);
		}
	}
	
	private class SmoothFilter extends Filter
	{		
		SmoothFilter(int edgeCondition)
		{
			super(3, 3, SMOOTH_FILTER, edgeCondition);
		}
	}
	
	private class EdgeDetectorFilter extends Filter
	{		
		EdgeDetectorFilter(int edgeCondition)
		{
			super(3, 3, EDGE_DETECTOR_FILTER, edgeCondition);
		}
	}
}