/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.oai;

import com.lyncode.xoai.model.oaipmh.Description;
import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.model.oaipmh.MetadataFormat;
import com.lyncode.xoai.model.oaipmh.Set;
import com.lyncode.xoai.serviceprovider.ServiceProvider;
import com.lyncode.xoai.serviceprovider.client.HttpOAIClient;
import com.lyncode.xoai.serviceprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import com.lyncode.xoai.serviceprovider.exceptions.NoSetHierarchyException;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import edu.harvard.iq.dataverse.harvest.client.FastGetRecord;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Leonid Andreev
 */
public class OaiHandler implements Serializable {
    private static final Logger logger = Logger.getLogger(OaiHandler.class.getCanonicalName());

    public OaiHandler() {
        
    }
    
    public OaiHandler(String baseOaiUrl) {
        this.baseOaiUrl = baseOaiUrl; 
    }
    
    public OaiHandler(String baseOaiUrl, String metadataPrefix) {
        this.baseOaiUrl = baseOaiUrl;
        this.metadataPrefix = metadataPrefix; 
    }
      
    public OaiHandler(HarvestingClient harvestingClient) throws OaiHandlerException {
        this.baseOaiUrl = harvestingClient.getHarvestingUrl();
        this.metadataPrefix = harvestingClient.getMetadataPrefix();
        
        if (StringUtils.isEmpty(baseOaiUrl)) {
            throw new OaiHandlerException("Valid OAI url is needed to create a handler");
        }
        this.baseOaiUrl = harvestingClient.getHarvestingUrl();
        if (StringUtils.isEmpty(metadataPrefix)) {
            throw new OaiHandlerException("HarvestingClient must have a metadataPrefix to create a handler");
        }
        this.metadataPrefix = harvestingClient.getMetadataPrefix();
        
        if (!StringUtils.isEmpty(harvestingClient.getHarvestingSet())) {
            this.setName = harvestingClient.getHarvestingSet();
        }
        
        this.fromDate = harvestingClient.getLastNonEmptyHarvestTime();
        
        this.harvestingClient = harvestingClient;
    }
    
    private String baseOaiUrl; //= harvestingClient.getHarvestingUrl();
    private String metadataPrefix; // = harvestingClient.getMetadataPrefix();
    private String setName; 
    private Date   fromDate;
    private Boolean setListTruncated = false;
    
    private ServiceProvider serviceProvider; 
    
    private HarvestingClient harvestingClient; 
    
    public String getSetName() {
        return setName; 
    }
    
    public String getBaseOaiUrl() {
        return baseOaiUrl; 
    }
    
    public Date getFromDate() {
        return fromDate;
    }
    
    public String getMetadataPrefix() {
        return metadataPrefix; 
    }
    
    public HarvestingClient getHarvestingClient() {
        return this.harvestingClient;
    }
    
    public void withSetName(String setName) {
        this.setName = setName;
    }
        
    public void withMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }
    
    public void withFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }
    
    public void setHarvestingClient(HarvestingClient harvestingClient) {
        this.harvestingClient = harvestingClient; 
    }
    
    public boolean isSetListTruncated() {
        return setListTruncated;
    }
    
    private ServiceProvider getServiceProvider() throws OaiHandlerException {
        if (serviceProvider == null) {
            if (baseOaiUrl == null) {
                throw new OaiHandlerException("Could not instantiate Service Provider, missing OAI server URL.");
            }
            Context context = new Context();

            context.withBaseUrl(baseOaiUrl);
            context.withGranularity(Granularity.Second);
            context.withOAIClient(new HttpOAIClient(baseOaiUrl));

            serviceProvider = new ServiceProvider(context);
        }
        
        return serviceProvider;
    }
    
    public ArrayList<String> runListSets() throws OaiHandlerException {
    
        ServiceProvider sp = getServiceProvider(); 
        
        Iterator<Set> setIter;
        
        long startMilSec = new Date().getTime();
        
        try {
            setIter = sp.listSets();
        } catch (NoSetHierarchyException nshe) {
            return null; 
        } catch (InvalidOAIResponse ior) {
            throw new OaiHandlerException("No valid response received from the OAI server.");
        }
        
        ArrayList<String> sets = new ArrayList<>();

        int count = 0;
        
        while ( setIter.hasNext()) {
            count++;
            Set set = setIter.next();
            String setSpec = set.getSpec();
            /*
            if (set.getDescriptions() != null && !set.getDescriptions().isEmpty()) {
                Description description = set.getDescriptions().get(0);
                
            }
            */
            
            if (count >= 100) {
                // Have we been waiting more than 30 seconds?
                if (new Date().getTime() - startMilSec > 30000) {
                    setListTruncated = true;
                    break; 
                }
            }
                         
            if (!StringUtils.isEmpty(setSpec)) {
                sets.add(setSpec);
            }
        }

        if (sets.size() < 1) {
            return null;
        }
        return sets; 
        
    }
    
    public List<String> runListMetadataFormats() throws OaiHandlerException {
        ServiceProvider sp = getServiceProvider();
                
        Iterator<MetadataFormat> mfIter;
        
        try {
            mfIter = sp.listMetadataFormats();
        } catch (InvalidOAIResponse ior) {
            throw new OaiHandlerException("No valid response received from the OAI server."); 
        }
        
        List<String> formats = new ArrayList<>();

        while ( mfIter.hasNext()) {
            MetadataFormat format = mfIter.next();
            String formatName = format.getMetadataPrefix();
            if (!StringUtils.isEmpty(formatName)) {
                formats.add(formatName);
            }
        }

        if (formats.size() < 1) {
            return null; 
        }
        
        return formats;
    }
    
    public Iterator<Header> runListIdentifiers() throws OaiHandlerException {
        ListIdentifiersParameters parameters = buildListIdentifiersParams();
        String trace= "valid:" + parameters.areValid() + "; prefix:" + parameters.getMetadataPrefix() + 
                    "; set:" + parameters.getSetSpec()+ "; from:" + parameters.getFrom() + "; until:" + parameters.getUntil() + "; granularity:" + parameters.getGranularity();
        try {
            logger.log(Level.ALL, trace);
            System.err.println (trace);
            return getServiceProvider().listIdentifiers(parameters);
        } catch (BadArgumentException bae) {
            bae.printStackTrace();
            throw new OaiHandlerException(trace + " BadArgumentException thrown when attempted to run ListIdentifiers");
        }
                
    }
    
    public FastGetRecord runGetRecord(String identifier) throws OaiHandlerException { 
        if (StringUtils.isEmpty(this.baseOaiUrl)) {
            throw new OaiHandlerException("Attempted to execute GetRecord without server URL specified.");
        }
        if (StringUtils.isEmpty(this.metadataPrefix)) {
            throw new OaiHandlerException("Attempted to execute GetRecord without metadataPrefix specified");
        }
        
        try {
            return new FastGetRecord(this.baseOaiUrl, identifier, this.metadataPrefix);
        } catch (ParserConfigurationException pce) {
            throw new OaiHandlerException("ParserConfigurationException executing GetRecord: "+pce.getMessage());
        } catch (SAXException se) {
            throw new OaiHandlerException("SAXException executing GetRecord: "+se.getMessage());
        } catch (TransformerException te) {
            throw new OaiHandlerException("TransformerException executing GetRecord: "+te.getMessage());
        } catch (IOException ioe) {
            throw new OaiHandlerException("IOException executing GetRecord: "+ioe.getMessage());
        }
    }
    
    
    private ListIdentifiersParameters buildListIdentifiersParams() throws OaiHandlerException {
        ListIdentifiersParameters mip = ListIdentifiersParameters.request();
        
        if (StringUtils.isEmpty(this.metadataPrefix)) {
            throw new OaiHandlerException("Attempted to create a ListIdentifiers request without metadataPrefix specified");
        }
        mip.withMetadataPrefix(metadataPrefix);

/*      MADROÑO, We need to do always complete havester
        if (this.fromDate != null) {
            mip.withFrom(this.fromDate);
        }*/

        if (!StringUtils.isEmpty(this.setName)) {
            mip.withSetSpec(this.setName);
        }
        
        return mip;
    }
    
    public void runIdentify() {
        // not implemented yet
        // (we will need it, both for validating the remote server,
        // and to learn about its extended capabilities)
    }
}
