import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
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
	public City getCity(String a, @Param("city") String c, String b) {
		return cities.get(c);
	}

	@POST("new")
	public City create(@Data City city) {
		city.setCreatedAt(new Date());
		cities.put(city.getCode(), city);
		return city;
	}

	@PUT(":city")
	public City update() {
		City city = cities.get("NYC");
		if (city != null) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				City upd = mapper.readValue("", City.class);
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
		cities.remove("NYC");
		return "City [" +"NYC" + "] deleted successfully";
	}

	@GET("/echo/:str")
	public String echo() {
		return "";
	}
}
