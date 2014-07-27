import com.kumarvv.vrest.AbstractResource;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: vvijayaram
 * Date: 7/24/14
 * Time: 6:01 PM
 * To change this template use File | Settings | File Templates.
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
