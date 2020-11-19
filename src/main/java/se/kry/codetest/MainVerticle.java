package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.commons.validator.routines.UrlValidator;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    private static final int PORT_NUMBER = 8080;
    private static final String SERVICE_DEFAULT_STATUS = "UNKNOWN";
    private static final String TM_FORMAT="yyyy-MM-dd HH:mm:ss.SSS";

    private Map<String, Service> services = new HashMap<>();
    private DBConnector connector;
    private BackgroundPoller poller;


    /**
     * This method is called when the verticle is deployed. It prepare the database and creates a HTTP server
     * and registers routes for different requests
     *
     * @param startFuture the future
     */

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        poller = new BackgroundPoller(vertx, connector);
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
        Future<Void> startupAction = prepareDatabase().compose(v ->
                startHttpServer());
        startupAction.setHandler(ar -> {
            if (ar.failed()) {
                LOGGER.error("Kry Application Startup Failed");
                startFuture.fail(ar.cause());
            } else {
                LOGGER.error("Kry Application Startup Success");
                startFuture.complete();
            }
        });

    }

    private Future<Void> startHttpServer() {
        Future<Void> future = Future.future();
        // Create a router object.
        Router router = Router.router(vertx);
        setRoutes(router);
        // Create the HTTP server
        vertx.createHttpServer().requestHandler(router).listen(PORT_NUMBER, start
                -> {
            if (start.succeeded()) {
                LOGGER.info("HTTP server started..");
                future.complete();
            } else {
                LOGGER.error("HTTP server startup failed, cause : "+start.cause());
                future.fail(start.cause());
            }
        });
        return future;
    }

    private void setRoutes(Router router) {
        LOGGER.info("Setting routes..");
        router.post().handler(BodyHandler.create());
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(this::pageRenderHandler);
        router.post("/service").handler(this::saveHandler);
        router.post("/delete").handler(this::deleteHandler);
    }

    private Future<Void> prepareDatabase() {
        Future<Void> future = Future.future();
        connector.getConnection().setHandler(ar -> {
            SQLConnection con = ar.result();
            con.execute(DBConnector.SQL_CREATE_SERVICE_TABLE, create -> {
                con.close();
                if (create.succeeded()) {
                    LOGGER.info("Service Table Creation Success");
                    loadServices(future);
                } else {
                    LOGGER.error("Service Table creation failed, cause :"+create.cause());
                    future.fail(create.cause());
                }
            });

        });
        return future;
    }

    /**
     * Here we load the data from Services tables in to the in memory services map
     * @param future the future
     */
    private void loadServices(Future<Void> future) {
        connector.query(DBConnector.SQL_SELECT_SERVICES_ALL).setHandler(ar
                -> {
            if (ar.succeeded()) {
                ar.result().getRows().forEach(json -> services.put
                        (json.getString("url"),  new Service(json.getString("name"), json.getString
                                ("url"), json.getString("insert_time"),json.getString("status"))));
                LOGGER.debug("Successfully loaded services from DB");
                future.complete();
            } else {
                LOGGER.error("Error loading services from DB, cause :"+ar.cause());
                future.fail(ar.cause());
            }
        });
    }

    /**
     * Handler for rendering the page.
     * @param context the routing context
     */
    private void pageRenderHandler(RoutingContext context) {
        System.out.println("Started rentering");
        List<JsonObject> jsonServices = services
                .entrySet()
                .stream()
                .map(entry -> {
                    Service service = entry.getValue();
                    return new JsonObject()
                                .put("name", service.getName())
                                .put("url", entry.getKey())
                                .put("status", service.getStatus());
                }).collect(Collectors.toList());
        context.response()
                .putHeader("content-type", "application/json")
                .end(new JsonArray(jsonServices).encode());
    }

    private void saveHandler(RoutingContext context) {
        LOGGER.debug("Received a Save event");
        // Read the request's content and create an instance of service.
        JsonObject jsonBody = context.getBodyAsJson();
        String url = jsonBody.getString("url");
        // In case the URL is not valid or the URL is a duplicate one, ignore the request
        if(isValidUrl(url) && !services.containsKey(url)) {
            String name = jsonBody.getString("name");
            String currentTm = getCurrentTime();
            JsonArray jsonArr = new JsonArray();
            jsonArr.add(name).add(url).add(currentTm).add(SERVICE_DEFAULT_STATUS);
            connector.updateWithParam(DBConnector.SQL_INSERT_SERVICE, jsonArr)
                    .setHandler(result -> {
                        if (result.succeeded()) {
                            LOGGER.info("URL details saved to DB");
                            services.put(url, new Service(name, url, currentTm, SERVICE_DEFAULT_STATUS));
                            context.response().putHeader("content-type", "text/plain")
                                    .end("OK");
                        } else {
                            LOGGER.error("Error while saving URL :"+url+" to DB, cause : "+result.cause());
                            context.fail(result.cause());
                        }
                    });
        } else {
            LOGGER.info("Invalid URL or it's an existing URL, Ignoring");
            context.response().setStatusCode(400).end("INVALID INPUT");
        }
    }

    private void deleteHandler(RoutingContext context) {
        LOGGER.debug("Received a delete event..");
        JsonObject jsonBody = context.getBodyAsJson();
        String url = jsonBody.getString("url");
        connector.updateWithParam(DBConnector.SQL_DELETE_SERVICE, new JsonArray().add(url))
                .setHandler(result -> {
                    if (result.succeeded()) {
                        LOGGER.info("Deleted URL : "+url+" from DB");
                        services.remove(url);
                        context.response().putHeader("content-type", "text/plain")
                                .end("OK");
                    } else {
                        LOGGER.info("Error while deleting URL : "+url+" from DB, cause : "+result.cause());
                        context.fail(result.cause());
                    }
                });
    }

    private String getCurrentTime() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(TM_FORMAT);
        return sdf.format(ts);
    }

    private boolean isValidUrl(String url) {
        return new UrlValidator().isValid(url);
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Main verticle");
        super.stop();
        if(poller!=null) {
            poller.stop();
        }
        if(connector!=null){
            connector.stop();
        }
    }
}



