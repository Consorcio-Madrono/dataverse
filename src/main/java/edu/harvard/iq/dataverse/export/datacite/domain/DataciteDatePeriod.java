package edu.harvard.iq.dataverse.export.datacite.domain;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;

import java.util.logging.Level;

/**
 * Created by bmckinney on 11/29/16.
 */
public class DataciteDatePeriod {
    
    String start;
    String end;
    
    public DataciteDatePeriod() {}

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public static DataciteDatePeriod getDataciteDatePeriod(DatasetField datasetField) {
        DataciteDatePeriod datePeriod = new DataciteDatePeriod();
        for (DatasetFieldCompoundValue compoundVal : datasetField.getDatasetFieldCompoundValues()) {
            for (DatasetField child : compoundVal.getChildDatasetFields()) {
                String type = child.getDatasetFieldType().getName();
                if (type.equalsIgnoreCase("dateOfCollectionStart")) {
                    datePeriod.setStart(child.getValue());
                }
                if (type.equalsIgnoreCase("dateOfCollectionEnd")) {
                    datePeriod.setEnd(child.getValue());
                }
            }
        }
        return datePeriod;
    }

}
