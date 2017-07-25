/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.prov;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.pass.cpl.*;

/**
 *
 * @author eli
 */

//class for addition of prov on 
public class PublishDatasetProvCommand extends ProvCommand {
    
     @EJB
     DataFileServiceBean datafileService;
    

    
    // will be changed with the addition of datafile versioning
    private void AddDatafileProv(String originator, String byAgent, String dsName, Dataset dataset){
        
        ProvFactory provFactory = new ProvFactory(originator);
        int dFnum = 0;

        // iterates throught datafiles in dataset, if there is already an agent
        // responsible then do nothing, else new attribution edge
        List<FileMetadata> datafileMetadataList = datafile.getFileMetadatas();
        for (Integer i = 0; i < datafileMetadataList.size(); i++){
            //logic here   
        }
     
        
        // for changing file metadata just change the collection not dataset, and no need to iterate through
        // the datafiles
        // Edge = AncestryEntry
        
        for (DataFile datafile: dataset.getFiles()){
            
            DataFile currentDatafile = datafileService.find(datafile.getId());
            String storageId = currentDatafile.getStorageIdentifier();
            
            CPLObject createdDatafile = provFactory.createEntity(storageId);
            
            if(datafile.getPublicationDate() == null){
 
                CPLEdge isMemberOf = provFactory.createIsMemberOf(dsName, storageId);
                
                CPLEdge wasAttrTo = provFactory.createWasAttributedTo(StorageId, byAgent);
                
            }
            else if(CPLObject.lookup(originator, storageId, "activity") != null) { 
                
                Long curDatasetVersion = datafile.getFileMetadatas().get(dFnum).getDatasetVersion();
                
                // going to have to iterate through all of the versions to find the previous one :`(
                
                CPLEdge isMemberOf = pF.createIsMemberOf(dsName, storageId);
                
                CPLEdge wasAttrTo = pF.createWasAttributedTo(StorageId, byAgent);
                
                CPLEdge wasGenBY = pF.createWasGeneratedBy();
                
                dFnum++;
                
                //from dataset version get datset
                //datset has a list of dataset versions
                //sort by major version/ minor version number
                //iterate through
              
            
            }
            else {
            
            }
        }    
            //CPLObject createdDatafile1 = pF.createEntity(storageId);
            
           
            //if (pF.CPLObject.lookup(originator, storageId, "activity") == null) {
            //    CPLEdge wasAttrTo = pF.createWasAttributedTo(byAgent, storageId);
            
            
            //Integer datafileMetadataSize = datafile.getFileMetadatas().size();
            //List<FileMetadata> datafileMetadataList = datafile.getFileMetadatas();
            
            
            for (Integer i = 0; i < datafileMetadataSize; i++){
                //accessing metadata
                
                
                
                
                //String storageId = currentDatafile.getStorageIdentifier();

                
                
                
                
                msg(datafile.getFileMetadatas().get(i).getLabel());
                msg(datafile.getFileMetadatas().get(i).getDescription());
                msg(Long.toString(datafile.getFileMetadatas().get(i).getDatasetVersion().getMinorVersionNumber()));
                msg(Long.toString(datafile.getFileMetadatas().get(i).getDatasetVersion().getVersionNumber()));
                msg(Boolean.toString(datafile.isRestricted()));
            }
                
            }
            //
            
            msg("DF" + dFnum + "storage identifier: " + dataFile.getStorageIdentifier());
            
            dFnum++;
            
    }
     
        DATAFILE VERSIONING PSUEDOCODE

        on creation of new dataset iterate through datafiles

        in datafile record will see if is a new version

        if it is get the ID of that version and trace it back to the prev version

        draw the is replacement line

        
    
    
    //create an execute command here
    CPLObject execute
    /*
    // creation of provenance for either a completely new dataset, or a new version of a dataset
    public static  void createProv(String originator, String name, String agent, String versionTransformation, String versionNumber, Dataset theDataset){
        
        
        
            ProvFactory pF = new ProvFactory(originator);
            pF.setBundle(originator, name);

            CPLObject createdDataset = pF.createEntity(name);
            
            
            CPLObject byAgent = pF.lookupOrCreateAgent(agent);

            CPLEdge wasAttrTo = pF.createWasAttributedTo(byAgent, createdDataset);

            addDatafileProv(originator, agent, name, theDataset);
        

        //if the dataset is being created for the first time
        if (versionNumber != "1.0" || versionTransformation == null){
            CPLObject vrsTransformation = pF.createActivity(versionTransformation); 
            CPLEdge wasAssociatedWith = pF.createWasAssociatedWith(vrsTransformation, agent);
            
            // TODO: go into metadata to get the previous version
            CPLEdge wasUsedBy = pF.createWasUsedBy(vrsTransformation, prevDataset);
            CPLEdge wasGeneratedBy = pF.createWasGeneratedBy(createdDataset, vrsTransformation);
        }
        
        msg("version: "+theDataset.getLatestVersion()); 
        msg("versionMajor: "+theDataset.getVersionNumber());
        msg("versionMinor: "+theDataset.getMinorVersionNumber()); 
        
    }
    
    // creation of provenance for either the linking of 2 problem sets via transformation
    public static void createProv(String originator, String name, String agent, String parentName, String versionNumber, String datasetTransformation, Dataset theDataset){

        // add a parentOriginator or is that redundant?
        // probably yes, but leave it so that in the future allow for
        // communication between installations
        
        ProvFactory pF = new ProvFactory(originator);
        pF.setBundle(originator, name);

        CPLObject parentDataset = CPLObject.lookup(originator, parentName, "entity");
        CPLObject createdDataset = pF.newEntity(name);
        CPLObject byAgent = pF.createAgent(agent);
        
        CPLEdge wasAttrTo = pF.createWasAttributedTo(byAgent, createdDataset);
        CPLEdge usd = pF.createUsed(parentName, datasetTransformation);
        CPLEdge wasGenBy = pF.createWasGeneratedBy(datasetTransformation, parentName);
        
        addDatafileProv(originator, agent, theDataset);
        
        
    }  

    
}



        publishProv.setAgent(getUser().getIdentifier());
        publishProv.setOriginator(ctxt.systemConfig().getDataverseSiteUrl());
        publishProv.setVersionNumber(theDataset.getVersionNumber() + "." + theDataset.getMinorVersionNumber());
        publishProv.setDatasetTransformation("FromUI");
        publishProv.setDatasetName(theDataset.getIdentifier() + publishProv.getVersionNumber());
        //prefix with UUID's
        publishProv.setParentName("FromUI");
        
        int dFnum = 1;
        for (DataFile dataFile : theDataset.getFiles()) {
            for (FileMetadata datafile : dataFile.getFileMetadatas()) {
                publishProv.addToAddedFiles(dataFile.getFileMetadatas().get(dFnum).getLabel());
            }
        }
               /**
         *****
         *****
         */
        /*
        String originator = ctxt.systemConfig().getDataverseSiteUrl();

        String versionNumber = theDataset.getVersionNumber() + "." + theDataset.getMinorVersionNumber();
        String versionTransformation = "fromUI / tracked change";
        String datasetTransformation = "fromUI";
        String name = theDataset.getIdentifier() + versionNumber;
        String agent = getUser().getIdentifier();
        String parentName = "fromUI";
        // make dataset object for parentName
      

        /*
        things needed from UI:
        parent dataset : null, parent
        transformation : automatic/manual
        
         
        PublishDatasetProvCommand.msg("CPLObject.lookup(" + "originator: " + originator + ", name: " + name + ", type: entity");
        PublishDatasetProvCommand.msg("agent: " + agent);

        // if no datasetTransformation or parentDatset is given by the user then it is a new dataset or a new version of a dataset
        if (isNewDataset == true) {
            PublishDatasetProvCommand.createProv(originator, name, agent, versionTransformation, versionNumber, theDataset);

        } else {
            PublishDatasetProvCommand.createProv(originator, name, agent, parentName, datasetTransformation, versionNumber, theDataset);
        }

        PublishDatasetProvCommand.msg("version: " + theDataset.getLatestVersion());
        PublishDatasetProvCommand.msg("versionMajor: " + theDataset.getVersionNumber());
        PublishDatasetProvCommand.msg("versionMinor: " + theDataset.getMinorVersionNumber());

        int dFnum = 1;
        for (DataFile dataFile : theDataset.getFiles()) {

            for (FileMetadata datafile : dataFile.getFileMetadatas()) {
                msg("test");
                msg(dataFile.getFileMetadatas().get(dFnum).getLabel());

            }

            PublishDatasetProvCommand.msg("DF" + dFnum + "storage identifier: " + dataFile.getStorageIdentifier());

            dFnum++;
        }
        */
        /**
         * ****
         *****
         */