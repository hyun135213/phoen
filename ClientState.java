package kr.serverphone.client;

import com.google.gson.*;
import java.util.*;

public final class ClientState {
	public static boolean op;
	public static String me = "";
	public static List<String> players = new ArrayList<>();
	public static Map<String, double[]> settings = new HashMap<>();
	public static Map<String, List<String[]>> chats = new HashMap<>();
	public static List<Pos> pos = new ArrayList<>();
	public static final List<Toast> toasts = new ArrayList<>();

	public record Pos(String n, double x, double z) {}
	public record Toast(String app, String pre, String hl, String post, long t) {}

	public static void handle(String json) {
		JsonObject o = JsonParser.parseString(json).getAsJsonObject();
		switch (o.get("t").getAsString()) {
			case "state" -> {
				op = o.get("op").getAsBoolean();
				me = o.get("me").getAsString();
				players = new ArrayList<>();
				o.getAsJsonArray("players").forEach(e -> players.add(e.getAsString()));
				settings = new HashMap<>();
				if (o.has("settings")) {
					for (var e : o.getAsJsonObject("settings").entrySet()) {
						JsonArray a = e.getValue().getAsJsonArray();
						double[] v = new double[5];
						for (int i = 0; i < 5; i++) v[i] = a.get(i).getAsDouble();
						settings.put(e.getKey(), v);
					}
				}
			}
			case "toast" -> toasts.add(new Toast(o.get("app").getAsString(), o.get("pre").getAsString(),
					o.get("hl").getAsString(), o.get("post").getAsString(), System.currentTimeMillis()));
			case "chat" -> chats.computeIfAbsent(o.get("from").getAsString(), k -> new ArrayList<>())
					.add(new String[]{o.get("from").getAsString(), o.get("text").getAsString()});
			case "pos" -> {
				List<Pos> l = new ArrayList<>();
				o.getAsJsonArray("p").forEach(e -> {
					JsonObject p = e.getAsJsonObject();
					l.add(new Pos(p.get("n").getAsString(), p.get("x").getAsDouble(), p.get("z").getAsDouble()));
				});
				pos = l;
			}
			default -> {}
		}
	}

	private ClientState() {}
}
