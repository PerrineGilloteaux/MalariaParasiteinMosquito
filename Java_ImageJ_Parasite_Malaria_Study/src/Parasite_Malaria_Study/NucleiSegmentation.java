/**
*
 * TODO: upload nuclei label mask
 * 
 * 
 */
package Parasite_Malaria_Study;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.io.DirectoryChooser;


import ij.measure.ResultsTable;
import ij.plugin.PlugIn;




	import ij.process.ByteProcessor;
	import ij.process.ColorProcessor;

	
	import ij.process.ImageProcessor;
	import ij.process.ShortProcessor;





import java.io.File;
import java.io.IOException;

		




import java.util.HashSet;
import java.util.Iterator;
	import java.util.List;
import java.util.Map;

import java.util.Set;

import java.lang.Object;

	
	import com.strandgenomics.imaging.iclient.ImageSpace;
	import com.strandgenomics.imaging.iclient.ImageSpaceObject;
//import com.strandgenomics.imaging.iclient.RecordBuilder;

	import com.strandgenomics.imaging.iclient.Project;
	import com.strandgenomics.imaging.iclient.Record;

	
	import com.strandgenomics.imaging.icore.Dimension;

import com.strandgenomics.imaging.icore.IPixelData;


import com.strandgenomics.imaging.icore.image.PixelArray;


	
public class NucleiSegmentation implements PlugIn {
	

Float[] MyNuclei;

public void run(String arg) {
		/*
		 * Connection and checks
		 */
		GenericDialog gd = new GenericDialog("1.0 Institut Curie:Segment Nuclei");
		//0.9 pixel->pixels for 3D object counter depends on calibration actually
		// 1.0 Exception displayed in log 
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin will ask for nuclei segmentation, and upload nuclei label mask and nuclei text files");
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
              IJ.log(e.toString());
              IJ.showMessage("Check your ImageJ proxy settings: None if you're local, www-cache 3128 if outside, Or check you have correctly generated a token (see help)");
              return;
        }
		
		try 
		{
			List<Project> projectList = ispace.getActiveProjects();
		} 
		  catch (Exception e)  
		{
			e.printStackTrace();
			 IJ.log(e.toString());
			IJ.showMessage("Error in getting active project");
			return;
		}
		

		GenericDialog gdproj = new GenericDialog("Analyse Parasite: attachments files to be processed");
	    gdproj.addStringField("Tag to search for", "DSX");
	    gdproj.addNumericField("Max Number of results", 1, 0);
	    gdproj.showDialog();
	   
	    
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
		 * 
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
						// check if there is a DAPI channel (at least 3 channels), actually otherwise we use cells
						int nbchannels=rec.getChannelCount();
						if (nbchannels>=2)
						{
							
							GenericDialog gdproj2 = new GenericDialog("Direct upload?");
						    gdproj2.addCheckbox("do not try to download data, I got it already", true);
						    
						    gdproj2.showDialog();
						    boolean directupload=gdproj2.getNextBoolean();
						    ImageStack myrecord=new ImageStack(rec.getImageWidth(), rec.getImageWidth()) ;
							//Now get the nuclei channel (we already have checked that there were 3 channel
							int c=0; //always channel 1
							// in the case of several time frames, only the first slice is taken into account to get the number of parasites for now, so the same for nuclei	
							int nbframes=1;
						
						   
							
								for (int t=0;t<nbframes;t++)
								{
									 if (directupload==false)
									    {
									
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
											 IJ.log(e.toString());
										}
						 				
								
									}
									ImagePlus toshow=new ImagePlus(IJ.d2s(guid,0)+"_c"+String.valueOf(c), myrecord);
									
									toshow.show();
								}
						    
								
									String MaskName="LabelledNucleiMask";
									
								  //create Nuclei map and (uplaod it?)
									int nbNuclei=createNucleiMap(myrecord, guid,  rec,t, MaskName,selectedPath) ;
									IJ.log(IJ.d2s(nbNuclei));
									ImagePlus labelledmaskNuclei = WindowManager.getCurrentImage();
									IJ.save(labelledmaskNuclei, selectedPath+IJ.d2s(guid,0)+"NucleiLabelledMask.tif");
									/*RecordBuilder rb = prj.createRecordBuilder(pd.getRecordLabel(), sequence.getSizeT(), sequence.getSizeZ(), channels, sites , sequence.getWidth(), sequence.getHeight(), getPixelDepth(sequence.getDataType_()), 1.0, 1.0, 1.0, ImageType.GRAYSCALE, new SourceFormat("IMG"), "", "/tmp", System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
									double interval = sequence.getTimeInterval();
									double elapsedtime=interval;
									if (sequence.getSizeT()>1)
										elapsedtime=interval /(sequence.getSizeT()-1);
									OMEXMLMetadataImpl omeMetadata = sequence.getMetadata(); 
									Double exposureTime=1.0;
									try{
										exposureTime = omeMetadata.getPlaneExposureTime(0, 0);
									}
									catch (Exception e)  {
										exposureTime=1.0;
									}
									
									for(int time = 0; time<sequence.getSizeT();time++)
									{
										for(int slice = 0;slice<sequence.getSizeZ();slice++)
										{
											for(int channel = 0;channel<sequence.getSizeC();channel++)
											{
												
												PixelArray rawData = null;
												if(getPixelDepth(sequence.getDataType_()) == PixelDepth.BYTE)
													rawData = new PixelArray.Byte(((byte[])sequence.getDataCopyXY(time, slice, channel)), sequence.getWidth(), sequence.getHeight());
												else if(getPixelDepth(sequence.getDataType_()) == PixelDepth.SHORT)
													rawData = new PixelArray.Short(((short[])sequence.getDataCopyXY(time, slice, channel)), sequence.getWidth(), sequence.getHeight());
												else if(getPixelDepth(sequence.getDataType_()) == PixelDepth.INT)
													rawData = new PixelArray.Integer(((int[])sequence.getDataCopyXY(time, slice, channel)), sequence.getWidth(), sequence.getHeight());
												else
												{
													MessageDialog.showDialog("unknown type");
													return;
												}
												
												PixelMetaData pixelData = new PixelMetaData(new Dimension(time, slice, channel, 0), sequence.getPixelSizeX(),sequence.getPixelSizeY(), sequence.getPixelSizeZ(), elapsedtime,exposureTime, new Date());
												
												rb.addImageData(new Dimension(time, slice, channel, 0), rawData, pixelData );
											}
										}
									}
									
									Record record = rb.commit();
									record.addCustomHistory("Record was uploaded using Icy CID interaction v2.0. "+pd.getRecordLabel());
									MessageDialog.showDialog("Record correctly uplaoded: new ID is : "+record.getGUID());
								}*/

									
									int success=writeNucleiInfo(selectedPath,guid,nbNuclei, "NucleiInfo",rec);
									IJ.log(IJ.d2s(success));
								}
						}
					}else
						IJ.log("This record was alrteady analyzed");
							
				}
					
				else 
						IJ.log("This record does not belong to Malaria Parasites projects");
				}
	    		
	    		catch (Exception e)
	    		{
	    			 IJ.log(e.toString());
	    			IJ.log("Lost connection, will try again in 10 seconds");
	    			try{
	    			    Thread.sleep(1000);
	    			    IJ.log("Trying again");
	    			    idx--;
	    			}
	    			catch(Exception e2)
	    			{
	    			   System.out.println("Exception caught, go to next record");
	    			   IJ.log(e.toString());
	    			   break;
	    			}
	    		
	    		}
	    	}
	    	
	    	
	    }  
	    IJ.log("DONE");
}
		

private int writeNucleiInfo(String selectedPath, long guid, int nbNuclei,
		String string,Record rec) {
	ResultsTable res=new ResultsTable();
	//info will be:
	//0) nuclei position
	//1-3) X,Y,Z
	//+ reverse or not
	//5)cell nuclei label; 
	//6) cell label index; 
	//7) dist to Nuclei; 
	//8) cell level under nuclei; (to be comapred with cell level in cell file)
	//9) cell level in average
	// + also get the max value and min value
	double minnuclei=rec.getSliceCount();
	double maxnuclei=0;
	for (int nucleus=0;nucleus<nbNuclei;nucleus++)
	{
		res.incrementCounter();
		
		//myParaiste contains ninfo
		int ninfoMyParasites=6;
		res.addValue("Label", nucleus+1);
		res.addValue("X", this.MyNuclei[nucleus*ninfoMyParasites]);
		res.addValue("Y", this.MyNuclei[nucleus*ninfoMyParasites+1]);
		res.addValue("Z", this.MyNuclei[nucleus*ninfoMyParasites+2]);
		res.addValue("Image CoS Z", this.MyNuclei[nucleus*ninfoMyParasites+3]);
		res.addValue("Volume ", this.MyNuclei[nucleus*ninfoMyParasites+5]); //no nuclei then should be 0
		if (this.MyNuclei[nucleus*ninfoMyParasites+2]<minnuclei)
		{
			minnuclei=this.MyNuclei[nucleus*ninfoMyParasites+2];
			
		}
		if (this.MyNuclei[nucleus*ninfoMyParasites+2]>maxnuclei)
		{
			maxnuclei=this.MyNuclei[nucleus*ninfoMyParasites+2];
		}
	}
		res.disableRowLabels();
		
		res.show("Results");
		try {
			res.saveAs(selectedPath+string+IJ.d2s(guid,0)+"_1.txt");
			IJ.run("Clear Results");
			File f = new File(selectedPath+string+IJ.d2s(guid,0)+"_1.txt");
			
			if (f.exists())
			{
				rec.addAttachment(f, string+IJ.d2s(guid,0)+"_1.txt", "Nuclei segmented (X,Y,Z image, Z reversed,Volume");
				rec.addCustomHistory("Nuclei were segmented using NucleiSegmentation plugin in Malaria Parasite Plugins package");
				rec.addUserAnnotation("Max height of nuclei", maxnuclei);
				rec.addUserAnnotation("Min height of nuclei", minnuclei);
				rec.addUserAnnotation("Already analyzed","yes");
			}
			else 
				IJ.log("File was not created for some reason");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	                         IJ.log("not written");
		}
		return 1;
	
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
	IJ.log("Label mask at the end to get the label as Mean");
	IJ.runMacroFile("MacroNuclei.ijm");
	IJ.log("Record "+guid);
	
	
	
	IJ.run("3D object counter...", "threshold=1 slice=1 min.=0 max.=100000 objects statistics");
	
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
		this.MyNuclei[count+2]=posnuclei;
		if (value==0)
			this.MyNuclei[count+3]=posnuclei;
		else 
			this.MyNuclei[count+3]=rec.getSliceCount()+1-posnuclei;
		//image coordinate system but physically reversed if needed
		this.MyNuclei[count+4]=(float) res.getValue("Mean",row); //SHould be the nuclei index
		this.MyNuclei[count+5] = (float) res.getValue("Volume (pixels^3)", row);
		count=count+6;
	}
	IJ.run("Clear Results");

	 
	 
	 
	return n;
	
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
	



	
