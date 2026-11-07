# Krosmocraft-Launcher-V2

Launcher for the Krosmocraft modded Minecraft server (krosmocraft.fr).

It installs Minecraft 26.3 with Fabric and its own Java from official sources, keeps the Krosmocraft files up to date and logs you in.

Modules: `bootstrap` (what gets installed, updates the launcher), `core` (install, files, login) and `app` (the UI).

Needs Java 25 to build:

```
./gradlew build
./gradlew :app:run
```