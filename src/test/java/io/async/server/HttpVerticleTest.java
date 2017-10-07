package io.async.server;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class HttpVerticleTest
{
	private Vertx vertx;

	@Before
	public void setUp(TestContext testCtx) throws Exception
	{
		vertx = Vertx.vertx();
	}

	@Test
	public void tearDown(TestContext testCtx) throws Exception
	{
		vertx.close(testCtx.asyncAssertSuccess());
	}
}
