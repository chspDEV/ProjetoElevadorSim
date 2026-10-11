package io.github.elevador.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.elevador.Config;
import io.github.elevador.Main;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(Config.TITULO_APLICACAO);
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(Config.LARGURA_TELA, Config.ALTURA_TELA);
        try {
            configuration.setWindowIcon(com.badlogic.gdx.Files.FileType.Classpath, "libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        } catch (Exception ignored) {
        }
        return configuration;
    }
}