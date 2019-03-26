package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.datacite.DataciteTemplateWriter;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import freemarker.template.TemplateException;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(Exporter.class)
public class OpenAireDataciteExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(OpenAireDataciteExporter.class.getName());

    public static String DATACITE_XML_NAMESPACE = "http://datacite.org/schema/kernel-4";
    public static String DATACITE_XML_SCHEMALOCATION = "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd";
    public static String DATACITE_XML_SCHEMA_URL = "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd";
    public static String DEFAULT_XML_VERSION = "4.0";
    public static String PROVIDER_NAME = "oai_datacite";
    public static String DISPLAY_NAME = "DataCite Metadata Schema 4.0";
    
    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream os) throws ExportException {
        try {
            DataciteTemplateWriter writer = new DataciteTemplateWriter();
            writer.setDataset(version.getDataset());
            writer.setSchemaUrl(DATACITE_XML_SCHEMA_URL);
            writer.process(os, true);
        } catch (TemplateException|IOException/*Exception*/ e) {
            throw new ExportException("Error performing DataCite export: " + e.getMessage());
        }
    }

    @Override
    public Boolean isXMLFormat() { return true; }

    @Override
    public Boolean isHarvestable() { return true; }

    @Override
    public Boolean isAvailableToUsers() { return true; }

    @Override
    public String getXMLNameSpace() throws ExportException { return DATACITE_XML_NAMESPACE; }

    @Override
    public String getXMLSchemaLocation() throws ExportException { return DATACITE_XML_SCHEMALOCATION; }

    @Override
    public String getXMLSchemaVersion() throws ExportException { return DEFAULT_XML_VERSION; }

    @Override
    public String getProviderName() { return PROVIDER_NAME; }

    @Override
    public String getDisplayName() { return  DISPLAY_NAME; }

    @Override
    public void setParam(String name, Object value) { }
}
