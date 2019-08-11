package name.martingeisse.majai.vm;

/**
 * Metadata for regular classes.
 */
public final class VmClass extends VmObjectMetadata {

    public VmClass(String name, VmClass parentClass, Object[] vtable) {
        super(name, parentClass, vtable);
    }

}
