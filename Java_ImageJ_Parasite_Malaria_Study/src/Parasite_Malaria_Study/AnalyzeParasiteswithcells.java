/**
 *
 * If Reverse record, think about reversing back the position befor upload and mask comparison.
 * Download and open DAPI channel for mask segmentation channel 1/3
 * Download and open channel 3/3 (Parasites and Dextran level) of guid.
 * 
 * Parasite information: For each parasite: do voronoi segmentation of cells of the gravity center plane. Get Cell index based on Nuclei value. 
 * Nuclei Information: Write down position Label, X,Y,Z, (reverse?) , mean RED CHannel level inside ROI, std Red Channel Level, Number of parasite inside
 * Write down this two files Nuclei-cellinfo and Parasiteinfo. Save them locally and uplaod them
 * 0.1 debug it2 on attachment 
 *  attachment.getFile return a null File corrected
 * 
 */
package Parasite_Malaria_Study;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.DirectoryChooser;


import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;



	import ij.process.ByteProcessor;
	import ij.process.ColorProcessor;

	
	import ij.process.ImageProcessor;
	import ij.process.ShortProcessor;




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

import java.lang.Object;

	
	import com.strandgenomics.imaging.iclient.ImageSpace;
	import com.strandgenomics.imaging.iclient.ImageSpaceObject;

	import com.strandgenomics.imaging.iclient.Project;
	import com.strandgenomics.imaging.iclient.Record;

	
	import com.strandgenomics.imaging.icore.Dimension;
import com.strandgenomics.imaging.icore.IAttachment;
import com.strandgenomics.imaging.icore.IPixelData;


import com.strandgenomics.imaging.icore.image.PixelArray;


	
public class AnalyzeParasiteswithcells implements PlugIn {
	
Float[] MyParasites;
Float[] MyCells;
Float[] MyNuclei;
Float[] MyParasitesNewInfo;
public void run(String arg) {
		/*
		 * Connection and checks
		 */
		GenericDialog gd = new GenericDialog("0.5 Institut Curie:Upload Parasite rectangle mask and Get Dextran info by cells, and parasite position in cell");
		
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin will ask for nuclei segmentation, and analyse parasite based on attachment, and upload a new file cell and pararsites analysed");
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
		/**
		 * For each record 
		 * 1) check if it is well belonging to the project 
		 * 2) Check if it was not already analysed
		 * 3) check if there is a DAPI channel (meaning here at least 3 channels available
		 * 4) Check if there is an attached file as described by prefix (meaning that parasites were already segmented, listed
		 * if 1-4) OK, download DAPI 3D stack, and apply function createNucleiMap -> which launch MacroNuclei.ijm, filled the mynuclei array and let the current iumage open= the color map of nuclei indexes.
		 * 
		 */
	    if (projectSpecGuids!=null)
	    {
	    	IJ.showMessage(" Number of record found:"+IJ.d2s(projectSpecGuids.length)+"\n All stack will openned without being saved, filtered, and their tag will be updated");
	    	DirectoryChooser dc=new DirectoryChooser("Select the directory where to save local files");
	    	String selectedPath=  dc.getDirectory();   
	    	//IJ settings
	    	IJ.run("Input/Output...", "jpeg=75 gif=-1 file=.txt use save_column"); //(to save header)
	    	IJ.run("Set 3D Measurements", "volume surface mean_gray_value centroid bounding_box dots_size=5 font_size=10 redirect_to=none");
	    	//For each record
	    	for (int idx=0;idx<projectSpecGuids.length;idx++)
	    	{
	    		long guid=projectSpecGuids[idx];
	    		
	    		try{
	    		Record rec = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
	    		
	    		
	    		IJ.log(IJ.d2s(guid,0));
	    		if(rec.getParentProject().getName().compareToIgnoreCase("Malaria parasite invasion in the mosquito tissues")==0)
	    		{
					// test if aleradyanalyzed
					Map<String, Object> annotations = rec.getUserAnnotations();
				
					Iterator<String> it = annotations.keySet().iterator();
					
					String value="no";
					while(it.hasNext())
					{
						String key = it.next();
						if (key.compareTo("Already analyzed")==0)
							value =  (String) annotations.get(key);
	
					}
					// only if not already analyzed then process
					if (value.compareToIgnoreCase("no")  ==0)
					{
						// check if there is a DAPI channel (at least 3 channels)
						int nbchannels=rec.getChannelCount();
						if (nbchannels==3)
						{
							String myFileName=prefix+IJ.d2s(guid,0)+suffix;
						
							int nparasites=0;
							// get the attachment files
							Boolean attachmenthere=false;
							try {
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
								//int nparasites=readParasiteTextFile(fp);
								IJ.log(IJ.d2s(nparasites)+"nparasites have been found");
								//info will be:
								//1)paraiste slice in image coordiante system (to compute distance with nucleus) ;
								//2)cell nuclei label; 
								//3) cell label index; 
								//4) dist to Nuclei; 
								//5) cell level under nuclei; (to be comapred with cell level in cell file)
								int nbinfo=5;
								this.MyParasitesNewInfo=new Float[nbinfo*nparasites];
								
								//Now get the nuclei channel (we already have checked that there were 3 channel
								int c=0; //always channel 1
								// in the case of several time frames, only the first slice is taken into account to get the number of parasites for now, so the same for nuclei	
								int nbframes=1;
								for (int t=0;t<nbframes;t++)
								{
									ImageStack myrecord=new ImageStack(rec.getImageWidth(), rec.getImageWidth()) ;
									for ( int s=0; s<rec.getSliceCount(); s++)
									{
										IJ.showStatus(Integer.toString(s)+"/"+Integer.toString(rec.getSliceCount()));
										try 
										{
											IPixelData  pixelData = rec.getPixelData(new Dimension(t, s, c, 0)); 
											// get appropriate ImageJ processor
											ImageProcessor myip = getImageProcessor(pixelData);
											if(myip == null) 
											{
												IJ.showMessage("Unknown pixel data");
												return;
											}
											myrecord.addSlice(String.valueOf(s),myip);
								
										}
										catch (Exception e)
										{
											e.printStackTrace();
										}
						 				
								
									}
									ImagePlus toshow=new ImagePlus(IJ.d2s(guid,0)+"_c"+String.valueOf(c), myrecord);
									
									toshow.show();
									String MaskName="LabelledNucleiMask";
									String prefixnewinfo="InfoParasiteWithincells";
								  //create Nuclei map and (uplaod it?)
									int nbNuclei=createNucleiMap(myrecord, guid,  rec,t, MaskName,selectedPath) ;
									IJ.log(IJ.d2s(nbNuclei));
									ImagePlus labelledmaskNuclei = WindowManager.getCurrentImage();
									IJ.save(labelledmaskNuclei, selectedPath+IJ.d2s(guid,0)+"NucleiLabelledMask.tif");
									//Parasite information:
									//For each parasite: do voronoi segmentation of cells of the gravity center plane. Get Cell index based on Nuclei value. 
									for (int idxparasite=0;idxparasite<nparasites;idxparasite++)
									{
										
										int success=generateCellInfoforParasite(nbinfo,idxparasite,rec,labelledmaskNuclei,selectedPath);
									}
									// upload nuclei segmentation (with nuclei numbers and indicated in the history
									// attch jpeg file for each parasite with cell number 
									//Nuclei Information: Write down position Label, X,Y,Z, (reverse?) , mean RED CHannel level inside , std Red Channel Level, Number of parasite inside
									//write file with Parasite info
									int success=writeParasitesInfo(selectedPath,guid,nparasites, prefixnewinfo);
								}
							}
							else
								IJ.log("No attachment named "+ myFileName);
						}
						else
							IJ.log("This record does not seem to have a DAPI channel. Please take note of it");
					}
					else 
						IJ.log("This record does not belong to Malaria Parasites projects");
				}
	    		}
	    		catch (Exception e)
	    		{
	    			IJ.log("Lost connection, will try again in 10 seconds");
	    			try{
	    			    Thread.sleep(1000);
	    			    IJ.log("Trying again");
	    			    idx--;
	    			}
	    			catch(Exception e2)
	    			{
	    			   System.out.println("Exception caught, go to next record");
	    			   break;
	    			}
	    		
	    		}
	    	}
			  
		  }
		

}

/**
 *  write a file an upload it with information regarding parasites
 * @param selectedPath
 * @param guid
 * @return
 */

private int writeParasitesInfo(String selectedPath, long guid, int nparasite,String prefixnewinfo) {
	
	ResultsTable res=new ResultsTable();
	//info will be:
	//0) Parasite index
	//1-3) X,Y,Z
	//4)paraiste slice in image coordiante system (to compute distance with nucleus) ;
	//5)cell nuclei label; 
	//6) cell label index; 
	//7) dist to Nuclei; 
	//8) cell level under nuclei; (to be comapred with cell level in cell file)
	//9) cell level in average
	for (int parasite=0;parasite<nparasite;parasite++)
	{
		res.incrementCounter();
		res.addValue("Object", parasite+1);//tag of the object
		//myParaiste contains ninfo
		int ninfoMyParasites=4;
		int ninfoMyParasitesnewinfo=6;
		res.addValue("X", this.MyParasites[parasite*ninfoMyParasites]);
		res.addValue("Y", this.MyParasites[parasite*ninfoMyParasites+1]);
		res.addValue("Z", this.MyParasites[parasite*ninfoMyParasites+2]);
		res.addValue("Image CoS Z", this.MyParasitesNewInfo[parasite*ninfoMyParasitesnewinfo]);
		res.addValue("Nuclei in Cell label", this.MyParasitesNewInfo[parasite*ninfoMyParasitesnewinfo+1]); //no nuclei then should be 0
		res.addValue("Cell Label", this.MyParasitesNewInfo[parasite*ninfoMyParasitesnewinfo+2]); //no nuclei then should be 0
		res.addValue("Distance to nuclei in um", this.MyParasitesNewInfo[parasite*ninfoMyParasitesnewinfo+3]); //no nuclei then should be 0
		res.addValue("level of cell under parasite", this.MyParasitesNewInfo[parasite*ninfoMyParasitesnewinfo+4]); //no nuclei then should be 0
		res.addValue("average level of cell ", this.MyParasitesNewInfo[parasite*ninfoMyParasitesnewinfo+5]);
		
	}
		res.disableRowLabels();
		
		res.show("Results");
		try {
			res.saveAs(selectedPath+prefixnewinfo+IJ.d2s(guid,0)+"_1.txt");
			IJ.run("Clear Results");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	                         IJ.log("not written");
		}
		return 1;
	
}



/**
 * For each parasite: do voronoi segmentation of cells of the gravity center plane. Get Cell index based on Nuclei value. Add roi? upload cell image?
 * Question: Damaged cells: some do not have signal any more under the parasites
 * @param idxparasite
 * @param rec
 * @param labelledmaskNuclei
 * @return
 */
private int generateCellInfoforParasite(int nbinfo,int idxparasite, Record rec, ImagePlus labelledmaskNuclei, String path) {
		
	int nbcolumn=4;
	double positionx=this.MyParasites[idxparasite*nbcolumn];
	double positiony=this.MyParasites[idxparasite*nbcolumn+1];
	double positionz=this.MyParasites[idxparasite*nbcolumn+2];
	
	
	double value=checkAnnotation_double("REVERSE",rec);
	int slice=0;
	if (value==0)
		slice=(int) positionz;
	else 
		slice=(int)(rec.getSliceCount()+1-(int)positionz);
	
	labelledmaskNuclei.setSlice(slice);
	
	labelledmaskNuclei.setActivated();
	IJ.run("Duplicate...", "tmp"); //for cell analysis
	// get appropriate ImageJ processor
	try{
		
		//Get Cell number in which is the parasite, and the corresponding nuclmei number, get 3D distance between parasite and nuclei
		// Do Voronoi segmentation
		// and create labelled cell image (saved)
		IJ.runMacroFile("MacroCells.ijm");
		// there is a labelled cell image opened
		ImagePlus labelledCellImage= WindowManager.getCurrentImage();
		IJ.save(labelledCellImage, path+"CellVoronoi_parasite"+IJ.d2s(idxparasite)+".tif");
		int cellnumber=getCellLabel(labelledCellImage,positionx,positiony);
		IJ.log(IJ.d2s(cellnumber));
		int nucleinumber=getNucleiNumber(labelledmaskNuclei,cellnumber);
		//clean a bit
		labelledCellImage.close();
		RoiManager rm=RoiManager.getInstance();
		rm.close();
		this.MyParasitesNewInfo[idxparasite*nbinfo]=(float) slice;
		this.MyParasitesNewInfo[idxparasite*nbinfo+1]=(float) cellnumber;
		this.MyParasitesNewInfo[idxparasite*nbinfo+2]=(float) nucleinumber;
		//nuclei number should be nuclei number-1, and if 0? then Inf
		if (nucleinumber==0) // then no nuclei inside the cell in this plane (so empty partition of voronoi)
		 	this.MyParasitesNewInfo[idxparasite*nbinfo+3]=(float) 1000.0;
		else
		{
			float xn=this.MyNuclei[(nucleinumber-1)*5];
			float yn=this.MyNuclei[(nucleinumber-1)*5+1];
			float zn=this.MyNuclei[(nucleinumber-1)*5+2];
			this.MyParasitesNewInfo[idxparasite*nbinfo+3]=(float) EuclideanDistance(positionx,positiony,positionz,xn,yn,zn,rec.getPixelSizeAlongXAxis(),rec.getPixelSizeAlongZAxis());
		}
			
		IPixelData  pixelCELLData = rec.getPixelData(new Dimension(0, slice, 1, 0)); // here we are ine case where we have DAPI channel (3 channels), so cells are always channel 1 (0-2)
		ImageProcessor myip = getImageProcessor(pixelCELLData);
		ImagePlus cellImage=new ImagePlus("cellImage_"+rec.getGUID(), myip);
		cellImage.show();
		IPixelData  pixelParasiteandDextranData = rec.getPixelData(new Dimension(0, slice, 2, 0)); // here we are ine case where we have DAPI channel (3 channels), so paraistes are always channel 2 (0-2)
		ImageProcessor myipparasite = getImageProcessor(pixelParasiteandDextranData);
		ImagePlus parasiteImage=new ImagePlus("ParasiteImage_"+rec.getGUID(), myipparasite);
		parasiteImage.show();
		Roi myParasiteRoi=getROIforParasite(parasiteImage,positionx,positiony);
		parasiteImage.close();
		float[] celllevel=getCellLevelUnderParasite(cellnumber,cellImage,labelledCellImage,positionx,positiony);
		
		//this.MyParasitesNewInfo[idxparasite*nbinfo+4]=celllevel[0];
		//this.MyParasitesNewInfo[idxparasite*nbinfo+5]=celllevel[1];
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	
	//get Dextran image IF dextran	
	String value2=checkAnnotation_string("Dextran",rec);
	if(value2.compareToIgnoreCase("yes")==0)
	{
	//get cell image corresponding
		IPixelData  pixelData2 = rec.getPixelData(new Dimension(0, slice, 2, 0)); // here we are ine case where we have DAPI channel (3 channels), so cells are always channel 1 (0-2)
		// get appropriate ImageJ processor
		try{
			ImageProcessor myip = getImageProcessor(pixelData2);
			ImagePlus DextranImage=new ImagePlus("cellImage_"+rec.getGUID(), myip);
			DextranImage.show();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	return 1;
}
private float[] getCellLevelUnderParasite(int cellnumber, ImagePlus cellImage,
		ImagePlus labelledCellImage, double positionx, double positiony) {
	// TODO Auto-generated method stub
	return null;
}

/**
 * TO WRITE
 * @param Imagetomeasure
 * @param parasite
 
 * @return intensity level under parasite in channel
 */
private float[] getIntensityLevelUnderParasite(ImagePlus Imagetomeasure, Roi parasite) {
	float intensityunderparasite=0;
	float averageintensityofcell=0;
	float[] result=new float[2];
	result[0]=intensityunderparasite;
	result[1]=averageintensityofcell;
	return result;
}

/**
 * TO WRITE
 * @param cellnumber
 * @param cellImage
 * @param labelledCellImage
 * @param positionx
 * @param positiony
 * @return intensity level under parasite in green channel (to be compared with cell average level)
 */
private Roi getROIforParasite(ImagePlus ParasiteImage,double positionx,double positiony) {
	int size=10;
	float intensityunderparasite=0;
	float averageintensityofcell=0;
	float[] result=new float[2];
	result[0]=intensityunderparasite;
	result[1]=averageintensityofcell;
	IJ.makeRectangle((int)(positionx-size/2),(int)( positiony-size/2), (int)size,(int)size);
	
	IJ.run("ESnake ", "target_brightness=Bright control_points=5 gaussian_blur=2 energy_type=Contour alpha=2.0E-5 max_iterations=500 immortal=true save=true");
	Roi myparasite=ParasiteImage.getRoi();
	return myparasite;
}

/**
 * return Euclidean Distance
 * @param positionx
 * @param positiony
 * @param positionz
 * @param float1
 * @param float2
 * @param float3
 * @param dz pixel size in um
 * @param dxy pixel size in um
 * @return Euclidean Distance in um
 */
private double EuclideanDistance(double positionx, double positiony,
		double positionz, float float1, float float2, float float3, double dxy, double dz) {
	double distance=Math.sqrt(Math.pow((positionx-float1)*dxy,2)+Math.pow((positiony-float2)*dxy,2)+Math.pow((positionz-float3)*dz,2));
	return distance;
}



/**
 * 

 * @param labelledmaskNuclei
 * @param cellnumber
 * @return nuclei label inside the cell 
 */
private int getNucleiNumber(ImagePlus labelledmaskNuclei, int cellnumber) {
	
	RoiManager rm=RoiManager.getInstance();
	
	rm.select(labelledmaskNuclei,cellnumber-1); //roi index starts at 0 while cell label starts at 1
	Roi mycell=labelledmaskNuclei.getRoi();
	int nucleilabel=maxValueofRoi(labelledmaskNuclei,mycell);
	return nucleilabel;
}

/**
 * 
 * @param labelledmaskNuclei
 * @param mycell
 * @return max velue of labelledmaskNuclei in mycell roi
 */

private int maxValueofRoi(ImagePlus labelledmaskNuclei, Roi mycell) {
	int height=labelledmaskNuclei.getHeight();
	int width=labelledmaskNuclei.getWidth();
	int mymax=0;
	// for now we ignore the case where a part of a nuclei could be inside. Maybe just shrinking the cell roi would help. But by the way, because it's Voronoi it should never happen.
	for (int x=0;x<width;x++)
	{
		for(int y=0;y<height;y++)
		{
			if (mycell.contains(x, y))
			{
				int []tmp=labelledmaskNuclei.getPixel(x, y);
				if ((tmp[0]>mymax)&&(tmp[0]!=255))
				{
					mymax=tmp[0];
				}
			}
				
		}
	}
	return mymax;
}

/**
 * 
 * @param string
 * @param rec
 * @return annotation key string
 */
private String checkAnnotation_string(String string, Record rec) {
	Map<String, Object> annotations = rec.getUserAnnotations();
	
	Iterator<String> it = annotations.keySet().iterator();
	String value="no";
	while(it.hasNext())
	{
		String key = it.next();
		if (key.compareTo(string)==0)
			value = (String) annotations.get(key);

	}
	return value;
}

/**
 * 
 * @param string
 * @param rec
 * @return annotation key double
 */

private double checkAnnotation_double(String string, Record rec) {
	// check if reverse
	Map<String, Object> annotations = rec.getUserAnnotations();
			
	Iterator<String> it = annotations.keySet().iterator();
	double value=0;
	while(it.hasNext())
	{
		String key = it.next();
		if (key.compareTo(string)==0)
			value = (Double) annotations.get(key);

	}
	return value;
}



private int createNucleiMap(ImageStack myrecord,long guid, Record rec,int t, String MaskName,String selectedPath) {
	IJ.run("Clear Results");
	IJ.runMacroFile("MacroNuclei.ijm");
	IJ.log("Record "+guid);
	
	
	
	IJ.run("3D object counter...", "threshold=10 slice=1 min.=500 max.=100000 objects statistics");
	
	ImagePlus imp = WindowManager.getCurrentImage();
	ResultsTable res=ResultsTable.getResultsTable();
	int n=res.getCounter();
	
	
	
	IJ.log(n+ "nuclei not filetered");
	double value=checkAnnotation_double("REVERSE",rec);
	
	
	 this.MyNuclei = new Float[5 * 500 ]; 
	int count=0;
	for (int row=0;row<n;row++){
		this.MyNuclei[count]=(float) res.getValue("X", row);
		this.MyNuclei[count+1]=(float) res.getValue("Y", row);
		float posnuclei=(float) res.getValue("Z", row); 
		if (value==0)
			this.MyNuclei[count+2]=posnuclei;
		else 
			this.MyNuclei[count+2]=rec.getSliceCount()+1-posnuclei;
		//image coordinate system but physically reversed if needed
		this.MyNuclei[count+3]=(float) res.getValue("Mean",row); //SHould be the nuclei index
		this.MyNuclei[count+4] = (float) res.getValue("Volume (pixel^3)", row);
		count=count+5;
	}
	IJ.run("Clear Results");

	 
	 
	 
	return n;
	
}
/**
 * 
 * @param cellslices
 * @param x
 * @param y
 * @return cell label
 */
private int getCellLabel(ImagePlus cellslices, double x, double y) {
	
	int[] labelcell=cellslices.getPixel((int)Math.round(x),(int)Math.round(y));
	IJ.log(IJ.d2s(labelcell[0]));
	return labelcell[0];
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
	


/**
 * Read the parasite text files, linearly fill the attribute array MyParasites with x,y,z (as written in the file) , volume
 * @param filename and path
 * @return the number of parasites
 */

private int readParasiteTextFile( File file){
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

/*
 * Gives values to this.mycells
 */
public void studyCells(Record rec, int guid,String path) {

	ResultsTable res=ResultsTable.getResultsTable();
	int n=res.getCounter();
	double[] positionx=new double[n];
	double[] positiony=new double[n];
	double[] positionz=new double[n];
	double[] averageintensity=new double[n];
	double[]stdintensity=new double[n];
	double[] volume=new double[n];
	
	for (int row=0;row<n;row++)
	{
		positionx[row]=res.getValue("X", row);
		positiony[row]=res.getValue("Y", row);
		positionz[row]=res.getValue("Z", row);
		averageintensity[row]=res.getValue("Mean",row);
		volume[row] = res.getValue("Volume (pixel^3)", row);
		stdintensity[row]=res.getValue("Volume (pixel^3)", row);
		
	}
	IJ.run("Clear Results");
	for (int row=0;row<n;row++)
	{
		
		res.incrementCounter();
		res.addValue("Cell", row+1);//tag of the object
	
		res.addValue("X", positionx[row]);
		res.addValue("Y", positiony[row]);
	
		// check if reverse
		Map<String, Object> annotations = rec.getUserAnnotations();
	
		Iterator<String> it = annotations.keySet().iterator();
	
		double value=checkAnnotation_double("REVERSE",rec);
		if (value==0)
			res.addValue("Z", positionz[row]);
		else 
			res.addValue("Z", rec.getSliceCount()+1-positionz[row]);
		res.addValue("Volume", volume[row] );
		res.addValue("DextranParasiteAverageIntensity",averageintensity[row]);
		res.addValue("DextranParasiteStdIntensity",stdintensity[row]);
	
	}

	
	res.show("Results");
	try 
	{
		res.saveAs(path+"CellNucleiInfo_"+IJ.d2s(guid,0)+"_1.txt");
		IJ.run("Clear Results");
	} 
	catch (IOException e) 
	{

		e.printStackTrace();
	}
		

	
	
 
}



}