package Parasite_Malaria_Study;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.iclient.Record;
import com.strandgenomics.imaging.icore.IAttachment;
import com.strandgenomics.imaging.icore.VODimension;
import com.strandgenomics.imaging.icore.vo.TextBox;
import com.strandgenomics.imaging.icore.vo.VisualObject;

public class Analyze_dead_parasites implements PlugIn  {

	
	public void run(String arg0) {
		ImageSpace ispace= ImageSpaceObject.getConnectionManager();
		List<Project> projectList = null;
		String version="0.0";
		GenericDialog gd = new GenericDialog(version+" Upload Dead rectangle mask and create a text file for dead parasites");
		
		try 
		{
			projectList = ispace.getActiveProjects();
			gd.addMessage("This plugin will analyse parasite based on attachment zip containing IJ ROI, and upload a new file called dead parasites");
			gd.showDialog();
			if (gd.wasCanceled())
				return;
		} 
		catch (Exception e)  
		{

			gd.setEchoChar('*');
			gd.addStringField("Token for MalariaParasitesPlugins", "");
			
			gd.addMessage("This plugin will analyse parasite based on attachment zip containing IJ ROI, and upload a new file called dead parasites");
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			String password=gd.getNextString();
			String hostIP="cid.curie.fr";
			int hostPort=443;
		 
			
			String AppID="hJ9qg0hdSVxcQpHkRVqKo7ew9DzzV5qGH9RMtKgf"; //Paris

			try
			{
				
			ispace.login(true,hostIP, hostPort,AppID, password);
			}


			catch (Exception e2)  
	        {
	              e.printStackTrace();
	              IJ.showMessage("Check your ImageJ proxy settings: None if you're local, www-cache 3128 if outside, Or check you have correctly generated a token (see help)");
	              return;
	        }
			
		}
		
		
		
		
		// get the parameter from the GUI
		
		
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
		

		GenericDialog gdproj = new GenericDialog("Analyse Dead Parasite: data to be processed");
		
	    gdproj.addStringField("Suffix", ".zip");
	    gdproj.addStringField("Prefix to upload", "Dead_Parasites");
	    gdproj.addStringField("Suffix", ".txt");
	    gdproj.addStringField("Tag to search for", "DSX");
	    gdproj.addNumericField("Max Number of results", 1, 0);
	    gdproj.showDialog();
	    String suffix=gdproj.getNextString();
	    String prefixu=gdproj.getNextString();
		String suffixu=gdproj.getNextString();
	    String tag=gdproj.getNextString();
	    int maxresult=(int) gdproj.getNextNumber();
	    Project p=ImageSpaceObject.getImageSpace().findProject("Malaria parasite invasion in the mosquito tissues");
	    Set<String> projectNames = new HashSet<String>();
	    projectNames.add(p.getName());
	    
		long[] projectSpecGuids = ImageSpaceObject.getImageSpace().search(tag, projectNames, null, maxresult);
		/**
		 * For each record 
		 * 1) check if it is well belonging to the project 
		 * 2) Check if it was not already analysed
		 * 3) check if there is a zip attachment
		 * 4) Download Record WARNING: as hyperstack, open ROI, reupload Dead parasites as visual annotation (Esnake from inside, dark region, should do the job)
		 * 5) first and fast step: just upload the ROI as it is.
		 * 5) attach annotation number of dead parasites, + attachment text file with position
		 */
		
	    
	   if (projectSpecGuids!=null)
		{
		   		File fp = null;
		   		IJ.showMessage(" Number of record found:"+IJ.d2s(projectSpecGuids.length)+"\n Only record with zip roi attachment and not already process (no txt uploaded with same name will be process");
		    	DirectoryChooser dc=new DirectoryChooser("Select the directory where to save local files for copy");
		    	String selectedPath=  dc.getDirectory();   
		    	//IJ settings
		    	
		    	//For each record
		    	for (int idx=0;idx<projectSpecGuids.length;idx++)
		    	{
		    		long guid=projectSpecGuids[idx];
		    	
		    		String myFileNametoupload=prefixu+IJ.d2s(guid,0)+suffixu;
		    		File f = new File(selectedPath+myFileNametoupload);
		    		IJ.log(f.getName());
					//check if there is a zip attachment
					////////////////////////////////////////////////////
					Record r = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
					Boolean notalreadyhere=true;
					Boolean toprocess=false;
					try {
					Collection<IAttachment> test =r.getAttachments();

					Iterator<IAttachment> it =test.iterator();
					
					while (it.hasNext())
					{
						IAttachment readattachment=it.next();
						if (readattachment.getName().endsWith(suffix))
						{
							toprocess=true;
							Iterator<IAttachment> it2 =test.iterator();
							while (it2.hasNext())
							{
								IAttachment readattachmenttest=it2.next();
								if (readattachmenttest.getName().compareToIgnoreCase(myFileNametoupload)==0)
								notalreadyhere=false;
								break;
							}
							fp=readattachment.getFile();
							
						}
					}
					}
					catch (Exception e)  
					{
						IJ.log("No Attachments");
					}
					
					if (notalreadyhere&&toprocess)
					{
						////////////////////////////////////////////////////////////////////////////		
						
						ResultsTable res=new ResultsTable();
						String overlayName="Dead_Parasites_visually_identified";
						boolean testfile=fp.canRead();
						int t=0; //only first slice processed
						if (testfile)
						{
							

							// check if reverse
							Map<String, Object> annotations = r.getUserAnnotations();
							
							Iterator<String> it = annotations.keySet().iterator();
							
							double value=0;
							while(it.hasNext()){
							 String key = it.next();
								if (key.compareTo("REVERSE")==0){

							  		 value = (Double) annotations.get(key);
								}
																																	
							
							}
							IJ.open(fp.getPath());
							if (suffix.endsWith("roi"))
								IJ.run("Add to Manager");
							RoiManager myroiManager=RoiManager.getInstance(); //maybe test getInstance2 aswell, specific to macro mode.
						    int nRoi=myroiManager.getCount();
						    
						    Roi[] roiarray=myroiManager.getRoisAsArray();
						    r.deleteVisualOverlays(0, overlayName);
							r.createVisualOverlays(0, overlayName);
						    for (int r1=0;r1<nRoi;r1++){
						    	IJ.log(roiarray[r1].getTypeAsString());
						    	IJ.log("z"+IJ.d2s(roiarray[r1].getZPosition()));
						    	int slice=roiarray[r1].getZPosition()-1;
						    	java.awt.Rectangle roirect=roiarray[r1].getBounds();
						    	IJ.log("x"+IJ.d2s(roirect.x));
						    	IJ.log("y"+IJ.d2s(roirect.y));
						    	IJ.log("h"+IJ.d2s(roirect.height));
						    	IJ.log("w"+IJ.d2s(roirect.width));
						    	List<VisualObject> vObjects2 = new ArrayList<VisualObject>();//yellow on the center slice
								List<VisualObject> vObjectstext = new ArrayList<VisualObject>(); // text annotation :parasite number 
									
									
								com.strandgenomics.imaging.icore.vo.Rectangle DeadParasitesposition = new com.strandgenomics.imaging.icore.vo.Rectangle(roirect.x,roirect.y,roirect.width,roirect.height);
								DeadParasitesposition.setPenColor(Color.YELLOW);
								DeadParasitesposition.setPenWidth(2.0f); 
								vObjects2.add(DeadParasitesposition);
									
								TextBox ParasiteNumber=new TextBox(roirect.x,roirect.y,10,10,IJ.d2s(r1+1,0));
								ParasiteNumber.setPenColor(Color.WHITE);
								ParasiteNumber.setPenWidth(2.0f);
								vObjectstext.add(ParasiteNumber);
								r.addVisualObjects(vObjects2, overlayName, new VODimension(t,slice, 0));
								r.addVisualObjects(vObjectstext, overlayName, new VODimension(t,slice, 0));
								//also write the files to be written
								res.incrementCounter();
								res.addValue("Object", r1+1);//tag of the object
					  		
								res.addValue("X", roirect.x+roirect.width/2);
								res.addValue("Y", roirect.y+roirect.width/2);
								
								if (value==0)
									res.addValue("Z", roiarray[r1].getZPosition());
								else 
									res.addValue("Z", r.getSliceCount()+1-roiarray[r1].getZPosition());
								res.addValue("Volume", 0 ); //not measured in first approximation
								res.addValue("AverageIntensity",0);
					  		//setResult("Surface", i, surface[i]); not the surface for now because of the actual version of globaltracking3D
						 }
						
						res.disableRowLabels();
						
						res.show("Results");
						myroiManager.close();
					    r.addUserAnnotation("Cell Holes potentially dead Parasites",nRoi);
						
						
						
						
						try {
							

							res.saveAs(selectedPath+myFileNametoupload);
							java.lang.String historyMessage="Field Cell Holes potentially dead Parasites was filled with value "+String.valueOf(nRoi)+" applying Analyze_dead_parasites"+version+" by "+ispace.getUser()+". A text file conatining position of this dead parasites was also added as attachment";
							r.addCustomHistory( historyMessage);
							r.addAttachment(f, myFileNametoupload, "Attached by Analyze_dead_parasites"+version+" This file was generated from the ImageJ ROI uploaded by Gloria.");
							IJ.run("Clear Results");
							IJ.log("upload done for this record");
							} 
						catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
					                         IJ.log("not written");
						}
						    
						    
					}
					
					
						
				}
				else 
						IJ.log(f.getName()+" already uploaded");
		        }
				
		    
		}
		        
		            
		else
		{
			IJ.showMessage("projects not found...");
		}
		
		IJ.showMessage("Done");
		//test ROI reading without image
	
		
	}

}
