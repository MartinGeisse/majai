package name.martingeisse.majai;

/**
 * Allocates the position of fields in an object, either from scratch (for java.lang.Object) or based on a parent
 * allocator (for subclasses). Byte-sized and halfword-sized "holes" are re-used. Word-sized "holes" are not re-used
 * since it is assumed that doublewords may be unaligned.
 *
 * An allocator can be "sealed" to protect against further changes. Only a sealed allocator may be used as the parent
 * of a child allocator since the intended use case of this class would cause a collision if fields in the parent
 * are allocated after creating a child allocator.
 */
public final class FieldAllocator {

    private int wordCount;
    private int halfwordOffset;
    private int byteOffset;
    private boolean sealed;

    public FieldAllocator() {
        wordCount = 0;
        halfwordOffset = -1;
        byteOffset = -1;
        sealed = false;
    }

    public FieldAllocator(FieldAllocator parent) {
        if (!parent.sealed) {
            throw new IllegalArgumentException("cannot use an unsealed field allocator as parent");
        }
        this.wordCount = parent.wordCount;
        this.halfwordOffset = parent.halfwordOffset;
        this.byteOffset = parent.byteOffset;
        this.sealed = false;
    }

    public boolean isSealed() {
        return sealed;
    }

    public int allocateByte() {
        checkNotSealed();
        if (byteOffset < 0) {
            int offset = allocateHalfword();
            byteOffset = offset + 1;
            return offset;
        } else {
            int offset = byteOffset;
            byteOffset = -1;
            return offset;
        }
    }

    public int allocateHalfword() {
        checkNotSealed();
        if (halfwordOffset < 0) {
            int offset = allocateWord();
            halfwordOffset = offset + 2;
            return offset;
        } else {
            int offset = halfwordOffset;
            halfwordOffset = -1;
            return offset;
        }
    }

    public int allocateWord() {
        checkNotSealed();
        int offset = wordCount;
        wordCount++;
        return offset;
    }

    public int allocateDoubleword() {
        checkNotSealed();
        int offset = wordCount;
        wordCount += 2;
        return offset;
    }

    public void seal() {
        checkNotSealed();
        sealed = true;
    }

    private void checkNotSealed() {
        if (sealed) {
            throw new IllegalStateException("this allocator has been sealed");
        }
    }

}
