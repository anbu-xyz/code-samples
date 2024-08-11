package uk.anbu.poc.filewatcher;

import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConfigManager.class).in(com.google.inject.Singleton.class);
        bind(DirectoryWatcherApp.class).in(com.google.inject.Singleton.class);
    }
}
