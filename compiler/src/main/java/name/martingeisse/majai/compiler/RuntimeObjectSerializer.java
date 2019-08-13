package name.martingeisse.majai.compiler;

import java.io.PrintWriter;

public abstract class RuntimeObjectSerializer {

    private final PrintWriter out;

    public RuntimeObjectSerializer(PrintWriter out) {
        this.out = out;
    }

    protected abstract String getLabel(Object o);

    public void serialize(Object o) {
        if (o == null) {
            out.println("   .word 0");
        } else if (o instanceof byte[]) {
            byte[] array = (byte[])o;
            out.println("   .word 0"); // TODO byte[] vtable
            out.println("   .word " + array.length);
            out.print(" .byte ");
            boolean first = true;
            for (byte b : array) {
                if (first) {
                    first = false;
                } else {
                    out.print(", ");
                }
                out.print(b & 0xff);
            }
            out.println();
        } else {
            throw new RuntimeException("cannot serialize for runtime: " + o);
        }
    }

}
