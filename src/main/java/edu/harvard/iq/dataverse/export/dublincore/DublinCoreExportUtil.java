/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.dublincore;

import com.google.gson.Gson;
import static edu.harvard.iq.dataverse.DOIDataCiteRegisterService.getLanguageCode;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.LicenseDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author skraffmi
 */
public class DublinCoreExportUtil {
 
    private static final Logger logger = Logger.getLogger(DublinCoreExportUtil.class.getCanonicalName());
    
    public static String OAI_DC_XML_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/"; 
    public static String OAI_DC_XML_SCHEMALOCATION = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
    
    public static String DC_XML_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    
    public static String DCTERMS_XML_NAMESPACE = "http://purl.org/dc/terms/";
    public static String DCTERMS_DEFAULT_NAMESPACE="http://dublincore.org/documents/dcmi-terms/";
    public static String DCTERMS_XML_SCHEMALOCATION="http://dublincore.org/schemas/xmls/qdc/dcterms.xsd";
    public static String DEFAULT_XML_VERSION = "2.0";
    
    public static String DC_FLAVOR_OAI = "dc";
    public static String DC_FLAVOR_DCTERMS = "dcterms";
    
    public static String DEFAULT_DC_FLAVOR = DC_FLAVOR_DCTERMS;
    protected static HashMap <String, String> recolectaAcronymsMap;
    protected static HashSet <String> recolectaAcronymsSet;

    protected static Map<String, String> langIsoCodes; // Madroño. Get the lang codes in the iso format

    public static void datasetJson2dublincore(JsonObject datasetDtoAsJson, OutputStream outputStream, String dcFlavor) throws XMLStreamException {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        //try {
        dto2dublincore(datasetDto, outputStream, dcFlavor);
        //} catch (XMLStreamException ex) {
        //    Logger.getLogger(DublinCoreExportUtil.class.getName()).log(Level.SEVERE, null, ex);
        //}
    }
    
    private static void dto2dublincore(DatasetDTO datasetDto, OutputStream outputStream, String dcFlavor) throws XMLStreamException {
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        if (DC_FLAVOR_DCTERMS.equals(dcFlavor)) {
		xmlw.writeStartDocument();
            xmlw.writeStartElement("metadata");
            xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xmlw.writeAttribute("xmlns:dc", DC_XML_NAMESPACE);
            xmlw.writeAttribute("xmlns:dcterms", DCTERMS_XML_NAMESPACE);
            xmlw.writeDefaultNamespace(DCTERMS_DEFAULT_NAMESPACE);
            //xmlw.writeAttribute("xsi:schemaLocation", DCTERMS_DEFAULT_NAMESPACE+" "+DCTERMS_XML_SCHEMALOCATION);
            createDC(xmlw, datasetDto, dcFlavor);
        } else if (DC_FLAVOR_OAI.equals(dcFlavor)) {      
            xmlw.writeStartElement("oai_dc:dc");        
            xmlw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xmlw.writeAttribute("xmlns:oai_dc", OAI_DC_XML_NAMESPACE);
            xmlw.writeAttribute("xmlns:dc", DC_XML_NAMESPACE);
            xmlw.writeAttribute("xsi:schemaLocation", OAI_DC_XML_NAMESPACE+" "+OAI_DC_XML_SCHEMALOCATION);
            //writeAttribute(xmlw, "version", DEFAULT_XML_VERSION);
            createOAIDC(xmlw, datasetDto, dcFlavor);
        }
        
        
        xmlw.writeEndElement(); // <metadata> or <oai_dc:dc>
        xmlw.flush();
    }
    
    //UPDATED by rmo-cdsp:
    // If the requested flavor is "OAI_DC" (the minimal, original 15 field format), 
    // we shuld NOT be exporting the extended, DCTERMS fields (aka not createDC)
    // - such as, for example, "dateSubmitted" ... (4.5.1?)
    // -- L.A. 
    // but use createOAIDC instead (the minimal, original 15 field format)
    
    private static void createDC(XMLStreamWriter xmlw, DatasetDTO datasetDto, String dcFlavor) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
  
        writeFullElement(xmlw, dcFlavor+":"+"title", dto2Primitive(version, DatasetFieldConstant.title));                       
        
        xmlw.writeStartElement(dcFlavor+":"+"identifier");
        xmlw.writeCharacters(globalId.toURL().toString());
        xmlw.writeEndElement(); // decterms:identifier       

        writeAuthorsElement(xmlw, version, dcFlavor);
        
        writeFullElement(xmlw, dcFlavor+":"+"publisher", datasetDto.getPublisher());
        writeFullElement(xmlw, dcFlavor+":"+"issued", datasetDto.getPublicationDate());
        
        writeFullElement(xmlw, dcFlavor+":"+"modified", datasetDto.getDatasetVersion().getLastUpdateTime());
        writeAbstractElement(xmlw, version, dcFlavor); // Description
        writeSubjectElement(xmlw, version, dcFlavor);   //Subjects and Key Words
        
        writeFullElementList(xmlw, dcFlavor+":"+"language", dto2PrimitiveList(version, DatasetFieldConstant.language));        
        
        writeRelPublElement(xmlw, version, dcFlavor);
        writeFullElement(xmlw, dcFlavor+":"+"date", dto2Primitive(version, DatasetFieldConstant.productionDate));  
        
        writeFullElement(xmlw, dcFlavor+":"+"contributor", dto2Primitive(version, DatasetFieldConstant.depositor));  
        
        writeContributorElement(xmlw, version, dcFlavor);
        writeFullElement(xmlw, dcFlavor+":"+"dateSubmitted", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit));  
        
        writeTimeElements(xmlw, version, dcFlavor);
        
        writeFullElementList(xmlw, dcFlavor+":"+"relation", dto2PrimitiveList(version, DatasetFieldConstant.relatedDatasets));

        writeFunderElement(xmlw, version); // MADROÑO
        
        writeFullElementList(xmlw, dcFlavor+":"+"type", dto2PrimitiveList(version, DatasetFieldConstant.kindOfData));
        
        writeFullElementList(xmlw, dcFlavor+":"+"source", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        
        //Geo Elements
        writeSpatialElements(xmlw, version, dcFlavor);
        
        //License and Terms
        LicenseDTO licDTO = version.getLicense();
        if(licDTO != null) {
            writeFullElement(xmlw, dcFlavor+":"+"license", licDTO.getName());
        }
        writeFullElement(xmlw, dcFlavor+":"+"rights", version.getTermsOfUse()); 
        writeFullElement(xmlw, dcFlavor+":"+"rights", version.getRestrictions()); 

    }
    
    private static void createOAIDC(XMLStreamWriter xmlw, DatasetDTO datasetDto, String dcFlavor) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
  
        writeFullElement(xmlw, dcFlavor+":"+"title", dto2Primitive(version, DatasetFieldConstant.title));                       
        
        xmlw.writeStartElement(dcFlavor+":"+"identifier");
        xmlw.writeCharacters(globalId.toURL().toString());
        xmlw.writeEndElement(); // decterms:identifier       

        writeAuthorsElement(xmlw, version, dcFlavor); //creator
        
        writeFullElement(xmlw, dcFlavor+":"+"publisher", datasetDto.getPublisher());
        
        writeAbstractElement(xmlw, version, dcFlavor); // Description
        writeSubjectElement(xmlw, version, dcFlavor);   //Subjects and Key Words
        
        // MADROÑO BEGIN 
        List<String> languages= dto2PrimitiveList(version, DatasetFieldConstant.language);
        
        List<String> isoLanguages= getIsoLanguages (languages); 
        writeFullElementList(xmlw, dcFlavor+":"+"language", isoLanguages);
        
        writeFullElement(xmlw, dcFlavor+":"+"date", dto2Primitive(version, DatasetFieldConstant.dateOfDeposit)); // MADROÑO. For us, the deposit date is more important one
            List <FileDTO> files= version.getFiles();
        for (FileDTO file: files) {
            String format= file.getDataFile().getContentType();
            writeFullElement(xmlw, dcFlavor+":"+"format", format); 
        }

        // MADROÑO END
        
        writeFullElement(xmlw, dcFlavor+":"+"contributor", dto2Primitive(version, DatasetFieldConstant.depositor));  
        
        writeContributorElement(xmlw, version, dcFlavor);
        
        writeFullElementList(xmlw, dcFlavor+":"+"relation", dto2PrimitiveList(version, DatasetFieldConstant.relatedDatasets));
        
        writeFunderElement(xmlw, version); // MADROÑO
        
        writeFullElementList(xmlw, dcFlavor+":"+"type", dto2PrimitiveList(version, DatasetFieldConstant.kindOfData));
        
        writeFullElementList(xmlw, dcFlavor+":"+"source", dto2PrimitiveList(version, DatasetFieldConstant.dataSources));
        
        boolean restrict = false;
        boolean closed = false;

        if (version.isFileAccessRequest()) {
            restrict = true;
        }
        if (version.getFiles() != null) {
            for (int i = 0; i < version.getFiles().size(); i++) {
                if (version.getFiles().get(i).isRestricted()) {
                    closed = true;
                    break;
                }
            }
        }
        // MADROÑO BEGIN
        if (restrict) {
            writeFullElement(xmlw, dcFlavor+":"+"rights", "info:eu-repo/semantics/restrictedAccess"); 
        } else if (!restrict && closed) {
            writeFullElement(xmlw, dcFlavor+":"+"rights", "info:eu-repo/semantics/closedAccess"); 
        } else {
            writeFullElement(xmlw, dcFlavor+":"+"rights", "info:eu-repo/semantics/openAccess"); 
        }
        
        //License and Terms
        LicenseDTO licDTO = version.getLicense();
        if(licDTO != null) {
            writeFullElement(xmlw, dcFlavor+":"+"rights", licDTO.getName());
        }
        writeFullElement(xmlw, dcFlavor+":"+"rights", version.getTermsOfUse()); 
        writeFullElement(xmlw, dcFlavor+":"+"rights", version.getRestrictions()); 
	// MADROÑO END
    }
    
    private static void writeAuthorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException {

        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        String authorName = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    authorName = next.getSinglePrimitive();
                                }
                            }
                            if (!authorName.isEmpty()) {
                                xmlw.writeStartElement(dcFlavor+":"+"creator");
                                xmlw.writeCharacters(authorName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeAbstractElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                        String descriptionText = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                    descriptionText =  next.getSinglePrimitive();
                                }
                            }
                            if (!descriptionText.isEmpty()){
                                xmlw.writeStartElement(dcFlavor+":"+"description");  
                                xmlw.writeCharacters(descriptionText);
                                xmlw.writeEndElement(); //abstract
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static void writeSubjectElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException{ 
        
        //Key Words and Subject
      
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())){
                        for ( String subject : fieldDTO.getMultipleVocab()){
                            xmlw.writeStartElement(dcFlavor+":"+"subject");
                            xmlw.writeCharacters(subject);
                            xmlw.writeEndElement(); //Keyword
                        }
                    }
                    
                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String keywordValue = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    keywordValue =  next.getSinglePrimitive();
                                }
                            }
                            if (!keywordValue.isEmpty()){
                                xmlw.writeStartElement(dcFlavor+":"+"subject"); 
                                xmlw.writeCharacters(keywordValue);
                                xmlw.writeEndElement(); //Keyword
                            }
                        }
                    }
                }
            }
        }             
    }
    
    private static void writeRelPublElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.publication.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String pubString = "";
                            String citation = "";
                            String IDType = "";
                            String IDNo = "";
                            String url = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.publicationCitation.equals(next.getTypeName())) {
                                    citation =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDType.equals(next.getTypeName())) {
                                    IDType =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDNumber.equals(next.getTypeName())) {
                                    IDNo =   next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationURL.equals(next.getTypeName())) {
                                    url =  next.getSinglePrimitive();
                                }
                            }
                            pubString = appendCommaSeparatedValue(citation, IDType);
                            pubString = appendCommaSeparatedValue(pubString, IDNo);
                            pubString = appendCommaSeparatedValue(pubString, url);
                            if (!pubString.isEmpty()){
                                xmlw.writeStartElement(dcFlavor+":"+"isReferencedBy"); 
                                xmlw.writeCharacters(pubString);
                                xmlw.writeEndElement(); //relPubl
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MADROÑO BEGIN
    private static void writeFunderElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        if (recolectaAcronymsMap== null) {
            recolectaAcronymsSet= new HashSet<> (Arrays.asList("AEI","CDTI","FECYT", "ISCIII", "MAAMA", "MICINN", "MICYT", "MINECO", "MINECO", "MECD", "MFOM", "MINETUR", "MARM", "MSSSI"));
            recolectaAcronymsMap= new HashMap<>();
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100011033", "AEI");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100001872", "CDTI");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100011100", "FECYT");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100004587", "ISCIII");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100004336", "MAAMA");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100004837", "MICINN");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100006280", "MICYT");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100003329", "MINECO");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100010198", "MINECO");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100003176", "MECD");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100008409", "MFOM");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100006591", "MINETUR");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100014211", "MARM");
            recolectaAcronymsMap.put ("http://dx.doi.org/10.13039/501100003751", "MSSSI");
        }
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.grantNumber.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> fieldDTOs : fieldDTO.getMultipleCompound()) {
                            String awardNumber = null;
                            String funderName = null;

                            for (FieldDTO next : fieldDTOs) {
                                if (DatasetFieldConstant.grantNumberValue.equals(next.getTypeName())) {
                                    awardNumber = next.getSinglePrimitive().replaceAll("\\/", "%2F");
                                }
                                if (DatasetFieldConstant.grantNumberAgency.equals(next.getTypeName())) {
                                    funderName = next.getSinglePrimitive();

                                    if (StringUtils.isNotBlank(funderName)) {
                                        String funderDoi= OAIRecordServiceBean.getFunderDOI(funderName);
                                        if (funderDoi!= null) {
                                            String recolectaAcronym= recolectaAcronymsMap.get(funderDoi);
                                            if (recolectaAcronym!= null) {
                                                funderName= recolectaAcronym;
                                            }
                                        }
                                    }
                                }
                            }

                            if (StringUtils.isNotBlank(funderName) && StringUtils.isNotBlank(awardNumber)) {
                                xmlw.writeStartElement("dc:relation");
                                if (recolectaAcronymsSet.contains(funderName))
                                    xmlw.writeCharacters("info:eu-repo/grantAgreement/" + funderName + "//" + awardNumber);
                                else
                                    xmlw.writeCharacters("info:eu-repo/grantAgreement/" + funderName + "/" + awardNumber);
                                xmlw.writeEndElement(); // labl
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static List <String> getIsoLanguages (List <String> origLanguages) {
        List <String> isoLanguagesList= new ArrayList <>();
        if (origLanguages== null) {
                isoLanguagesList.add ("und");
        } else{ 
            for (String origLanguage: origLanguages) {
                isoLanguagesList.add(getLangIsoCode (origLanguage));
            }
            if (isoLanguagesList.isEmpty())
                isoLanguagesList.add ("und");
        }
        return isoLanguagesList;
    }
        
    public static String getLangIsoCode (String langName) {
        String isoCode= getLanguageCode (langName);
        if (isoCode== null || isoCode.equals("null"))
            isoCode="mis";
        return isoCode;
    }
    // MADROÑO END


    private static void writeContributorElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.contributor.equals(fieldDTO.getTypeName())) {
                        String contributorName = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.contributorName.equals(next.getTypeName())) {
                                    contributorName =  next.getSinglePrimitive();
                                }
                            }
                            if (!contributorName.isEmpty()){
                                xmlw.writeStartElement(dcFlavor+":"+"contributor");  
                                xmlw.writeCharacters(contributorName);
                                xmlw.writeEndElement(); //abstract
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeTimeElements(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.timePeriodCovered.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.timePeriodCoveredStart.equals(next.getTypeName())) {
                                    dateValStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.timePeriodCoveredEnd.equals(next.getTypeName())) {
                                    dateValEnd = next.getSinglePrimitive();
                                }
                            }
                            if (!dateValStart.isEmpty()) {
                                writeFullElement(xmlw, dcFlavor+":"+"temporal", dateValStart); 
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeFullElement(xmlw, dcFlavor+":"+"temporal", dateValEnd); 
                            }
                        }
                    }
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.dateOfCollectionStart.equals(next.getTypeName())) {
                                    dateValStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.dateOfCollectionEnd.equals(next.getTypeName())) {
                                    dateValEnd = next.getSinglePrimitive();
                                }
                            }
                            if (!dateValStart.isEmpty()) {
                               writeFullElement(xmlw, dcFlavor+":"+"temporal", dateValStart); 
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeFullElement(xmlw, dcFlavor+":"+"temporal", dateValEnd); 
                            }
                        }
                    }
                }
            }
        }    
    }
    
        private static void writeSpatialElements(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO, String dcFlavor) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if("geospatial".equals(key)){                
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.geographicCoverage.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.country.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, dcFlavor+":"+"spatial", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.city.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, dcFlavor+":"+"spatial", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.state.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, dcFlavor+":"+"spatial", next.getSinglePrimitive());
                                } 
                                if (DatasetFieldConstant.otherGeographicCoverage.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, dcFlavor+":"+"spatial", next.getSinglePrimitive());
                                } 
                            }
                        }
                    }
                }
            }
        }   
    }
    
    private static String appendCommaSeparatedValue(String inVal, String next) {
        if (!next.isEmpty()) {
            if (!inVal.isEmpty()) {
                return inVal + ", " + next;
            } else {
                return next;
            }
        }
        return inVal;
    }
    
    
    private static String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getSinglePrimitive();
                }
            }
        }
        return null;
    }
    
    private static List<String> dto2PrimitiveList(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getMultiplePrimitive();
                }
            }
        }
        return null;
    }
    
    private static void writeFullElementList(XMLStreamWriter xmlw, String name, List<String> values) throws XMLStreamException {
        //For the simplest Elements we can 
        // MADROÑO BEGIN
        if (name.equals("dc:type")) {
                xmlw.writeStartElement(name);
                xmlw.writeCharacters("info:eu-repo/semantics/dataset");
                xmlw.writeEndElement(); // labl
                xmlw.writeStartElement(name);
                xmlw.writeCharacters("info:eu-repo/semantics/publishedVersion");
                xmlw.writeEndElement(); // labl
        } else // RECOLECTA and LaReferencia compatibility. Only set the primary type 
        // MADROÑO END
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                xmlw.writeStartElement(name);
                xmlw.writeCharacters(value);
                xmlw.writeEndElement(); // labl
            }
        }
        
    }
    
    
    private static void writeFullElement (XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        //For the simplest Elements we can 
        if (!StringUtilisEmpty(value)) {
            xmlw.writeStartElement(name);
            xmlw.writeCharacters(value);
            xmlw.writeEndElement(); // labl
        }
    }

    private static void writeAttribute(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        if (!StringUtilisEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }

    private static boolean StringUtilisEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }
    
}
