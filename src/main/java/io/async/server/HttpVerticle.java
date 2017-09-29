package io.async.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;

public class HttpVerticle extends AbstractVerticle
{
	private static final Logger log = LogManager.getLogger(HttpVerticle.class);

	private JDBCClient jdbcClient;
	private WebClient webClient;
	private Router router;

	final DataSource dataSource;
	final int httpPort;
	final WikiQueriesHandler queriesHandler;

	public HttpVerticle(int httpPort, DataSource dataSource, WikiQueriesHandler queriesHandler)
	{
		super();
		this.dataSource = dataSource;
		this.httpPort = httpPort;
		this.queriesHandler = queriesHandler;
	}

	@Override
	public void start(Future<Void> future) throws Exception
	{
		log.info("starting http verticle");
		jdbcClient = JDBCClient.create(vertx, dataSource);
		webClient = WebClient.create(vertx, new WebClientOptions().setFollowRedirects(true));
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
		router.get("/wikiTitle").handler(this::pageQueryHandler);
		router.get("/wikiPage/:title").handler(this::pageFetchHandler);
	}

	private void pageQueryHandler(RoutingContext rctx)
	{
		queriesHandler.handleQuery(rctx, jdbcClient);
	}

	private void pageFetchHandler(RoutingContext rctx)
	{
		queriesHandler.handleFetch(rctx, webClient);
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
		final HttpVerticle httpVerticle = new HttpVerticle(HTTP_PORT, dataSource, new WikiQueriesHandler());
		Vertx.vertx().deployVerticle(httpVerticle);
		log.info("http verticle is up and running");
	}
}
