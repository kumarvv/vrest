package com.kumarvv.vrest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO: add tests
 */
public class RESTServerTest {

	RESTServer restServer = new RESTServer();

	@Before
	private void setup() {
	}

	@Test
	public void testScanResources() {
		Assert.assertEquals("Hello", true, true);
	}

	public void testToJSON() {
	}

}
