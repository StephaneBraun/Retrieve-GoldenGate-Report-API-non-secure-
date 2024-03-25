
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class oggProperties {
 
    private static ResourceBundle   RESOURCE_BUNDLE;
    static {
        try{
        	
        	String prop = getOggReport.defaultProperties;
        	
        	File file = new File(getOggReport.getPropertiesOgg());
        	URL[] urls = {file.toURI().toURL()};
        	ClassLoader loader = new URLClassLoader(urls);
            RESOURCE_BUNDLE = ResourceBundle.getBundle(prop, Locale.getDefault(), loader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    oggProperties() {System.out.println(getOggReport.defaultProperties);
    }
 
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}