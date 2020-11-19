package se.kry.codetest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the implementation of poller service. it gets a list of services (defined by a URL)
 * and periodically does a HTTP GET to each URL's and saves the response to the database ("OK" or "FAIL").
 */

public class BackgroundPoller {

    private static final String STATUS_OK = "OK";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final int HTTP_CODE_SUCCESS = 200;
    private static final int TIMEOUT = 5000;
    private static final int MAX_TRIES = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundPoller
            .class);
    private static final String FILE_NAME = "failed_services.csv";

    private Map<String, String> servicesFailedDbSave = new HashMap<>();
    private Map<String, Integer> urlTryCounter = new HashMap<>();
    private WebClient webClient;
    private DBConnector connector;

    public BackgroundPoller(Vertx vertx, DBConnector connector) {
        WebClientOptions options = new WebClientOptions();
        options.setKeepAlive(false);
        webClient = WebClient.create(vertx, options);
        this.connector = connector;
    }


    public Future<List<String>> pollServices(Map<String, Service> services) {

        LOGGER.info("Poller starting.");

        if (services.isEmpty()) {
            LOGGER.info("No URLs to process..Exiting...");
            Future.succeededFuture();
        }
        services.forEach((url, service) -> {
            //If the URL has already been tried the MAX ALLOWED times, don't try again
            if (!hasMaxRetriesExhausted(url)) {
                String currentStatus = service.getStatus();
                webClient.getAbs(url).timeout(TIMEOUT).send(ar -> {
                    String urlCallStatus = STATUS_UNKNOWN;
                    if (ar.succeeded()) {
                        if (ar.result().statusCode() == HTTP_CODE_SUCCESS) {
                            urlCallStatus = STATUS_OK;
                        } else {
                            urlCallStatus = STATUS_FAIL;
                        }
                    } else {
                        //If the request failed (either because of timeout or invalid URL), make it unknown
                        updateUrlTriesCounter(url);
                        LOGGER.error("Error calling the URL :" + url + "; cause = " + ar.cause());
                    }
                    //Save to DB only if there is a change in status
                    if (!currentStatus.equals(urlCallStatus)) {
                        service.setStatus(urlCallStatus);
                        saveToDb(url, urlCallStatus);
                    }
                });
            }
        });
        return Future.succeededFuture();
    }

    private void updateUrlTriesCounter(String url) {
        int noOfTries = urlTryCounter.get(url) == null ? 0 : urlTryCounter.get(url);
        urlTryCounter.put(url, noOfTries + 1);
    }

    private boolean hasMaxRetriesExhausted(String url) {
        if (urlTryCounter.get(url) == null) {
            return false;
        }
        return urlTryCounter.get(url) >= MAX_TRIES;
    }

    /**
     * Save the URL & Status to Database
     *
     * @param url    the URL
     * @param status the status
     * @return the Future indicating the status of operation
     */
    private void saveToDb(String url, String status) {
        connector.updateWithParam(DBConnector.SQL_UPDATE_SERVICE, new JsonArray().add(status).add(url))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        LOGGER.debug("Updated status against URL : " + url);
                        if (servicesFailedDbSave.containsKey(url)) {
                            servicesFailedDbSave.remove(url);
                        }
                    } else {
                        LOGGER.error("Error while updating the status, URL :" + url + " from DB, cause : " + ar.cause());
                        servicesFailedDbSave.put(url, status);
                    }
                });
    }

    /**
     * Stop the Poller Service
     */
    public void stop() {
        webClient.close();

        if (servicesFailedDbSave.isEmpty()) {
            return;
        }
        try (FileWriter writer = new FileWriter(FILE_NAME); BufferedWriter bw = new BufferedWriter(writer);) {
            for (Map.Entry<String, String> entry : servicesFailedDbSave.entrySet()) {
                bw.write(entry.getKey() + '|' + entry.getValue());
                bw.newLine();
            }
        } catch (IOException e) {
            LOGGER.error("Error while writing the data to File.." + e.getMessage());
        }
    }
}