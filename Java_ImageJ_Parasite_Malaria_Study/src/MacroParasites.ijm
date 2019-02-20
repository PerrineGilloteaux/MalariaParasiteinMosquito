nbstack=nSlices;
rename("ori");

run("Duplicate...", "title=mask duplicate range=1-&nbstack");
run("Smooth", "stack");
run("Subtract Background...", "rolling=5 sliding stack");
run("Exp", "stack");
setAutoThreshold("Moments dark stack");
run("Wait For User", "ready?");
run("Convert to Mask", "  black");
run("Fill Holes", "stack");

