package Parasite_Malaria_Study;




import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Iterator;
import javax.swing.JFrame;
import java.io.File;
import java.io.FileNotFoundException;
	import java.io.IOException;

import org.math.plot.*;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;

import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.iclient.Record;


import com.strandgenomics.imaging.icore.IAttachment;


import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
/**
 * In version 1.0: 
 * @author Perrine
 * FIG A: a plot (TPI, Z normanlized to cell layer as fiited by a surface spline?
 */
public class PlotResults implements PlugIn {
	
	Float[] MyParasites;
	
	public void run(String arg) {
		
		/*
		 * Connection and checks
		 */
		GenericDialog gd = new GenericDialog("1.0 Plot Results");
		
		gd.setEchoChar('*');
		gd.addStringField("Token for MalariaParasitesPlugins", "");
		
		
		gd.addMessage("This plugin displays results");
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
	    gdproj.addStringField("Annotation to check for", "TPI");
	    
	    gdproj.addNumericField("MIN Annotation value to keep (included)", 20.0, 1);
	    gdproj.addNumericField("MAX Annotation value to keep (included)", 20.0, 1);
	    gdproj.addStringField("Tag to exclude for", "yes");
	    gdproj.addStringField("Annotation to check for exclusion", "Bended");
	    gdproj.addNumericField("Max Number of results", 100, 0);
	    gdproj.showDialog();
	    String prefix=gdproj.getNextString();
	    String suffix=gdproj.getNextString();
	    
	    String tag=gdproj.getNextString();
	    String check=gdproj.getNextString();
	    
	    double minvalueexpected=gdproj.getNextNumber();
	    double maxvalueexpected=gdproj.getNextNumber();
	    String valueexclude=gdproj.getNextString();
	    String checkexclude=gdproj.getNextString();
	    int maxresult=(int) gdproj.getNextNumber();
	    Project p=ImageSpaceObject.getImageSpace().findProject("Malaria parasite invasion in the mosquito tissues");
	    Set<String> projectNames = new HashSet<String>();
	    projectNames.add(p.getName());
	    
		long[] projectSpecGuids = ImageSpaceObject.getImageSpace().search(tag, projectNames, null, maxresult);
		
	    if (projectSpecGuids!=null)
	    {
	    	IJ.showMessage(" Number of record found:"+IJ.d2s(projectSpecGuids.length)+"\n All files will be analyzed and plotted");
	    	Plot3DPanel plot = new Plot3DPanel();
	    	Plot3DPanel plot2 = new Plot3DPanel();
	    	//For each record
	    	for (int idx=0;idx<projectSpecGuids.length;idx++)
	    	{
	    		long guid=projectSpecGuids[idx];
	    		
	    		try{
	    			Record rec = ImageSpaceObject.getImageSpace().findRecordForGUID(guid);
	    			IJ.log(IJ.d2s(guid,0));
	    			if(rec.getParentProject().getName().compareToIgnoreCase("Malaria parasite invasion in the mosquito tissues")==0)
	    			{
	    				// check if it is filling the criteria of annotation (example TPI=20.0)
	    				double value=checkAnnotation_double(check, rec) ;
	    				String valueS =checkAnnotation_string(checkexclude, rec);
						if ((value>=minvalueexpected)&&(value<=maxvalueexpected))
						{
							if (valueS.compareToIgnoreCase(valueexclude)!=0) // if 0 means equals, we want not equal
							{
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
								//int nparasites=readParasiteTextFile(fp);
								IJ.log(IJ.d2s(nparasites)+"nparasites have been found");
								//info will be:
								//1)paraiste slice in image coordiante system (to compute distance with nucleus) ;
								//2)cell nuclei label; 
								//3) cell label index; 
								//4) dist to Nuclei; 
								//5) cell level under nuclei; (to be comapred with cell level in cell file)
							
									
								 double[] x=new double[1];
								 double[] y=new double[1];
								 double[] z=new double[1];
								 double[] zn=new double[1];
								 // find max intensity of parasite
								 float maxintensity=0;
								 for (int i=0;i<nparasites;i++)
								 {
									 if (this.MyParasites[(5*i)+4]>maxintensity)
										 maxintensity=this.MyParasites[(5*i)+4];
									 
									
								 }
								 IJ.log(IJ.d2s(maxintensity));
								 float minintensity=200; //average bg value
								float[] reds=new float[32];
								float[] greens=new float[32];
								float[] blues=new float[32];
								 int factor=ice(reds,greens,blues); //should be 32
								 double valuen=checkAnnotation_double("Max height of nuclei",rec );
								 double maxz=0;
								 double minz=0;
								 if (valuen !=0)
								 {
									
									maxz=checkAnnotation_double("Max height of nuclei",rec);
									minz=checkAnnotation_double("Min height of nuclei",rec);
									if((maxz-minz)<10)
									{
										IJ.log("not bended from nuclei positions");
									double valuer=checkAnnotation_double("REVERSE",rec);
									if (valuer==1)
									{
										double tmp=minz;
										minz=rec.getSliceCount()+1-maxz;
										
										maxz=rec.getSliceCount()+1-tmp;
									}
									 IJ.log("Nuclei height (reverse taken into account ) is between "+IJ.d2s(minz)+ "and "+IJ.d2s(maxz));
									}
								 }
								
								 for (int i=0;i<nparasites;i++)
								 {
									 x[0]=this.MyParasites[5*i];
									 y[0]=this.MyParasites[(5*i)+1];
									 if (valuen !=0)
									 {
										 if((maxz-minz)<10)
										 {
										 zn[0]=(this.MyParasites[(5*i)+2]-minz)/(maxz-minz); // we normailze by the number of slices?
										 float normalizedIntensity2=(float) ((this.MyParasites[(5*i)+4]-minintensity)/(maxintensity-minintensity));
										 int indexedintensity2=Math.round((factor-1)*normalizedIntensity2);
										 Color rgb2=new Color(reds[indexedintensity2],greens[indexedintensity2],blues[indexedintensity2]);
										 plot.addScatterPlot("my3Dplotznotnormalizedlut",rgb2,x,y,zn);
										 }
										 else
										 IJ.log("not plotted in norm plot because Nuclei height (reverse taken into account ) is between "+IJ.d2s(minz)+ "and "+IJ.d2s(maxz));
									 }
									 //zn[0]=this.MyParasites[(5*i)+2]/rec.getSliceCount(); // we normailze by the number of slices?
									 z[0]=this.MyParasites[(5*i)+2];
									
									 
									 
								     
									
									 float normalizedIntensity=(float) (this.MyParasites[(5*i)+4]/(maxintensity));
									 
									 int indexedintensity=Math.round((factor-1)*normalizedIntensity);
									
									Color rgb=new Color(reds[indexedintensity],greens[indexedintensity],blues[indexedintensity]);
									
									
									 plot2.addScatterPlot("my3Dplotznormalized",rgb,x,y,z);
								 }
								
									
									
							}
							else
							{
								IJ.log("No attachment named "+ myFileName);
							}
						}
						else
						{
							IJ.log("This record have been excluded");
						}
						}
						 else
						 {
							
							IJ.log("This record does not have "+check+" "+IJ.d2s(minvalueexpected)+"-"+IJ.d2s(maxvalueexpected)+ " but "+IJ.d2s(value));
						 }
					}
					else 
						IJ.log("This record does not belong to Malaria Parasites projects");
				}// end try
	    		catch (Exception e)
	    		{
	    			IJ.log("Lost connection, will try again in 10 seconds");
	    			try
	    			{
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
	    	
	    	
	    	} // end for
	    	// put the PlotPanel in a JFrame, as a JPanel
	  		  JFrame frame = new JFrame("z and intensity normalized (only nuclei processed)");
	  		 	frame.setSize(600, 600);

	  		  frame.setContentPane(plot);
	  		  frame.setVisible(true);
	  		JFrame frame2 = new JFrame("z and intensity not normalized");
		 	frame2.setSize(600, 600);

		  frame2.setContentPane(plot2);
		  frame2.setVisible(true);
	}//end if
	
} // end run     
		
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
private double checkAnnotation_double(String string, Record rec) {
	// check if reverse
	Map<String, Object> annotations = rec.getUserAnnotations();
			
	Iterator<String> it = annotations.keySet().iterator();
	double value=0;
	while(it.hasNext())
	{
		String key = it.next();
		if (key.compareTo(string)==0)
		{
			//Object annotat= annotations.get(key); test class (and if strcmp etcc...?)
			try{
			value = Double.parseDouble((String) annotations.get(key));
			}
			catch (Exception e)
			{
				value = (Double) annotations.get(key);
			}
		}

	}
	return value;
}			
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



int ice(float[] reds, float[] greens, float[] blues) {
    int[] r = {0,0,0,0,0,0,19,29,50,48,79,112,134,158,186,201,217,229,242,250,250,250,250,251,250,250,250,250,251,251,243,230};
    int[] g = {156,165,176,184,190,196,193,184,171,162,146,125,107,93,81,87,92,97,95,93,93,90,85,69,64,54,47,35,19,0,4,0};
    int[] b = {140,147,158,166,170,176,209,220,234,225,236,246,250,251,250,250,245,230,230,222,202,180,163,142,123,114,106,94,84,64,26,27};
    for (int i=0; i<r.length; i++) {
        reds[i] = (float)r[i]/255;
        greens[i] = (float)g[i]/255;
        blues[i] = (float)b[i]/255;
    }
    return r.length;
}
}