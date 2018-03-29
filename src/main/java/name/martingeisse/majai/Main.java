package name.martingeisse.majai;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.FileInputStream;
import java.util.Arrays;

/**
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {
		ClassNode classNode = new ClassNode(Opcodes.ASM6);
		try (FileInputStream fileInputStream = new FileInputStream("out/production/classes/name/martingeisse/majai/Test.class")) {
			new ClassReader(fileInputStream).accept(classNode, ClassReader.SKIP_DEBUG);
		}
		Compiler compiler = new Compiler(Arrays.asList(classNode));
		byte[] program = compiler.compile();
		VirtualMachine virtualMachine = new VirtualMachine(program);
		virtualMachine.run();
	}

}
