package name.martingeisse.majai;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class ClassFileLoader {

	public InputStream open(String className) throws IOException {
		return new FileInputStream("out/production/classes/" + className.replace('.', '/') + ".class");
	}

}
