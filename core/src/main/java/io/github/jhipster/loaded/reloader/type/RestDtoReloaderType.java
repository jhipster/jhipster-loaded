package io.github.jhipster.loaded.reloader.type;

/**
 * Defines the REST DTO reloader type
 *
 * A REST DTO class must be located inside the package defined
 * by the configuration property named hotReload.package.restdto
 */
public final class RestDtoReloaderType implements ReloaderType {

    private static final String name = "dtos";
    public static final RestDtoReloaderType instance = new RestDtoReloaderType();

    @Override
    public String getName() {
        return name;
    }
}
