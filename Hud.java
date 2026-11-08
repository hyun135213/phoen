package kr.serverphone.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.Iterator;
import java.util.List;

/** 손에 든 폰(우하단) + 상단 알림 */
final class Hud {
	private static final int GOLD = 0xFFF5C04A;
	private static final long LIFE = 4000;

	static void render(DrawContext c, RenderTickCounter tick) {
		MinecraftClient mc = MinecraftClient.getInstance();
		TextRenderer tr = mc.textRenderer;
		int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();

		if (mc.currentScreen == null) { // 손에 든 폰
			int x = sw - 44, y = sh - 26;
			c.fill(x - 2, y - 2, x + 30, sh, 0xFFD9A441);
			c.fill(x, y, x + 28, sh, 0xFF14161C);
			c.fill(x + 9, y + 3, x + 19, y + 5, 0xFF000000);
			c.drawTextWithShadow(tr, Text.literal("P"), x + 11, y + 12, GOLD);
		}

		long now = System.currentTimeMillis();
		int y = 6, shown = 0;
		Iterator<ClientState.Toast> it = ClientState.toasts.iterator();
		while (it.hasNext()) {
			ClientState.Toast t = it.next();
			long age = now - t.t();
			if (age > LIFE) { it.remove(); continue; }
			if (shown++ >= 3) break;
			float k = age < 250 ? age / 250f : age > LIFE - 250 ? (LIFE - age) / 250f : 1f;
			int off = (int) ((1 - k) * -50);

			Text body = Text.literal(t.pre())
					.append(Text.literal(t.hl()).styled(s -> s.withColor(0xF5C04A)))
					.append(Text.literal(t.post()));
			int w = Math.min(280, sw - 12), x = (sw - w) / 2;
			List<OrderedText> lines = tr.wrapLines(body, w - 44);
			int n = Math.min(2, lines.size());
			int h = Math.max(34, 22 + 10 * n);
			int top = y + off;

			c.fill(x, top, x + w, top + h, 0xF01F222B);
			c.fill(x, top, x + w, top + 1, 0xFF3A3F4D);
			c.fill(x, top + h - 1, x + w, top + h, 0xFF3A3F4D);
			c.fill(x, top, x + 1, top + h, 0xFF3A3F4D);
			c.fill(x + w - 1, top, x + w, top + h, 0xFF3A3F4D);
			c.fill(x + 6, top + 6, x + 26, top + 26, 0xFF2A2310);
			c.drawTextWithShadow(tr, Text.literal(t.app().substring(0, 1)), x + 13, top + 12, GOLD);
			c.drawTextWithShadow(tr, Text.literal(t.app()), x + 32, top + 6, 0xFFFFFFFF);
			String now_ = "지금";
			c.drawTextWithShadow(tr, Text.literal(now_), x + w - 8 - tr.getWidth(now_), top + 6, 0xFF8B90A0);
			for (int i = 0; i < n; i++) c.drawTextWithShadow(tr, lines.get(i), x + 32, top + 18 + i * 10, 0xFFFFFFFF);
			y += h + 4;
		}
	}

	private Hud() {}
}
