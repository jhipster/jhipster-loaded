package io.github.jhipster.loaded.reloader.type;

/**
 * Defines the Entity reloader type
 *
 * A Entity class must support javax.persistence.Entity classes
 */
public final class EntityReloaderType implements ReloaderType {

    private static final String name = "entities";
    public static final EntityReloaderType instance = new EntityReloaderType();

    @Override
    public String getName() {
        return name;
    }

}
