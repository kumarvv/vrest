package com.kumarvv.vrest;

/**
 * Created with IntelliJ IDEA.
 * User: vvijayaram
 * Date: 7/24/14
 * Time: 6:01 PM
 * To change this template use File | Settings | File Templates.
 */
@AbstractResource.Path("/test")
public class TestResource extends AbstractResource {

	@GET
	public String all() {
		return "this is all I have!";
	}

	@GET("hello/:name")
	public String hello() {
		return "Hello " + getUrlParam("name");
	}

	@GET(":peru/sir")
	public String sir() {
		return getUrlParam("peru") + " sir!";
	}

}
