package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.validation.ConstraintViolation;



/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.AddDataset )
public class CreateDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    
    private static final Logger logger = Logger.getLogger(CreateDatasetVersionCommand.class.getName());
    
    final DatasetVersion newVersion;
    final Dataset dataset;
    
    private void msg(String str) {
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println(str);
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }
    
    public CreateDatasetVersionCommand(DataverseRequest aRequest, Dataset theDataset, DatasetVersion aVersion) {
        super(aRequest, theDataset);
        dataset = theDataset;
        newVersion = aVersion;
    }
    
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        DatasetVersion latest = dataset.getLatestVersion();
        if ( latest.isWorkingCopy() ) {
            // In the case of ImportType.MIGRATION, it's possible that 
            // the newVersion will be released (ie versions are not migrated in the order that 
            // they were created in the old system), so check the newVersion state 
            // before throwing an Exception
            if (newVersion.getVersionState().equals(VersionState.DRAFT)){
                throw new IllegalCommandException("Latest version is already a draft. Cannot add another draft", this);
            }
        }
        msg("TESTSETRESTESTS");
        
       msg("CDVCversion: "+dataset.getLatestVersion()); 
       int dFnum = 1;
       for (DataFile dataFile: dataset.getFiles() ){
            msg("DF" + dFnum + "storage identifier: " + dataFile.getStorageIdentifier());
            dFnum++;
        } 
       
        newVersion.setDatasetFields(newVersion.initDatasetFields());
     
        Set<ConstraintViolation> constraintViolations = newVersion.validate();
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
            }
            throw new IllegalCommandException(validationFailedString, this);
        }
        
        Iterator<DatasetField> dsfIt = newVersion.getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }
        Iterator<DatasetField> dsfItSort = newVersion.getDatasetFields().iterator();
        while (dsfItSort.hasNext()) {
            dsfItSort.next().setValueDisplayOrder();
        }
        
        List<FileMetadata> newVersionMetadatum = new ArrayList<>(latest.getFileMetadatas().size());
        for ( FileMetadata fmd : latest.getFileMetadatas() ) {
            FileMetadata fmdCopy = fmd.createCopy();
            fmdCopy.setDatasetVersion(newVersion);
            newVersionMetadatum.add( fmdCopy );
            logger.info( "added file metadata " + fmdCopy );
        }
        newVersion.setFileMetadatas(newVersionMetadatum);
        
        
        Timestamp now = new Timestamp(new Date().getTime());
        newVersion.setCreateTime(now);
        newVersion.setLastUpdateTime(now);
        dataset.setModificationTime(now);
        newVersion.setDataset(dataset);
        ctxt.em().persist(newVersion);

        // TODO make async
    //    ctxt.index().indexDataset(dataset);
        
        return newVersion;
    }
    
}
