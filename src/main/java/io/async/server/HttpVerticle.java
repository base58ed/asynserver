package io.async.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;

public class HttpVerticle extends AbstractVerticle
{
	private static final Logger log = LogManager.getLogger(HttpVerticle.class);

	private JDBCClient jdbcClient;
	private Router router;

	final DataSource dataSource;
	final int httpPort;

	public HttpVerticle(int httpPort, DataSource dataSource)
	{
		super();
		this.dataSource = dataSource;
		this.httpPort = httpPort;
	}

	@Override
	public void start(Future<Void> future) throws Exception
	{
		log.info("starting http verticle");
		jdbcClient = JDBCClient.create(vertx, dataSource);
		router = Router.router(vertx);
		initRoutes(router);

		vertx.createHttpServer()
			.requestHandler(httpServerRequest -> router.accept(httpServerRequest))
			.listen(config().getInteger("http.port", httpPort), result -> {
				if (result.succeeded()) {
					future.complete();
				} else {
					future.fail(result.cause());
				}
			});
	}

	@Override
	public void stop() throws Exception
	{
		jdbcClient.close();
	}

	private void initRoutes(Router router)
	{
		router.route("/*").handler(BodyHandler.create());
		router.get("/hello").handler(HttpVerticle::helloBodyHandler);
	}

	private static void helloBodyHandler(RoutingContext rctx)
	{
		rctx.response().end("hello world");
	}

	public static void main(String[] args)
	{

		String JDBC_URL = "jdbc:mysql://localhost:3307/WIKI_TEST?useSSL=false&amp;zeroDateTimeBehavior=convertToNull&amp;autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf-8&amp;connectionCollation=utf8_general_ci&amp;characterSetResults=utf8";
		String JDBC_USER = "root";
		String JDBC_PASS = "testadmin";
		int HTTP_PORT = 8080;

		final HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(JDBC_URL);
		hikariConfig.setUsername(JDBC_USER);
		hikariConfig.setPassword(JDBC_PASS);
		hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
		hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		hikariConfig.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 2 + 2);
		hikariConfig.setIdleTimeout(36000);
		hikariConfig.setConnectionTestQuery("SELECT 1;");
		hikariConfig.setConnectionTimeout(20000);
		final HikariDataSource dataSource = new HikariDataSource(hikariConfig);
		log.info("initialized dataSource");
		final HttpVerticle httpVerticle = new HttpVerticle(HTTP_PORT, dataSource);
		Vertx.vertx().deployVerticle(httpVerticle);
		log.info("http verticle is up and running");
	}
}
