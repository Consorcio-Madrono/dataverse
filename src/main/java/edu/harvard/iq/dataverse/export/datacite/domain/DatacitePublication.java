package edu.harvard.iq.dataverse.export.datacite.domain;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;

import java.util.ArrayList;
import java.util.List;

public class DatacitePublication {
    
    String citation;
    String url;
    String idNumber;
    String idType;
    
    public DatacitePublication() {}

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public static List<DatacitePublication> getDatacitePublications(DatasetField datasetField) {
        List<DatacitePublication> list = new ArrayList<>();
        for (DatasetFieldCompoundValue compoundVal : datasetField.getDatasetFieldCompoundValues()) {
            DatacitePublication datacitePublication = new DatacitePublication();
            for (DatasetField child : compoundVal.getChildDatasetFields()) {
                String type = child.getDatasetFieldType().getName();
                if (type.equalsIgnoreCase("publicationURL")) {
                    datacitePublication.setUrl(child.getValue());
                }
                if (type.equalsIgnoreCase("publicationIDType")) {
                    String typeValue = child.getValue();
		    if (typeValue != null) {
                      if (typeValue.equalsIgnoreCase("doi"))
                          typeValue = typeValue.toUpperCase();
                      datacitePublication.setIdType(typeValue);
		    }
                }
                if (type.equalsIgnoreCase("publicationIDNumber")) {
                    datacitePublication.setIdNumber(child.getValue());
                }
            }
            list.add(datacitePublication);
        }
        return list;
    }

}
