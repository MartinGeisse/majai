package name.martingeisse.majai.vm;

/**
 * Meta-data for primitive arrays.
 */
public final class VmPrimitiveArrayMetadata extends VmObjectMetadata {

    public VmPrimitiveArrayMetadata(String name, VmClass parentClass, Object[] vtable) {
        super(name, parentClass, vtable);
    }

}
