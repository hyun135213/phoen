package kr.serverphone;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ServerPhone implements ModInitializer {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** 플레이어 이름(소문자) -> 설정값 5개 */
	private static final Map<String, double[]> DATA = new HashMap<>();
	private static Path file;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(Net.S2C.TYPE, Net.S2C.CODEC);
		PayloadTypeRegistry.playC2S().register(Net.C2S.TYPE, Net.C2S.CODEC);
		file = FabricLoader.getInstance().getConfigDir().resolve("serverphone.json");
		load();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity p = handler.getPlayer();
			apply(p);
			sendState(server, p);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> apply(newP));

		ServerPlayNetworking.registerGlobalReceiver(Net.C2S.TYPE, (payload, ctx) -> {
			ServerPlayerEntity p = ctx.player();
			MinecraftServer srv = ctx.server();
			JsonObject o;
			try { o = JsonParser.parseString(payload.json()).getAsJsonObject(); } catch (Exception e) { return; }
			if (!o.has("t")) return;
			switch (o.get("t").getAsString()) {
				case "state" -> sendState(srv, p);
				case "pos" -> sendPos(srv, p);
				case "chat" -> onChat(srv, p, o);
				case "set" -> { if (isOp(srv, p)) onSet(srv, p, o); }
				case "announce" -> { if (isOp(srv, p)) onAnnounce(srv, o); }
				default -> {}
			}
		});
	}

	// ---- OP 판정 (1.21.9+ 에서 GameProfile -> PlayerConfigEntry 로 바뀜. 컴파일 오류 나면 여기만 고치면 됨) ----
	static boolean isOp(MinecraftServer srv, ServerPlayerEntity p) {
		return srv.getPlayerManager().isOperator(p.getPlayerConfigEntry());
	}

	static String name(ServerPlayerEntity p) { return p.getName().getString(); }

	static double[] get(String name) {
		double[] v = DATA.get(name.toLowerCase());
		return v == null ? Net.DEF.clone() : v.clone();
	}

	// ---- 속성 적용 ----
	static void apply(ServerPlayerEntity p) {
		double[] v = get(name(p));
		base(p, EntityAttributes.ATTACK_DAMAGE, 1.0 * v[0] / 100);
		base(p, EntityAttributes.ENTITY_INTERACTION_RANGE, v[1]);
		base(p, EntityAttributes.MAX_HEALTH, v[2] * 2);
		base(p, EntityAttributes.MOVEMENT_SPEED, 0.1 * v[3] / 100);
		base(p, EntityAttributes.JUMP_STRENGTH, 0.42 * v[4] / 100);
		if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
	}

	static void base(ServerPlayerEntity p, RegistryEntry<EntityAttribute> a, double v) {
		EntityAttributeInstance i = p.getAttributeInstance(a);
		if (i != null) i.setBaseValue(v);
	}

	// ---- 전송 ----
	static void send(ServerPlayerEntity p, JsonObject o) {
		ServerPlayNetworking.send(p, new Net.S2C(o.toString()));
	}

	static void toast(ServerPlayerEntity p, String app, String pre, String hl, String post) {
		JsonObject o = new JsonObject();
		o.addProperty("t", "toast");
		o.addProperty("app", app);
		o.addProperty("pre", pre);
		o.addProperty("hl", hl);
		o.addProperty("post", post);
		send(p, o);
	}

	static void sendState(MinecraftServer srv, ServerPlayerEntity p) {
		boolean op = isOp(srv, p);
		JsonObject o = new JsonObject();
		o.addProperty("t", "state");
		o.addProperty("op", op);
		o.addProperty("me", name(p));
		JsonArray names = new JsonArray();
		for (ServerPlayerEntity q : srv.getPlayerManager().getPlayerList()) names.add(name(q));
		o.add("players", names);
		if (op) { // 설정값은 OP에게만 보낸다
			JsonObject st = new JsonObject();
			for (ServerPlayerEntity q : srv.getPlayerManager().getPlayerList()) {
				JsonArray a = new JsonArray();
				for (double d : get(name(q))) a.add(d);
				st.add(name(q), a);
			}
			o.add("settings", st);
		}
		send(p, o);
	}

	static void sendPos(MinecraftServer srv, ServerPlayerEntity p) {
		JsonObject o = new JsonObject();
		o.addProperty("t", "pos");
		JsonArray arr = new JsonArray();
		for (ServerPlayerEntity q : srv.getPlayerManager().getPlayerList()) {
			JsonObject e = new JsonObject();
			e.addProperty("n", name(q));
			e.addProperty("x", q.getX());
			e.addProperty("z", q.getZ());
			arr.add(e);
		}
		o.add("p", arr);
		send(p, o);
	}

	// ---- 핸들러 ----
	static void onChat(MinecraftServer srv, ServerPlayerEntity from, JsonObject o) {
		ServerPlayerEntity to = srv.getPlayerManager().getPlayer(o.get("to").getAsString());
		if (to == null) return;
		String text = o.get("text").getAsString();
		if (text.length() > 100) text = text.substring(0, 100);
		JsonObject c = new JsonObject();
		c.addProperty("t", "chat");
		c.addProperty("from", name(from));
		c.addProperty("text", text);
		send(to, c);
		toast(to, "메시지", name(from) + ": ", "", text);
	}

	static void onSet(MinecraftServer srv, ServerPlayerEntity admin, JsonObject o) {
		String target = o.get("target").getAsString();
		JsonArray arr = o.getAsJsonArray("v");
		double[] nv = new double[5];
		for (int i = 0; i < 5; i++) {
			nv[i] = Math.max(Net.MIN[i], Math.min(Net.MAX[i], arr.get(i).getAsDouble()));
		}
		List<ServerPlayerEntity> targets = new ArrayList<>();
		if (target.equals("ALL")) targets.addAll(srv.getPlayerManager().getPlayerList());
		else {
			ServerPlayerEntity t = srv.getPlayerManager().getPlayer(target);
			if (t != null) targets.add(t);
		}
		for (ServerPlayerEntity t : targets) {
			double[] old = get(name(t));
			DATA.put(name(t).toLowerCase(), nv.clone());
			apply(t);
			for (int i = 0; i < 5; i++) {
				if (old[i] == nv[i]) continue;
				toast(t, "신 설정", "관리자가 " + Net.LABEL[i] + Net.JOSA[i] + " ",
						Net.fmt(nv[i]) + Net.UNIT[i], Net.ro(i) + " 바꿔버리셨다.");
			}
		}
		save();
		for (ServerPlayerEntity q : srv.getPlayerManager().getPlayerList()) {
			if (isOp(srv, q)) sendState(srv, q);
		}
	}

	static void onAnnounce(MinecraftServer srv, JsonObject o) {
		String text = o.get("text").getAsString();
		if (text.length() > 120) text = text.substring(0, 120);
		String target = o.get("target").getAsString();
		if (target.equals("ALL")) {
			for (ServerPlayerEntity q : srv.getPlayerManager().getPlayerList()) toast(q, "신 설정", "", "", text);
		} else {
			ServerPlayerEntity t = srv.getPlayerManager().getPlayer(target);
			if (t != null) toast(t, "신 설정", "", "", text);
		}
	}

	// ---- 저장 ----
	static void load() {
		try {
			if (!Files.exists(file)) return;
			JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			for (var e : o.entrySet()) {
				JsonArray a = e.getValue().getAsJsonArray();
				double[] v = new double[5];
				for (int i = 0; i < 5; i++) v[i] = a.get(i).getAsDouble();
				DATA.put(e.getKey(), v);
			}
		} catch (Exception ex) { ex.printStackTrace(); }
	}

	static void save() {
		try {
			JsonObject o = new JsonObject();
			for (var e : DATA.entrySet()) {
				JsonArray a = new JsonArray();
				for (double d : e.getValue()) a.add(d);
				o.add(e.getKey(), a);
			}
			Files.writeString(file, GSON.toJson(o));
		} catch (Exception ex) { ex.printStackTrace(); }
	}
}
