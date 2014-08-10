package utils;
import java.io.File;


public class Utils {
	
	public static String getFileExtension(File f)
	{
		String filename = f.getName();
		
		int i = filename.lastIndexOf('.');
		
		if(i != -1 && i < filename.length()-1)
			return filename.substring(i+1);
		
		return null;
	}
}
