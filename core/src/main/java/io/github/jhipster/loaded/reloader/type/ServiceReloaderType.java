package io.github.jhipster.loaded.reloader.type;

/**
 * Defines the Service reloader type
 *
 * A Entity class must support org.springframework.stereotype.Service classes
 */
public final class ServiceReloaderType implements ReloaderType {

    private static final String name = "services";
    public static final ServiceReloaderType instance = new ServiceReloaderType();

    @Override
    public String getName() {
        return name;
    }
}
