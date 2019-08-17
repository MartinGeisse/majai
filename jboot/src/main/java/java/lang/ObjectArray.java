package java.lang;

import name.martingeisse.majai.vm.VmObjectMetadata;

final class ObjectArray extends Array {

    private final VmObjectMetadata elementMetadata;

    ObjectArray(int length, VmObjectMetadata elementMetadata) {
        super(length);
        this.elementMetadata = elementMetadata;
    }

    VmObjectMetadata getElementMetadata() {
        return elementMetadata;
    }

}
