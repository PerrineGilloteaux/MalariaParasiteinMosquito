/*
 * This plugin uploads overlays for Nuclei from Nuclei Attachments
09/02/2014
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
import com.strandgenomics.imaging.icore.vo.Rectangle;
import com.strandgenomics.imaging.icore.vo.TextBox;
import com.strandgenomics.imaging.icore.vo.VisualObject;


	
public class ADD_NucleiOverlaysFromFiles implements PlugIn {
	
Float[] MyNuclei;

public void run(String arg) {
		
		GenericDialog gd = new GenericDialog("0.0 Institut Curie : Upload Nuclei overlays ");
		
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin will upload mask based on txt files for nuclei attached to a record");
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
			
			 
			GenericDialog gdproj = new GenericDialog("Parasites segmentation: text files to be processed");
		    
		    gdproj.addStringField("Prefix to search for ", "NucleiInfo");
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
				
				
					IJ.log(myFileName);
					int nbnuclei=0;
					//Fill the Nuclei and cell information
					////////////////////////////////////////////////////
					Record r = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
					Boolean attachmenthere=false;
					try {
					Collection<IAttachment> test =r.getAttachments();

					Iterator<IAttachment> it =test.iterator();
					
					while (it.hasNext())
					{
						IAttachment readattachment=it.next();
						if (readattachment.getName().compareToIgnoreCase(myFileName)==0)
						{
							attachmenthere=true;
							File myFile=readattachment.getFile();
							nbnuclei=readNucleiTextFile(myFile);
							break;
						}
					}
					}
					catch (Exception e)  
					{
						IJ.log("No Attachments");
					}
					
					
						////////////////////////////////////////////////////////////////////////////		
						if (attachmenthere){
						//fill the parasite information
						
						String overlayName="UploadedNucleiFromAttachment";
					
						double checkN=uploadNuclei(r,0, overlayName,4,nbnuclei);
						r.addUserAnnotation("NNucleiSegmented",nbnuclei);
						java.lang.String historyMessage="Field Nnuclei was filled with value "+String.valueOf(nbnuclei)+" applying ADD_NucleiOverlays v0.1 by ppaulgi. Overlays for nuclei center of gravity were uploaded.";
						r.addCustomHistory( historyMessage);
						
						IJ.log(IJ.d2s(checkN));
						}
						else{
							IJ.log("No attachment with Nuclei for this record");
						}
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
 * upload the nuclei as rectangle, 
 * @param rec
 * @param t
 * @param overlayName
 * @param nbcolumn
 * @param nbnuclei
 * @return n copy of nbnuclei
 */

public double uploadNuclei( Record rec,int t, String overlayName, int nbcolumn, int nbnuclei) {
	
	double width=30; //width of the ellipse overlay in pixels
	double length=30; //length of the ellipse overlay in pixels
	
	int n=nbnuclei;
	double[] positionx=new double[n];
	double[] positiony=new double[n];
	double[] positionz=new double[n];
	

	
	for (int row=0;row<n;row++)
	{
		positionx[row]=this.MyNuclei[row*nbcolumn];
		positiony[row]=this.MyNuclei[row*nbcolumn+1];
		positionz[row]=this.MyNuclei[row*nbcolumn+2];
		
	}
	rec.deleteVisualOverlays(0, overlayName);
	rec.createVisualOverlays(0, overlayName);
	for(int nuclei=0;nuclei<n;nuclei++)
	{
		
		double upperleftx=positionx[nuclei]-width/2;
		double upperlefty=positiony[nuclei]-length/2;
			
		
		
		int slice= (int)positionz[nuclei];
		
			
	
		
		List<VisualObject> vObjects2 = new ArrayList<VisualObject>();//yellow on the center slice
		List<VisualObject> vObjectstext = new ArrayList<VisualObject>(); // text annotation : nuclei number 
			
			
		Rectangle Nucleiposition2 = new Rectangle(upperleftx,upperlefty,width,length);
		Nucleiposition2.setPenColor(Color.RED);
		Nucleiposition2.setPenWidth(2.0f); 
		vObjects2.add(Nucleiposition2);
			
		TextBox NucleiNumber=new TextBox(upperleftx,upperlefty,width,length,IJ.d2s(nuclei+1,0));
		NucleiNumber.setPenColor(Color.WHITE);
		NucleiNumber.setPenWidth(2.0f);
		vObjectstext.add(NucleiNumber);
		rec.addVisualObjects(vObjects2, overlayName, new VODimension(t,slice, 0));
		rec.addVisualObjects(vObjectstext, overlayName, new VODimension(t,slice, 0));

				
	}

	
		return n;
}



private int readNucleiTextFile(File file) {
	int count = 0;
	   this.MyNuclei = new Float[4 * 500 ]; // For the moment max number of lines= 500
	    int nbNuclei=0;
	    try 
	    {

	                
	        if (file.canRead()) 
	        {
	        	Scanner fileScan = new Scanner(file);
	        	// These nuclei files have headers
	        	String tmp=fileScan.next();
	        	 tmp=fileScan.next();
	        	tmp=fileScan.next();
	        	tmp=fileScan.next();
	        	tmp=fileScan.next();
	        	tmp=fileScan.next();
	        	tmp=fileScan.next();
	        	tmp=fileScan.next();
	        	
	        	while (fileScan.hasNext()) 
	            {
	            	
	            	fileScan.next(); //label
	            	for (int k = 0; k < 4; k++) 
	                { // we just want to get x, y ,z , z image cos (as parasites)
	            		this.MyNuclei[count] = Float.parseFloat(fileScan.next()); 
	                   count++;
	                }
	            	fileScan.next(); //volume not used
	            	
	            	nbNuclei++ ;  
	             }

	            fileScan.close();

	         }//end if file can read.
	     } 
	    catch (FileNotFoundException e)
	    {
	         e.printStackTrace();
	    }
	    return nbNuclei;
}


	




}