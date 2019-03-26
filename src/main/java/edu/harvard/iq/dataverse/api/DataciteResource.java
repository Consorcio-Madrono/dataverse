package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.export.OpenAireDataciteExporter;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


@Path("datacite")
public class DataciteResource extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(DataciteResource.class.getName());

    @EJB
    PermissionServiceBean permissionServiceBean;

    @EJB
    DatasetServiceBean datasetService;

    @GET
    @Path("{doi1}/{doi2}/{doi3}")
    @Produces(MediaType.TEXT_XML)
    public Response getFilesystemImport(@PathParam("doi1") String doi1,
                                        @PathParam("doi2") String doi2,
                                        @PathParam("doi3") String doi3) {

        try {
            String doi = "doi:" + doi1 + "/" + doi2 + "/" + doi3;
            Dataset dataset = datasetService.findByGlobalId(doi);

            StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(OutputStream os) {
                    try {
                        OpenAireDataciteExporter exporter = new OpenAireDataciteExporter();
                        exporter.exportDataset(dataset.getLatestVersion(), null, os);
                        os.flush();
                        os.close();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "1 DataCite Export Exception: " + e.getMessage());
                    }
                }
            };
            return Response.ok(stream).build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "2 DataCite Export Exception: " + e.getMessage());
            return null;
        }
    }
    
}
