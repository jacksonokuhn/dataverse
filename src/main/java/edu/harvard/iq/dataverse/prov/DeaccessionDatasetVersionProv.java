/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.prov;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author jacksonokuhn
 */
public class DeaccessionDatasetVersionProv extends Prov {
        
    DatasetVersion theVersion;
    
    public DeaccessionDatasetVersionProv(DatasetVersion versionIn){
        this.theVersion = versionIn;
    }
    
    public CPLObject execute(CommandContext ctxt) {
        
        Timestamp now = new Timestamp(new Date().getTime());
        
        // Connect to CPL
        tryAttachODBC("DSN=CPL");
        
        String originator = ctxt.systemConfig().getDataverseSiteUrl();
        String name = theVersion.getDataset().getIdentifier() + "." + theVersion.getFriendlyVersionNumber();
        
        CPLObject datasetProv = CPLObject.lookup(originator, name, CPLObject.ENTITY, null);
        
        datasetProv.addProperty("prov:invalidatedAtTime", now);
        
        // TODO potentionally add activity + 2 edges
        
        return datasetProv;
    }
}
