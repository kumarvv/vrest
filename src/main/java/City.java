import java.io.Serializable;
import java.util.Date;

/**
 * sample model
 */
public class City implements Serializable {

	public City() {
	}
	public City(String code, String name) {
		this.code = code;
		this.name = name;
		this.createdAt = new Date();
	}

	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private Date createdAt;

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreated() {
		return createdAt == null ? null : createdAt.toString();
	}

	private Date updatedAt;

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getUpdated() {
		return updatedAt == null ? null : updatedAt.toString();
	}
}
