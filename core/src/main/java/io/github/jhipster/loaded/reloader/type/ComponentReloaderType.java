package io.github.jhipster.loaded.reloader.type;

/**
 * Defines the Component reloader type
 *
 * A Entity class must support org.springframework.stereotype.Component classes
 */
public final class ComponentReloaderType implements ReloaderType {

    private static final String name = "components";
    public static final ComponentReloaderType instance = new ComponentReloaderType();

    @Override
    public String getName() {
        return name;
    }
}
