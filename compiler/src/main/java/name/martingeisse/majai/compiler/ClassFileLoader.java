package name.martingeisse.majai.compiler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class ClassFileLoader {

	private static final String[] pathPrefixes = {
		"jboot/build/classes/java/main/",
		"jboot/out/production/classes/",
		"compiler/build/classes/java/main/",
		"compiler/out/production/classes/",
		"vm/build/classes/java/main/",
		"vm/out/production/classes/",
	};

	public InputStream open(String className) throws IOException {
		for (String prefix : pathPrefixes) {
			try {
				return new FileInputStream(prefix + NameUtil.normalizeClassName(className) + ".class");
			} catch (FileNotFoundException e) {
				// continue with next prefix
			}
		}
		throw new RuntimeException("could not find class file for class " + className);
	}

}
