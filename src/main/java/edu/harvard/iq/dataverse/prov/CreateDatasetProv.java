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
 * @author jacksonokuhn
 */
public class CreateDatasetProv extends Prov {
    
    Dataset theDataset;
    
    public CreateDatasetProv(Dataset datasetIn){
        this.theDataset = datasetIn;
    }
    
    public CPLObject execute(CommandContext ctxt) {
        
        // Connect to CPL
        tryAttachODBC("DSN=CPL");
        
        // Set up ProvFactory
        String bundleName = ""; // TODO figure out
        ProvFactor provFactory = createFactoryWithDataverseBundle(bundleName);
        
        // Create dataset object
        String datasetName = theDataset.getId() + 
                                "." + theDataset.getVersionNumber() + 
                                "." + theDataset.getMinorVersionNumber()
        CPLObject datasetProv = provFactory.createCollection(datasetName);
        datasetProv.addProperty("prov:generatedAtTime", theDataset.getCreateDate());
        
        // Find or create user and add relation
        CPLObject creatorProv = provFactory.lookupOrCreateAgent(theDataset.getCreator());
        provFactory.createWasAttributedTo(datasetProv, creatorProv);
        
        // Add datafiles
        for (DataFile file : theDataset.getFiles()){
            CPLObject oldFileProv = CPLObject.lookup(originator, file.getId(), CPLObject.ENTITY, null);
            if(oldFileProv != null){
                provFactory.createHadMember(datasetProv, oldFileProv);
            } else{
                CPLObject fileProv = provFactory.createEntity(file.getId());
                provFactory.createHadMember(datasetProv, fileProv);
                provFactory.createWasAttributedTo(fileProv, creatorProv);
            }
            // TODO maybe do something with FileMetadata?
        }
        
        // Connect to previous version if exists
        if(theDataset.getVersionNumber() > 1 || theDataset.getMinorVersionNumber() > 0){
            DatasetVersion prevVersion = theDataset.getVersions().get(1);
            CPLObject prevVersionProv = CPLObject.lookup(originator, 
                                            theDataset.getId() + "." + prevVersion.getFriendlyVersionNumber(),
                                            CPLObject.ENTITY, null);
            ProvFactory.createWasDerivedFrom(datasetProv, prevVersionProv);
        }
        
        return datasetProv;
    }
}
