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

}
