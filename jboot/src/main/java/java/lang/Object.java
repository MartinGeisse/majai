package java.lang;

import name.martingeisse.majai.vm.VmClass;

public class Object {

    private Object[] vtable;

    public Object() {
    }

    native final VmClass getVmClass();

    final boolean isInstanceOf(VmClass c) {
        return getVmClass().isEqualOrSubclassOf(c);
    }

}
