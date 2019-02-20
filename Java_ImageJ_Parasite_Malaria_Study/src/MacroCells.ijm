title=getTitle();
setThreshold(1, 255);
run("Convert to Mask");

run("Voronoi");
setThreshold(1, 255);
run("Convert to Mask");
run("Invert");
roiManager("Reset");

run("Analyze Particles...", "size=0-Infinity circularity=0.00-1.00 show=[Count Masks] add");
run("3-3-2 RGB");
roiManager("Deselect");
roiManager("Save", "D:\\USERS\\test\\RoiSetCells_"+title+".zip");
