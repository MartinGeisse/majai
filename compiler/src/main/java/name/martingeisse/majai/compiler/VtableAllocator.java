package name.martingeisse.majai.compiler;

/**
 * Allocates the position of methods in a vtable, either from scratch (for java.lang.Object) or based on a parent
 * allocator (for subclasses). Overriding methods re-use the vtable index of the overridden method. Other methods
 * are assigned a new index.
 * <p>
 * An allocator can be "sealed" to protect against further changes. Only a sealed allocator may be used as the parent
 * of a child allocator since the intended use case of this class would break method overriding if an overridden
 * method is added to the parent after creating a child allocator.
 */
public final class VtableAllocator {

	private boolean sealed;

	public VtableAllocator() {
		sealed = false;
	}

	public VtableAllocator(VtableAllocator parent) {
		if (!parent.sealed) {
			throw new IllegalArgumentException("cannot use an unsealed vtable allocator as parent");
		}
		this.sealed = false;
	}

	public boolean isSealed() {
		return sealed;
	}

	public int allocateMethod() {
		checkNotSealed();
		return 0;
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
