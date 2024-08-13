package uk.anbu.samples.filewatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import javax.swing.*;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConfigManager.class).in(com.google.inject.Singleton.class);
        bind(DirectoryWatcherApp.class).in(com.google.inject.Singleton.class);
    }

    public static void main(String[] args) {
        var module = Guice.createInjector(new AppModule());
        var app = module.getInstance(DirectoryWatcherApp.class);

        SwingUtilities.invokeLater(app::run);
    }
}
