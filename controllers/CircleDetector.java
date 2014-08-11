package controllers;

import java.beans.PropertyChangeListener;

public interface CircleDetector {
	public void detectCircles(int maxRadius, PropertyChangeListener changeListener);
	public void showHoughSpaceImg();
	public void showDetectedCirclesImg();
}
