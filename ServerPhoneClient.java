package kr.serverphone.client;

import kr.serverphone.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ServerPhoneClient implements ClientModInitializer {
	static KeyBinding OPEN;

	@Override
	public void onInitializeClient() {
		KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(Net.MOD, "main"));
		OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.serverphone.open", GLFW.GLFW_KEY_P, cat));

		ClientPlayNetworking.registerGlobalReceiver(Net.S2C.TYPE, (payload, ctx) -> ClientState.handle(payload.json()));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (OPEN.wasPressed()) {
				if (mc.currentScreen == null && mc.player != null) {
					ClientPlayNetworking.send(new Net.C2S("{\"t\":\"state\"}"));
					mc.setScreen(new PhoneScreen());
				}
			}
		});

		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.of(Net.MOD, "hud"), Hud::render);
	}
}
