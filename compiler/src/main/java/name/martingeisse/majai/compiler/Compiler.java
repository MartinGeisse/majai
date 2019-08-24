package name.martingeisse.majai.compiler;

import name.martingeisse.majai.vm.*;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Compiler implements CodeTranslator.Context {

	private final ClassFileLoader classFileLoader;
	private final String mainClassName;
	private final PrintWriter out;

	private final Map<String, ClassInfo> classInfos;
	private final Map<String, VmObjectMetadataContributor> metadataContributors;
	private boolean resolutionClosed;

	private final Set<String> compiledClasses;
	private final FieldAllocator staticFieldAllocator;
	private final RuntimeObjects runtimeObjects;

	private WellKnownClassInfos wellKnownClassInfos;

	public Compiler(ClassFileLoader classFileLoader, String mainClassName, Writer out) {
		this(classFileLoader, mainClassName, new PrintWriter(out));
	}

	public Compiler(ClassFileLoader classFileLoader, String mainClassName, PrintWriter out) {
		this.classFileLoader = classFileLoader;
		this.mainClassName = mainClassName;
		this.out = out;
		this.classInfos = new HashMap<>();
		this.resolutionClosed = false;
		this.metadataContributors = new HashMap<>();
		this.compiledClasses = new HashSet<>();
		this.staticFieldAllocator = new FieldAllocator();
		this.runtimeObjects = new RuntimeObjects(new RuntimeObjects.Context() {

			@Override
			public ClassInfo resolveClass(String name) {
				return Compiler.this.resolveClass(name);
			}

			@Override
			public WellKnownClassInfos getWellKnownClassInfos() {
				return Compiler.this.getWellKnownClassInfos();
			}

		});
	}

	public void compile() {
		try (InputStream inputStream = getClass().getResourceAsStream("start.S")) {
			IOUtils.copy(inputStream, out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		wellKnownClassInfos = new WellKnownClassInfos();
		wellKnownClassInfos.javaLangObject = resolveClass("java.lang.Object");
		wellKnownClassInfos.javaLangString = resolveClass("java.lang.String");
		//
		wellKnownClassInfos.booleanArray = buildPrimitiveArrayMetadata("[Z");
		wellKnownClassInfos.byteArray = buildPrimitiveArrayMetadata("[B");
		wellKnownClassInfos.shortArray = buildPrimitiveArrayMetadata("[S");
		wellKnownClassInfos.charArray = buildPrimitiveArrayMetadata("[C");
		wellKnownClassInfos.intArray = buildPrimitiveArrayMetadata("[I");
		wellKnownClassInfos.floatArray = buildPrimitiveArrayMetadata("[F");
		wellKnownClassInfos.longArray = buildPrimitiveArrayMetadata("[J");
		wellKnownClassInfos.doubleArray = buildPrimitiveArrayMetadata("[D");
		//
		wellKnownClassInfos.javaLangObjectArray = (VmObjectArrayMetadata)resolveObjectMetadataContributor("[Ljava.lang.Object;");

		compileClass(mainClassName);
		compileAllResolvedClasses();
		resolutionClosed = true;
		emitStaticFields();
		emitRuntimeObjectsAliasLabels(true);
		runtimeObjects.emit(out);
		emitRuntimeObjectsAliasLabels(false);
		out.println();
		out.println(".data");
		out.println("dynamicHeap:");
		out.flush();
	}

	private VmPrimitiveArrayMetadata buildPrimitiveArrayMetadata(String name) {
		VmClass objectClass = (VmClass)wellKnownClassInfos.javaLangObject.runtimeMetadataContributor;
		VmPrimitiveArrayMetadata metadata = new VmPrimitiveArrayMetadata(name, objectClass, objectClass.getVtable());
		metadataContributors.put(name, metadata);
		return metadata;
	}

	/*
		Note: We cannot allow cyclic resolution since resolution must be fully finished before returning. Specifically,
		it is not possible to break a cycle by returning a not-fully-resolved class here, since a case may happen where
		this cycle-breaking strategy causes a superclass to be returned not-fully-resolved.
		Therefore, all recursive resolution must be inherently non-cyclic (i.e. only superclasses and interfaces).
		-
		Fully resolved means that the field locations and vtable indices are allocated and that the objects for
		run-time metadata have been created. However, these objects have not been filled with data yet, since that
		data is generated during compilation. Referring to these objects is possible, though.
	 */
	public ClassInfo resolveClass(String name) {
		if (name.startsWith("[")) {
			throw new IllegalArgumentException("cannot use resolveClass() for array classes");
		}
		name = NameUtil.normalizeClassName(name);
		ClassInfo classInfo = classInfos.get(name);
		if (classInfo == null) {
			if (resolutionClosed) {
				throw new IllegalStateException("class resolution has been closed already (trying to resolve " + name + ")");
			}

			// build a ClassInfo object
			classInfo = new ClassInfo();
			try (InputStream inputStream = classFileLoader.open(name)) {
				new ClassReader(inputStream).accept(classInfo, ClassReader.SKIP_DEBUG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// resolve superclasses first, then build a field allocator and a vtable allocator
			ClassInfo superclassInfo;
			if (classInfo.superName == null) {
				superclassInfo = null;
				classInfo.fieldAllocator = new FieldAllocator();
				classInfo.vtableAllocator = new VtableAllocator();
			} else {
				superclassInfo = resolveClass(classInfo.superName);
				classInfo.fieldAllocator = new FieldAllocator(superclassInfo.fieldAllocator);
				classInfo.vtableAllocator = new VtableAllocator(superclassInfo.vtableAllocator);
			}

			// allocate fields
			for (FieldNode field : classInfo.fields) {
				FieldInfo fieldInfo = (FieldInfo) field;
				FieldAllocator thisFieldAllocator = (field.access & Opcodes.ACC_STATIC) == 0 ? classInfo.fieldAllocator : staticFieldAllocator;
				switch (field.desc) {

					case "B":
						fieldInfo.storageOffset = thisFieldAllocator.allocateByte();
						break;

					case "S":
					case "C":
						fieldInfo.storageOffset = thisFieldAllocator.allocateHalfword();
						break;

					case "J":
					case "D":
						fieldInfo.storageOffset = thisFieldAllocator.allocateDoubleword();
						break;

					default:
						fieldInfo.storageOffset = thisFieldAllocator.allocateWord();
						break;

				}
			}
			classInfo.fieldAllocator.seal();

			// allocate vtable entries
			for (MethodNode method : classInfo.methods) {
				MethodInfo methodInfo = (MethodInfo) method;
				if ((method.access & Opcodes.ACC_STATIC) == 0 && !method.name.equals("<init>")) {
					methodInfo.vtableIndex = classInfo.vtableAllocator.allocateMethod(methodInfo);
				}
			}
			classInfo.vtableAllocator.seal();

			// create run-time metadata objects but do not fill them with data yet
			if ((classInfo.access & Opcodes.ACC_INTERFACE) != 0) {
				classInfo.runtimeMetadataContributor = new VmInterface(name);
			} else {
				VmClass parentClass = (superclassInfo == null ? null : (VmClass) superclassInfo.runtimeMetadataContributor);
				classInfo.runtimeMetadataContributor = new VmClass(name, parentClass, classInfo.vtableAllocator.buildVtable());
			}

			classInfos.put(name, classInfo);
		}
		return classInfo;
	}

	private VmObjectMetadataContributor resolveObjectMetadataContributor(String name) {
		VmObjectMetadataContributor result = metadataContributors.get(name);
		if (result == null) {
			if (resolutionClosed) {
				throw new IllegalStateException("class resolution has been closed already (trying to resolve " + name + ")");
			} else if (name.startsWith("[")) {
				// we can't go here for primitive elements because primitive arrays are pre-built and so are
				// already available in the metadataContributors.
				VmClass objectClass = (VmClass)wellKnownClassInfos.javaLangObject.runtimeMetadataContributor;
				String elementSpec = name.substring(1);
				VmObjectMetadataContributor elementType;
				if (elementSpec.startsWith("[")) {
					elementType = resolveObjectMetadataContributor(elementSpec);
				} else if (elementSpec.startsWith("L") && elementSpec.endsWith(";")) {
					elementType = resolveObjectMetadataContributor(elementSpec.substring(1, elementSpec.length() - 1));
				} else {
					throw new RuntimeException("invalid array class name: " + name);
				}
				result = new VmObjectArrayMetadata(name, objectClass, objectClass.getVtable(), elementType);
			} else {
				result = resolveClass(name).runtimeMetadataContributor;
			}
			metadataContributors.put(name, result);
		}
		return result;
	}

	@Override
	public WellKnownClassInfos getWellKnownClassInfos() {
		return wellKnownClassInfos;
	}

	/**
	 * Compiles all currently resolved classes as well as, recursively, classes that get resolved while compiling.
	 */
	private void compileAllResolvedClasses() {
		while (true) {
			Set<String> batch = new HashSet<>(classInfos.keySet());
			batch.removeAll(compiledClasses);
			if (batch.isEmpty()) {
				break;
			}
			for (String className : batch) {
				compileClass(className);
			}
		}
	}

	/**
	 * Compiles a single class (does nothing if that class is already compiled).
	 */
	private void compileClass(String name) {
		try {
			name = NameUtil.normalizeClassName(name);
			if (compiledClasses.add(name)) {
				ClassInfo classInfo = resolveClass(name);

				// compile dependencies
				if (classInfo.superName != null) {
					compileClass(classInfo.superName);
				}

				// compile the class itself
				out.println("//");
				out.println("// class " + NameUtil.denormalizeClassName(name));
				out.println("//");
				out.println("");
				for (MethodNode methodNode : classInfo.methods) {
					new CodeTranslator(this, out, (MethodInfo) methodNode).translate();
				}

				out.println();
			}
		} catch (Exception e) {
			throw new RuntimeException("error compiling class " + name, e);
		}
	}

	@Override
	public int getArrayHeaderSize() {
		return (wellKnownClassInfos.javaLangObject.fieldAllocator.getWordCount() + 1) * 4;
	}

	private void emitStaticFields() {
		staticFieldAllocator.seal();
		out.println("//");
		out.println("// static fields");
		out.println("//");
		out.println("");
		out.println(".data");
		out.println("staticFields:");
		out.println("\t.fill " + staticFieldAllocator.getWordCount() + ", 4, 0");
		out.println();
	}

	private void emitRuntimeObjectsAliasLabels(boolean dryRun) {
		if (!dryRun) {
			out.println("//");
			out.println("// alias labels for runtime objects");
			out.println("//");
			out.println("");
			out.println(".data");
		}
		for (ClassInfo classInfo : classInfos.values()) {
			VmObjectMetadataContributor contributor = classInfo.runtimeMetadataContributor;
			if (contributor instanceof VmObjectMetadata) {
				String generatedLabel = getRuntimeObjectLabel(((VmObjectMetadata) contributor).getVtable());
				if (!dryRun) {
					out.println(".set " + NameUtil.mangleClassName(classInfo) + "_vtable, " + generatedLabel);
				}
			}
		}
		if (!dryRun) {
			out.println();
		}
	}

	@Override
	public String getRuntimeObjectLabel(Object o) {
		return runtimeObjects.getLabel(o);
	}

}
