package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * API endpoint for F-UJI FAIR Assessment Service integration.
 * Acts as a proxy to avoid CORS issues and keep credentials secure.
 */
@Path("fuji")
@Tag(name = "fuji", description = "F-UJI FAIR Assessment Service integration")
public class FujiApi extends AbstractApiBean {

    private static final int TIMEOUT_SECONDS = 120;

    @EJB
    SettingsServiceBean settingsService;

    /**
     * Check if the FUJI service is configured and available.
     */
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check FUJI service status", 
               description = "Returns whether the FUJI service is configured and available")
    @APIResponse(responseCode = "200", description = "Service status")
    public Response getStatus() {
        String fujiUrl = settingsService.getValueForKey(SettingsServiceBean.Key.FujiServiceUrl);
        
        if (fujiUrl == null || fujiUrl.isEmpty()) {
            return ok(Json.createObjectBuilder()
                    .add("configured", false)
                    .add("message", "FUJI service URL not configured"));
        }
        
        return ok(Json.createObjectBuilder()
                .add("configured", true)
                .add("serviceUrl", fujiUrl));
    }

    /**
     * Evaluate a dataset's FAIR compliance using F-UJI.
     * 
     * @param pid The persistent identifier (DOI or Handle) of the dataset
     * @return The FUJI evaluation results
     */
    @GET
    @Path("evaluate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Evaluate dataset FAIR compliance", 
               description = "Evaluates a dataset using the F-UJI FAIR assessment service")
    @APIResponse(responseCode = "200", description = "FUJI evaluation results")
    @APIResponse(responseCode = "400", description = "Invalid request or PID")
    @APIResponse(responseCode = "503", description = "FUJI service not available")
    public Response evaluateDataset(
            @Parameter(description = "Dataset persistent identifier (DOI or Handle)", required = true)
            @QueryParam("pid") String pid) {
        
        if (pid == null || pid.trim().isEmpty()) {
            return badRequest("PID parameter is required");
        }
        
        String normalizedPid = pid.trim();
        if (normalizedPid.startsWith("doi:")) {
            normalizedPid = "https://doi.org/" + normalizedPid.substring(4);
        } else if (normalizedPid.matches("^10\\.\\d{4,9}/.*")) {
            normalizedPid = "https://doi.org/" + normalizedPid;
        }
        
        String fujiUrl = settingsService.getValueForKey(SettingsServiceBean.Key.FujiServiceUrl);
        if (fujiUrl == null || fujiUrl.isEmpty()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "FUJI service not configured")
                            .build())
                    .build();
        }
        
        String fujiUsername = settingsService.getValueForKey(SettingsServiceBean.Key.FujiUsername);
        String fujiPassword = settingsService.getValueForKey(SettingsServiceBean.Key.FujiPassword);
        
        String metricVersion = settingsService.getValueForKey(SettingsServiceBean.Key.FujiMetricVersion);
        if (metricVersion == null || metricVersion.isEmpty()) {
            metricVersion = "0.8";
        }
        
        try {
            JsonObject requestBody = Json.createObjectBuilder()
                    .add("object_identifier", normalizedPid)
                    .add("test_debug", false)
                    .add("use_datacite", true)
                    .add("metric_version", metricVersion)
                    .build();
            
            String requestBodyString = requestBody.toString();
            
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fujiUrl))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            
            if (fujiUsername != null && !fujiUsername.isEmpty() 
                    && fujiPassword != null && !fujiPassword.isEmpty()) {
                String auth = fujiUsername + ":" + fujiPassword;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            if (statusCode >= 200 && statusCode < 300) {
                try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
                    JsonObject fujiResponse = reader.readObject();
                    return Response.ok(fujiResponse).build();
                }
            } else if (statusCode == 401) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(Json.createObjectBuilder()
                                .add("status", "ERROR")
                                .add("message", "FUJI authentication failed")
                                .add("fujiStatus", statusCode)
                                .build())
                        .build();
            } else {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(Json.createObjectBuilder()
                                .add("status", "ERROR")
                                .add("message", "FUJI service returned an error")
                                .add("fujiStatus", statusCode)
                                .add("fujiResponse", responseBody)
                                .build())
                        .build();
            }
            
        } catch (java.net.ConnectException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "Cannot connect to FUJI service")
                            .add("details", e.getMessage())
                            .build())
                    .build();
        } catch (java.net.http.HttpTimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT)
                    .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "FUJI service timeout")
                            .add("details", e.getMessage())
                            .build())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "Error communicating with FUJI service")
                            .add("details", e.getMessage())
                            .build())
                    .build();
        }
    }
}
