vrest
=====

Simple REST server with just two classes (server and REST resource).

- No HTTP server / servlet container required
- Uses Java SE socket to listen and process client requests
- Multi-threaded request processing (100 threads by default)
- Uses jackson mapper (org.codehaus.jackson) for JSON processing
- Jave SE 1.7 or above 

Limitations: 
- its not a production ready server, but gives quick REST resources for testing your clients 
- supports JSON data communications only 


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



