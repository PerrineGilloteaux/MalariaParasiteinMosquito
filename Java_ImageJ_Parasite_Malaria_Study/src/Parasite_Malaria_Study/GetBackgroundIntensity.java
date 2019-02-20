package Parasite_Malaria_Study;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.iclient.Record;
import com.strandgenomics.imaging.icore.Dimension;
import com.strandgenomics.imaging.icore.IAttachment;
import com.strandgenomics.imaging.icore.IPixelData;
import com.strandgenomics.imaging.icore.image.PixelArray;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;

/*
 * Step 1: find slices where there is at least one parasite
 * Step 2 : for each of this slides, create a new array containing the background value= the mode of these files
 * Step 3 : write a txt file called BackgroundValuesofParasiteChannel containing slicenumber /t beackground value
 */
public class GetBackgroundIntensity implements PlugIn {
	private Float[] MyParasites;
	private int[] ListofSlices;
	private int[] BackgroundValue;

	public void run(String arg) {
		/*
		 * Connection and checks
		 */
		GenericDialog gd = new GenericDialog("0.9 get background instensity");
		
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin analyse for each record at the z-position in slice of each parasite the background valyue, defined as the mode of intensity in this stack");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		// get the parameter from the GUI
		
		String password=gd.getNextString();
		String hostIP="cid.curie.fr";
		int hostPort=443;
	 
		ImageSpace ispace= ImageSpaceObject.getConnectionManager();
		String AppID="hJ9qg0hdSVxcQpHkRVqKo7ew9DzzV5qGH9RMtKgf"; //Paris

		try
		{
			
		ispace.login(true,hostIP, hostPort,AppID, password);
		}


		catch (Exception e)  
        {
              e.printStackTrace();
              IJ.showMessage("Check your ImageJ proxy settings: None if you're local, www-cache 3128 if outside, Or check you have correctly generated a token (see help)");
              return;
        }
		List<Project> projectList = null;
		try 
		{
			projectList = ispace.getActiveProjects();
		} 
		catch (Exception e)  
		{
			e.printStackTrace();
			IJ.showMessage("Error in getting active project");
			return;
		}
		

		GenericDialog gdproj = new GenericDialog("Analyse Parasite: attachments files to be processed");
		gdproj.addStringField("Prefix to search for ", "SemiManualOverlay_");
	    gdproj.addStringField("Suffix", "_1.txt");
	    gdproj.addStringField("Tag to search for", "DSX");
	    gdproj.addNumericField("Max Number of results", 1, 0);
	    gdproj.showDialog();
	    String prefix=gdproj.getNextString();
	    String suffix=gdproj.getNextString();
	    String tag=gdproj.getNextString();
	    int maxresult=(int) gdproj.getNextNumber();
	    Project p=ImageSpaceObject.getImageSpace().findProject("Malaria parasite invasion in the mosquito tissues");
	    Set<String> projectNames = new HashSet<String>();
	    projectNames.add(p.getName());
	    
		long[] projectSpecGuids = ImageSpaceObject.getImageSpace().search(tag, projectNames, null, maxresult);
		//long[] projectSpecGuids={1613};
		DirectoryChooser dc=new DirectoryChooser("Select the directory where to save local files");
    	String selectedPath=  dc.getDirectory();   
		for (int idx=0;idx<projectSpecGuids.length;idx++)
    	{
    		
			long guid=projectSpecGuids[idx];
    		// for each record
    		Record rec = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
    		
    		
    		String myFileName=prefix+IJ.d2s(guid,0)+suffix;
			int nparasites=0;
			// get the attachment files
			Boolean attachmenthere=false;
			try 
			{
				Collection<IAttachment> test =rec.getAttachments();
			
				Iterator<IAttachment> it2 =test.iterator();
	
				while (it2.hasNext())
				{
					IAttachment readattachment=it2.next();
					if (readattachment.getName().compareToIgnoreCase(myFileName)==0)
					{
						attachmenthere=true;
						File fp=readattachment.getFile();
						boolean testfile=fp.canRead();
						if (testfile)
							IJ.log("file OK");
						else
							IJ.log("file NOT OK");
						nparasites=readParasiteTextFile(fp);
						break;
					}
				}
			}
			catch (Exception e)  
			{
				IJ.log("No Attachments");
			}
    		if (attachmenthere)
    		{
    		// Get Parasites
    		int n=nparasites;
		
    		double[] positionz=new double[n];
		
    		int nbcolumn=5;
    		for (int row=0;row<n;row++)
    		{
			
    			positionz[row]=this.MyParasites[row*nbcolumn+2];
			
    		}
    		ListofSlices=new int[n];
    		BackgroundValue=new int[n];
    		for(int parasite=0;parasite<n;parasite++)
    		{
			
			
				
    			int slice=0;
    			// check if reverse
    			Map<String, Object> annotations = rec.getUserAnnotations();
			
    			Iterator<String> it = annotations.keySet().iterator();
			
    			double value=0;
    			while(it.hasNext())
    			{
    				String key = it.next();
    				if (key.compareTo("REVERSE")==0)
    					value = (Double) annotations.get(key);

    			}
    			if (value==0)
    				slice= (int)positionz[parasite];
    			else 
    				slice=(int)(rec.getSliceCount()+1-(int)positionz[parasite]);
		
    			ListofSlices[parasite]=slice;
		
		
    			BackgroundValue[parasite]=getbgvalue(rec,slice);
    			
    		}
    		int done=WriteandUploadBackgroundResults(selectedPath,rec);
    		IJ.log(String.valueOf(done));
    		
    		}
		}
		IJ.log("done");
}
	
	private int WriteandUploadBackgroundResults(String selectedPath,Record rec) {
		
		ResultsTable res=new ResultsTable();
		
		String namefile = "BackgroundvaluesforParasitesintensity";
		for (int parasite=0;parasite<this.ListofSlices.length;parasite++)
		{
			res.incrementCounter();
			
			//myParaiste contains ninfo
			
			res.addValue("Label", parasite+1);
			res.addValue("Slice", this.ListofSlices[parasite]);
			res.addValue("BackgroundMeanValue", this.BackgroundValue[parasite]);
			
		}
			res.disableRowLabels();
			
			res.show("Results");
			try {
				res.saveAs(selectedPath+namefile+IJ.d2s(rec.getGUID(),0)+"_1.txt");
				IJ.run("Clear Results");
				File f = new File(selectedPath+namefile+IJ.d2s(rec.getGUID(),0)+"_1.txt");
				
				if (f.exists())
				{
					rec.addAttachment(f, namefile+IJ.d2s(rec.getGUID(),0)+"_1.txt", "Background value computed as mean intensity of slice (Parasite number, slice in image, background intensity)");
					rec.addCustomHistory("Background value to correct oarasites intensity were computed using GetBackgroundIntensity plugin in Malaria Parasite Plugins package");
				
				}
				else 
				{
					IJ.log("File was not created for some reason");
					return 0;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		                         IJ.log("not written");
			}
			return (int) rec.getGUID();
		
	
		// TODO Auto-generated method stub
		
	}

	private int getbgvalue(Record rec, int slice) {
		
		   
			int channelParasite=rec.getChannelCount()-1; //Parasites are always last channels
			
			
			ImageStack myrecord=new ImageStack(rec.getImageWidth(), rec.getImageWidth()) ;
				if (slice>=rec.getSliceCount())
					slice=rec.getSliceCount()-1;
				try 
				{
					IPixelData  pixelData = rec.getPixelData(new Dimension(0, slice, channelParasite, 0)); 
					// get appropriate ImageJ processor
					ImageProcessor myip = getImageProcessor(pixelData);
					if(myip == null) 
					{
						
						IJ.log(IJ.d2s(rec.getGUID() ,0) +"pb nunknown pixel data no bg value set arbitrarily to 0 for slice "+ String.valueOf(slice));
						return 0;
					}
					myrecord.addSlice(String.valueOf(slice),myip);
		
				}
				catch (Exception e)
				{
					e.printStackTrace();
					 IJ.log(e.toString());
				}
 				
		
			
			ImagePlus toshow=new ImagePlus(IJ.d2s(rec.getGUID() ,0)+"_slice"+String.valueOf(slice), myrecord);
			
			//toshow.show();
			ImageProcessor toanalyse=toshow.getProcessor();
			ImageStatistics stats=toanalyse.getStatistics();
			int bgvalue= (int)stats.mean;
			toshow.close();
		return bgvalue;
	}

	private int readParasiteTextFile( File file)
	{
		int count = 0;
		this.MyParasites = new Float[6 * 500 ]; // For the moment max number of lines= 500
		int nbParasites=0;
		try 
		{
			if (file.canRead()) 
			{  
				Scanner fileScan = new Scanner(file);
		    	while (fileScan.hasNext()) 
		    	{
		    		fileScan.next(); //label
		    	    for (int k = 0; k < 5; k++) 
		    	    { // we just want to get x, y ,z ,volume and average 3D intensity.
		    	    	this.MyParasites[count] = Float.parseFloat(fileScan.next()); 
		    	        count++;
		    	    }
		    	   nbParasites++ ;  
		    	}
		    	fileScan.close();
			}//end if file can read.
		 } 
		catch (FileNotFoundException e)
		{
		    e.printStackTrace();
		}
		return nbParasites;
	}	
	
	/**
	* returns appropriate image processor for input pixel data
	* @param pixelData
	* @return
	* @throws IOException
	*/
	private static ImageProcessor getImageProcessor(IPixelData pixelData) throws IOException
	{
			PixelArray rawData = pixelData.getRawData();
			if(rawData instanceof PixelArray.Byte)
			{
			  // create byteprocessor if the data is of type BYTE
			  byte[] img = (byte[]) rawData.getPixelArray();
			  return new ByteProcessor(pixelData.getImageWidth(), pixelData.getImageHeight(), img, null);
			}
			else if(rawData instanceof PixelArray.Short)
			{
			    // create shortprocessor if the data is of type SHORT
			    short[] img = (short[]) rawData.getPixelArray();
			    return new ShortProcessor(pixelData.getImageWidth(), pixelData.getImageHeight(), img, null);
			}
			else if(rawData instanceof PixelArray.Integer)
			{
			// create colorprocessor if the data is of type INT
			     int[] img = (int[]) rawData.getPixelArray();
			     return new ColorProcessor(pixelData.getImageWidth(), pixelData.getImageHeight(), img);
			 }
			
			                 		
			return null;
	}
}
