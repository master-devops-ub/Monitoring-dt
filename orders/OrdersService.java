///usr/bin/env jbang "$0" "$@" ; exit $?
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:1.11.0.Final}@pom
//DEPS io.quarkus:quarkus-resteasy
//DEPS io.quarkus:quarkus-resteasy-jackson
//DEPS io.quarkus:quarkus-jdbc-mysql
//DEPS io.quarkus:quarkus-agroal
//DEPS io.quarkiverse.loggingjson:quarkus-logging-json:1.1.1
//DEPS com.squareup.okhttp3:okhttp:4.9.1

//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Dquarkus.http.port=80

//FILES application.properties

import static io.quarkiverse.loggingjson.providers.KeyValueStructuredArgument.*;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.TUESDAY;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import javax.inject.Inject;
import io.agroal.api.AgroalDataSource;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import okhttp3.Call;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;

@Path("/api")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdersService {

	private static final Logger log = Logger.getLogger(OrdersService.class);
	private static int counter = 0;

	private final Map<Integer, Integer> orders = new HashMap();
	private final OkHttpClient client = new OkHttpClient();

	private final String URL = "http://blockchain:80/api/blockchain";

	@Inject
	AgroalDataSource dataSource;

	@Context
	HttpServerRequest request;

	@GET
	@Path("/orders/health")
	public void health() {}

	@POST
	@Path("/orders/purchase")
	public String blockChain(Order number) throws IOException {
		counter++;
		long before = System.currentTimeMillis();
		if (number == null) {
			number = new Order(1000);
		}
		String json = new StringBuilder()
				.append("{")
				.append("\"number\": "+number.getId())
				.append("}")
				.toString();
		RequestBody body = RequestBody.create(
				json,
				okhttp3.MediaType.parse("application/json; charset=utf-8")
		);
		Request request = new Request.Builder()
				.url(URL + "/")
				.addHeader("Content-Type", "application/json")
				.post(body)
				.build();

		save(number.getId());

		Call call = client.newCall(request);
		try(Response response = call.execute()) {
			System.out.println(response);
		}

		long after = System.currentTimeMillis();
		final String latency = String.valueOf(after - before);

		return latency;
	}

	public void save(int id) {
		final String sql = "INSERT INTO orders(orderid, date) VALUES(?,?)";
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		// do we need this? 😅 let's see what will happen here...
		for(int i=0; i<500; i++) {
			orders.put(id+i, id+i);
		}
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setInt(1, id);
				statement.setTimestamp(2, timestamp);
				int result = statement.executeUpdate();
			} catch (SQLException throwable) {
				throwable.printStackTrace();
			}
		} catch (SQLException throwable) {
			throwable.printStackTrace();
		}
	}

	public String now() {
		//String now = LocalDateTime.now(Clock.systemUTC()).toString();
		return ZonedDateTime.now(ZoneOffset.UTC).toString();
	}

	public static class Order {

		private int id;

		public Order() {
		}

		public Order(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

}
