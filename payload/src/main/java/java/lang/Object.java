package java.lang;

public class Object {

    private final Object[] vtable;

    public Object() {
        // The vtable is actually initialized before the constructor gets called. We just fool javac here to allow
        // it to be final without getting a compiler error.
        this.vtable = getVTable();
    }

    Object[] getVTable() {
        return vtable;
    }

}
