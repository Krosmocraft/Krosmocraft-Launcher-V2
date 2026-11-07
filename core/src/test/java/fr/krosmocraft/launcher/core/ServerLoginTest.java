package fr.krosmocraft.launcher.core;

import fr.krosmocraft.launcher.core.auth.AuthResult;
import fr.krosmocraft.launcher.core.auth.ServerSessions;
import fr.krosmocraft.launcher.core.server.ServerEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerLoginTest {

	static final ServerEntry LIVE = ServerEntry.LIVE_FALLBACK;
	static final ServerEntry ALICE = new ServerEntry("Beta Alice", "play.krosmocraft.fr", 56202, "Beta", "play.krosmocraft.fr", 24462);
	static final ServerEntry BOB = new ServerEntry("Beta Bob", "play.krosmocraft.fr", 56203, "Beta", "play.krosmocraft.fr", 24463);

	static AuthResult.Success token(String t) {
		return new AuthResult.Success("Pepsi", t, "1.0", "3");
	}

	@Test
	void parseServers() {
		List<ServerEntry> list = ServerEntry.parse("""
				Beta Alice/play.krosmocraft.fr/56202/Beta/24462
				 Beta Bob / play.krosmocraft.fr / 56203 / Beta / 24463\s
				""");
		assertEquals(List.of(LIVE, ALICE, BOB), list);
		assertEquals("http://play.krosmocraft.fr:24462", list.get(1).authUrl());
		assertEquals("beta", list.get(1).channel());
	}

	@Test
	void liveFirst() {
		assertEquals("http://launcher.krosmocraft.fr:24454", LIVE.authUrl());
		assertEquals("live", LIVE.channel());
		assertEquals(List.of(LIVE), ServerEntry.parse(""));
		assertEquals(LIVE, ServerEntry.parse("Beta Alice/play.krosmocraft.fr/56202/Beta/24462").getFirst());
	}

	@Test
	void skipBadLines() {
		List<ServerEntry> list = ServerEntry.parse("""
				Beta Old/play.krosmocraft.fr/56202/Beta
				Beta Extra/play.krosmocraft.fr/56202/Beta/24462/x
				Beta Port/play.krosmocraft.fr/abc/Beta/24462
				Beta Auth/play.krosmocraft.fr/56202/Beta/
				Beta Zero/play.krosmocraft.fr/0/Beta/24462
				Beta Big/play.krosmocraft.fr/56202/Beta/70000
				/play.krosmocraft.fr/56202/Beta/24462
				Beta NoHost//56202/Beta/24462
				Beta NoType/play.krosmocraft.fr/56202//24462
				Live/evil.example/25565/Live/1234
				Beta Copy/play.krosmocraft.fr/56119/Beta/24462
				garbage

				Beta Alice/play.krosmocraft.fr/56202/Beta/24462
				Beta Alice/play.krosmocraft.fr/56299/Beta/24499
				""");
		assertEquals(List.of(LIVE, ALICE), list);
	}

	@Test
	void tokenPerServer() {
		ServerSessions sessions = new ServerSessions();
		sessions.put(LIVE, token("live-token"));

		assertEquals("live-token", sessions.get(LIVE).orElseThrow().token());
		assertEquals(Optional.empty(), sessions.get(ALICE));

		sessions.put(ALICE, token("alice-token"));
		assertEquals("alice-token", sessions.get(ALICE).orElseThrow().token());
		assertEquals("live-token", sessions.get(LIVE).orElseThrow().token());
		assertTrue(sessions.get(BOB).isEmpty());
		assertTrue(sessions.get(null).isEmpty());
	}

	@Test
	void tokenKeptAfterListReload() {
		ServerSessions sessions = new ServerSessions();
		sessions.put(ALICE, token("alice-token"));
		ServerEntry reloaded = ServerEntry.parse("Beta Alice/play.krosmocraft.fr/56202/Beta/24462").get(1);
		assertEquals("alice-token", sessions.get(reloaded).orElseThrow().token());
	}

	@Test
	void logoutClearsAll() {
		ServerSessions sessions = new ServerSessions();
		sessions.put(LIVE, token("a"));
		sessions.put(ALICE, token("b"));
		sessions.clear();
		assertTrue(sessions.get(LIVE).isEmpty());
		assertTrue(sessions.get(ALICE).isEmpty());
	}
}
