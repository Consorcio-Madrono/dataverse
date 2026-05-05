package es.consorciomadrono;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
/**
 *
 *
 * @author juancorr
 * 
 * CONSORCIO MADROÑO. New class to read the funder dois from the fundersmap table to 
 * have a better OpenAIRE compability
 * 
 */

public class FunderNamesParser {
    
    final String FUNDERS_REGISTRY_PATH="src/main/resources/es/consorciomadrono/fundersRegistry.rdf";
    final String PSQL_WRITERS_FILE    = "psqlFundersWriter.sql";
    private HashMap <String,String> fundersMap= new HashMap<>();

    public FunderNamesParser() {
    }
 
    public static void main (String [] args) {
        FunderNamesParser parser= new FunderNamesParser ();
        parser.parse ();
        parser.writeMapToFile ();
    }
    
    private void parseLabel (Element funderElement, String labelName, String funderId) {
        NodeList prefLabelNodeList= funderElement.getElementsByTagName(labelName);
        for (int j = 0; j < prefLabelNodeList.getLength(); j++)
        {
            Node labelNode= prefLabelNodeList.item(0);
            if (labelNode.getNodeType() == Node.ELEMENT_NODE) {
                Element labelElement= (Element)labelNode;
                NodeList literalFormList= labelElement.getElementsByTagName("skosxl:literalForm");
                for (int k = 0; k < literalFormList.getLength(); k++)
                {
                    Node literalFormNode= literalFormList.item(0);
                    if (literalFormNode.getNodeType() == Node.ELEMENT_NODE) {
                        String label= ((Element) literalFormNode).getTextContent();
                        fundersMap.put(label, funderId);
                    }
                }
            }                               
        }
    }
    
    private void parse() {
        try {
            //creating a constructor of file class and parsing an XML file
            File file = new File(FUNDERS_REGISTRY_PATH);
            //an instance of factory that gives a document builder
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            //an instance of builder to parse the specified xml file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
            NodeList funders = doc.getElementsByTagName("skos:Concept");
            // nodeList is not iterable, so we are using for loop
            for (int i = 0; i < funders.getLength(); i++)
            {
                Node funder= funders.item(i);
                Element funderElement= (Element)funder;
                if (funder.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList regionList = funderElement.getElementsByTagName("svf:region");
                    if (regionList.getLength() > 0) {
                        Node regionNode= regionList.item(0);
                        if (regionNode.getNodeType() == Node.ELEMENT_NODE && regionNode.getTextContent().equals("Europe")) {
                            String funderId= funderElement.getAttribute("rdf:about");
                            parseLabel (funderElement, "skosxl:prefLabel", funderId);
                            parseLabel (funderElement, "skosxl:altLabel", funderId);
                        }
                    }
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            Logger.getLogger(FunderNamesParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }          

    
    private void writeMapToFile() {
        File file = new File(PSQL_WRITERS_FILE);
        BufferedWriter bf = null;
        try{
            //create new BufferedWriter for the output file
            bf= new BufferedWriter(new FileWriter(file));
 
            bf.write("CREATE TABLE FUNDERS_MAP (ID TEXT NOT NULL, DOI TEXT NOT NULL, PRIMARY KEY (ID));");

            //iterate map entries
            for(HashMap.Entry<String, String> entry : fundersMap.entrySet()){
                //put key and value separated by a colon
                String insertString= "INSERT INTO FUNDERS_MAP (ID, DOI) VALUES ('" + 
                        entry.getKey().replaceAll("'", "''") + "' , '" + entry.getValue() + "');";
                bf.write(insertString);
                //new line
                bf.newLine();
            }
            bf.flush();
 
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try{
                //always close the writer
                bf.close();
            }catch(IOException e){}
        }
    }
}
