package name.martingeisse.majai.vm;

/**
 * Meta-data for object arrays.
 */
public final class VmObjectArrayMetadata extends VmObjectMetadata {

    private final VmObjectMetadataContributor elementType;

    public VmObjectArrayMetadata(String name, VmClass parentClass, Object[] vtable, VmObjectMetadataContributor elementType) {
        super(name, parentClass, vtable);
        this.elementType = elementType;
    }

    public VmObjectMetadataContributor getElementType() {
        return elementType;
    }

}
