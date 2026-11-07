package fr.krosmocraft.launcher.core.auth;

public sealed interface AuthResult {

	record Success(String pseudo, String token, String version, String playerCount) implements AuthResult {
	}

	record NeedsPseudo() implements AuthResult {
	}

	record PseudoRejected(Reason reason) implements AuthResult {
		public enum Reason { TAKEN, INVALID, BAD_SIZE }
	}

	record WrongCredentials() implements AuthResult {
	}

	record Unreachable(String detail) implements AuthResult {
	}
}
