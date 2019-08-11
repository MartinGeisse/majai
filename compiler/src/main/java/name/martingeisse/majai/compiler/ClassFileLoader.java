package name.martingeisse.majai.compiler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class ClassFileLoader {

	public InputStream open(String className) throws IOException {
		try {
			return new FileInputStream("compiler/build/classes/java/main/" + NameUtil.normalizeClassName(className) + ".class");
		} catch (FileNotFoundException e) {
			return new FileInputStream("jboot/build/classes/java/main/" + NameUtil.normalizeClassName(className) + ".class");
		}
	}

}
