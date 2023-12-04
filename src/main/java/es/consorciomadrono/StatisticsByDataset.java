/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package es.consorciomadrono;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JuanCorrales. Consorcio Madroño
 * 
 * CONSORCIO MADROÑO. New utility class to read statistics by datasets from the file dataset_metrics.xhtml
 * 
 */
public class StatisticsByDataset {
    private static final HashMap <Long /*DatasetId*/, DatasetMetricsByMonth> datasetMetricsByMonth= new HashMap ();
    
    public static DatasetMetricsByMonth getDatasetMetricsByMonth (Dataset dataset) {
        Long datasetId= dataset.getId();
        DatasetMetricsByMonth metricsByMonth= datasetMetricsByMonth.get(datasetId);
        if (metricsByMonth== null || (metricsByMonth.getCount()!= dataset.getDatasetMetrics().size())) { 
            metricsByMonth= new DatasetMetricsByMonth ();
            List <DatasetMetrics> metrics= dataset.getDatasetMetrics();
            int metricsSize= metrics.size();
            metricsByMonth.createDatasetMetricsByMonth(metrics, metricsSize);
            datasetMetricsByMonth.remove(datasetId);
            datasetMetricsByMonth.put(datasetId, metricsByMonth);
        }
        return datasetMetricsByMonth.get(datasetId);
    }
}
