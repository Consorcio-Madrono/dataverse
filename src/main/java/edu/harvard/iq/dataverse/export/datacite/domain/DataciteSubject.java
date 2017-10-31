package edu.harvard.iq.dataverse.export.datacite.domain;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;

import java.util.ArrayList;
import java.util.List;

public class DataciteSubject {

    String scheme;
    String uri;
    String value;

    public DataciteSubject() {}

    public DataciteSubject(String value) {
        this.value = value;
    }

    public DataciteSubject(String value, String uri, String scheme) {
        this.value = value;
        this.uri = uri;
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public static List<DataciteSubject> getDataciteSubjects(DatasetField datasetField) {
        List<DataciteSubject> list = new ArrayList<>();
        for (DatasetFieldCompoundValue compoundVal : datasetField.getDatasetFieldCompoundValues()) {
            DataciteSubject dataciteSubject = new DataciteSubject();
            for (DatasetField child : compoundVal.getChildDatasetFields()) {
                String type = child.getDatasetFieldType().getName();
                if (type.equalsIgnoreCase("keywordValue") || type.equalsIgnoreCase("topicClassValue")) {
                    dataciteSubject.setValue(child.getValue());
                }
                if (type.equalsIgnoreCase("keywordVocabularyURI") || type.equalsIgnoreCase("topicClassVocabURI")) {
                    dataciteSubject.setUri(child.getValue());
                }
                if (type.equalsIgnoreCase("keywordVocabulary") || type.equalsIgnoreCase("topicClassVocab")) {
                    dataciteSubject.setScheme(child.getValue());
                }
            }
            list.add(dataciteSubject);
        }
        return list;
    }
}