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

	@GET("/params")
	public Map<String, String> getRequestParams(@Params Map<String, String> params) {
		return params;
	}
}
