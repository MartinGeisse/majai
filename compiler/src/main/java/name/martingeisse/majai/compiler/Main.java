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
		try {
//			try (FileOutputStream fileOutputStream = new FileOutputStream("all.S")) {
//				try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
//					Compiler compiler = new Compiler(new ClassFileLoader(), "name/martingeisse/majai/payload/Test", outputStreamWriter);
//					compiler.compile();
//				}
//			}

			try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
				new Compiler(new ClassFileLoader(), "name/martingeisse/majai/payload/Test", outputStreamWriter).compile();
			}

			File outputFolder = new File("out/majai");
			outputFolder.mkdirs();
			File outputFile = new File(outputFolder, "test.S");
			try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
				try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
					new Compiler(new ClassFileLoader(), "name/martingeisse/majai/payload/Test", outputStreamWriter).compile();
				}
			}

		} catch (Throwable t) {
			// unmix System.out and System.err on the IntelliJ console
			Thread.sleep(1000);
			throw t;
		}
	}

}
