///usr/bin/env jbang "$0" "$@" ; exit $?
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:1.11.0.Final}@pom
//DEPS io.quarkus:quarkus-resteasy
//DEPS io.quarkus:quarkus-resteasy-jackson
//DEPS io.quarkiverse.loggingjson:quarkus-logging-json:1.1.1

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

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;

@Path("/api")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BlockChainService {

	private static final Logger log = Logger.getLogger(BlockChainService.class);
	private static int counter = 0;

	@Context
	HttpServerRequest request;

	@GET
	@Path("/blockchain/health")
	public void health() {}

	@GET
	@Path("/blockchain/summary/")
	public String summary() throws IOException {
		counter++;
		long before = System.currentTimeMillis();
		try {
			int value = TUESDAY.getValue() * FRIDAY.getValue() * TUESDAY.getValue() * FRIDAY.getValue();
			Thread.sleep((long) (FRIDAY.getValue() * value * 10 + Math.random() * 400));
		} catch (InterruptedException ie){
			System.out.println(ie);
		}
		long after = System.currentTimeMillis();
		final String latency = String.valueOf(after - before);
		log.infov("BlockChain Summary: # requests {0} latency: {1}", counter,
				kv("labels.latency", latency),
				kv("labels.method", "GET"),
				kv("labels.statusCode", 200),
				kv("labels.clientIP", request.remoteAddress().host()),
				kv("labels.path", "/blockchain/summary/")
		);
		return String.format("{ \"requests\": %1s, \"latency\": %2s }", counter, latency);
	}

//	@GET
//	@Path("/blockchain/{id}")
//	public String blockChain(@PathParam("id") Integer number) throws IOException {
	@POST
	@Path("/blockchain/")
	public String blockChain(BlockChain number) throws IOException {
		counter++;
		long before = System.currentTimeMillis();
		if (number == null) {
			number = new BlockChain(1000);
		}
		writeFile(number.getNumber());
		long after = System.currentTimeMillis();
		final String latency = String.valueOf(after - before);
		log.infov("BlockChain result: requests {0} latency: {1}", counter,
				kv("labels.latency", latency),
				kv("labels.method", "POST"),
				kv("labels.statusCode", 200),
				kv("labels.clientIP", request.remoteAddress().host()),
				kv("labels.path", "/blockchain/"+number.getNumber())
		);
		return latency;
	}

	public void writeFile(Integer number) throws IOException {
		FileWriter fileWriter = new FileWriter("blockchain.txt");
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.print("Blockchain checks");
		int last = 10 + (int) Math.random() * 10;
		int parts = number / last;
		for (int i = 0; i < last; i++) {
			//String result = calculatePrimeNumbers(number / 2, number + i);
			String result = calculatePrimeNumbers(parts * (i), parts * (i + 1));
			printWriter.printf("%s", result);
			String now = now();
			log.infov("BlockChain: {0} time: {1}", result, now, kv("result", result), kv("now", now));
			//log.infov("done",kv("result", result), kv("now", now));
			printWriter.flush();
		}
		printWriter.close();
	}

	public String now() {
		//String now = LocalDateTime.now(Clock.systemUTC()).toString();
		return ZonedDateTime.now(ZoneOffset.UTC).toString();
	}

	public String calculatePrimeNumbers(int from, int to) {
		String output = "calculating between " + from + " and " + to + " are:";
		for (int j = from; j <= to; j++) {
			int count = 0;
			for (int i = 1; i <= j; i++) {
				if (j % i == 0) {
					count++;
				}
			}
			if (j % 9 == 0 || j % 15 == 0) {
				output += " " + j;
			}
		}
		return output;
	}

	public static class BlockChain {

		private int number;

		public BlockChain() {
		}

		public BlockChain(int number) {
			this.number = number;
		}

		public Integer getNumber() {
			return number;
		}
	}
}
