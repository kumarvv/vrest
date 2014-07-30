vrest
=====

Simple single class REST server using Java SE only.

- Jave SE 1.6 or above 
- No HTTP server / servlet container required 
- Uses Java SE socket to listen and process client requests
- Multi-threaded request processing (100 threads by default)
- Uses jackson mapper (org.codehaus.jackson) for JSON processing

<strong>Limitations:</strong> 
- Not a production ready server (for quick REST services testing only) 
- supports JSON data communications only 

Setup
-----

Download or checkout and run following maven command: 

```bash
$ mvn exec:exec -DRESTServer
```

This command should scan all the classes in current directory for REST resources and listens on 4001 port: 

```bash
$ mvn exec:exec -DRESTServer
[INFO] Scanning for projects...
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for vjrest:vjrest:jar:1.0-SNAPSHOT
[WARNING] 'build.plugins.plugin.version' for org.codehaus.mojo:exec-maven-plugin is missing. @ line 29, column 14
[WARNING] 
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] 
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
[WARNING] 
[INFO] 
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building vjrest 1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:1.3.2:exec (default-cli) @ vjrest ---
INFO: scanning resources...
INFO: Found api: CityResource.delete => /cities/:city
INFO: Found api: CityResource.all => /cities
INFO: Found api: CityResource.create => /cities/new
INFO: Found api: CityResource.update => /cities/:city
INFO: Found api: CityResource.getCity => /cities/:city
INFO: Found api: CityResource.echo => /echo/:str
INFO: Found api: CityResource.getRequestParams => /params
INFO: scanning completed.
INFO: starting server...
INFO: listening on http://localhost:4001

```

Sample Server: 
--------------

```java
import com.kumarvv.vrest.AbstractRestServer;

/**
 * Sample REST Server listens at 4001
 */
public class MyServer extends AbstractRestServer {
	public static void main(String[] args) {
		MyServer myServer = new MyServer();
		myServer.start(4001, new Class<?>[] { MyResource.class });
	}
}
```

This class starts the server in localhost:4001 and scans MyResource class for REST resources. 


Sample REST Resource: 
---------------------

```java
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.kumarvv.vrest.RESTServer.*;

/**
 * Sample request with full CRUD
 */
@Path("/cities")
public class CityResource {

	private static Map<String, City> cities;
	static {
		cities = new HashMap<String, City>();
		cities.put("NYC", new City("NYC", "New York"));
		cities.put("LAX", new City("LAX", "Los Angeles"));
		cities.put("SFO", new City("SFO", "San Francisco"));
		cities.put("BOS", new City("BOS", "Boston"));
	}

	@GET
	public Map<String, City> all() {
		return cities;
	}

	@GET(":city")
	public City getCity(@Param("city") String cityCode) {
		return cities.get(cityCode);
	}

	@POST("new")
	public City create(@Data City city) {
		city.setCreatedAt(new Date());
		cities.put(city.getCode(), city);
		return city;
	}

	@PUT(":city")
	public City update(@Param("city") String cityCode, @Data City upd) {
		City city = cities.get(cityCode);
		if (city != null) {
			city.setName(upd.getName());
			city.setUpdatedAt(new Date());
			cities.put(city.getCode(), city);
			return city;
		} else {
			return null;
		}
	}

	@DELETE(":city")
	public String delete(@Param("city") String cityCode) {
		cities.remove(cityCode);
		return "City [" + cityCode + "] deleted successfully";
	}

	@GET("/echo/:str")
	public String echo(@Param("str") String str) {
		return "echo: " + str;
	}
}
```

This class generates REST resources in following context paths: 
```
GET /cities          => maps to MyResource.all() 
GET /cities/:city    => maps to MyResource.getCity()
POST /cities/new     => maps to MyResource.create() 
PUT /cities/:city    => maps to MyResource.update() 
DELETE /cities/:city => maps to MyResource.delete() 
GET /echo/:str       => maps to MyResource.echo() 
```
Note on <code>/echo/:str</code>, starting with <code>/</code> in <code>@GET("/echo/:str")</code> makes the resource to be at root bypassing the <code>@Path</code> annotation at class level. All other resources have <code>/cities</code> as prefix from <code>@Path</code> annotation. 


Sample Request/Response: 
--------------

GET request of <code>http://localhost:4001/cities/NYC</code> will return following json: 
```json
{
    "code": "NYC",
    "name": "New York",
    "created": "Sun Jul 18 13:24:12 EDT 2014",
    "updated": "Sun Jul 18 13:24:34 EDT 2014"
}
```



