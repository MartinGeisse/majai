
compiler: contains the java-to-native compiler (cross-compiling and
native-compiling).

jboot: contains the boot classpath classes, e.g. java.lang.Object.

No formal dependency exists in the build.gradle files, and the compiler should be able to run
with a real JDK boot classpath (cross-compilation from Linux), the jboot classes (native
compilation) or a different version of the jboot classes. The interface for this compatibility
are the Java specs.

Concept: The classes used in the native system are obviously the same as those generated during
cross-compilation, but they are also the same as the host classes during cross-compilation.
That is, the "cross-compiler" must always compile its own system, but while doing so can *run*
in a different system. (In the future, during cross-compilation, module isolation, e.g. like OSGI,
should be used to separate the compiler and its own system from the system it is running in).

What *is* different during cross-compilation is the boot classpath, since it must be the
boot classpath of the system the compiler is running in. The Java specs fortunately keep those
classes pretty stable. Where they aren't stable, the object serializer during cross-compilation
uses objects with the fields from the host boot classpath and generates objects with the fields
from the target boot classpath, knowing the layout of the target boot classpath. That's not too
hard, except when e.g. a new Java version adds a new field that is important to us. In such
cases, a workaround must be used to deliver the additional field values to the object serializer.

