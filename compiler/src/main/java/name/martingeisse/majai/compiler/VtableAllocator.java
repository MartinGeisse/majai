package name.martingeisse.majai.compiler;

import name.martingeisse.majai.compiler.runtime.LabelReference;
import name.martingeisse.majai.vm.VmObjectMetadata;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

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

	private final List<String> entryKeys;
	private final List<MethodInfo> entryMethods;
	private boolean sealed;

	public VtableAllocator() {
		entryKeys = new ArrayList<>();
		entryMethods = new ArrayList<>();
		sealed = false;
		for (int i = 0; i < LayoutConstants.VTABLE_FIXED_ENTRY_COUNT; i++) {
			entryKeys.add("");
			entryMethods.add(null);
		}
	}

	public VtableAllocator(VtableAllocator parent) {
		if (!parent.sealed) {
			throw new IllegalArgumentException("cannot use an unsealed vtable allocator as parent");
		}
		entryKeys = new ArrayList<>(parent.entryKeys);
		entryMethods = new ArrayList<>(parent.entryMethods);
		this.sealed = false;
	}

	public boolean isSealed() {
		return sealed;
	}

	public int allocateMethod(MethodInfo method) {
		checkNotSealed();
		String key = getKey(method);
		int index = entryKeys.indexOf(key);
		if (index < 0) {
			index = entryKeys.size();
			entryKeys.add(key);
			entryMethods.add(method);
		} else {
			entryMethods.set(index, method);
		}
		return index;
	}

	private static String getKey(MethodNode method) {
		int index = method.desc.indexOf(')');
		if (index < 0) {
			throw new RuntimeException("invalid method descriptor: " + method.desc);
		}
		return method.name + method.desc.substring(0, index + 1);
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

	public Object[] buildVtable(VmObjectMetadata parentMetadata) {
		if (!sealed) {
			throw new IllegalArgumentException("cannot use an unsealed vtable allocator to build the vtable");
		}
		Object[] vtable = new Object[entryMethods.size()];
		vtable[LayoutConstants.VTABLE_METADATA_INDEX] = parentMetadata;
		for (int i = 1; i < entryMethods.size(); i++) {
			vtable[i] = new LabelReference(NameUtil.mangleMethodName(entryMethods.get(i)));
		}
		return vtable;
	}

	public MethodInfo findByName(String name) {
		if (!sealed) {
			throw new IllegalArgumentException("cannot use an unsealed vtable allocator to find a method by name");
		}
		for (MethodInfo methodInfo : entryMethods) {
			if (methodInfo != null && methodInfo.name.equals(name)) {
				return methodInfo;
			}
		}
		throw new RuntimeException("method not found: " + name);
	}

}
