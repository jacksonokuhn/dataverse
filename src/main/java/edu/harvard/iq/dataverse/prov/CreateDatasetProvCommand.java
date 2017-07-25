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
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.pass.cpl.*;
/**
 *
 * @author jacksonokuhn
 */
public class CreateDatasetProvCommand extends AbstractCommand<Dataset> {
    
    Dataset theDataset;
    
    public CreateDatasetProvCommand(Dataset datasetIn){
        super(aRequest, theDataset.getOwner());
        this.theDataset = datasetIn;
    }
    
    public CPLObject execute(CommandContext ctxt) throws CommandException {
        
        // Connect to CPL
        try{
            if(!CPL.isAttached()){
                CPL.attachODBC("DSN=CPL");
            }
        } catch (CPLException e){
            if(e.getErrorCode() != CPLDirectConstants.CPL_E_ALREADY_INITIALIZED){
                throw new CommandException("CPL backend failed to attach.", this);
            }
        }
        
        // Set up ProvFactory
        String originator = ctxt.systemConfig().getDataverseSiteUrl();
        String bundleName = ""; //TODO
        
        ProvFactory provFactory = 
                new ProvFactory(originator);
        
        provFactory.setBundle(originator, bundleName);
        
        // Create dataset object
        String datasetName = theDataset.getIdentifier() + 
                                "." + theDataset.getVersionNumber() + 
                                "." + theDataset.getMinorVersionNumber()
        CPLObject datasetProv = provFactory.createCollection(datasetName);
        datasetProv.addProperty("prov:generatedAtTime", theDataset.getCreateDate());
        
        // Find or create user and add relation
        CPLObject creatorProv = provFactory.lookupOrCreateAgent(theDataset.getCreator());
        provFactory.createWasAttributedTo(datasetProv, creatorProv);
        
        // Add datafiles
        for (DataFile file : theDataset.getFiles()){
            CPLObject oldFileProv = CPLObject.lookup(originator, file.getStorageIdentifier(), CPLObject.ENTITY, null);
            if(oldFileProv != null){
                // TODO figure what to do here
                // probably check if updated then 
            } else{
                CPLObject fileProv = provFactory.createEntity(file.getStorageIdentifier());
                provFactory.createHadMember(datasetProv, fileProv);
                provFactory.createWasAttributedTo(fileProv, creatorProv);
            }
            // TODO maybe do something with FileMetadata?
            // TODO do files get new storage ids when they're reversioned?
        }
        
        //Connect to previous version
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
