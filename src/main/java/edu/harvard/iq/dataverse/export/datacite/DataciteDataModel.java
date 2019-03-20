package edu.harvard.iq.dataverse.export.datacite;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.export.datacite.domain.DataciteDatePeriod;
import edu.harvard.iq.dataverse.export.datacite.domain.DataciteFunder;
import edu.harvard.iq.dataverse.export.datacite.domain.DatacitePerson;
import edu.harvard.iq.dataverse.export.datacite.domain.DatacitePublication;
import edu.harvard.iq.dataverse.export.datacite.domain.DataciteSubject;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;


public class DataciteDataModel {

    private static final Logger logger = Logger.getLogger(DataciteDataModel.class.getCanonicalName());

    private Map<String, Object> data = new HashMap<>();
    
    public DataciteDataModel() { 
    }
    
    public DataciteDataModel(Dataset dataset) {
        setDataset(dataset);
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setdata(Map<String, Object> data) {
        this.data = data;
    }
    
    public void setDataset(Dataset dataset) {
        DatasetVersion version = dataset.getLatestVersion();
        
        // add required datacite metadata
        data.put("title", version.getTitle());
        data.put("version", version.getFriendlyVersionNumber());
        data.put("publisher", version.getRootDataverseNameforCitation());
        data.put("publisherYear", version.getVersionYear());
        data.put("lastUpdated", new SimpleDateFormat("yyyy-MM-dd").format(version.getLastUpdateTime()));
        data.put("description", version.getDescription().replaceAll("<","&lt;").replaceAll(">","&gt;"));

        //data.put("contacts", version.getDatasetContacts());
        //data.put("producers", version.getDatasetProducers());

        // rights
        if (version.getTermsOfUseAndAccess().getLicense() != null) {
            if (version.getTermsOfUseAndAccess().getLicense().equals(TermsOfUseAndAccess.License.CC0)) {
                data.put("rights", "CC0 1.0 Universal");
                data.put("rightsUri", "https://creativecommons.org/publicdomain/zero/1.0/");
            } else {
                data.put("rights", version.getTermsOfUseAndAccess().getTermsOfUse().replaceAll("<","&lt;").replaceAll(">","&gt;"));
            }
        }

        // identifier
        // compose DOI if necessary
        data.put("identifierType", dataset.getProtocol().toUpperCase());
        if (dataset.getProtocol().equalsIgnoreCase("DOI")) {
            data.put("identifier", dataset.getAuthority() + dataset.getDoiSeparator() + dataset.getIdentifier());
        } else {
            data.put("identifier", dataset.getIdentifier());
        }
        // authors
        data.put("authors", version.getDatasetAuthors());
        // language default
        data.put("language", "en-us");

        // load datasetfields
        List<DataciteSubject> subjectList = new ArrayList<>();
        List<DatacitePerson> contributorList = new ArrayList<>();
        List<DatacitePublication> publicationList = new ArrayList<>();
        List<DataciteFunder> funderList = new ArrayList<>();
        for (DatasetField field : version.getDatasetFields()) {

            String fname = field.getDatasetFieldType().getName();

            // add all subjects, keywords and topics
            if (fname.equalsIgnoreCase("keyword")) {
                subjectList.addAll(DataciteSubject.getDataciteSubjects(field));
            }
            if (fname.equalsIgnoreCase("topicClassification")) {
                subjectList.addAll(DataciteSubject.getDataciteSubjects(field));
            }
            if (fname.equalsIgnoreCase("subject")) {
                for (String subject : field.getValues()) {
                    subjectList.add(new DataciteSubject(subject));
                }
            }
            // contributors
            if (fname.equalsIgnoreCase("contributor")) {
                contributorList.addAll(DatacitePerson.getDatacitePersons(field));
            }
            // language
            if (fname.equalsIgnoreCase("language")) {
		if (LanguageAlpha3Code.findByName(field.getValue()).size()>0)
                  data.put("language", LanguageAlpha3Code.findByName(field.getValue()).get(0).name());
            }
            // resource type
            if (fname.equalsIgnoreCase("kindOfData")) {
                data.put("resourceType", field.getValue());
            }
            // alternative title
            if (fname.equalsIgnoreCase("alternativeTitle")) {
                data.put("alternativeTitle", field.getValue());
            }
            // subtitle
            if (fname.equalsIgnoreCase("subtitle")) {
                data.put("subtitle", field.getValue());
            }
            // date: deposited or submitted todo: is this correct?
            if (fname.equalsIgnoreCase("dateOfDeposit")) {
                data.put("dateOfDeposit", field.getValue());
            }
            // date: collected - todo: begin or end date?
            if (fname.equalsIgnoreCase("dateOfCollection")) {
                data.put("dateOfCollection", DataciteDatePeriod.getDataciteDatePeriod(field));
            }
            if (fname.equalsIgnoreCase("publication")) {
                publicationList.addAll(DatacitePublication.getDatacitePublications(field));
            }
            if (fname.equalsIgnoreCase("grantNumber")) {
                funderList.addAll(DataciteFunder.getDataciteFunders(field));
            }
        }
        // add special lists
        data.put("subjects", subjectList);
        data.put("contributors", contributorList);
        data.put("publications", publicationList);
        data.put("funders", funderList);
        
    }

    /**
     * Validate the DataCite XML
     * @return validation result: VALID or NOT_VALID
     */
    public String validate(String xml, String schemaUrl) {
        StringBuilder validateMessage = new StringBuilder();
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new URL(schemaUrl));
            Validator validator = schema.newValidator();
            try {
                validator.validate(new StreamSource(new StringReader(xml)));
                validateMessage.append("VALID");
            } catch (SAXException e) {
                validateMessage.append("NOT_VALID - ").append(e.getLocalizedMessage());
            }
        } catch(Exception e) {
            validateMessage.append("NOT_VALID - ").append(e.getMessage());
        }
        return validateMessage.toString();
    }
    
}
