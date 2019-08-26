package name.martingeisse.majai.vm;

public final class VmField {

    private VmObjectMetadata objectMetadata;

    public VmObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    void bind(VmObjectMetadata objectMetadata) {
        if (objectMetadata == null) {
            // TODO cannot compile this yet
            // throw new IllegalArgumentException("cannot bind to null");
        }
        if (this.objectMetadata != null) {
            // TODO cannot compile this yet
            // throw new IllegalStateException("already bound");
        }
        this.objectMetadata = objectMetadata;
    }

}
