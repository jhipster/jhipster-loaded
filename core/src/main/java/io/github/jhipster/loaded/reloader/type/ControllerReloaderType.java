package io.github.jhipster.loaded.reloader.type;

/**
 * Defines the Controller reloader type
 *
 * A Entity class must support org.springframework.stereotype.Controller classes
 * or org.springframework.web.bind.annotation.RestController classes
 */
public final class ControllerReloaderType implements ReloaderType {

    private static final String name = "controllers";
    public static final ControllerReloaderType instance = new ControllerReloaderType();

    @Override
    public String getName() {
        return name;
    }
}
