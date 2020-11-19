package se.kry.codetest;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class DBConnector {
   public static final String SQL_CREATE_SERVICE_TABLE = "CREATE TABLE IF NOT" +
        " EXISTS service (name TEXT NOT NULL, url TEXT NOT NULL PRIMARY KEY, " +
        "created_datetime " +
        "TEXT NOT NULL, status TEXT NOT NULL)";
   public static final String SQL_SELECT_SERVICES_ALL = "SELECT name, url, " +
        "created_datetime, status from service";
   public static final String SQL_INSERT_SERVICE = "INSERT INTO service " +
        "values(?,?,?,?)";
   public static final String SQL_UPDATE_SERVICE = "update service set status=? where url=?";
   public static final String SQL_DELETE_SERVICE = "DELETE from service  " +
           "where url=?";

   private static final Logger LOGGER = LoggerFactory.getLogger(DBConnector
        .class);

   private final String DB_PATH = "poller.db";
   private final SQLClient client;

   public DBConnector(Vertx vertx) {
      JsonObject config = new JsonObject()
           .put("url", "jdbc:sqlite:" + DB_PATH)
           .put("driver_class", "org.sqlite.JDBC")
           .put("max_pool_size", 30);

      client = JDBCClient.createShared(vertx, config);
   }

   /**
    * This method returns a connection from pool
    * @return SQLConnection
    */
    Future<SQLConnection> getConnection() {
      Future<SQLConnection> con = Future.future();
      client.getConnection(ar -> {
         if(ar.succeeded()){
            con.complete(ar.result());
         } else {
            LOGGER.error("Error connecting to database, cause :"+ar.cause());
            con.fail(ar.cause());
         }
      });
      return con;
   }


   public Future<ResultSet> query(String query) {
      return query(query, new JsonArray());
   }

   public Future<ResultSet> query(String query, JsonArray params) {
      if (query == null || query.isEmpty()) {
         return Future.failedFuture("Query is null or empty");
      }
      if (!query.endsWith(";")) {
         query = query + ";";
      }
      Future<ResultSet> queryResultFuture = Future.future();
      client.queryWithParams(query, params, result -> {
         if (result.failed()) {
            queryResultFuture.fail(result.cause());
         } else {
            queryResultFuture.complete(result.result());
         }
      });
      return queryResultFuture;
   }


   public Future<ResultSet> updateWithParam(String query, JsonArray params) {
      if (query == null || query.isEmpty()) {
         return Future.failedFuture("Query is null or empty");
      }

      if (!query.endsWith(";")) {
         query = query + ";";
      }
      Future<ResultSet> queryResultFuture = Future.future();
      client.updateWithParams(query, params, result -> {
         if (result.failed()) {
            queryResultFuture.fail(result.cause());
         } else {
            queryResultFuture.complete();
         }
      });
      return queryResultFuture;
   }

   public void stop(){
      LOGGER.info("Stopping DB Connector");
      client.close();
   }

}
