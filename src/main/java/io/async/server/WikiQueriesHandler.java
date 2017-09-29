package io.async.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.async.server.model.QueryResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Unbox;

import java.util.List;
import java.util.stream.Collectors;

public class WikiQueriesHandler
{
	private static final Logger log = LogManager.getLogger(WikiQueriesHandler.class);

	private static final ObjectMapper mapper = new ObjectMapper();

	public void handleQuery(RoutingContext rctx, JDBCClient jdbcClient)
	{
		final String titleName = rctx.request().getParam("name");
		jdbcClient.getConnection(asyncResult -> {
			if (asyncResult.succeeded())
			{
				log.info("acquired sql connection in vertx async jdbcClient");
				try (SQLConnection sqlConnection = asyncResult.result())
				{
					sqlConnection.queryWithParams(QUERY_TITLE, new JsonArray().add("%" + titleName + "%"), asyncDataSetHandler -> {
						if (asyncDataSetHandler.succeeded())
						{
							final ResultSet resultSet = asyncDataSetHandler.result();
							final List<String> pageTitlesInDb = resultSet.getRows()
								.stream()
								.map(entries -> entries.getString("pageTitle")).collect(Collectors.toList());
							log.info("found {} titles in database matching name {}", Unbox.box(pageTitlesInDb.size()), titleName);
							try
							{
								rctx.response()
									.putHeader("Content-Type", "application/json; charset=utf-8")
									.end(mapper.writeValueAsString(new QueryResult(pageTitlesInDb)));
							}
							catch (JsonProcessingException e)
							{
								rctx.response().putHeader("Content-Type", "application/json; charset=utf-8").end("fail 4");
							}
						}
						else
						{
							log.error("dataset handler failed: {}", asyncDataSetHandler.cause());
							rctx.response().end("failed 3");
						}
					});
				}
				catch (Exception e)
				{
					log.error("caught exception while querying database: {}", e.getCause());
					rctx.response().end("failed 2");
				}
			}
			else
			{
				log.error("failed to get sql connection in vertx async jdbcClient: {}", asyncResult.cause());
				rctx.response().end("failed 1");
			}
		});
	}

	public void handleFetch(RoutingContext rctx, WebClient webClient)
	{
		String pageTitle = rctx.pathParam("title");
		if (pageTitle == null || pageTitle.isEmpty())
		{
			rctx.response().setStatusCode(400).end();
			return;
		}
		final String endPoint = "en.wikipedia.org";
		//https://en.wikipedia.org/api/rest_v1/page/html/Thread(OS)
		webClient.request(HttpMethod.GET, 443, endPoint, "/api/rest_v1/page/html" + "/" + pageTitle)
			.ssl(true)
			.send(httpResponseAsyncResult -> {
				if (httpResponseAsyncResult.succeeded())
				{
					log.info("succeeded http response");
					final HttpResponse<Buffer> respBuffer = httpResponseAsyncResult.result();
					final String respBody = respBuffer.bodyAsString();
					log.info("resp json: {}", respBody);
					rctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(respBody);
				}
				else
				{
					log.error("failed http response");
					rctx.response().end("failed http 1: {}", httpResponseAsyncResult.cause().getMessage());
				}
			});
	}

	private static final String QUERY_TITLE = "select page_title as pageTitle from TITLES t where  MATCH(t.page_title) AGAINST(?);";
}
