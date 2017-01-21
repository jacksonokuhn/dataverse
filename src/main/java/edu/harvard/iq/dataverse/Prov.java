/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.Arrays;
import java.util.List;

/Users/eli/NetBeansProjects/Dataverse/src/main/java/edu/harvard/iq/dataverse/Prov.java

//December is release of provenance

/**
 *
 * @author eli
 */
public class Prov extends ProvCommand{
    
    private Boolean changedDSMetadata = false;
    private Boolean isNewDSVersion = false;
    private Boolean changedDFMetadata = false;
    private Boolean isNewDFVersion = false;
    private Boolean isNewDS = false;
    private Boolean isDeaccessed = false;
    private List<String> addedFiles;
    private List<String> removedFiles;
    private List<String> changedFiles;
    private String agent;
    private String originator;
    private String versionNumber;
    private String datasetTransformation;
    private String datasetName;  
    private String parentName;
    private String bundleProvUUID;
    
    private String uploadProvFile;
    
    
    
    public void setChangedDSMetadata(Boolean bool) {
        changedDSMetadata = bool;
    }
    
    public Boolean getChangedDSMetadata() {
        return this.changedDSMetadata;
    }
    
    public void setChangedDFMetadata(Boolean bool) {
        changedDSMetadata = bool;
    }
    
    public Boolean getChangedDFMetadata() {
        return this.changedDFMetadata;
    }
    
    public void setIsNewDS(Boolean bool) {
        isNewDS = bool;
    }
     
    public Boolean getIsNewDS() {
        return this.isNewDS;
    }
    
    public void setIsDeaccessed(Boolean bool) {
        isDeaccessed = bool;
    }
    
    public Boolean getIsDeaccessed() {
        return this.isDeaccessed;
    }
    
    public void addToAddedFiles(String FileId) {
        addedFiles.add(FileId);
    }
    
    public void removeFromAddedFiles(String FileId) {
        addedFiles.remove(FileId);
    }
     
    public List<String> getAddedFiles() {
        return this.addedFiles;
    }
    
    public void addToChangedFiles(String FileId) {
        changedFiles.add(FileId);
    }
    
    public void removeFromChangedFiles(String FileId) {
        changedFiles.remove(FileId);
    }
    
    public List<String> getChangedFiles() {
        return this.removedFiles;   
    }
    
    public Boolean addToRemovedFiles(String FileId) {
        return addedFiles.add(FileId);
    }
    
    public Boolean removeFromRemovedFiles(String FileId) {
        return addedFiles.remove(FileId);
    }
    
    public List<String> getRemovedFiles() {
        return this.removedFiles;
    }
    
    public Boolean isEmpty(List<String> filesList) {
        return filesList == Arrays.asList("");
    }
    
    public void setOriginator(String dataverseSiteUrl) {
       originator = dataverseSiteUrl; 
    }
    
    public String getOriginator() {
        return this.originator;
    }
    
    public void setVersionNumber(String vrsNumber) {
        versionNumber = vrsNumber;
    }
    
    public String getVersionNumber() {
        return this.versionNumber;
    }
    
    public void setDatasetTransformation(String transformation) {
        datasetTransformation = transformation;
    }
    
    public String getDatasetTransformation() {
        return this.datasetTransformation;
    }
    
    public void setDatasetName(String name) {
        datasetName = name;
    }
    
    public String getDatasetName() {
        return this.datasetName;
    }
    
    public void setParentName(String name) {
        parentName = name;
    }
    
    public String getParentName() {
        return this.parentName;
    } 
    
    public void setAgent(String setAgent) {
        agent = setAgent;
    }
    
    public String getAgent() {
        return this.agent;
    }
    
    public void setBundleProvUUID String setBundleProvUUID) {
        bundleProvUUID = setBundleProvUUID;
    }
    
    public String getBundleProvUUID() {
        
        return this.bundleProvUUID;
    }
}
