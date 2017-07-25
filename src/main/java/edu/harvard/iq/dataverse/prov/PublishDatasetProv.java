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
public class PublishDatasetProv extends Prov {    
    
    Dataset theDataset;
    
    public CreateDatasetProv(Dataset datasetIn){
        this.theDataset = datasetIn;
    }
    
    public CPLObject execute(CommandContext ctxt) {
        
        // Connect to CPL
        tryAttachODBC("DSN=CPL");
        
        String originator = ctxt.systemConfig().getDataverseSiteUrl();
        String name = theDataset.getId() + "." + theDataset.getVersionNumber() + 
                                "." + theDataset.getMinorVersionNumber();
        
        CPLObject datasetProv = CPLObject.lookup(originator, name, CPLObject.ENTITY, null);
        
        datasetProv.addProperty("dvrs:published", theDataset.getPublicationDate());
        
        return datasetProv;
    }
}