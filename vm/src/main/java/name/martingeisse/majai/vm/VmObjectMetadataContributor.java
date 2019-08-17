package name.martingeisse.majai.vm;

/**
 * Base class for {@link VmObjectMetadata} and {@link VmInterface}.
 */
public abstract class VmObjectMetadataContributor {

    private final String name;
    private final String simpleName;

    public VmObjectMetadataContributor(String name) {
        this.name = name;
        this.simpleName = deriveSimpleName(name);
    }

    private static String deriveSimpleName(String name) {
        int index = name.lastIndexOf('/');
        return (index == -1 ? name : name.substring(index + 1));
    }

    public String getName() {
        return name;
    }

    public String getSimpleName() {
        return simpleName;
    }

}
