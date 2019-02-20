package Parasite_Malaria_Study;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

	import ij.process.ByteProcessor;
	import ij.process.ColorProcessor;

	
	import ij.process.ImageProcessor;
	import ij.process.ShortProcessor;
import ij.measure.ResultsTable;

import java.awt.Color;
import java.awt.Polygon;
	import java.io.IOException;
		

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
	import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.Object;
//import mcib3d.image3d.processing.FillHoles3D; // FROM Thomas Boudier
	/*import org.apache.commons.httpclient.HttpClient;
	import org.apache.commons.httpclient.HttpException;
	import org.apache.commons.httpclient.methods.PostMethod;*/

	
	import com.strandgenomics.imaging.iclient.ImageSpace;
	import com.strandgenomics.imaging.iclient.ImageSpaceObject;

	import com.strandgenomics.imaging.iclient.Project;
	import com.strandgenomics.imaging.iclient.Record;

	
	import com.strandgenomics.imaging.icore.Dimension;
import com.strandgenomics.imaging.icore.IPixelData;

import com.strandgenomics.imaging.icore.VODimension;
import com.strandgenomics.imaging.icore.image.PixelArray;
import com.strandgenomics.imaging.icore.vo.Rectangle;
import com.strandgenomics.imaging.icore.vo.TextBox;
import com.strandgenomics.imaging.icore.vo.VisualObject;



	
public class Nuclei_distanceMap implements PlugIn {
	

public void run(String arg) {
		
		GenericDialog gd = new GenericDialog("0.2 Institut Curie: beta version of Gloria nuclei analysis software using cid 1.26");
		
		gd.setEchoChar('*');
		gd.addStringField("Token", "");
		
		
		gd.addMessage("This plugin will contains tools for cell analysis ");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		// get the parameter from the GUI
		
		String password=gd.getNextString();
		String hostIP="cid.curie.fr";
		int hostPort=443;
	 
		ImageSpace ispace= ImageSpaceObject.getConnectionManager();
		String AppID="yhep1dK9bSbs72kSRGP9mI4LF0IZUUxueyjW958P"; //Paris
		IJ.run("Set Measurements", "volume surface mean_gray_value centroid bounding_box dots_size=5 font_size=10 redirect_to=ori");
		IJ.run("Input/Output...", "jpeg=75 gif=-1 file=.txt use"); //(not to save header)
		
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
			
		    GenericDialog gdproj = new GenericDialog("Nuclei Records to be processed");
		   
		    gdproj.addStringField("Tag to search for", "");
		    gdproj.addNumericField("Max Number of results", 1, 0);
		    gdproj.showDialog();
		   // int projindex=gdproj.getNextChoiceIndex();
		    String tag=gdproj.getNextString();
		    int maxresult=(int) gdproj.getNextNumber();
		    Project p=ImageSpaceObject.getImageSpace().findProject("Malaria Parasite invasion in the mosquito tissues");
		    Set<String> projectNames = new HashSet<String>();
		    projectNames.add(p.getName());
		   
			long[] projectSpecGuids = ImageSpaceObject.getImageSpace().search(tag, projectNames, null, maxresult);
		    if (projectSpecGuids!=null)
		        {
					IJ.showMessage(" Number of record found:"+IJ.d2s(projectSpecGuids.length)+"\n All stack will openned without being saved, filtered, and their tag will be updated");
		            for (int idx=0;idx<projectSpecGuids.length;idx++)
		            {
						Record r = ImageSpaceObject.getImageSpace().findRecordForGUID(projectSpecGuids[idx]);
		            	
						String title=r.getSourceFilename();
						int nbslices=r.getSliceCount();
						int nbframes=r.getFrameCount();
						int width=r.getImageWidth();
						int height=r.getImageHeight();
						//whatever it is 2 channel or 3 channels, it is always the last channel containing the parasite.
						if (r.getChannelCount()==3) {// if 2 channel no nuclei
							int c=0; //always channel 1
						// in the case of several time frames, only the first slice is taken into account to get the number of parasites for now.
							
						
						Calibration cal=new Calibration();
						 cal.setUnit("um"); 
						 cal.pixelWidth=r.getPixelSizeAlongXAxis();
						 cal.pixelHeight=r.getPixelSizeAlongYAxis();
						 cal.pixelDepth=r.getPixelSizeAlongZAxis();
			           //only one site normally in this project
						 String overlayName= "AllNuclei";
						 	r.deleteVisualOverlays(0, overlayName);
							r.createVisualOverlays(0,overlayName );
						 	//TMP: only first frame for movies.
							nbframes=1;
						 for (int t=0;t<nbframes;t++){
							int pb=0;
						  ImageStack myrecord=new ImageStack(width, height) ;
								for ( int s=0; s<nbslices; s++)
								{
									IJ.showStatus(Integer.toString(s)+"/"+Integer.toString(nbslices));
									try 
										{
											IPixelData  pixelData = r.getPixelData(new Dimension(t, s, c, 0)); 
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
								ImagePlus toshow=new ImagePlus(title+"_c"+String.valueOf(c), myrecord);
								
								toshow.show();
								 double nbNuclei=0;
								try{
								nbNuclei=countNuclei(myrecord,r.getGUID(),r,t,overlayName);
								}
								catch (Exception e )
								{
									
									IJ.log(e.getMessage());
									t=t-1;
									 pb=1;
									 IJ.run("Close All");
									try {
										
										Thread.sleep(1000);
									} catch (InterruptedException e1) {
										// TODO Auto-generated catch block
										IJ.log(e1.getMessage());
									}
									 
								}
								
								if ((t==0)&&(pb==0)){
									r.addUserAnnotation("NNuclei",nbNuclei);
									java.lang.String historyMessage="Field NNuclei was filled with value "+String.valueOf(nbNuclei)+" applying Parasite_Malaria_Study v1.0 by ppaulgi";
									r.addCustomHistory( historyMessage);
								}
							 
		        }
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
}



public double countNuclei(ImageStack myrecord,long guid, Record rec,int t, String overlayName) {
	IJ.log("Processing Record "+IJ.d2s(guid,0));
	IJ.run("Clear Results");
	IJ.runMacroFile("MacroNuclei.ijm");
	ImagePlus imp1 = WindowManager.getCurrentImage();
	IJ.saveAsTiff(imp1, "D:\\Images\\collabexterieures\\Gloria\\NucleiMask_"+IJ.d2s(guid,0)+"_"+IJ.d2s(t+1,0)+".tif") ;
	IJ.log("Mask saved as  "+"D:\\Images\\collabexterieures\\Gloria\\NucleiMask_"+IJ.d2s(guid,0)+"_"+IJ.d2s(t+1,0)+".tif");
	IJ.run("3D object counter...", "threshold=10 slice=1 min.=3000 max.=25000 objects statistics");
	IJ.saveAsTiff(imp1, "D:\\Images\\collabexterieures\\Gloria\\NucleiMaskfiltered_"+IJ.d2s(guid,0)+"_"+IJ.d2s(t+1,0)+".tif") ;
	ImagePlus imp = WindowManager.getCurrentImage();
	ResultsTable res=ResultsTable.getResultsTable();
	int n=res.getCounter();
	double[] positionx=new double[n];
	double[] positiony=new double[n];
	double[] positionz=new double[n];
	double[] averageintensity=new double[n];
	double[] volume=new double[n];
	int[] firstslice=new int[n];
	int[] lastslice=new int[n];
	for (int row=0;row<n;row++){
		positionx[row]=res.getValue("X", row);
		positiony[row]=res.getValue("Y", row);
		positionz[row]=res.getValue("Z", row);
		averageintensity[row]=res.getValue("Mean",row);
		volume[row] = res.getValue("Volume (pixel^3)", row);
		firstslice[row] = (int) res.getValue("BZ", row)-1;
		lastslice[row]=firstslice[row]+(int) res.getValue("B-depth", row)-1;
	}
	IJ.run("Clear Results");
	


double width=30; //width of the rectangle overlay in pixels
double length=30; //length of the rectangle overlay in pixels
int ncorrected=0; 

RoiManager myroiManager=RoiManager.getInstance();
	

IJ.run("Clear Results");
for(int parasite=0;parasite<n;parasite++){
		if (averageintensity[parasite]>0)
		{
			ncorrected++;
			double upperleftx=positionx[parasite]-width/2;
			double upperlefty=positiony[parasite]-length/2;
			int slice=(int)positionz[parasite];
			
			
			
			//rec.deleteVisualOverlays(0,overlayName);
			
	
			List<VisualObject> vObjects = new ArrayList<VisualObject>();//green on all slices where the parasites was but center
			List<VisualObject> vObjects2 = new ArrayList<VisualObject>();//yellow on the center slice
			List<VisualObject> vObjectstext = new ArrayList<VisualObject>(); // text annotation : parasite number on all slices
			
			//I also add for visualisation purpose in another color all slices (or mask)
			Rectangle parasiteposition2 = new Rectangle(upperleftx,upperlefty,width,length);
			parasiteposition2.setPenColor(Color.GREEN);
			parasiteposition2.setPenWidth(2.0f); 
			vObjects2.add(parasiteposition2);
			
			TextBox ParasiteNumber=new TextBox(upperleftx,upperlefty,width,length,IJ.d2s(parasite+1,0));
			ParasiteNumber.setPenColor(Color.WHITE);
			ParasiteNumber.setPenWidth(2.0f);
		    vObjectstext.add(ParasiteNumber);
		   
		    
		   
		    
			for (int i=firstslice[parasite];i<=lastslice[parasite];i++){
				if (i!=slice)
				//	try {
					rec.addVisualObjects(vObjects2, overlayName, new VODimension(t,i, 0));
				rec.addVisualObjects(vObjectstext, overlayName, new VODimension(t,i, 0));
				//	}
				//catch (Exception e) {
					// then we need to cretae a new overlayname
				//	overlayName="";
				//}
			}
			/*for (int i=firstslice[parasite];i<=lastslice[parasite];i++){
				
				imp.setSlice(i);
				ip = imp.getProcessor();
				List<double[]> listcoordinates=get2DRoiforparasite(i,parasite, myroiManager,ip);
				for (int j=0;j<listcoordinates.size();j++){
					double[] coordinates=listcoordinates.get(j);
					if (coordinates!=null){ 
						GeometricPath  boundaries=new GeometricPath(nbinitsegment);
						boundaries.setPenColor(Color.GREEN);
						boundaries.setPenWidth(2.0f);
						for (int k=0;k<coordinates.length;k+=2)
							boundaries.lineTo(coordinates[k], coordinates[k+1]);
						vObjectsboundaries.add(boundaries);
						rec.addVisualObjects(vObjectsboundaries, overlayName, new VODimension(t,i, 0));
						vObjectsboundaries.clear();
					}
				
				}
			}*/
			
			Rectangle parasiteposition = new Rectangle(upperleftx,upperlefty,width,length);
			parasiteposition.setPenColor(Color.YELLOW);
			parasiteposition.setPenWidth(2.0f);
			vObjects.add(parasiteposition);
			
			rec.addVisualObjects(vObjects, overlayName, new VODimension(t, slice, 0));
			
			
			res.incrementCounter();
			res.addValue("Object", parasite+1);//tag of the object
  		
			res.addValue("X", positionx[parasite]);
			res.addValue("Y", positiony[parasite]);
			
			// check if reverse
			Map<String, Object> annotations = rec.getUserAnnotations();
			
			Iterator<String> it = annotations.keySet().iterator();
			
			double value=0;
			while(it.hasNext()){
			 String key = it.next();
				if (key.compareTo("REVERSE")==0){

			  		 value = (Double) annotations.get(key);
				}
																													
			
			}
			if (value==0)
				res.addValue("Z", positionz[parasite]);
			else 
				res.addValue("Z", rec.getSliceCount()+1-positionz[parasite]);
			res.addValue("Volume", volume[parasite] );
			res.addValue("AverageIntensity",averageintensity[parasite]);
  		//setResult("Surface", i, surface[i]); not the surface for now because of the actual version of globaltracking3D
	 }
	}
	res.disableRowLabels();
	rec.addCustomHistory("Overlay "+overlayName+" have been created through local client Nuclei_distanceMap");
	res.show("Results");

	myroiManager.run("reset");
	
 
	
	try {
		

		res.saveAs("D:\\Images\\collabexterieures\\Gloria\\NucleiOverlay_"+IJ.d2s(guid,0)+"_"+IJ.d2s(t+1,0)+".txt");
		IJ.run("Clear Results");
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
IJ.run("Close All");
myroiManager.close();
	

	
	return ncorrected;
}



/*
   private List<double[]> get2DRoiforparasite(int slice, int parasite,RoiManager myroiManager, ImageProcessor ip) {

	// TODO Auto-generated method stub
	int index=0;
	List<double[]> listofParasiteRoi=new ArrayList<double[]>();
	Roi[] roilist = myroiManager.getRoisAsArray();
	int nbroi=0;
	int[] listindex=new int[10];
	boolean found=false;
	for (index=0;index<myroiManager.getCount();index++){
		myroiManager.select(index);
		String label=myroiManager.getName(index);
		int slicenumber=myroiManager.getSliceNumber(label);
		
		if (slicenumber==slice){
			Polygon p= roilist[index].getPolygon();
			
			int[] intx=p.xpoints;
		    int[] inty=p.ypoints;
		    int meanx=mymeanof(intx);
		    int meany=mymeanof(inty);
			double proposedlabel=ip.getPixel(meanx, meany);
			if (proposedlabel==parasite+1){
				found=true;
				listindex[nbroi]=index;
				nbroi++;
				
			}
		}
	}
	if (found) {
		
		if (nbroi<10){
		for (int n=0;n<nbroi;n++){
			Polygon p= roilist[listindex[n]].getPolygon();
			int N=p.npoints;
			int[] intx=p.xpoints;
			int[] inty=p.ypoints;
			double[] xy=new double[2*N]; // max number of point in roi=1000
			int count=0;
			for(int i=0;i<N;i++)
			{
				xy[count]=(double)intx[i];
				count++;
				xy[count]=(double)inty[i];
				count++;
			} 
			listofParasiteRoi.add(xy);
		}
    
		}
	}
	return listofParasiteRoi;
	
}

 */


/*
private int mymeanof(int[] intx) {
	// get the average value (rounded to be int) of an int array
	int mean=0;
	for (int i=0;i<intx.length;i++){
	mean+=intx[i];
	}
		mean/=intx.length;
	return mean;
}
*/


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