/*
 */
package es.consorciomadrono;

import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JuanCorrales. Consorcio Madroño
 * 
 * CONSORCIO MADROÑO. New utility class to read statistics by datasets from the file dataset_metrics.xhtml
 * 
 */
public class DatasetMetricsByMonth {
    Long id= (long) -1;
    int count= -1;
    private final HashMap<String, HashMap<String, Long>> uViewsByCountry    = new HashMap<>(); // The first key is the date, the second the country
    private final HashMap<String, HashMap<String, Long>> tViewsByCountry    = new HashMap<>(); // The first key is the date, the second the country
    private final HashMap<String, HashMap<String, Long>> uDownloadsByCountry= new HashMap<>(); // The first key is the date, the second the country
    private final HashMap<String, HashMap<String, Long>> tDownloadsByCountry= new HashMap<>(); // The first key is the date, the second the country
    private final HashMap<String, Long> viewsTotalByMonth     = new HashMap<>(); // The key is the date
    private final HashMap<String, Long> viewsUniqueByMonth    = new HashMap<>(); // The key is the date
    private final HashMap<String, Long> downloadsTotalByMonth = new HashMap<>(); // The key is the date
    private final HashMap<String, Long> downloadsUniqueByMonth= new HashMap<>(); // The key is the date

    
    private String globalEndDate = "0000";
    private String globalInitDate= "9999";
   
    private ArrayList <Long> getDataListWithMonths (int initYear, int initMonth, int endYear, int endMonth, HashMap <String, Long> dataMap) { 
        ArrayList <Long> dataList= new ArrayList ();
        String endDate    = ""+ endYear;
        endDate = endDate + (endMonth > 9 ? "-" + endMonth : "-0" + endMonth);
        String currentDate= "" + initYear;
        currentDate = currentDate + (initMonth > 9 ? "-" + initMonth: "-0" + initMonth);
        int currentYear   = initYear;
        int currentMonth  = initMonth;
//        Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "******** getDataListWithMonths {0}  {1}   {2}", new Object[]{currentDate.compareTo(endDate), currentDate, endDate});
        while (currentDate.compareTo(endDate)<= 0) {
            Long data= dataMap.get (currentDate);
            if (data!= null)
                dataList.add(data);
            else
                dataList.add(0L);
            
            currentMonth+= 1;
            if (currentMonth>12) {
                currentYear+= 1;
                currentMonth= 1;
            }
            currentDate= "" + currentYear;
            currentDate = currentDate + (currentMonth > 9 ? "-" + currentMonth: "-0" + currentMonth);
//            Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "******** getDataListWithMonths {0}  {1}   {2}", new Object[]{currentDate.compareTo(endDate), currentDate, endDate});
        }

        return dataList;
    }

    public ArrayList <String> getMonthsList () {
        if (globalInitDate.length()<7)
            return new ArrayList();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getMonthsList (initYear, initMonth, endYear, endMonth);
    }

    public ArrayList <String> getMonthsList (int initYear, int initMonth, int endYear, int endMonth) { 
//        Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## getMonthsList");
        ArrayList <String> monthsList= new ArrayList ();
        String endDate    = "\"" + endYear;
        endDate = endDate + (endMonth > 9 ? "-" + endMonth : "-0" + endMonth) + "\"";
        String currentDate= "\"" + initYear;
        currentDate = currentDate + (initMonth > 9 ? "-" + initMonth: "-0" + initMonth) + "\"";
        int currentYear   = initYear;
        int currentMonth  = initMonth;
        while (currentDate.compareTo(endDate)<= 0) {
//            Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## getMonthsList {0}  {1}   {2}", new Object[]{currentDate, currentYear, currentMonth});
            monthsList.add(currentDate);
            
            currentMonth+= 1;
            if (currentMonth>12) {
                currentYear+= 1;
                currentMonth= 1;
            }
            currentDate= "\"" + currentYear;
            currentDate= currentDate + (currentMonth > 9 ? "-" + currentMonth: "-0" + currentMonth) + "\"";
        }

        return monthsList;
    }

    
    public HashMap <String, HashMap<String, Long>> getViewsUniqueByCountry (int initYear, int initMonth, int endYear, int endMonth) {
        return uViewsByCountry;
    }
    
    public HashMap<String, HashMap<String, Long>>  getDownloadsUniqueByCountry (int initYear, int initMonth, int endYear, int endMonth) {
        return uDownloadsByCountry;
    }
    
    public HashMap <String, HashMap<String, Long>> getViewsTotalByCountry (int initYear, int initMonth, int endYear, int endMonth) {
        return tViewsByCountry;
    }
    
    public HashMap<String, HashMap<String, Long>>  getDownloadsTotalByCountry (int initYear, int initMonth, int endYear, int endMonth) {
        return tDownloadsByCountry;
    }
    
    public ArrayList <String> getDataByMonthAndCountry (int initYear, int initMonth, int endYear, int endMonth, HashMap<String, HashMap<String, Long>> dataByCountry) {
        ArrayList <String> monthList=getMonthsList(initYear, initMonth, endYear, endMonth);
        
//        Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## getDataByMonthAndCountry dataByCountry: {0}", new Object[]{dataByCountry});
        ArrayList <String> dataByCountryAndMonthList= new ArrayList<>();
        for (String month: monthList) {
            String monthWithoutQuotes= month.replaceAll("\"", "");
            HashMap<String, Long> dataByCountryAndMonthMap= dataByCountry.get (monthWithoutQuotes);
            String dataByCountryAndMonth= "";
            if (dataByCountryAndMonthMap!= null) {
                for (String country: dataByCountryAndMonthMap.keySet ()) {
                    // Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## En el for");
                    dataByCountryAndMonth+= country + ":" + dataByCountryAndMonthMap.get(country) + ";";
                }
            } 
            dataByCountryAndMonthList.add("\"" + dataByCountryAndMonth + "\"");
        }
        return dataByCountryAndMonthList;
    }
   
    public ArrayList <Long> getViewsTotalByMonth (int initYear, int initMonth, int endYear, int endMonth) {
        return getDataListWithMonths (initYear, initMonth, endYear, endMonth, viewsTotalByMonth);
    }
    
    public ArrayList <Long>  getViewsUniqueByMonth (int initYear, int initMonth, int endYear, int endMonth) {
        ArrayList <Long> dataList= getDataListWithMonths (initYear, initMonth, endYear, endMonth, viewsUniqueByMonth);
//        Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "$$$$$$$$$$$$ getViewsUniqueByMonth: initYear: {0};  initMonth {1}; endYear: {2};  endMonth {2};", new Object[]{initYear, initMonth, endYear, endMonth});
        return dataList;
    }
    
    public ArrayList <Long>  getDownloadsTotalByMonth (int initYear, int initMonth, int endYear, int endMonth) {
        return getDataListWithMonths (initYear, initMonth, endYear, endMonth, downloadsTotalByMonth);
    }
    
    public ArrayList <Long>  getDownloadsUniqueByMonth (int initYear, int initMonth, int endYear, int endMonth) {
        return getDataListWithMonths (initYear, initMonth, endYear, endMonth, downloadsUniqueByMonth);
    }

    public ArrayList <String>  getDownloadsUniqueByMonthAndCountry () {
        if (globalInitDate.length()<7)
            return new ArrayList<> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getDataByMonthAndCountry (initYear, initMonth, endYear, endMonth, uDownloadsByCountry);
    }
    
    public ArrayList <String>  getDownloadsTotalByMonthAndCountry () {
        if (globalInitDate.length()<7)
            return new ArrayList<> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getDataByMonthAndCountry (initYear, initMonth, endYear, endMonth, tDownloadsByCountry);
    }
    
    public ArrayList <String>  getViewsUniqueByMonthAndCountry () {
        if (globalInitDate.length()<7)
            return new ArrayList<> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getDataByMonthAndCountry (initYear, initMonth, endYear, endMonth,uViewsByCountry);
    }

    public ArrayList <String>  getViewsTotalByMonthAndCountry () {
        if (globalInitDate.length()<7)
            return new ArrayList<> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getDataByMonthAndCountry (initYear, initMonth, endYear, endMonth,tViewsByCountry);
    }

    public ArrayList <Long>  getDownloadsTotalByMonth () {
        if (globalInitDate.length()<7)
            return new ArrayList<Long> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getDownloadsTotalByMonth (initYear, initMonth, endYear, endMonth);
    }
    
    public ArrayList <Long>  getDownloadsUniqueByMonth () {
        if (globalInitDate.length()<7)
            return new ArrayList<Long> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getDownloadsUniqueByMonth (initYear, initMonth, endYear, endMonth);
    }
    
    public ArrayList <Long> getViewsTotalByMonth () {
        if (globalInitDate.length()<7)
            return new ArrayList<Long> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getViewsTotalByMonth (initYear, initMonth, endYear, endMonth);
    }
    
    public ArrayList <Long>  getViewsUniqueByMonth () {
        if (globalInitDate.length()<7)
            return new ArrayList<Long> ();
        int initYear = Integer.parseInt(globalInitDate.substring(0,4));
        int initMonth= Integer.parseInt(globalInitDate.substring(5,7));
        int endYear  = Integer.parseInt(globalEndDate.substring (0,4));
        int endMonth = Integer.parseInt(globalEndDate.substring (5,7));
        return getViewsUniqueByMonth (initYear, initMonth, endYear, endMonth);
    }
    
    private void addDataToMap (String date, long metricCount, String country, HashMap <String,Long> metricMap, HashMap<String, HashMap<String, Long>> countByCountry) {
//        Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "******** addDataToMap 1.");
        Long countTotal= metricMap.get(date);
        if (countTotal!= null) {

            //Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## addDataToMap1 date: {0}  countTotal:{1} metricCount:{2} metricMap: {3}", new Object[]{date, countTotal, metricCount, metricMap});
            countTotal+= metricCount;
            if (countByCountry.get(date).get(country)!= null) {
                HashMap <String, Long> countryMap= countByCountry.get(date);
                Long countByCountryAndMonth= countryMap.get(country);
                //Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## addDataToMap1 date: {0}  countByCountryAndMonth:{1} metricCount:{2} metricMap: {3}", new Object[]{date, countByCountryAndMonth, metricCount, metricMap});
                countByCountryAndMonth+= metricCount;
                countryMap.replace(country, countByCountryAndMonth);
                countByCountry.replace(date, countryMap);
            } else {
                HashMap <String, Long> countryMap= countByCountry.get(date);
                countryMap.put(country, metricCount);
                countByCountry.replace(date, countryMap);
                //Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## addDataToMap3 date: {0}  country:{1} countByCountry: {2}", new Object[]{date, country, countByCountry});
            }
            metricMap.replace(date, countTotal);
            //Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## addDataToMap1 date: {0}  countTotal:{1} metricMap: {2}", new Object[]{date, countTotal,  metricMap});
        }
        else {
            metricMap.put(date, metricCount);
            HashMap <String, Long> newCountByCountry= new HashMap<>();
            newCountByCountry.put(country, metricCount);
            countByCountry.put(date, newCountByCountry);
            //Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "######## addDataToMap4 date: {0}  metricCount:{1} metricMap: {2}", new Object[]{date, metricCount, metricMap});
        }
    }
    
    private void addDataToMap (String date, long metricCount, HashMap <String,Long> metricMap) {
        Long countTotal= metricMap.get(date);
        if (countTotal!= null) {
//            Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "########### Fecha: {0}; Antes countTotal: {1} ; metricCount {2}", new Object[]{date, countTotal, metricCount});
            metricCount= countTotal + metricCount;
            metricMap.replace(date, metricCount);
        }
        else {
//            Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "########### Fecha: {0}; NUEVA countTotal: {1} ; metricCount {2}", new Object[]{date, countTotal, metricCount});
            metricMap.put(date, metricCount);
        }
    }
    
    public String getInitDate () {
        return "\"" + globalInitDate + "\"";
    }
    
    public String getEndDate () {
        return "\"" + globalEndDate + "\"";
    }
    
    public static ArrayList <String> getCountriesList () {
        return DatasetPage.getCountriesList();
    }
    

    public DatasetMetricsByMonth createDatasetMetricsByMonth (List<DatasetMetrics> datasetMetricsList, int count) {
        this.count= count;
        for (DatasetMetrics metrics: datasetMetricsList) {
            if (id == -1)
                id= metrics.getDataset().getId();

            String fullDate = metrics.getMonthYear();
            if (fullDate!= null) {

                String date = fullDate.substring(0, 7);
//                Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "#####*****###### createDatasetMetricsByMonth globalInitDate: {0}; globalEndDate: {1} ; date {2}", new Object[]{globalInitDate, globalEndDate, date});
                if (date.compareTo(globalInitDate)< 0) {
                    globalInitDate= date;
                }
                if (date.compareTo(globalEndDate)> 0) {
                    globalEndDate= date;
                }
                if (date!= null) {
                    Long newViewsTotal      = metrics.getViewsTotalRegular();
                    Long newViewsUnique     = metrics.getViewsUniqueRegular();
                    Long newDownloadsTotal  = metrics.getDownloadsTotalRegular(); 
                    Long newDownloadsUnique = metrics.getDownloadsUniqueRegular();
                    String newCountry       = metrics.getCountryCode();
                    if (newCountry== null || newCountry.length()!=2) {
                        //Logger.getLogger(DatasetMetricsByMonth.class.getName()).log(Level.SEVERE, "#####***____****###### createDatasetMetricsByMonth. date: {0}; country {1}; length {2}", new Object[]{date, newCountry, newCountry.length()});
                        newCountry="--";
                    }
                    
                    if (newViewsTotal != null && newViewsTotal > 0L) 
                        addDataToMap(date, newViewsTotal, newCountry, viewsTotalByMonth,tViewsByCountry);
                    if (newViewsUnique != null && newViewsUnique > 0L) 
                        addDataToMap(date, newViewsUnique, newCountry, viewsUniqueByMonth,uViewsByCountry);
                    if (newDownloadsTotal != null && newDownloadsTotal > 0L) 
                        addDataToMap(date, newDownloadsTotal, newCountry, downloadsTotalByMonth,tDownloadsByCountry);
                    if (newDownloadsUnique != null && newDownloadsUnique > 0L) 
                        addDataToMap(date, newDownloadsUnique, newCountry, downloadsUniqueByMonth,uDownloadsByCountry);
               }
            }
        }
        return this;
    }

    public int getCount () {
        return count;
    }
}