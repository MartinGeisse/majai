package java.lang;

import name.martingeisse.majai.vm.VmObjectMetadataContributor;

/**
 * An array whose run-time class is an object array class (possibly an array-of-interface class) but NOT an
 * array-of-arrays class. Those are special because the VmObjectMetadata is not sufficient to fully describe the
 * Java-visible run-time array class.
 */
final class SingleDimensionalObjectArray extends Array {

    private final VmObjectMetadataContributor elementMetadataContributor;

    public SingleDimensionalObjectArray(int length, VmObjectMetadataContributor elementMetadataContributor) {
        super(length);
        this.elementMetadataContributor = elementMetadataContributor;
    }

    public VmObjectMetadataContributor getElementMetadataContributor() {
        return elementMetadataContributor;
    }

}
