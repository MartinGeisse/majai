package name.martingeisse.majai;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
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
	private final Set<String> compiledClasses;
	private final FieldAllocator staticFieldAllocator;

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
		this.compiledClasses = new HashSet<>();
		this.staticFieldAllocator = new FieldAllocator();
	}

	public void compile() {
		try (InputStream inputStream = getClass().getResourceAsStream("start.S")) {
			IOUtils.copy(inputStream, out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		javaLangObject = resolveClass("java.lang.Object");
		javaLangArray = resolveClass("java.lang.Array");
		compileClass(mainClassName); // TODO compile all resolved classes? e.g. static initializers
		emitStaticFields();
		out.flush();
	}

	public ClassInfo resolveClass(String name) {
		name = NameUtil.normalizeClassName(name);
		ClassInfo info = classInfos.get(name);
		if (info == null) {

			// build a ClassInfo object
			info = new ClassInfo();
			try (InputStream inputStream = classFileLoader.open(name)) {
				new ClassReader(inputStream).accept(info, ClassReader.SKIP_DEBUG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			info.initializeClassInfo();

			// resolve superclasses first, then build a field allocator
			if (info.superName == null) {
				info.fieldAllocator = new FieldAllocator();
			} else {
				ClassInfo superclassInfo = resolveClass(info.superName);
				info.fieldAllocator = new FieldAllocator(superclassInfo.fieldAllocator);
			}

			// allocate fields TODO store the locations!
			for (FieldNode field : info.fields) {
				FieldInfo fieldInfo = (FieldInfo)field;
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

			// publish the ClassInfo for this class
			classInfos.put(name, info);

		}
		return info;
	}

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
					new CodeTranslator(this, out, classInfo, methodNode).translate();
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
		out.println("	.fill " + staticFieldAllocator.getWordCount() + ", 4, 0");
		out.println();
	}

}
