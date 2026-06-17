package edu.harvard.iq.dataverse.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArray;
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
 * API endpoint for F-UJI FAIR Assessment Service integration. CSUC / MADROÑO
 * Acts as a proxy to avoid CORS issues and keep credentials secure.
 */
@Path("fuji")
@Tag(name = "fuji", description = "F-UJI FAIR Assessment Service integration")
public class FujiApi extends AbstractApiBean {

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 120;
    private static final int CACHE_HOURS = 48;
    private static final int CACHE_MAX_ENTRIES = 500;
    private static final int CACHE_MAX_AGE_SECONDS = CACHE_HOURS * 3600;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    /** Caché en memòria de resultats F-UJI per PID i versió de mètriques (TTL 48 h). */
    private static final Cache<String, JsonObject> EVALUATION_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(CACHE_HOURS))
            .maximumSize(CACHE_MAX_ENTRIES)
            .build();

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
        
        String normalizedPid = normalizePid(pid.trim());
        
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

        String cacheKey = buildCacheKey(normalizedPid, metricVersion);
        JsonObject cachedResult = EVALUATION_CACHE.getIfPresent(cacheKey);
        if (cachedResult != null) {
            return buildEvaluationResponse(cachedResult, true);
        }
        
        try {
            JsonObject requestBody = Json.createObjectBuilder()
                    .add("object_identifier", normalizedPid)
                    .add("test_debug", false)
                    .add("use_datacite", true)
                    .add("metric_version", metricVersion)
                    .build();
            
            String requestBodyString = requestBody.toString();
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fujiUrl))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
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
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            if (statusCode >= 200 && statusCode < 300) {
                try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
                    JsonObject fujiResponse = reader.readObject();
                    if (isCacheableEvaluation(fujiResponse)) {
                        EVALUATION_CACHE.put(cacheKey, fujiResponse);
                    }
                    return buildEvaluationResponse(fujiResponse, false);
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

    private static String normalizePid(String pid) {
        if (pid.startsWith("doi:")) {
            return "https://doi.org/" + pid.substring(4);
        }
        if (pid.matches("^10\\.\\d{4,9}/.*")) {
            return "https://doi.org/" + pid;
        }
        return pid;
    }

    private static String buildCacheKey(String normalizedPid, String metricVersion) {
        return normalizedPid + "|" + metricVersion;
    }

    private static Response buildEvaluationResponse(JsonObject fujiResponse, boolean fromCache) {
        return Response.ok(fujiResponse)
                .header("X-FUJI-Cache", fromCache ? "HIT" : "MISS")
                .header("Cache-Control", "private, max-age=" + CACHE_MAX_AGE_SECONDS)
                .build();
    }

    /**
     * Només es cachegen avaluacions completes (URL resolta i metadades collides).
     * Evita guardar resultats parcials per errors de xarxa/DNS durant 48 h.
     */
    private static boolean isCacheableEvaluation(JsonObject fujiResponse) {
        if (fujiResponse == null || !fujiResponse.containsKey("resolved_url")) {
            return false;
        }
        String resolvedUrl = fujiResponse.getString("resolved_url");
        if (resolvedUrl == null || resolvedUrl.isBlank() || "not defined".equalsIgnoreCase(resolvedUrl)) {
            return false;
        }
        if (!fujiResponse.containsKey("harvested_metadata")) {
            return false;
        }
        JsonArray harvested = fujiResponse.getJsonArray("harvested_metadata");
        return harvested != null && !harvested.isEmpty();
    }
}
