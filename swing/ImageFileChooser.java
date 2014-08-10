package swing;
import java.io.File;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import utils.Utils;


public class ImageFileChooser extends JFileChooser{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9089761751766764662L;
	private static String[] supportedImgExtensions = {"gif","jpg","png"};
	
	
	public ImageFileChooser()
	{
		setFileFilter(new ImageFileFilter());
		setCurrentDirectory(new File("/home/harry/Uni/Computer Vision/Assignments/images"));
	}
	
	private class ImageFileFilter extends FileFilter
	{

		@Override
		public boolean accept(File f) {
	        if (f.isDirectory()) {
	            return true;
	        }
	        
	        String extension = Utils.getFileExtension(f);
	        if(Arrays.asList(supportedImgExtensions).contains(extension))
	        	return true;
	 
	        return false;
		}

		@Override
		public String getDescription() {
			return "Image file chooser";
		}
		
	}
}
