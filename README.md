vREST
=====

Simple single class REST server using Java SE only.

- Jave SE 1.6 or above 
- No HTTP server / servlet container required 
- Uses Java SE socket to listen and process client requests
- Multi-threaded request processing (100 threads by default)
- Uses jackson mapper (org.codehaus.jackson) for JSON processing
- Automatic scanning of all REST resources in current classpath (no configurations required)

<strong>Limitations:</strong> 
- Not a production ready server (for quick REST services testing only) 
- supports JSON data communications only 

Setup
-----

Download or checkout and run following maven command: 

```bash
$ mvn compile
$ mvn exec:exec -DRESTServer
```

This command should scan all the classes in current directory for REST resources and listens on 4001 port: 

```bash
$ mvn exec:exec -DRESTServer
[INFO] Scanning for projects...
...
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

Usage 
------

Import static properties from RESTServer class and add <code>@Resource</code> annotation with path to any POJO (or non-POJO) classes: 

```java 
import static com.kumarvv.vrest.RESTServer.*;

@Resource("/cities")
public class CityResource {
...
```

Add <code>@GET</code> (or <code>@POST, @PUT, @UPDATE</code>) annotation to the class method. This annotation also supports resource path to append to class resource path. 

```java 
@GET
public Map<String, City> all() {
...

@GET("favorties")  // resolves to "/cities/favorites"
public Map<String, City> favorites() {
	return cities;
}
```

Add <code>@Param</code> to inject request or url parameter value to method arguments: 

```java
@GET(":city")
public City getCity(@Param("city") String cityCode) {
	return cities.get(cityCode);
}
```

Request using <code>/cities/NYC</code> url will pass the <code>NYC</code> value to <code>cityCode</code> method argument. 
Add <code>@Data</code> to inject request payload (form-data) to method argument: 

```java
@PUT(":city")
public City update(@Param("city") String cityCode, @Data City upd) {
	City city = cities.get(cityCode);
...
```


Sample REST Resource class 
---------------------------

```java
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.kumarvv.vrest.RESTServer.*;

/**
 * Sample request with full CRUD
 */
@Resource("/cities")
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

	@GET("/params")
	public Map<String, String> getRequestParams(@Params Map<String, String> params) {
		return params;
	}
}
```

This class generates REST resources in following context paths: 
```
GET /cities          => maps to CityResource.all() 
GET /cities/:city    => maps to CityResource.getCity()
POST /cities/new     => maps to CityResource.create() 
PUT /cities/:city    => maps to CityResource.update() 
DELETE /cities/:city => maps to CityResource.delete() 
GET /echo/:str       => maps to CityResource.echo() 
GET /params          => maps to CityResource.getRequestParams()
```
Note on <code>/echo/:str</code>, starting with <code>/</code> in <code>@GET("/echo/:str")</code> makes the resource to be at root bypassing the <code>@Resource</code> annotation at class level. All other resources have <code>/cities</code> as prefix from <code>@Resource</code> annotation. 


Sample Request/Response 
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



