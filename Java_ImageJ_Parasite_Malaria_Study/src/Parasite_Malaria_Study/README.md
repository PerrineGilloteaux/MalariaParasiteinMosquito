List of Malaria plugins:

NucleiSegmentation : Segment in 3D the nuclei , uupload overlays with the position of nuclei, and attach information about nuclei to the raw data. 

Parasite_Malaria_Study: upload the paraiste position and number after local visual check of the segmentation performed by a macro launched by the plugin

Analyze_dead_Parasites: This plugin will analyse parasite based on attachment zip containing IJ ROI, and upload a new file called dead parasites, visually identified. It will upload a visual annotation called Dead as rectangle mask and create a text file for dead parasites. 

AnalyzeParasiteswith cells: Upload Parasite rectangle mask as visual annotation and Get Dextran info by cells, and parasite position in cell. This plugin will ask for nuclei segmentation based on a macro launched by the program, and analyse parasite based on attachment, and upload a new file cell and pararsites analysed


ADD_NUCLEIOverlaysFromFiles: from the txt files generated by NucleiSegmentation and stored in the database, add ellipses as visual overlays to show the position on images of nuclei.

ADD_ParasitesOverlaysFromFiles: from the txt files generated by Parasite_Malaria_Study and stored in the database, add ellipses as visual overlays to show the position on images of parasites.



GetBackgroundIntensity : compute the backround value for correcting the intensity of parasites, per slice.


PlotResults : Plot some uploaded results. Mainly demo purposed

PlotResultsIntensity : idem focusing on parasites intensity

Upload2DXYversusZprojection: upload a set of representation of segemnted data for visual check, changing the orientation. 

