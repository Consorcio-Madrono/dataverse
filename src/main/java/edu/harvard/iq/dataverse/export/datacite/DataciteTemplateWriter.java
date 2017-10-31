package edu.harvard.iq.dataverse.export.datacite;

import edu.harvard.iq.dataverse.Dataset;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.TemplateException;
import freemarker.template.MalformedTemplateNameException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataciteTemplateWriter {

    private static final Logger logger = Logger.getLogger(DataciteTemplateWriter.class.getName());

    private Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
    private DataciteDataModel dataciteDataModel = new DataciteDataModel();
    private Dataset dataset;
    private TemplateLoader templateLoader;
    private String template = "datacite_40.ftl";
    private String schemaUrl;
    
    public DataciteTemplateWriter() {

        ClassTemplateLoader ctl = new ClassTemplateLoader(this.getClass(), "/templates");
        cfg.setTemplateLoader(ctl);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);

    }

    public Configuration getCfg() {
        return cfg;
    }

    public void setCfg(Configuration cfg) {
        this.cfg = cfg;
    }

    public DataciteDataModel getDataciteDataModel() {
        return dataciteDataModel;
    }

    public void setDataciteDataModel(DataciteDataModel dataciteDataModel) {
        this.dataciteDataModel = dataciteDataModel;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
        this.dataciteDataModel.setDataset(dataset);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    public void setTemplateLoader(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
        cfg.setTemplateLoader(this.templateLoader);
    }

    public String getSchemaUrl() {
        return schemaUrl;
    }

    public void setSchemaUrl(String schemaUrl) {
        this.schemaUrl = schemaUrl;
    }

    public void process(OutputStream outputStream, boolean validate) /*throws Exception*/ throws  MalformedTemplateNameException, IOException, TemplateNotFoundException, TemplateException {
        if (validate) {
            StringWriter sw = new StringWriter();
            this.process(sw);
            String result = this.dataciteDataModel.validate(sw.toString(), this.getSchemaUrl());

            if (!result.equalsIgnoreCase("VALID")) {
                logger.log(Level.SEVERE, "VALIDATION ERROR: " + result);
                return;
            }
        }
        process(new OutputStreamWriter(outputStream));
    }
    
    private void process(Writer writer) /*throws Exception  */ throws TemplateNotFoundException, TemplateException, MalformedTemplateNameException, IOException {
        Template template = cfg.getTemplate(this.template);
        template.process(this.dataciteDataModel.getData(), writer);
	writer.flush ();
    }    
    
}
