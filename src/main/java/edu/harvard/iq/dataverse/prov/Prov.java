/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.prov;

import edu.harvard.pass.cpl.*;

/**
 *
 * @author jacksonokuhn
 */
public class Prov {
    
    public Prov(){} 
    
    // Connect to CPL
    public static void tryAttachODBC(String ODBCString){
        try{
            if(!CPL.isAttached()){
                CPL.attachODBC(ODBCString);
            }
        } catch (CPLException e){
            if(e.getErrorCode().isError() || e.getErrorCode() != CPLDirectConstants.CPL_E_ALREADY_INITIALIZED){
                throw new CPLException(e.getErrorCode());
            }
        }
    }
    
    public static ProvFactory createFactoryWithDataverseBundle(String bundleName){
        
        String originator = ctxt.systemConfig().getDataverseSiteUrl();
        
        ProvFactory provFactory = 
                new ProvFactory(originator);
        
        CPLObject bundle = CPLObject.create(originator, bundleName, CPLObject.ENTITY, null);
        
        bundle.addProperty("dvrs", "http://guides.dataverse.org/"); // TODO
        
        provFactory.setBundle(bundle);
        
        return provFactory;
    }
}
