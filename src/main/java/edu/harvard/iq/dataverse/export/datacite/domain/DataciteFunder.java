package edu.harvard.iq.dataverse.export.datacite.domain;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;

import java.util.ArrayList;
import java.util.List;

public class DataciteFunder {
    
    String name;
    String identifierType;
    String identifier;
    String awardUri;
    String awardNumber;
    String awardTitle;
    
    public DataciteFunder() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getAwardUri() {
        return awardUri;
    }

    public void setAwardUri(String awardUri) {
        this.awardUri = awardUri;
    }

    public String getAwardNumber() {
        return awardNumber;
    }

    public void setAwardNumber(String awardNumber) {
        this.awardNumber = awardNumber;
    }

    public String getAwardTitle() {
        return awardTitle;
    }

    public void setAwardTitle(String awardTitle) {
        this.awardTitle = awardTitle;
    }

    public static List<DataciteFunder> getDataciteFunders(DatasetField datasetField) {
        List<DataciteFunder> list = new ArrayList<>();
        for (DatasetFieldCompoundValue compoundVal : datasetField.getDatasetFieldCompoundValues()) {
            DataciteFunder dataciteFunder = new DataciteFunder();
            for (DatasetField child : compoundVal.getChildDatasetFields()) {
                String type = child.getDatasetFieldType().getName();
                if (type.equalsIgnoreCase("grantNumberValue")) {
                    dataciteFunder.setAwardNumber(child.getValue());
                }
                if (type.equalsIgnoreCase("grantNumberAgency")) {
                    dataciteFunder.setName(child.getValue());
                }
            }
            list.add(dataciteFunder);
        }
        return list;
    }
}
