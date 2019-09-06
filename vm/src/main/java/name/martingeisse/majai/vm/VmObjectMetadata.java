package name.martingeisse.majai.vm;

/**
 * Base class for VmClass and specialized variants of it (e.g. for arrays).
 *
 * An object metadata contains the full description of how an object behaves. The typical case is a class. Unlike
 * java.lang.Class, object metadata is decoupled from static fields and exists only once in the VM multiverse.
 * To obtain a java.lang.Class, the metadata provides a method to get the Class object for the caller's universe.
 *
 * Specialized implementations exist for class-like cases that cannot be described as a normal class, such as arrays.
 *
 * An interface description ({@link VmInterface)} is *not* an object metadata since it does not fully describe the
 * object's behavior. Instead, the object's metadata refers to interface descriptions.
 */
public abstract class VmObjectMetadata extends VmObjectMetadataContributor {

    private final VmClass parentClass;
    private final Object[] vtable;

    public VmObjectMetadata(String name, VmClass parentClass, Object[] vtable) {
        super(name);
        this.parentClass = parentClass;
        this.vtable = vtable;
    }

    public VmClass getParentClass() {
        return parentClass;
    }

    public Object[] getVtable() {
        return vtable;
    }

    public static boolean objectIsInstanceOf(Object object, VmObjectMetadata metadata) {
        return false;
    }

    public static Object castReference(Object object, VmObjectMetadata metadata) {
        return null;
    }




    public static boolean isEqualOrSubclassOf(VmObjectMetadata x, VmObjectMetadata y) {
        if (x == y) {
            return true;
        }
        VmClass parent = x.getParentClass();
        if (parent == null) {
            return false;
        }
        return isEqualOrSubclassOf(parent, y);
    }

}
