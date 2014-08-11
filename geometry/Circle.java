package geometry;

import utils.Constants;

public class Circle
{
	Coordinate2D origin;
	int radius;
	
	public Circle(Coordinate2D origin, int radius) {
		this.origin = origin;
		this.radius = radius;
	}
	
	public Coordinate2D getOrigin()
	{
		return origin;
	}
	
	public int getRadius()
	{
		return radius;
	}
	
	public double getPerimeter()
	{
		return Constants.TWO_PI * radius;
	}
	
	@Override
	public String toString()
	{
		return ("[" + origin.toString() + "," + radius + "]" );
	}
}
