package name.martingeisse.majai;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
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
public class Compiler {

	private final ClassFileLoader classFileLoader;
	private final String mainClassName;
	private final PrintWriter out;
	private final Map<String, ClassInfo> classInfos;
	private final Set<String> compiledClasses;

	public Compiler(ClassFileLoader classFileLoader, String mainClassName, Writer out) {
		this(classFileLoader, mainClassName, new PrintWriter(out));
	}

	public Compiler(ClassFileLoader classFileLoader, String mainClassName, PrintWriter out) {
		this.classFileLoader = classFileLoader;
		this.mainClassName = mainClassName;
		this.out = out;
		this.classInfos = new HashMap<>();
		this.compiledClasses = new HashSet<>();
	}

	public void compile() {
		try (InputStream inputStream = getClass().getResourceAsStream("start.S")) {
			IOUtils.copy(inputStream, out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		compileClass(mainClassName);
		out.flush();
	}

	private ClassInfo resolveClass(String name) {
		name = normalizeClassName(name);
		ClassInfo info = classInfos.get(name);
		if (info == null) {
			info = new ClassInfo();
			try (InputStream inputStream = classFileLoader.open(name)) {
				new ClassReader(inputStream).accept(info, ClassReader.SKIP_DEBUG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			info.initializeClassInfo();
			classInfos.put(name, info);
		}
		return info;
	}

	private void compileClass(String name) {
		name = normalizeClassName(name);
		if (compiledClasses.add(name)) {
			ClassInfo classInfo = resolveClass(name);

			// compile dependencies
			// compileClass(classInfo.superName);

			// compile the class itself
			out.println("//");
			out.println("// class " + denormalizeClassName(name));
			out.println("//");
			out.println("");
			for (MethodNode methodNode : classInfo.methods) {
				compileMethod(classInfo, methodNode);
			}

			out.println();
		}
	}

	private void compileMethod(ClassInfo classInfo, MethodNode methodNode) {
		if (methodNode.name.equals("<init>")) {
			return;
		}
		if (methodNode.name.equals("<clinit>")) {
			throw new UnsupportedOperationException("static initializer not yet supported");
		}
		if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
			throw new UnsupportedOperationException("only static methods allowed: " + methodNode.name);
		}
		out.println(mangleMethodName(classInfo, methodNode) + ':');
		out.println("");
	}

	private static String normalizeClassName(String name) {
		return name.replace('.', '/');
	}

	private static String denormalizeClassName(String name) {
		return name.replace('/', '.');
	}

	private static String mangleClassName(ClassInfo classInfo) {
		return normalizeClassName(classInfo.name).replace('/', '_');
	}

	private static String mangleMethodName(ClassInfo classInfo, MethodNode methodNode) {
		return mangleClassName(classInfo) + '_' + methodNode.name + '_' + methodNode.desc.replace("(", "").replace(')', '_').replace(';', '_');
	}

}
