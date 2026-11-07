package fr.krosmocraft.launcher.core.auth;

import fr.krosmocraft.launcher.core.server.ServerEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ServerSessions {

	private final Map<String, AuthResult.Success> sessions = new HashMap<>();

	public void put(ServerEntry server, AuthResult.Success session) {
		sessions.put(server.key(), session);
	}

	public Optional<AuthResult.Success> get(ServerEntry server) {
		return Optional.ofNullable(server == null ? null : sessions.get(server.key()));
	}

	public void clear() {
		sessions.clear();
	}
}
