package io.github.jhipster.loaded.reloader.type;

/**
 * Defines the Repository reloader type
 *
 * A Entity class must support org.springframework.stereotype.Repository classes
 */
public final class RepositoryReloaderType implements ReloaderType {

    private static final String name = "repositories";
    public static final RepositoryReloaderType instance = new RepositoryReloaderType();

    @Override
    public String getName() {
        return name;
    }
}
