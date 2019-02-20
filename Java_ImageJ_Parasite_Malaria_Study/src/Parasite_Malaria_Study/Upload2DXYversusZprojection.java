/* This plugin uploads a 2 frame pictures of xy plot of nulcei and parasites to check their position against the cell layer
 */
package Parasite_Malaria_Study;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.Polygon;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.lang.Object;

	
import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.PixelMetaData;
import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.iclient.Record;
import com.strandgenomics.imaging.iclient.RecordBuilder;
import com.strandgenomics.imaging.icore.Channel;
import com.strandgenomics.imaging.icore.Dimension;
import com.strandgenomics.imaging.icore.IAttachment;
import com.strandgenomics.imaging.icore.ImageType;
import com.strandgenomics.imaging.icore.Site;
import com.strandgenomics.imaging.icore.SourceFormat;
import com.strandgenomics.imaging.icore.VODimension;
import com.strandgenomics.imaging.icore.image.PixelDepth;
import com.strandgenomics.imaging.icore.vo.Ellipse;
import com.strandgenomics.imaging.icore.vo.TextBox;
import com.strandgenomics.imaging.icore.vo.VisualObject;
import com.strandgenomics.imaging.icore.image.PixelArray;

	
public class Upload2DXYversusZprojection implements PlugIn {
	
Float[] MyParasites;
Float[] MyNuclei;
Float[] MyParasitesdead;
public void run(String arg) {
	
		String version="3";
		//v1.1: added thumbnail generation (attempt)
		//v1.2 : just nuclei as thumbnails +pb LUT?
		//v1.3 size changed to 0.5 to make smaller image and no set thumbnail
		// v2.1 also upload dead parasite
		// v2.2 also upload tag to find back parasites
		//TO DO bug sur dead parasites en ajout
		// 3.0 bug OK: corrected to take dead parasites_ and dead parasites files: problem in the roi version
		GenericDialog gd = new GenericDialog(version +" Institut Curie : Upload Projection ");
		
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin will upload projection based on txt files, with history to link images. Please make sure Foreground color is set to 255 255 255");
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
			
			
			GenericDialog gdproj = new GenericDialog("attached text files to be processed");
		    
		    gdproj.addStringField("Prefix to search for parasite  ", "SemiManualOverlay_");
		    gdproj.addStringField("Prefix to search for potential dead parasite  ", "Dead_Parasites");
		    gdproj.addStringField("Prefix to search for nuclei  ", "NucleiInfo");
		    gdproj.addStringField("Suffix", "_1.txt");
		    gdproj.addStringField("Tag to search for", "DSX");
		    gdproj.addNumericField("Max Number of results", 1, 0);
		    gdproj.showDialog();
		    
		    String prefixparasites=gdproj.getNextString();
		    String prefixparasitesdead=gdproj.getNextString();
		    String prefixnuclei=gdproj.getNextString();
		    String suffix=gdproj.getNextString();
		    String tag=gdproj.getNextString();
		    int maxresult=(int) gdproj.getNextNumber();
		    
		    Project p=ImageSpaceObject.getImageSpace().findProject("Malaria parasite invasion in the mosquito tissues");
		    Set<String> projectNames = new HashSet<String>();
		    projectNames.add(p.getName());
		    
			long[] projectSpecGuids = ImageSpaceObject.getImageSpace().search(tag, projectNames, null, maxresult);
			if (projectSpecGuids==null){
				
				IJ.showMessage("no record was find matching this request");
				return;
			}
				
			for (int idx=0;idx<projectSpecGuids.length;idx++){
				long guid=projectSpecGuids[idx];
		
		    	try {
		    	Record rec = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
		    	
		    	IJ.log(IJ.d2s(guid));
		    	
		    	String myFileNamenuclei=prefixnuclei+IJ.d2s(guid,0)+suffix;
		    	String myFileNameparasite=prefixparasites+IJ.d2s(guid,0)+suffix;
		    	String myFileNameparasitedead=prefixparasitesdead+IJ.d2s(guid,0)+".txt";
		    	String myFileNameparasitedead2=prefixparasitesdead+"_"+IJ.d2s(guid,0)+".txt";
				int nparasites=0;
				int nnuclei=0;
				int nparasitesdead=0;
				Boolean attachmenthere=false;
				
					Collection<IAttachment> test =rec.getAttachments();
				
					Iterator<IAttachment> it2 =test.iterator();
		
					while (it2.hasNext())
					{
						IAttachment readattachment=it2.next();
						if (readattachment.getName().compareToIgnoreCase(myFileNamenuclei)==0)
						{
							attachmenthere=true;
							File fp=readattachment.getFile();
							boolean testfile=fp.canRead();
							nnuclei=readNucleiTextFile(fp);
							Iterator<IAttachment> it3 =test.iterator();
							while (it3.hasNext())
							{
								IAttachment readattachmentparasite=it3.next();
								if (readattachmentparasite.getName().compareToIgnoreCase(myFileNameparasite)==0)
								{
									File fp2=readattachmentparasite.getFile();
									boolean testfile2=fp2.canRead();
									nparasites=readParasiteTextFile(fp2);
									
								}
								if (readattachmentparasite.getName().compareToIgnoreCase(myFileNameparasitedead)==0)
								{
									File fpdead=readattachmentparasite.getFile();
									boolean testfile2=fpdead.canRead();
									nparasitesdead=readParasitedeadTextFile(fpdead);
									
								}
								if (readattachmentparasite.getName().compareToIgnoreCase(myFileNameparasitedead2)==0)
								{
									File fpdead=readattachmentparasite.getFile();
									boolean testfile2=fpdead.canRead();
									nparasitesdead=readParasitedeadTextFile(fpdead);
									
								}
							}
							break;
							
						}
					}
				
				
					
				if (attachmenthere)
					{
						////////////////////////////////////////////////////////////////////////////		
							Project proj=rec.getParentProject();
						//fill the parasite information
						String recordLabel="Projection";
						int noOfFrames=2;
						int noOfSlices=1;
						List<Channel> channels = new ArrayList<Channel>();
						
						Channel channelparasite = new Channel("Parasites");
						channels.add(channelparasite);
						Channel channelnuclei= new Channel("Nuclei");
						channels.add(channelnuclei);
						Channel channeldeadparasite= new Channel("Dead Parasites");
						channels.add(channeldeadparasite);
						
						List<Site> sites = new ArrayList<Site>();
						sites.add(new Site(0, "Site 0"));
						double isopixelsize=1;
						int xlengthpixel=rec.getImageWidth();
						int ylengthpixel=rec.getImageHeight();
						int zlengthpixel=rec.getSliceCount();
						double pixsizex = rec.getPixelSizeAlongXAxis();
						double pixsizey = rec.getPixelSizeAlongYAxis();
						double pixsizez = rec.getPixelSizeAlongZAxis();
						double xlengthum=xlengthpixel*rec.getPixelSizeAlongXAxis();
						double ylengthum=ylengthpixel*rec.getPixelSizeAlongYAxis();
						
						double zlengthum=zlengthpixel*rec.getPixelSizeAlongZAxis();
						int sizePlotX=(int) Math.round(xlengthum/isopixelsize);
						int sizePlotY=(int) Math.round(ylengthum/isopixelsize);
						int sizePlotZ=(int) Math.round(zlengthum/isopixelsize);
						
						if (sizePlotX!=sizePlotY)
						{
							IJ.log(" We have a problem: it should be the same if height and width was the same (512 in this prioject usually)");
							break;
						}
						PixelDepth pdepth=PixelDepth.BYTE;
						SourceFormat sourceFormat=new SourceFormat("PROJECTION");
						String sourceFileName=IJ.d2s(guid);
						String sourceFolder="";
						long sourceTime=System.currentTimeMillis();;
						long creationTime=System.currentTimeMillis();;
						long acquiredTime=System.currentTimeMillis();
						RecordBuilder rb= proj.createRecordBuilder(recordLabel, noOfFrames, noOfSlices, channels, sites, sizePlotX,  sizePlotZ, pdepth,isopixelsize, isopixelsize,0, ImageType.GRAYSCALE, sourceFormat, sourceFileName, sourceFolder, sourceTime, creationTime, acquiredTime);
						
						// we create isotropic plot using 0.1 as z pixel size and x or y pixel size.
						
						PixelMetaData pixelDataxz1 = new PixelMetaData(new Dimension(0, 0, 0, 0), isopixelsize,isopixelsize, 0, 0,0, new Date());
						PixelMetaData pixelDatayz1 = new PixelMetaData(new Dimension(1, 0, 0, 0), isopixelsize,isopixelsize, 0, 0,0, new Date());
						PixelMetaData pixelDataxz2 = new PixelMetaData(new Dimension(0, 0, 1, 0), isopixelsize,isopixelsize, 0, 0,0, new Date());
						PixelMetaData pixelDatayz2 = new PixelMetaData(new Dimension(1, 0, 1, 0), isopixelsize,isopixelsize, 0, 0,0, new Date());
						PixelMetaData pixelDataxz3 = new PixelMetaData(new Dimension(0, 0, 2, 0), isopixelsize,isopixelsize, 0, 0,0, new Date());
						PixelMetaData pixelDatayz3 = new PixelMetaData(new Dimension(1, 0, 2, 0), isopixelsize,isopixelsize, 0, 0,0, new Date());
						IJ.log("File calibration was "+IJ.d2s(rec.getPixelSizeAlongXAxis())+"X"+IJ.d2s(rec.getPixelSizeAlongXAxis())+"X"+IJ.d2s(rec.getPixelSizeAlongZAxis()));
						
						ImageProcessor ipxz=new ByteProcessor(sizePlotX,sizePlotZ);
							
						ImageProcessor ipyz=new ByteProcessor(sizePlotX,sizePlotZ);
						ImagePlus toshow=new ImagePlus("parasites xz", ipxz);
						
						toshow.show();
						IJ.run("Red");
						ipxz=toshow.getProcessor();
						
						// we deal first with parasites
						int ninfoparasite=3;
						int wum=2;
						int hum=2;
						int w=(int) Math.round(wum/isopixelsize);
						int h=(int) Math.round(hum/isopixelsize);
						List<VisualObject> vObjectsxz = new ArrayList<VisualObject>();
						for (int p1=0;p1<nparasites;p1++){
							// position is given in pixel in rec pixelsize, we need it in the new pixel size (0.1)
							double xum=this.MyParasites[p1*ninfoparasite]*pixsizex;
							
							double zum=this.MyParasites[p1*ninfoparasite+2]*pixsizez;
							int x=(int) Math.round(xum/isopixelsize-w/2);
							
							int z=(int) Math.round(zum/isopixelsize-h/2);
							toshow.setActivated();
							IJ.makeOval(x, z, w, h);
							IJ.run("Fill", "slice");
							TextBox ParasiteNumber=new TextBox(x,z,w,w,IJ.d2s(p1+1,0));
							ParasiteNumber.setPenColor(Color.WHITE);
							ParasiteNumber.setPenWidth(3.0f);
							vObjectsxz.add(ParasiteNumber);
							
						}
						
						ImagePlus toshow2=new ImagePlus("parasites yz", ipyz);
						
						toshow2.show();	
						IJ.run("Red");
						List<VisualObject> vObjectsyz = new ArrayList<VisualObject>();
						for (int p1=0;p1<nparasites;p1++){
							// position is given in pixel in rec pixelsize, we need it in the new pixel size (0.1)
							
							double yum=this.MyParasites[p1*ninfoparasite+1]*pixsizey;
							double zum=this.MyParasites[p1*ninfoparasite+2]*pixsizez;
							
							int y=(int) Math.round(yum/isopixelsize-w/2);
							int z=(int) Math.round(zum/isopixelsize-h/2);
							
							toshow2.setActivated();
							
							IJ.makeOval(y, z, w, h);
							IJ.run("Fill", "slice");
							
							
							TextBox ParasiteNumber=new TextBox(y,z,w,w,IJ.d2s(p1+1,0));
							ParasiteNumber.setPenColor(Color.WHITE);
							ParasiteNumber.setPenWidth(3.0f);
							vObjectsyz.add(ParasiteNumber);
						}
						
						ipxz=toshow.getProcessor();
						ipyz=toshow2.getProcessor();
						PixelArray rawDataxzparasites=new PixelArray.Byte((byte[]) ipxz.getPixels(), ipxz.getWidth(), ipxz.getHeight());
						PixelArray rawDatayzparasites=new PixelArray.Byte((byte[]) ipyz.getPixels(), ipyz.getWidth(), ipyz.getHeight());
						rb.addImageData(new Dimension(0, 0, 0, 0), rawDataxzparasites, pixelDataxz1 ); // because x and y have the same dimension 512 by 512
						
						rb.addImageData(new Dimension(1, 0, 0, 0), rawDatayzparasites, pixelDatayz1 );
						
						
						ipxz=new ByteProcessor(sizePlotX,sizePlotZ);
						ipyz=new ByteProcessor(sizePlotX,sizePlotZ);

						ImagePlus toshow3=new ImagePlus("nuclei xz", ipxz);
						
						toshow3.show();
						IJ.run("Blue");
						ipxz=toshow3.getProcessor();
						
						
						// then with Nuclei
						// we deal first with parasites
						int ninfonuclei=4;
						wum=2;
						hum=2;
						w=(int) Math.round(wum/isopixelsize);
						h=(int) Math.round(hum/isopixelsize);
						for (int n=0;n<nnuclei;n++){
							// position is given in pixel in rec pixelsize, we need it in the new pixel size (0.1)
							double xum=this.MyNuclei[n*ninfonuclei]*pixsizex;
							
							double zum=this.MyNuclei[n*ninfonuclei+3]*pixsizez; // we use Z cos not somple z (so this is the one in the same cos as parasites
							int x=(int) Math.round(xum/isopixelsize-w/2);
							
							int z=(int) Math.round(zum/isopixelsize-h/2);
							toshow3.setActivated();
							ipxz=toshow3.getProcessor();
							
							IJ.makeOval(x, z, w, h);
							IJ.run("Fill", "slice");
						
						}
						ImagePlus toshow4=new ImagePlus("nuclei yz", ipyz);
						
						toshow4.show();	
						IJ.run("Blue");
						ipyz=toshow4.getProcessor();
						for (int n=0;n<nnuclei;n++){
							// position is given in pixel in rec pixelsize, we need it in the new pixel size (0.1)
							
							double yum=this.MyNuclei[n*ninfonuclei+1]*pixsizey;
							double zum=this.MyNuclei[n*ninfonuclei+3]*pixsizez; // we use Z cos not somple z (so this is the one in the same cos as parasites
							
							int y=(int) Math.round(yum/isopixelsize-w/2);
							int z=(int) Math.round(zum/isopixelsize-h/2);
							toshow3.setActivated();
							ipxz=toshow3.getProcessor();
							
							
							toshow4.setActivated();
							
							IJ.makeOval(y, z, w, h);
							IJ.run("Fill", "slice");
						}
						ipxz=toshow3.getProcessor();
						ipyz=toshow4.getProcessor();
						PixelArray rawDataxznuclei=new PixelArray.Byte((byte[]) ipxz.getPixels(), ipxz.getWidth(), ipxz.getHeight());
						PixelArray rawDatayznuclei=new PixelArray.Byte((byte[]) ipyz.getPixels(), ipyz.getWidth(), ipyz.getHeight());
						
						rb.addImageData(new Dimension(0, 0, 1, 0), rawDataxznuclei, pixelDataxz2 );
					
						rb.addImageData(new Dimension(1, 0, 1, 0), rawDatayznuclei, pixelDatayz2 );
						 
						// For dead Parasites

						ipxz=new ByteProcessor(sizePlotX,sizePlotZ);
						ipyz=new ByteProcessor(sizePlotX,sizePlotZ);

						ImagePlus toshow5=new ImagePlus("dead xz", ipxz);
						
						toshow5.show();
						IJ.run("Green");
						ipxz=toshow5.getProcessor();
						
						
						// then with Nuclei
						// we deal first with parasites
						IJ.log("Dead Parasites:"+nparasitesdead);
						wum=2;
						hum=2;
						w=(int) Math.round(wum/isopixelsize);
						h=(int) Math.round(hum/isopixelsize);
						List<VisualObject> vObjectsdeadxz = new ArrayList<VisualObject>();//yellow on the center slice
						for (int n=0;n<nparasitesdead;n++){
							// position is given in pixel in rec pixelsize, we need it in the new pixel size (0.1)
							double xum=this.MyParasitesdead[n*ninfoparasite]*pixsizex;
							
							double zum=this.MyParasitesdead[n*ninfoparasite+2]*pixsizez; // we use Z cos not somple z (so this is the one in the same cos as parasites
							int x=(int) Math.round(xum/isopixelsize-w/2);
							
							int z=(int) Math.round(zum/isopixelsize-h/2);
							toshow5.setActivated();
							ipxz=toshow5.getProcessor();
							
							IJ.makeOval(x, z, w, h);
							IJ.run("Fill", "slice");
							TextBox ParasiteNumber=new TextBox(x,z,w,w,IJ.d2s(n+1,0));
							ParasiteNumber.setPenColor(Color.WHITE);
							ParasiteNumber.setPenWidth(3.0f);
							vObjectsdeadxz.add(ParasiteNumber);
						
						}
						ImagePlus toshow6=new ImagePlus("dead yz", ipyz);
						
						toshow6.show();	
						IJ.run("Green");
						ipyz=toshow6.getProcessor();
						List<VisualObject> vObjectsdeadyz = new ArrayList<VisualObject>();//yellow on the center slice
						for (int n=0;n<nparasitesdead;n++){
							// position is given in pixel in rec pixelsize, we need it in the new pixel size (0.1)
							
							double yum=this.MyParasitesdead[n*ninfoparasite+1]*pixsizey;
							double zum=this.MyParasitesdead[n*ninfoparasite+2]*pixsizez; // we use Z cos not somple z (so this is the one in the same cos as parasites
							
							int y=(int) Math.round(yum/isopixelsize-w/2);
							int z=(int) Math.round(zum/isopixelsize-h/2);
							toshow6.setActivated();
							ipxz=toshow6.getProcessor();
							
							
							toshow6.setActivated();
							
							IJ.makeOval(y, z, w, h);
							IJ.run("Fill", "slice");
							
							
								
								
						
								
							TextBox ParasiteNumber=new TextBox(y,z,w,w,IJ.d2s(n+1,0));
							ParasiteNumber.setPenColor(Color.WHITE);
							ParasiteNumber.setPenWidth(3.0f);
							vObjectsdeadyz.add(ParasiteNumber);
							
							
						}
						ipxz=toshow5.getProcessor();
						ipyz=toshow6.getProcessor();
						PixelArray rawDataxzdead=new PixelArray.Byte((byte[]) ipxz.getPixels(), ipxz.getWidth(), ipxz.getHeight());
						PixelArray rawDatayzdead=new PixelArray.Byte((byte[]) ipyz.getPixels(), ipyz.getWidth(), ipyz.getHeight());
						
						rb.addImageData(new Dimension(0, 0, 2, 0), rawDataxzdead, pixelDataxz3 );
					
						rb.addImageData(new Dimension(1, 0, 2, 0), rawDatayzdead, pixelDatayz3 );
						 
						
						
						 Record newrecord = rb.commit();
						
						newrecord.setChannelLUT(0, "Red");
						newrecord.setChannelLUT(1, "Blue");
						newrecord.setChannelLUT(2, "Green");
						long newguid=newrecord.getGUID();
						newrecord.addCustomHistory("this record is a plot with XZ and YZ nuclei and parasites from record guid:"+IJ.d2s(guid,0)+" . Circle are 2 micrometers of diameters. ");
						String overlayName="Potential Dead Parasites tag ";
						newrecord.createVisualOverlays(0, overlayName);
						newrecord.addVisualObjects(vObjectsdeadxz, overlayName, new VODimension(0,0, 0));
						
						newrecord.addVisualObjects(vObjectsdeadyz, overlayName, new VODimension(1,0, 0));
						
						overlayName=" Parasites alive tag ";
						newrecord.createVisualOverlays(0, overlayName);
						newrecord.addVisualObjects(vObjectsxz, overlayName, new VODimension(0,0, 0));
						
						newrecord.addVisualObjects(vObjectsyz, overlayName, new VODimension(1,0, 0));
						//rec.addUserAnnotation("NNucleiSegmented",nnuclei);
						java.lang.String historyMessage="A record displaying XZ plots and YZ plot of parasite and nuclei position have been uploaded from this record as guid:"+IJ.d2s(newguid,0)+" applying Upload2DXYversusZprojection version "+ version;
						rec.addCustomHistory( historyMessage);
						/*ImagePlus[] composite=new ImagePlus[2];
						composite[0]=toshow;
						composite[1]=toshow3;
						ImagePlus newcomposite = RGBStackMerge.mergeChannels(composite, true);
						newcowmposite.show();*/
						/*toshow3.show();
						IJ.run("Duplicate...", "title=[tmp]");
						IJ.run("Size...", "width=256 height=256 average interpolation=Bilinear");
						IJ.run("RGB Color");
						
						IJ.save("tmp.tif");*/
						/*File name=new File("tmp.tif");
						newrecord.setThumbnail(name); *///attention set channel LUT looks lire cancelled by setthumbnails...*/
						
						IJ.log(IJ.d2s(guid)+ " done.");
						IJ.run("Close All");
					}
					else 
						IJ.log(" no attachement was found for nuclei");
				
		    }
			catch (Exception e)  
			{
				IJ.log("No Attachments or incorrect file");
				IJ.log(e.getMessage());
				break;
			}
		}
		}
		IJ.log("DONE");
}

// this file are different from other because they have row and column header
private int readParasitedeadTextFile(File file) {
	int count = 0;
	   this.MyParasitesdead = new Float[3* 10 ]; // For the moment max number of lines= 500
	    int nbParasites=0;
	    try 
	    {
    
	        if (file.canRead()) 
	        {
	        	Scanner fileScan = new Scanner(file);
	        	//header 
	        	fileScan.next();
	        	fileScan.next();
	        	fileScan.next();
	        	fileScan.next();
	        	fileScan.next();
	        	fileScan.next();
	            while (fileScan.hasNext()) 
	            {
	            	
	            	fileScan.next(); //label 1
	            	fileScan.next(); //label 2
	            	for (int k = 0; k < 3; k++) 
	                { // we just want to get x, y ,z 
	            		this.MyParasitesdead[count] = Float.parseFloat(fileScan.next()); 
	                   count++;
	                }
	            	fileScan.next(); //Average and volume not used
	            	fileScan.next(); 
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



/**
 * returns the appropriate raw data for given image processor
 * @param myip
 * @return appropriate raw data for given image processor
 */
private static PixelArray getProcessedRawData(ImageProcessor myip)
{
	if(myip instanceof ByteProcessor)
	{
		byte[] processedData= (byte[])myip.getPixels();
		return new PixelArray.Byte(processedData, myip.getWidth(), myip.getHeight());
	}
	else if(myip instanceof ShortProcessor)
	{
		short[] processedData= (short[])myip.getPixels();
		return new PixelArray.Short(processedData, myip.getWidth(), myip.getHeight());
	}
	else if(myip instanceof ColorProcessor)
	{
		int[] processedData= (int[])myip.getPixels();
		return new PixelArray.Integer(processedData, myip.getWidth(), myip.getHeight());
	}
	else if(myip instanceof FloatProcessor)
	{
		float[] processedData= (float[])myip.getPixels();
		return new PixelArray.Float(processedData, myip.getWidth(), myip.getHeight());
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
	   this.MyParasites = new Float[3* 500 ]; // For the moment max number of lines= 500
	    int nbParasites=0;
	    try 
	    {
	         			
	    	
	        
	                
	        if (file.canRead()) 
	        {
	        	Scanner fileScan = new Scanner(file);
	            while (fileScan.hasNext()) 
	            {
	            	
	            	fileScan.next(); //label
	            	for (int k = 0; k < 3; k++) 
	                { // we just want to get x, y ,z 
	            		this.MyParasites[count] = Float.parseFloat(fileScan.next()); 
	                   count++;
	                }
	            	fileScan.next(); //Average and volume not used
	            	fileScan.next(); 
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