package edu.harvard.iq.dataverse.export;

import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.Node;
import com.jayway.restassured.path.xml.element.NodeChildren;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class OpenAireExporterTest {

    private final OpenAireExporter openAireExporter;

    public OpenAireExporterTest() {
        openAireExporter = new OpenAireExporter();
    }

    /**
     * Test of getProviderName method, of class OpenAireExporter.
     */
    @Test
    public void testGetProviderName() {
        System.out.println("getProviderName");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "oai_datacite";
        String result = instance.getProviderName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDisplayName method, of class OpenAireExporter.
     */
    @Test
    public void testGetDisplayName() {
        System.out.println("getDisplayName");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "DataCite OpenAIRE";
        String result = instance.getDisplayName();
        assertEquals(expResult, result);
    }

    /**
     * Test of exportDataset method, of class OpenAireExporter.
     */
    @Test
    public void testExportDataset() throws Exception {
        System.out.println("exportDataset");
        // TODO: add more fields to json to increase code coverage.
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-spruce1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject jsonObject = jsonReader.readObject();
        DatasetVersion nullVersion = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        openAireExporter.exportDataset(nullVersion, jsonObject, byteArrayOutputStream);
        String xmlOnOneLine = new String(byteArrayOutputStream.toByteArray());
        String xmlAsString = XmlPrinter.prettyPrintXml(xmlOnOneLine);
        System.out.println("XML: " + xmlAsString);
        XmlPath xmlpath = XmlPath.from(xmlAsString);
        assertEquals("Spruce Goose", xmlpath.getString("resource.titles.title"));
        assertEquals("Spruce, Sabrina", xmlpath.getString("resource.creators.creator"));
        assertEquals("1.0", xmlpath.getString("resource.version"));
        
        datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/backToFuture.json");
        datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        jsonObject = jsonReader.readObject();
        nullVersion = null;
        byteArrayOutputStream = new ByteArrayOutputStream();
        openAireExporter.exportDataset(nullVersion, jsonObject, byteArrayOutputStream);
        xmlOnOneLine = new String(byteArrayOutputStream.toByteArray());
        xmlAsString = XmlPrinter.prettyPrintXml(xmlOnOneLine);
        System.out.println("XML: " + xmlAsString);
        xmlpath = XmlPath.from(xmlAsString);
        assertEquals("Back to the Future", xmlpath.getList("resource.titles.title").get(0));
        assertEquals("Robert Zemeckis", xmlpath.getString("resource.creators.creator.creatorName"));
        assertEquals("0001-0002-0003-0004", xmlpath.get("resource.creators.creator.nameIdentifier.findAll {it.@nameIdentifierScheme == 'ORCID'}"));
        assertEquals("1985-01", xmlpath.get("resource.dates.date.findAll { it.@dateType == 'Created'}"));
        assertEquals("1985-01-01", xmlpath.get("resource.dates.date.findAll { it.@dateType == 'Issued'}"));
        assertEquals("1985-02/1985-03", xmlpath.get("resource.dates.date.findAll { it.@dateType == 'Collected'}"));
        assertEquals("United States; California; Hill Valley; fantasy cityHill Valley City32.64542843   116.36588226", xmlpath.getString("resources.geoLocations.geoLocation.geoLocationPlace"));

    }

    /**
     * Test of isXMLFormat method, of class OpenAireExporter.
     */
    @Test
    public void testIsXMLFormat() {
        System.out.println("isXMLFormat");
        OpenAireExporter instance = new OpenAireExporter();
        Boolean expResult = true;
        Boolean result = instance.isXMLFormat();
        assertEquals(expResult, result);
    }

    /**
     * Test of isHarvestable method, of class OpenAireExporter.
     */
    @Test
    public void testIsHarvestable() {
        System.out.println("isHarvestable");
        OpenAireExporter instance = new OpenAireExporter();
        Boolean expResult = true;
        Boolean result = instance.isHarvestable();
        assertEquals(expResult, result);
    }

    /**
     * Test of isAvailableToUsers method, of class OpenAireExporter.
     */
    @Test
    public void testIsAvailableToUsers() {
        System.out.println("isAvailableToUsers");
        OpenAireExporter instance = new OpenAireExporter();
        Boolean expResult = true;
        Boolean result = instance.isAvailableToUsers();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXMLNameSpace method, of class OpenAireExporter.
     */
    @Test
    public void testGetXMLNameSpace() throws Exception {
        System.out.println("getXMLNameSpace");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "http://datacite.org/schema/kernel-4";
        String result = instance.getXMLNameSpace();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXMLSchemaLocation method, of class OpenAireExporter.
     */
    @Test
    public void testGetXMLSchemaLocation() throws Exception {
        System.out.println("getXMLSchemaLocation");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";
        String result = instance.getXMLSchemaLocation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXMLSchemaVersion method, of class OpenAireExporter.
     */
    @Test
    public void testGetXMLSchemaVersion() throws Exception {
        System.out.println("getXMLSchemaVersion");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "4.1";
        String result = instance.getXMLSchemaVersion();
        assertEquals(expResult, result);
    }

    /**
     * Test of setParam method, of class OpenAireExporter.
     */
    @Test
    public void testSetParam() {
        System.out.println("setParam");
        String name = "";
        Object value = null;
        OpenAireExporter instance = new OpenAireExporter();
        instance.setParam(name, value);
    }

}
