/**
 * This plugin uploads overlay, attach txt semi manual and tag the recard as ParasitesSegmented

 * 1)LIRE LES FICHIERS 
 * 1)Uploader les position des parasites en faisant attention a REVERSE

 * For each text file
 * Read PArasite positions from D:/USERS/Perrine/SemiManualOverlay_guid_1.txt and upload them as tubes (ellipse on 2D) 
 * If Reverse record, think about reversing back the position before upload and mask comparison.
 * upload the txt file as user attachment
 * Annotate the record as 'Parasites Segmented' and add the annotation NParasitesSegmented=N
 * 
 * 0.3: check if the attachment already exists, if yes skip it.
 *  0.4 catch java null exception (bug in get attachment


V56E4dZ33E7mqSvywlzGZUWO0uErLmZex40xdKeA
p0hALEq0x6B2SM05dczIXpu4XLGJV0ZgjIlM00x9
 */
package Parasite_Malaria_Study;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.lang.Object;

	
import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.iclient.Record;
import com.strandgenomics.imaging.icore.IAttachment;
import com.strandgenomics.imaging.icore.VODimension;
import com.strandgenomics.imaging.icore.vo.Ellipse;
import com.strandgenomics.imaging.icore.vo.TextBox;
import com.strandgenomics.imaging.icore.vo.VisualObject;


	
public class ADD_ParasitesOverlaysFromFiles implements PlugIn {
	
Float[] MyParasites;

public void run(String arg) {
		
		GenericDialog gd = new GenericDialog("0.4 Institut Curie : Upload Parasites ");
		
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin will upload mask based on txt files, and attach text file from local text files ");
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
		}
		
		if(projectList != null)
		{
			
			DirectoryChooser dc=new DirectoryChooser("Select the directory containing the text files SemiManual");
			String path=  dc.getDirectory();  
			GenericDialog gdproj = new GenericDialog("Parasites segmentation: text files to be processed");
		    
		    gdproj.addStringField("Prefix to search for ", "SemiManualOverlay_");
		    gdproj.addStringField("Suffix", "_1.txt");
		    gdproj.addNumericField("MIN GUID", 1600, 0);
		    gdproj.addNumericField("MAX GUID", 2120, 0);
		    gdproj.showDialog();
		    
		    String prefix=gdproj.getNextString();
		    String suffix=gdproj.getNextString();
		   int minguid=(int)gdproj.getNextNumber();
		   int maxguid=(int)gdproj.getNextNumber();
		    for (int guid=minguid;guid<=maxguid;guid++){
		    	
		    	String myFileName=prefix+IJ.d2s(guid,0)+suffix;
				File f = new File(path+myFileName);
				
				if (f.exists())
				{
					IJ.log(f.getName());
					//Fill the Nuclei and cell information
					////////////////////////////////////////////////////
					Record r = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
					Boolean notalreadyhere=true;
					try {
					Collection<IAttachment> test =r.getAttachments();

					Iterator<IAttachment> it =test.iterator();
					
					while (it.hasNext())
					{
						IAttachment readattachment=it.next();
						if (readattachment.getName().compareToIgnoreCase(myFileName)==0)
						{
							notalreadyhere=false;
							break;
						}
					}
					}
					catch (Exception e)  
					{
						IJ.log("No Attachments");
					}
					
					if (notalreadyhere)
					{
						////////////////////////////////////////////////////////////////////////////		
							
						//fill the parasite information
						int nbparasites=readParasiteTextFile(myFileName,path);
						String overlayName="UploadedParasites";
					
						double checkN=uploadParasites(r,0, overlayName,4,nbparasites);
						r.addUserAnnotation("NParasitesSegmented",nbparasites);
						java.lang.String historyMessage="Field Nparasites was filled with value "+String.valueOf(nbparasites)+" applying ADD_ParasitesOverlay v0.1 by ppaulgi. A text file was also added as attachment, and a comment was done";
						r.addCustomHistory( historyMessage);
						r.addAttachment(f, myFileName, "Attached by ADD_ParasitesOverlay v0.1. This file was generated by Gloria using Parasite_Malaria_sudy app.");
						r.addUserComments("Parasites Segmented on first frame from local txt files (attached). To be checked, please add checked as a comment if checked. Only the gravity center of the parasite is shown here.");
						IJ.log(IJ.d2s(checkN));
					}
					else 
						IJ.log(f.getName()+" already uploaded");
		        }
				else 
					IJ.log(f.getName()+" does not exist");
		    }
		}
		        
		            
		else
		{
			IJ.showMessage("projects not found...");
		}
		ispace.logout();
		IJ.showMessage("Done");
		           

}

/**
 * upload the position as ellipse, taking into account  or not.
 * @param rec
 * @param t
 * @param overlayName
 * @param nbcolumn
 * @param nbparasites
 * @return n copy of nbparasites
 */

public double uploadParasites( Record rec,int t, String overlayName, int nbcolumn, int nbparasites) {
	
	double width=30; //width of the ellipse overlay in pixels
	double length=30; //length of the ellipse overlay in pixels
	
	int n=nbparasites;
	double[] positionx=new double[n];
	double[] positiony=new double[n];
	double[] positionz=new double[n];
	double[] volume=new double[n];

	
	for (int row=0;row<n;row++)
	{
		positionx[row]=this.MyParasites[row*nbcolumn];
		positiony[row]=this.MyParasites[row*nbcolumn+1];
		positionz[row]=this.MyParasites[row*nbcolumn+2];
		volume[row]=this.MyParasites[row*nbcolumn+3];
	}
	rec.deleteVisualOverlays(0, overlayName);
	rec.createVisualOverlays(0, overlayName);
	for(int parasite=0;parasite<n;parasite++)
	{
		
		double upperleftx=positionx[parasite]-width/2;
		double upperlefty=positiony[parasite]-length/2;
			
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
			
	
		
		List<VisualObject> vObjects2 = new ArrayList<VisualObject>();//yellow on the center slice
		List<VisualObject> vObjectstext = new ArrayList<VisualObject>(); // text annotation : parasite number 
			
			
		Ellipse parasiteposition2 = new Ellipse(upperleftx,upperlefty,width,length);
		parasiteposition2.setPenColor(Color.YELLOW);
		parasiteposition2.setPenWidth(2.0f); 
		vObjects2.add(parasiteposition2);
			
		TextBox ParasiteNumber=new TextBox(upperleftx,upperlefty,width,length,IJ.d2s(parasite+1,0));
		ParasiteNumber.setPenColor(Color.WHITE);
		ParasiteNumber.setPenWidth(2.0f);
		vObjectstext.add(ParasiteNumber);
		rec.addVisualObjects(vObjects2, overlayName, new VODimension(t,slice, 0));
		rec.addVisualObjects(vObjectstext, overlayName, new VODimension(t,slice, 0));

				
	}

	
		return n;
}



/**
 * Read the parasite text files, linearly fill the attribute array MyParasites with x,y,z (as written in the file) , volume
 * @param filename and path
 * @return the number of parasites
 */

private int readParasiteTextFile( String filename, String path){
	int count = 0;
   this.MyParasites = new Float[6 * 500 ]; // For the moment max number of lines= 500
    int nbParasites=0;
    try 
    {
         			
    	
        File file = new File(path + File.separator + filename);
                
        if (file.canRead()) 
        {
        	Scanner fileScan = new Scanner(file);
            while (fileScan.hasNext()) 
            {
            	
            	fileScan.next(); //label
            	for (int k = 0; k < 4; k++) 
                { // we just want to get x, y ,z and volume.
            		this.MyParasites[count] = Float.parseFloat(fileScan.next()); 
                   count++;
                }
            	fileScan.next(); //Average intenisty not used
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



	




}