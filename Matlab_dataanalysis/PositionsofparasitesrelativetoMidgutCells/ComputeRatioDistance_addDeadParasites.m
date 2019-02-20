% What have been modified is the Z taken into account, was wrong for
% reverse.
% + maybe discard or ponderate by the sapce visible below and under
%NEED toi check 
javaaddpath('./work/client-api.jar')
javaclasspath; %% copy this line in the navigator first time
import java.io.File
import java.lang.String
import java.lang.Double
import java.util.HashSet;
import com.strandgenomics.imaging.iclient.ImageSpace
import java.util.Set
import com.strandgenomics.imaging.iclient.ImageSpaceObject
import com.strandgenomics.imaging.icore.SearchCondition
%methods ImageSpaceObject
% 
ispace = com.strandgenomics.imaging.iclient.ImageSpaceObject.getConnectionManager();
str = String('hello');
AppID= String ('hJ9qg0hdSVxcQpHkRVqKo7ew9DzzV5qGH9RMtKgf'); %Paris
Server=String ('cid.curie.fr');
password=String('rTUTdNerIpdQN4RumPoqI8gCq48WSuQAvL3ILS4b');
ispace.login(true,Server,443,AppID, password);
projectList = ispace.getActiveProjects();
is=ImageSpaceObject.getImageSpace();
p=is.findProject('Malaria parasite invasion in the mosquito tissues');

tag=String('DSX');
 where = HashSet();
 where.add(p.getName());
	  
 conditions=  HashSet();
 % conditions.add(sc); this con,ditions will return no results (but maybe
 % check the type of annotation TPI etc? so I leave it empty for now.
projectSpecGuids=is.search(tag,where, conditions, 100);

prefixParasite1='Dead_Parasites_';
prefixParasite2='Dead_Parasites';
suffixParasite='.txt';
prefixNuclei='NucleiInfo';
suffix='_1.txt';
AllParasites=[];
 %declared the same class
 for i=1:length(projectSpecGuids)
    myFileNameParasites1=[prefixParasite1,num2str(projectSpecGuids(i)),suffixParasite];
    myFileNameParasites2=[prefixParasite2,num2str(projectSpecGuids(i)),suffixParasite];
    myFileNameNuclei=[prefixNuclei,num2str(projectSpecGuids(i)),suffix];
   
    rec = is.findRecordForGUID(projectSpecGuids(i));
    test=rec.getAttachments();
    it=test.iterator();
    Nucleifound=0;
    Parasitesfound=0;
  
    while (it.hasNext())
        readattach=it.next();
        if (readattach.getName().compareToIgnoreCase(myFileNameParasites1)==0)
            myfile=readattach.getFile();
            name=myfile.getPath();
            c=char(name);
            Parasites= importdata(c);
            Parasites=Parasites.data;
            Parasitesfound=1;
        end
         if (readattach.getName().compareToIgnoreCase(myFileNameParasites2)==0)
            myfile=readattach.getFile();
            name=myfile.getPath();
            c=char(name);
            Parasites= importdata(c);
            Parasites=Parasites.data;
            Parasitesfound=1;
        end
        if (readattach.getName().compareToIgnoreCase(myFileNameNuclei)==0)
            myfile=readattach.getFile();
            name=myfile.getPath();
            c=char(name);
            Nuclei= importdata(c);
            Nucleifound=1;
        end
         
    end
  
    if and(Parasitesfound,Nucleifound)
       
    
        Xnuclei=Nuclei.data(:,2)*rec.getPixelSizeAlongXAxis() ;
        Ynuclei=Nuclei.data(:,3)*rec.getPixelSizeAlongYAxis() ;
        Znuclei=Nuclei.data(:,5)*rec.getPixelSizeAlongZAxis() ; %in cos as parasites
        sf = fit( [ Xnuclei, Ynuclei], Znuclei, 'poly33','Robust','Bisquare');

        guid=projectSpecGuids(i);
        plot(sf, [ Xnuclei, Ynuclei], Znuclei)
        Xparasites=Parasites(:,3)*rec.getPixelSizeAlongXAxis() ;
        Yparasites=Parasites(:,4)*rec.getPixelSizeAlongYAxis() ;
        Zparasites=Parasites(:,5)*rec.getPixelSizeAlongZAxis() ;
        %Careful Parasites dead have a column in addition, soi x is 3
        AverageIntensityParasites=Parasites(:,7) ;
        hold on;
        plot3(Xparasites,Yparasites,Zparasites,'*r')
       
         saveas(gcf,[num2str(guid),'.jpg']) ;
         
        % to get nearest nuclei neighbors
        %[IDX,D] = knnsearch([X1,Y1,Z1],[xp,yp,zp]);
        % to get distance from the cell layer?
         [X,Y] = meshgrid(0:rec.getPixelSizeAlongXAxis():512*rec.getPixelSizeAlongXAxis() ,0:rec.getPixelSizeAlongYAxis():512*rec.getPixelSizeAlongYAxis());
         z = feval(sf,X,Y);
         figure,  plot3(X,Y,z,'ob');
         %pos of a point=(x,y,z(x,y))
         Xvector=reshape(X,size(X,1)*size(X,2),1);
            Yvector=reshape(Y,size(Y,1)*size(Y,2),1);
            Zvector=reshape(z,size(Y,1)*size(Y,2),1);
            
         [IDX,D] = knnsearch([ Xvector,Yvector,Zvector],[Xparasites,Yparasites,Zparasites]);   
         %array GUID, corrected X,Y,Z of parasites (corrected by the celle
         %layer position) + TPI + average instensity
         
       
        
         annotations=rec.getUserAnnotations();
         TPI=annotations.get('TPI');
         TPI=str2double(TPI);
         %guid=projectSpecGuids(i);
        ParasitesOneGUID=zeros(length(Xparasites),6); 
        for p=1:length(Xparasites)
             hold on,plot3([Xvector(IDX(p));Xparasites(p)],[Yvector(IDX(p));Yparasites(p)],[Zvector(IDX(p));Zparasites(p)],'*-k')
            ParasitesOneGUID(p,1)=guid;
            ParasitesOneGUID(p,2)=Xparasites(p);
            ParasitesOneGUID(p,3)=Yparasites(p);
            ParasitesOneGUID(p,4)=Zparasites(p)-Zvector(IDX(p));
            ParasitesOneGUID(p,5)=TPI;
           ParasitesOneGUID(p,6)= AverageIntensityParasites(p);
        end
         xlim([0 512])
         ylim([0 512])
          zlim([0 512])
         saveas(gcf,[num2str(guid),'corrected.jpg']) ;
        AllParasites=[AllParasites;ParasitesOneGUID];
        close all;
        disp([num2str(projectSpecGuids(i)), ' processed']);
       else
        disp(['Onefile was missing for record' ,num2str(projectSpecGuids(i)), 'I''ve skipped it'])
        
   
    end
 
 end
   figure,
    Z=zeros(size(X));
    mesh(X,Y,Z); hold on;
    scatter3( AllParasites(:,2),AllParasites(:,3),AllParasites(:,4),3,AllParasites(:,6));
    figure,
    scatter3( AllParasites(:,6),AllParasites(:,5),AllParasites(:,4));
     
    