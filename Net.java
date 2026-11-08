package kr.serverphone;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 패킷 정의 + 공유 상수. 내용은 JSON 문자열 하나로 주고받는다. */
public final class Net {
	public static final String MOD = "serverphone";

	// 신 설정 항목: 공격력, 공격사거리, 최대체력, 이동속도, 점프력
	public static final String[] LABEL = {"공격력", "공격사거리", "최대체력", "이동속도", "점프력"};
	public static final String[] JOSA = {"을", "를", "을", "를", "을"};
	public static final String[] UNIT = {"%", "칸", "칸", "%", "%"};
	public static final double[] MIN = {0, 1, 1, 0, 0};
	public static final double[] MAX = {300, 10, 40, 300, 300};
	public static final double[] STEP = {10, 0.5, 1, 10, 10};
	public static final double[] DEF = {100, 3, 10, 100, 100};

	public static String fmt(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
	}

	/** "100%로", "3.5칸으로" */
	public static String ro(int i) {
		return UNIT[i].equals("%") ? "로" : "으로";
	}

	public record S2C(String json) implements CustomPayload {
		public static final Id<S2C> TYPE = new Id<>(Identifier.of(MOD, "s2c"));
		public static final PacketCodec<ByteBuf, S2C> CODEC = PacketCodecs.STRING.xmap(S2C::new, S2C::json);
		@Override public Id<? extends CustomPayload> getId() { return TYPE; }
	}

	public record C2S(String json) implements CustomPayload {
		public static final Id<C2S> TYPE = new Id<>(Identifier.of(MOD, "c2s"));
		public static final PacketCodec<ByteBuf, C2S> CODEC = PacketCodecs.STRING.xmap(C2S::new, C2S::json);
		@Override public Id<? extends CustomPayload> getId() { return TYPE; }
	}

	private Net() {}
}
