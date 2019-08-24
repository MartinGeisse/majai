package name.martingeisse.majai.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {
		System.out.println((new String[0]).getClass());
		File outputFolder = new File("out/majai");
		outputFolder.mkdirs();
		File outputFile = new File(outputFolder, "selftest.S");
		try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
			try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
				new Compiler(new ClassFileLoader(), "name/martingeisse/majai/payload/SelfTest", outputStreamWriter).compile();
			}
		}
	}

}
