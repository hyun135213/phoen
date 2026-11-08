package kr.serverphone.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.serverphone.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.*;

public class PhoneScreen extends Screen {
	static final int PW = 220, PH = 250;
	static final int GOLD = 0xFFF5C04A, WHITE = 0xFFFFFFFF, GRAY = 0xFF8B90A0;
	static final String[] DIR = {"북", "북동", "동", "남동", "남", "남서", "서", "북서"};

	private String page = "home", chatWith, godTarget = "ALL", mapTarget;
	private double[] draft = Net.DEF.clone();
	private TextFieldWidget input;
	private int px, py, tk;

	public PhoneScreen() { super(Text.literal("서버 폰")); }

	@Override public boolean shouldPause() { return false; }

	private void go(String p) {
		page = p;
		if (p.equals("map")) requestPos();
		clearAndInit();
	}

	private static void sendJson(JsonObject o) { ClientPlayNetworking.send(new Net.C2S(o.toString())); }
	private static void requestPos() { ClientPlayNetworking.send(new Net.C2S("{\"t\":\"pos\"}")); }

	private void label(String s, int x, int y, int color) {
		addDrawable((c, mx, my, d) -> c.drawTextWithShadow(textRenderer, Text.literal(s), x, y, color));
	}
	private void title(String s) {
		addDrawable((c, mx, my, d) -> c.drawCenteredTextWithShadow(textRenderer, Text.literal(s), px + PW / 2, py + 10, GOLD));
	}
	private void btn(String s, int x, int y, int w, int h, Runnable r) {
		addDrawableChild(ButtonWidget.builder(Text.literal(s), b -> r.run()).dimensions(x, y, w, h).build());
	}
	private List<String> others() {
		List<String> l = new ArrayList<>(ClientState.players);
		l.remove(ClientState.me);
		return l;
	}

	@Override
	protected void init() {
		px = (width - PW) / 2;
		py = Math.max(4, (height - PH) / 2);
		addDrawable((c, mx, my, d) -> {
			c.fill(px - 3, py - 3, px + PW + 3, py + PH + 3, 0xFFD9A441);
			c.fill(px, py, px + PW, py + PH, 0xFF14161C);
		});
		if (!page.equals("home")) btn("‹ 홈", px + 6, py + 6, 40, 16, () -> go("home"));
		switch (page) {
			case "home" -> home();
			case "god" -> god();
			case "msg" -> msg();
			case "chat" -> chat();
			case "map" -> map();
		}
	}

	// ---------- 홈 ----------
	private void home() {
		title("서버 폰");
		int y = py + 34, w = PW - 24;
		btn("메시지", px + 12, y, w, 24, () -> go("msg")); y += 30;
		btn("지도", px + 12, y, w, 24, () -> go("map")); y += 30;
		if (ClientState.op) { // OP 에게만 보이는 앱
			btn("신 설정", px + 12, y, w, 24, () -> {
				draft = ClientState.settings.getOrDefault(godTarget, Net.DEF).clone();
				go("god");
			});
		}
		btn("닫기", px + 12, py + PH - 30, w, 20, this::close);
	}

	// ---------- 신 설정 (OP) ----------
	private void god() {
		if (!ClientState.op) { go("home"); return; }
		title("신 설정");
		List<String> t = new ArrayList<>();
		t.add("ALL");
		t.addAll(ClientState.players);
		int ti = Math.max(0, t.indexOf(godTarget));
		btn("◀", px + 8, py + 28, 20, 16, () -> setGod(t.get((ti + t.size() - 1) % t.size())));
		btn("▶", px + PW - 28, py + 28, 20, 16, () -> setGod(t.get((ti + 1) % t.size())));
		String shown = godTarget.equals("ALL") ? "전체" : godTarget;
		addDrawable((c, mx, my, d) -> c.drawCenteredTextWithShadow(textRenderer, Text.literal("대상: " + shown), px + PW / 2, py + 32, WHITE));

		for (int i = 0; i < 5; i++) addDrawableChild(new PSlider(px + 10, py + 50 + i * 21, PW - 20, 18, i));

		btn("초기화", px + 10, py + 160, 92, 18, () -> { draft = Net.DEF.clone(); clearAndInit(); });
		btn("적용하기", px + PW - 102, py + 160, 92, 18, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("t", "set");
			o.addProperty("target", godTarget);
			JsonArray a = new JsonArray();
			for (double v : draft) a.add(v);
			o.add("v", a);
			sendJson(o);
		});

		label("공지 보내기", px + 10, py + 186, GRAY);
		input = new TextFieldWidget(textRenderer, px + 10, py + 198, PW - 66, 18, Text.empty());
		input.setMaxLength(100);
		addDrawableChild(input);
		btn("공지", px + PW - 52, py + 198, 42, 18, () -> {
			String s = input.getText().trim();
			if (s.isEmpty()) return;
			JsonObject o = new JsonObject();
			o.addProperty("t", "announce");
			o.addProperty("target", godTarget);
			o.addProperty("text", s);
			sendJson(o);
			input.setText("");
		});
	}

	private void setGod(String name) {
		godTarget = name;
		draft = ClientState.settings.getOrDefault(name, Net.DEF).clone();
		clearAndInit();
	}

	private class PSlider extends SliderWidget {
		private final int i;
		PSlider(int x, int y, int w, int h, int i) {
			super(x, y, w, h, Text.empty(), (draft[i] - Net.MIN[i]) / (Net.MAX[i] - Net.MIN[i]));
			this.i = i;
			updateMessage();
		}
		private double val() {
			double v = Net.MIN[i] + value * (Net.MAX[i] - Net.MIN[i]);
			return Math.round(v / Net.STEP[i]) * Net.STEP[i];
		}
		@Override protected void updateMessage() {
			setMessage(Text.literal(Net.LABEL[i] + ": " + Net.fmt(val()) + Net.UNIT[i]));
		}
		@Override protected void applyValue() { draft[i] = val(); }
	}

	// ---------- 메시지 ----------
	private void msg() {
		title("메시지");
		List<String> o = others();
		if (o.isEmpty()) label("접속 중인 다른 플레이어가 없습니다", px + 12, py + 40, GRAY);
		for (int i = 0; i < Math.min(8, o.size()); i++) {
			String n = o.get(i);
			List<String[]> h = ClientState.chats.get(n);
			String last = (h == null || h.isEmpty()) ? "" : "  · " + h.get(h.size() - 1)[1];
			btn(n + last, px + 12, py + 30 + i * 24, PW - 24, 20, () -> { chatWith = n; go("chat"); });
		}
	}

	private void chat() {
		title(chatWith);
		addDrawable((c, mx, my, d) -> {
			List<OrderedText> lines = new ArrayList<>();
			for (String[] m : ClientState.chats.getOrDefault(chatWith, List.of())) {
				lines.addAll(textRenderer.wrapLines(Text.literal(m[0] + ": " + m[1]), PW - 20));
			}
			int max = 17, s = Math.max(0, lines.size() - max);
			for (int i = s; i < lines.size(); i++) c.drawTextWithShadow(textRenderer, lines.get(i), px + 10, py + 30 + (i - s) * 10, WHITE);
		});
		input = new TextFieldWidget(textRenderer, px + 10, py + PH - 28, PW - 66, 18, Text.empty());
		input.setMaxLength(100);
		addDrawableChild(input);
		btn("전송", px + PW - 52, py + PH - 28, 42, 18, () -> {
			String s = input.getText().trim();
			if (s.isEmpty()) return;
			ClientState.chats.computeIfAbsent(chatWith, k -> new ArrayList<>()).add(new String[]{ClientState.me, s});
			JsonObject o = new JsonObject();
			o.addProperty("t", "chat");
			o.addProperty("to", chatWith);
			o.addProperty("text", s);
			sendJson(o);
			input.setText("");
		});
	}

	// ---------- 지도 ----------
	private void map() {
		title("지도");
		List<String> o = others();
		if (mapTarget == null || !o.contains(mapTarget)) mapTarget = o.isEmpty() ? null : o.get(0);
		final int mx0 = px + 10, my0 = py + 28, mw = PW - 20, mh = 130;

		addDrawable((c, mx, my, d) -> {
			c.fill(mx0, my0, mx0 + mw, my0 + mh, 0xFF2D5A34);
			List<ClientState.Pos> ps = ClientState.pos;
			if (ps.isEmpty()) return;
			double minX = 1e18, maxX = -1e18, minZ = 1e18, maxZ = -1e18;
			for (var p : ps) { minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x()); minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z()); }
			double span = Math.max(Math.max(maxX - minX, maxZ - minZ), 100) * 1.4;
			double cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2, sc = Math.min(mw, mh) / span;
			ClientState.Pos me = null, tg = null;
			for (var p : ps) { if (p.n().equals(ClientState.me)) me = p; if (p.n().equals(mapTarget)) tg = p; }
			if (me != null && tg != null) {
				int ax = (int) (mx0 + mw / 2 + (me.x() - cx) * sc), az = (int) (my0 + mh / 2 + (me.z() - cz) * sc);
				int bx = (int) (mx0 + mw / 2 + (tg.x() - cx) * sc), bz = (int) (my0 + mh / 2 + (tg.z() - cz) * sc);
				for (int s = 0; s <= 40; s++) c.fill(ax + (bx - ax) * s / 40, az + (bz - az) * s / 40, ax + (bx - ax) * s / 40 + 1, az + (bz - az) * s / 40 + 1, GOLD);
			}
			for (var p : ps) {
				int x = (int) (mx0 + mw / 2 + (p.x() - cx) * sc), y = (int) (my0 + mh / 2 + (p.z() - cz) * sc);
				int col = p.n().equals(ClientState.me) ? GOLD : p.n().equals(mapTarget) ? 0xFFE5484D : WHITE;
				c.fill(x - 2, y - 2, x + 3, y + 3, col);
				c.drawTextWithShadow(textRenderer, Text.literal(p.n()), x + 5, y - 4, col);
			}
		});

		if (o.isEmpty()) { label("다른 플레이어가 없습니다", px + 12, py + 168, GRAY); return; }
		int ti = o.indexOf(mapTarget);
		btn("◀", px + 8, py + 164, 20, 16, () -> { mapTarget = o.get((ti + o.size() - 1) % o.size()); clearAndInit(); });
		btn("▶", px + PW - 28, py + 164, 20, 16, () -> { mapTarget = o.get((ti + 1) % o.size()); clearAndInit(); });
		addDrawable((c, mx, my, d) -> {
			c.drawCenteredTextWithShadow(textRenderer, Text.literal("추적: " + mapTarget), px + PW / 2, py + 168, WHITE);
			ClientState.Pos me = null, tg = null;
			for (var p : ClientState.pos) { if (p.n().equals(ClientState.me)) me = p; if (p.n().equals(mapTarget)) tg = p; }
			if (me == null || tg == null) return;
			double dx = tg.x() - me.x(), dz = tg.z() - me.z();
			int dist = (int) Math.round(Math.hypot(dx, dz));
			double ang = (Math.toDegrees(Math.atan2(dx, -dz)) + 360) % 360;
			String dir = DIR[(int) Math.round(ang / 45) % 8];
			c.drawCenteredTextWithShadow(textRenderer, Text.literal("거리 " + dist + "칸 · " + dir + "쪽"), px + PW / 2, py + 192, GOLD);
			c.drawCenteredTextWithShadow(textRenderer, Text.literal("내 위치 " + (int) me.x() + ", " + (int) me.z()), px + PW / 2, py + 206, GRAY);
		});
	}

	@Override
	public void tick() {
		if (page.equals("map") && ++tk % 20 == 0) requestPos(); // 1초마다 위치 갱신
	}
}
