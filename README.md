vrest
=====

Simple REST server with just two classes (server and REST resource).

- No HTTP server / servlet container required.
- Uses Java SE socket to listen and process client requests.
- Multi-threaded request processing (100 threads by default).
- Uses jackson mapper (org.codehaus.jackson) for JSON processing

Sample Server: 
--------------

```
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

```
/**
 * Sample request with full CRUD
 */
@AbstractResource.Path("/cities")
public class MyResource extends AbstractResource {

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
	public City getCity() {
		return cities.get(getUrlParam("city"));
	}

	@POST("new")
	public City create() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			City city = mapper.readValue(getRequestParam("Payload"), City.class);
			city.setCreatedAt(new Date());
			cities.put(city.getCode(), city);
			return city;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@PUT(":city")
	public City update() {
		City city = cities.get(getUrlParam("city"));
		if (city != null) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				City upd = mapper.readValue(getRequestParam("Payload"), City.class);
				city.setName(upd.getName());
				city.setUpdatedAt(new Date());
				cities.put(city.getCode(), city);
				return city;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	@DELETE(":city")
	public String delete() {
		cities.remove(getUrlParam("city"));
		return "City [" + getUrlParam("city") + "] deleted successfully";
	}

	@GET("/echo/:str")
	public String echo() {
		return getUrlParam("str");
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



