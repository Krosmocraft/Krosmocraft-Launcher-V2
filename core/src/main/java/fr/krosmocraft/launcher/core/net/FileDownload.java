package fr.krosmocraft.launcher.core.net;

import java.nio.file.Path;

public record FileDownload(String url, Path target, String sha1, long size) {
}
