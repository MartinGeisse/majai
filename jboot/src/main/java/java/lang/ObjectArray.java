package java.lang;

import name.martingeisse.majai.vm.VmObjectMetadata;

final class ObjectArray extends Array {

    /**
     * TODO: This is not sufficient. It would cause a String[][] to be an ObjectArray[] at run-time, but that is not
     * sufficient to handle run-time type checking (cast and instanceof). A sufficient implementation would be, for
     * example, to store ultimate element type and dimension.
     */
    private final VmObjectMetadata elementMetadata;

    ObjectArray(int length, VmObjectMetadata elementMetadata) {
        super(length);
        this.elementMetadata = elementMetadata;
    }

    VmObjectMetadata getElementMetadata() {
        return elementMetadata;
    }

}
