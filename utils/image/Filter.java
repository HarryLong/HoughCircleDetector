package utils.image;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public abstract class Filter {
	Kernel kernel;
	int edgeCondition;

	public Filter(int kWidth, int kHeight, float[] kData, int edgeCondition) {
		this.kernel = new Kernel(kWidth, kHeight, kData);
		this.edgeCondition = edgeCondition;
	}

	public BufferedImage processImage(BufferedImage image) {
		ConvolveOp filter = new ConvolveOp(kernel, edgeCondition, null);
		return filter.filter(image, null);
	}
}