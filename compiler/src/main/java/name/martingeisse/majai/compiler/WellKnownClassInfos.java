package name.martingeisse.majai.compiler;

import name.martingeisse.majai.vm.VmObjectArrayMetadata;
import name.martingeisse.majai.vm.VmPrimitiveArrayMetadata;

/**
 * TODO We possibly need only the metadata contributors, so we might rename this class. But then, we might even remove
 * it altogether and just use the resolve methods.
 */
public final class WellKnownClassInfos {

    public ClassInfo javaLangObject;
    public ClassInfo javaLangString;

    public VmPrimitiveArrayMetadata booleanArray;
    public VmPrimitiveArrayMetadata byteArray;
    public VmPrimitiveArrayMetadata shortArray;
    public VmPrimitiveArrayMetadata charArray;
    public VmPrimitiveArrayMetadata intArray;
    public VmPrimitiveArrayMetadata floatArray;
    public VmPrimitiveArrayMetadata longArray;
    public VmPrimitiveArrayMetadata doubleArray;

    public VmObjectArrayMetadata javaLangObjectArray;

}
