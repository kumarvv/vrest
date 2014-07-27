package com.kumarvv.vrest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * (c) kappals.com
 *
 * User: Vijay
 * Date: 7/25/14
 * Time: 9:26 PM
 */
public abstract class AbstractResource {

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Path {
		public String value();
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface GET {
		public String value() default "";
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface POST {
		public String value() default "";
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface PUT {
		public String value() default "";
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DELETE {
		public String value() default "";
	}

	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Param {
		public String value();
	}

	private Map<String, String> requestParams = new HashMap<String, String>();
	public Map<String, String> geRequestParams() {
		return requestParams;
	}

	private Map<String, String> urlParams = new HashMap<String, String>();
	public Map<String, String> getUrlParams() {
		return urlParams;
	}

	public void initParams(Map<String, String> requestParams, Map<String, String> urlParams) {
		this.requestParams = requestParams;
		this.urlParams = urlParams;
	}

	public String getUrlParam(String param) {
		if (urlParams != null) {
			return this.urlParams.get(param);
		} else {
			return null;
		}
	}
	public String getRequestParam(String param) {
		if (requestParams != null) {
			return this.requestParams.get(param);
		} else {
			return null;
		}
	}
}
