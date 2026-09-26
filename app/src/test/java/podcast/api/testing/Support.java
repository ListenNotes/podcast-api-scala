package podcast.api.testing;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Loopback fixture shared by the language examples. Never connects to an API. */
public final class Support implements AutoCloseable {
    public record Request(String method, URI uri, String key, String body) { }
    private final HttpServer server;
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    public volatile int status = 200;
    public volatile String responseBody = "{\"ok\":true}";
    public Support() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI(),
                    exchange.getRequestHeaders().getFirst("X-ListenAPI-Key"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                exchange.getResponseHeaders().set("X-ListenAPI-Usage", "12");
                exchange.getResponseHeaders().set("X-ListenAPI-FreeQuota", "300");
                byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
    }
    public String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v2"; }
    public Request take() throws Exception {
        Request request = requests.poll(5, TimeUnit.SECONDS);
        if (request == null) throw new AssertionError("Missing loopback request");
        return request;
    }
    public void close() { server.stop(0); }
    public static List<JSONObject> operations() throws Exception {
        try (var input = Support.class.getResourceAsStream("/api-contract.json")) {
            if (input == null) throw new IllegalStateException("Missing API contract");
            List<JSONObject> result = new ArrayList<>();
            new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                .getJSONArray("operations").forEach(value -> result.add((JSONObject) value));
            return result;
        }
    }
    public static Map<String, String> parameters(JSONObject op) {
        Map<String, String> result = new LinkedHashMap<>();
        op.getJSONObject("example_params").toMap().forEach((key, value) -> {
            if (value != null) result.put(key, value.toString());
        });
        return result;
    }
    public static Map<String, String> decode(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) return values;
        for (String pair : text.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return values;
    }
    public static void verify(JSONObject op, Map<String, String> values, Request request) {
        String expectedPath = "/api/v2" + op.getString("path");
        Map<String, String> query = new LinkedHashMap<>(), body = new LinkedHashMap<>();
        for (Object entry : op.getJSONArray("parameters")) {
            JSONObject parameter = (JSONObject) entry;
            String name = parameter.getString("name");
            if (!values.containsKey(name)) continue;
            if (parameter.getString("in").equals("path")) {
                expectedPath = expectedPath.replace("{" + name + "}",
                    URLEncoder.encode(values.get(name), StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A"));
            } else {
                (parameter.getString("in").equals("body") ? body : query).put(name, values.get(name));
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(op.getString("method"), request.method());
        org.junit.jupiter.api.Assertions.assertEquals(expectedPath, request.uri().getRawPath());
        org.junit.jupiter.api.Assertions.assertEquals(query, decode(request.uri().getRawQuery()));
        org.junit.jupiter.api.Assertions.assertEquals(body, decode(request.body()));
    }
}
