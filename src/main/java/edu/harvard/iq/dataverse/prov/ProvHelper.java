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
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;

/**
 *
 * @author jacksonokuhn
 */
public class ProvHelper {   
    
    @EJB
    SystemConfig systemConfig;
    
    static String prefix = "dv";
    String namespace;
    
    static String ODBCString = "DSN=CPL"; // TODO Will need updating with Dataverse db info
        
    public ProvHelper(){
        this.namespace = systemConfig.getDataverseSiteUrl(); 
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
                new ProvFactory(namespace);
        
        CPLBundle bundle = CPLBundle.create(bundleName);
        
        bundle.addPrefix(prefix, namespace);
        
        provFactory.setBundle(bundle);
        
        return provFactory;
    }
    
    public CPLObject publishDatasetProv(DatasetVersion theVersion, User user) {
        
        // Connect to CPL
        tryAttachODBC();
        
        // Set up ProvFactory
        Dataset theDataset = theVersion.getDataset();
        String datasetVersionName = theDataset.getIdentifier() + 
                                "-" + theVersion.getFriendlyVersionNumber();
        String bundleName = datasetVersionName + "-publish";
        ProvFactory provFactory = createFactoryWithDataverseBundle(gbundleName);
        CPLObject datasetProv = provFactory.createCollection(datasetVersionName);
        
        // Create dataset object
        datasetProv.addProperty("prov", "generatedAtTime", theVersion.getReleaseTime().toString());

        // Connect to previous version if exists
        if(theVersion.getVersionNumber() > 1 || theVersion.getMinorVersionNumber() > 0){
            DatasetVersion prevVersion = theDataset.getVersions().get(theDataset.getVersions().indexOf(theVersion) + 1);
            CPLObject prevVersionProv = CPLObject.tryLookup(prefix, 
                                            theDataset.getIdentifier() + "-" + prevVersion.getFriendlyVersionNumber(),
                                            CPLObject.ENTITY, null);
            if(prevVersionProv == null){
                this.publishDatasetProv(prevVersion, null);
                prevVersionProv = CPLObject.lookup(prefix, 
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
            CPLObject contributorProv = CPLObject.tryLookup(prefix, identifier, CPLObject.AGENT, null);
            if(contributorProv == null){
                contributorProv = provFactory.createAgent(identifier);
            }
            provFactory.createWasAttributedTo(datasetProv, contributorProv);
        }
        
        // Find or create publisher and add relations
        if(user instanceof AuthenticatedUser){
            CPLObject userProv = CPLObject.tryLookup(prefix, user.getIdentifier(), CPLObject.AGENT, null);
            if(userProv == null){
                userProv = provFactory.createAgent(user.getIdentifier());
            }
            provFactory.createWasAssociatedWith(datasetProv, userProv);
        }
        

        // Add datafiles
        for (FileMetadata metadata : theVersion.getFileMetadatas()){            
            CPLObject fileProv = CPLObject.lookup(prefix, metadata.getDataFile().getId(), CPLObject.ENTITY, null);
            if(fileProv == null){
                fileProv = provFactory.createEntity(metadata.getDataFile().getId());
            }
            CPLObject metaProv = provFactory.createEntity(metadata.getDataFile().getId()+ "-" + theVersion.getFriendlyVersionNumber() + "-md");
            
            provFactory.createHadMember(datasetProv, fileProv);
            provFactory.createHadMember(datasetProv, metaProv);
            provFactory.createWasInfluencedBy(fileProv, metaProv);

            if(metadata.getDataFile().getPreviousDataFileId() != null){
                CPLObject prevFileProv = CPLObject.tryLookup(prefix, metadata.getDataFile().getPreviousDataFileId(), CPLObject.ENTITY, null);
                if(prevFileProv == null){
                    prevFileProv = provFactory.createEntity(metadata.getDataFile().getPreviousDataFileId());
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
        
        String versionName = theVersion.getDataset().getIdentifier() + "-" + theVersion.getFriendlyVersionNumber();
        String bundleName = versionName + "-deaccession";
        
        ProvFactory provFactory = createFactoryWithDataverseBundle(bundleName);
                
        CPLObject datasetProv = CPLObject.tryLookup(prefix, versionName, CPLObject.ENTITY, null);
        
        if(datasetProv == null){
            datasetProv = publishDatasetProv(theVersion, null);
        }
        
        datasetProv.addProperty("prov", "invalidatedAtTime", now.toString());
        CPLObject deaccessionProv = provFactory.createActivity(versionName + "-deaccession");
        provFactory.createWasInvalidatedBy(datasetProv, deaccessionProv);
        if(user instanceof AuthenticatedUser){
            CPLObject userProv = CPLObject.tryLookup(prefix, user.getIdentifier(), CPLObject.AGENT, null);
            if(userProv == null){
                userProv = provFactory.createAgent(user.getIdentifier());
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
        
        String versionName = theVersion.getDataset().getIdentifier() + "-" + theVersion.getFriendlyVersionNumber();
        String bundleName = versionName + "-delete";
        
        ProvFactory provFactory = createFactoryWithDataverseBundle(bundleName);
                
        CPLObject datasetProv = CPLObject.tryLookup(prefix, versionName, CPLObject.ENTITY, null);
        
        if(datasetProv == null){
            datasetProv = publishDatasetProv(theVersion, null);
        }
        
        datasetProv.addProperty("prov", "invalidatedAtTime", now.toString());
        CPLObject deaccessionProv = provFactory.createActivity(versionName + "-delete");
        provFactory.createWasInvalidatedBy(datasetProv, deaccessionProv);
        if(user instanceof AuthenticatedUser){
            CPLObject userProv = CPLObject.tryLookup(prefix, user.getIdentifier(), CPLObject.AGENT, null);
            if(userProv == null){
                userProv = provFactory.createAgent(user.getIdentifier());
            }
            provFactory.createWasAssociatedWith(datasetProv, userProv);        
        }
                
        return datasetProv;
    }
    
    public CPLObject importDataFileProvJSON(String json, String bundleName, DataFile anchorFile, String anchorFileName){
            
        tryAttachODBC();
        
        CPLObject anchorProv = CPLObject.tryLookup(prefix, anchorFile.getId(), CPLObject.ENTITY, null);
        
        Map <CPLObject, String> hm = new HashMap<CPLObject, String>();
        hm.put(anchorProv, anchorFileName)

        return CPLJsonUtility.importJson(json, bundleName, hm);
    }
    
    public CPLObject importDatasetVersionProvJSON(String json, String bundleName, DatasetVersion anchorDataset, String anchorDatasetName){
            
        tryAttachODBC();
        
        String versionName = anchorDataset.getDataset().getIdentifier() + "-" + anchorDataset.getFriendlyVersionNumber();
        CPLObject anchorProv = CPLObject.tryLookup(prefix, versionName, CPLObject.ENTITY, null);

        Map <CPLObject, String> hm = new HashMap<CPLObject, String>();
        hm.put(anchorProv, anchorDatasetName)

        return CPLJsonUtility.importJson(filepath, bundleName, hm);
    }
    
    public String exportProvBundleJSON(CPLBundle bundle){
        CPLBundle[] bundleArray= {bundle};
        return CPLJsonUtility.exportBundleJson(bundleArray);
    }
    
    public CPLRelation createDatasetDependencyProv(DatasetVersion userVersion, DatasetVersion usedVersion){
        
        tryAttachODBC();

        String userVersionName = userVersion.getDataset().getIdentifier() + "-" + userVersion.getFriendlyVersionNumber();
        String usedVersionName = usedVersion.getDataset().getIdentifier() + "-" + usedVersion.getFriendlyVersionNumber();
        String bundleName = userVersion.getDataset().getIdentifier() + "-" + userVersion.getFriendlyVersionNumber() + "->" +
                        usedVersion.getDataset().getIdentifier() + "-" + usedVersion.getFriendlyVersionNumber();        
        
        CPLBundle bundleProv = CPLBundle.create(bundleName);
        
        CPLObject userVersionProv = CPLObject.tryLookup(prefix, userVersionName, CPLObject.ENTITY, null);
        if(userVersionProv == null){
            userVersionProv = publishDatasetProv(userVersion, null);
        }
        
        CPLObject usedVersionProv = CPLObject.tryLookup(prefix, usedVersionName, CPLObject.ENTITY, null);
        if(usedVersionProv == null){
            usedVersionProv = publishDatasetProv(usedVersion, null);
        }
        
        return CPLRelation.create(userVersionProv, usedVersionProv, CPLRelation.WASINFLUENCEDBY, bundleProv);
    }
    
    // TODO Function for connecting papers to datasets provenance
}
