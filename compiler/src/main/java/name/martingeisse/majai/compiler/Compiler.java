package name.martingeisse.majai.compiler;

import name.martingeisse.majai.vm.VmClass;
import name.martingeisse.majai.vm.VmInterface;
import name.martingeisse.majai.vm.VmObjectMetadata;
import name.martingeisse.majai.vm.VmObjectMetadataContributor;
import org.apache.commons.io.FilenameUtils;
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
	private boolean classResolutionClosed;

	private final Set<String> compiledClasses;
	private final FieldAllocator staticFieldAllocator;
	private final RuntimeObjects runtimeObjects;

	private ClassInfo javaLangObject;
	private ClassInfo javaLangArray;

	public Compiler(ClassFileLoader classFileLoader, String mainClassName, Writer out) {
		this(classFileLoader, mainClassName, new PrintWriter(out));
	}

	public Compiler(ClassFileLoader classFileLoader, String mainClassName, PrintWriter out) {
		this.classFileLoader = classFileLoader;
		this.mainClassName = mainClassName;
		this.out = out;
		this.classInfos = new HashMap<>();
		this.classResolutionClosed = false;
		this.compiledClasses = new HashSet<>();
		this.staticFieldAllocator = new FieldAllocator();
		this.runtimeObjects = new RuntimeObjects(this::resolveClass);
	}

	public void compile() {
		try (InputStream inputStream = getClass().getResourceAsStream("start.S")) {
			IOUtils.copy(inputStream, out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		javaLangObject = resolveClass("java.lang.Object");
		javaLangArray = resolveClass("java.lang.Array");
		resolveClass("java/lang/String");

		compileClass(mainClassName);
		compileAllResolvedClasses();
		classResolutionClosed = true;
		emitStaticFields();
		emitRuntimeObjectsAliasLabels(true);
		runtimeObjects.emit(out);
		emitRuntimeObjectsAliasLabels(false);
		out.println();
		out.println(".data");
		out.println("dynamicHeap:");
		out.flush();
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
		ClassInfo info = classInfos.get(name);
		if (info == null) {
			if (classResolutionClosed) {
				throw new IllegalStateException("class resolution has been closed already (trying to resolve " + name + ")");
			}

			// build a ClassInfo object
			info = new ClassInfo();
			try (InputStream inputStream = classFileLoader.open(name)) {
				new ClassReader(inputStream).accept(info, ClassReader.SKIP_DEBUG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// resolve superclasses first, then build a field allocator and a vtable allocator
			ClassInfo superclassInfo;
			if (info.superName == null) {
				superclassInfo = null;
				info.fieldAllocator = new FieldAllocator();
				info.vtableAllocator = new VtableAllocator();
			} else {
				superclassInfo = resolveClass(info.superName);
				info.fieldAllocator = new FieldAllocator(superclassInfo.fieldAllocator);
				info.vtableAllocator = new VtableAllocator(superclassInfo.vtableAllocator);
			}

			// allocate fields
			for (FieldNode field : info.fields) {
				FieldInfo fieldInfo = (FieldInfo) field;
				FieldAllocator thisFieldAllocator = (field.access & Opcodes.ACC_STATIC) == 0 ? info.fieldAllocator : staticFieldAllocator;
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
			info.fieldAllocator.seal();

			// allocate vtable entries
			for (MethodNode method : info.methods) {
				MethodInfo methodInfo = (MethodInfo) method;
				if ((method.access & Opcodes.ACC_STATIC) == 0 && !method.name.equals("<init>")) {
					methodInfo.vtableIndex = info.vtableAllocator.allocateMethod(methodInfo);
				}
			}
			info.vtableAllocator.seal();

			// create run-time metadata objects but do not fill them with data yet
			if ((info.access & Opcodes.ACC_INTERFACE) != 0) {
				info.runtimeMetadataContributor = new VmInterface(name);
			} else {
				VmClass parentClass = (superclassInfo == null ? null : (VmClass) superclassInfo.runtimeMetadataContributor);
				info.runtimeMetadataContributor = new VmClass(name, parentClass, info.vtableAllocator.buildVtable());
			}

			// publish the ClassInfo for this class
			classInfos.put(name, info);

		}
		return info;
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
		return javaLangArray.fieldAllocator.getWordCount() * 4;
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
