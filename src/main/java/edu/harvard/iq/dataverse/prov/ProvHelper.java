/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.prov;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.pass.cpl.*;
import java.sql.Timestamp;
import java.util.Date;
import javax.ejb.EJB;

/**
 *
 * @author jacksonokuhn
 */
public class ProvHelper {   
    
    @EJB
    SystemConfig systemConfig;
    
    String originator;
    
    static String ODBCString = "DSN=CPL"; // TODO Will need updating with Dataverse db info
        
    public ProvHelper(){
        this.originator = systemConfig.getDataverseSiteUrl(); 
    }
    
    //TODO consider initializing and attaching on dv startup instead
    
    // Connect to CPL
    private static void tryAttachODBC(){
 
        try{
            if(!CPL.isAttached()){
                CPL.attachODBC(ODBCString);
            }
        } catch (CPLException e){
            if(CPLException.isError(e.getErrorCode()) && e.getErrorCode() != -5){
                throw new CPLException(e.getErrorCode());
            }
        }
    }
    
    private ProvFactory createFactoryWithDataverseBundle(String bundleName){      
              
        ProvFactory provFactory = 
                new ProvFactory(this.originator);
        
        CPLObject bundle = CPLObject.create(this.originator, bundleName, CPLObject.ENTITY, null);
        
        bundle.addProperty("dv", this.originator);
        
        provFactory.setBundle(bundle);
        
        return provFactory;
    }
    
    public CPLObject publishDatasetProv(DatasetVersion theVersion, User user) {
        
        // Connect to CPL
        tryAttachODBC();
        
        // Set up ProvFactory
        Dataset theDataset = theVersion.getDataset();
        String datasetVersionName = "dv:" + theDataset.getIdentifier() + 
                                "-" + theVersion.getFriendlyVersionNumber();
        String bundleName = datasetVersionName + "-publish";
        ProvFactory provFactory = createFactoryWithDataverseBundle(bundleName);
        CPLObject datasetProv = provFactory.createCollection(datasetVersionName);
        
        // Create dataset object
        datasetProv.addProperty("prov:generatedAtTime", theVersion.getReleaseTime().toString());

        // Connect to previous version if exists
        if(theVersion.getVersionNumber() > 1 || theVersion.getMinorVersionNumber() > 0){
            DatasetVersion prevVersion = theDataset.getVersions().get(theDataset.getVersions().indexOf(theVersion) + 1);
            CPLObject prevVersionProv = CPLObject.tryLookup(originator, 
                                            theDataset.getIdentifier() + "-" + prevVersion.getFriendlyVersionNumber(),
                                            CPLObject.ENTITY, null);
            if(prevVersionProv == null){
                this.publishDatasetProv(prevVersion, null);
                prevVersionProv = CPLObject.lookup(originator, 
                                            theDataset.getIdentifier() + "-" + prevVersion.getFriendlyVersionNumber(),
                                            CPLObject.ENTITY, null);
            }
            
            provFactory.createWasDerivedFrom(datasetProv, prevVersionProv);
        }
        
        // Create publish activity
        CPLObject publishProv = provFactory.createActivity(datasetVersionName + "-publish");
        provFactory.createWasGeneratedBy(datasetProv, publishProv);
        
        // Find or create contributors and add relations
        for(String identifier: theVersion.getVersionContributorIdentifiers()){
            CPLObject contributorProv = CPLObject.tryLookup(originator, "dv:" + identifier, CPLObject.AGENT, null);
            if(contributorProv == null){
                contributorProv = provFactory.createAgent(identifier);
            }
            provFactory.createWasAttributedTo(datasetProv, contributorProv);
        }
        
        // Find or create publisher and add relations
        if(user instanceof AuthenticatedUser){
            CPLObject userProv = CPLObject.tryLookup(originator, "dv:" + user.getIdentifier(), CPLObject.AGENT, null);
            if(userProv == null){
                userProv = provFactory.createAgent("dv:" + user.getIdentifier());
            }
            provFactory.createWasAssociatedWith(datasetProv, userProv);
        }
        

        // Add datafiles
        for (FileMetadata metadata : theVersion.getFileMetadatas()){            
            CPLObject fileProv = CPLObject.lookup(originator, "dv:" + metadata.getDataFile().getId(), CPLObject.ENTITY, null);
            if(fileProv == null){
                fileProv = provFactory.createEntity("dv:" + metadata.getDataFile().getId());
            }
            CPLObject metaProv = provFactory.createEntity("dv:" + metadata.getDataFile().getId()+ "-" + theVersion.getFriendlyVersionNumber() + "-md");
            
            provFactory.createHadMember(datasetProv, fileProv);
            provFactory.createHadMember(datasetProv, metaProv);
            provFactory.createWasInfluencedBy(fileProv, metaProv);

            if(metadata.getDataFile().getPreviousDataFileId() != null){
                CPLObject prevFileProv = CPLObject.tryLookup(originator, "dv:" + metadata.getDataFile().getPreviousDataFileId(), CPLObject.ENTITY, null);
                if(prevFileProv == null){
                    prevFileProv = provFactory.createEntity("dv:" + metadata.getDataFile().getPreviousDataFileId());
                }
                provFactory.createWasDerivedFrom(fileProv, prevFileProv);
            }
        }
        
        return datasetProv;
    }
    
    public CPLObject deaccessionDatasetVersionProv(DatasetVersion theVersion, User user) {
        
        Timestamp now = new Timestamp(new Date().getTime());
        
        // Connect to CPL
        tryAttachODBC();
        
        String versionName = "dv:" + theVersion.getDataset().getIdentifier() + "-" + theVersion.getFriendlyVersionNumber();
        String bundleName = versionName + "-deaccession";
        
        ProvFactory provFactory = createFactoryWithDataverseBundle(bundleName);
                
        CPLObject datasetProv = CPLObject.tryLookup(originator, versionName, CPLObject.ENTITY, null);
        
        if(datasetProv == null){
            datasetProv = publishDatasetProv(theVersion, null);
        }
        
        datasetProv.addProperty("prov:invalidatedAtTime", now.toString());
        CPLObject deaccessionProv = provFactory.createActivity(versionName + "-deaccession");
        provFactory.createWasInvalidatedBy(datasetProv, deaccessionProv);
        if(user instanceof AuthenticatedUser){
            CPLObject userProv = CPLObject.tryLookup(originator, "dv:" + user.getIdentifier(), CPLObject.AGENT, null);
            if(userProv == null){
                userProv = provFactory.createAgent("dv:" + user.getIdentifier());
            }
            provFactory.createWasAssociatedWith(datasetProv, userProv);
        }            
                
        return datasetProv;
    }
    
    // Unused for now
    public CPLObject deleteDatasetProv(Dataset theDataset, User user) {
        
        Timestamp now = new Timestamp(new Date().getTime());
        
        // Connect to CPL
        tryAttachODBC();
        
        DatasetVersion theVersion = theDataset.getReleasedVersion();
        
        if(theVersion == null){
            return null;
        }
        
        String versionName = "dv:" + theVersion.getDataset().getIdentifier() + "-" + theVersion.getFriendlyVersionNumber();
        String bundleName = versionName + "-delete";
        
        ProvFactory provFactory = createFactoryWithDataverseBundle(bundleName);
                
        CPLObject datasetProv = CPLObject.tryLookup(originator, versionName, CPLObject.ENTITY, null);
        
        if(datasetProv == null){
            datasetProv = publishDatasetProv(theVersion, null);
        }
        
        datasetProv.addProperty("prov:invalidatedAtTime", now.toString());
        CPLObject deaccessionProv = provFactory.createActivity(versionName + "-delete");
        provFactory.createWasInvalidatedBy(datasetProv, deaccessionProv);
        if(user instanceof AuthenticatedUser){
            CPLObject userProv = CPLObject.tryLookup(originator, "dv:" + user.getIdentifier(), CPLObject.AGENT, null);
            if(userProv == null){
                userProv = provFactory.createAgent("dv:" + user.getIdentifier());
            }
            provFactory.createWasAssociatedWith(datasetProv, userProv);        
        }
                
        return datasetProv;
    }
    
    public CPLObject importFileProvJSON(String filepath, String bundleName, DataFile anchorFile, AuthenticatedUser user){
            
        tryAttachODBC();
        
        CPLObject anchorProv = CPLObject.tryLookup(originator, "dv:" + anchorFile.getId(), CPLObject.ENTITY, null);
        CPLObject userProv = CPLObject.tryLookup(originator, "dv:" + user.getIdentifier(), CPLObject.AGENT, null);
        
        return CPLJsonUtility.importJson(filepath, originator, bundleName, anchorProv, userProv);
    }
    
    public void exportProvBundleJSON(CPLObject bundle, String filepath){
        CPLJsonUtility.exportBundleJson(bundle, filepath);
    }
    
    public CPLRelation createDatasetDependencyProv(DatasetVersion userVersion, DatasetVersion usedVersion){
        
        tryAttachODBC();

        String userVersionName = "dv:" + userVersion.getDataset().getIdentifier() + "-" + userVersion.getFriendlyVersionNumber();
        String usedVersionName = "dv:" + usedVersion.getDataset().getIdentifier() + "-" + usedVersion.getFriendlyVersionNumber();
        String bundleName = "dv:" +  userVersion.getDataset().getIdentifier() + "-" + userVersion.getFriendlyVersionNumber() + "->" +
                        usedVersion.getDataset().getIdentifier() + "-" + usedVersion.getFriendlyVersionNumber();        
        
        CPLObject bundleProv = CPLObject.create(originator, bundleName, CPLObject.BUNDLE, null);
        
        CPLObject userVersionProv = CPLObject.tryLookup(originator, userVersionName, CPLObject.ENTITY, null);
        if(userVersionProv == null){
            userVersionProv = publishDatasetProv(userVersion, null);
        }
        
        CPLObject usedVersionProv = CPLObject.tryLookup(originator, usedVersionName, CPLObject.ENTITY, null);
        if(usedVersionProv == null){
            usedVersionProv = publishDatasetProv(usedVersion, null);
        }
        
        return CPLRelation.create(userVersionProv, usedVersionProv, CPLRelation.WASINFLUENCEDBY, bundleProv);
    }
    
    // TODO Function for connecting papers to datasets provenance
}
