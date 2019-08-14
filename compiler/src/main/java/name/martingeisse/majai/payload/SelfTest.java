package name.martingeisse.majai.payload;

/**
 *
 */
public class SelfTest {

	public static native void out(int value);

	public static void test() {
//
//		// test static native call and small-integer pushing
//		out(-2);
//		out(-1);
//		out(0);
//		out(1);
//		out(2);
//		out(3);
//		out(4);
//		out(5);
//		out(6);
//		out(7);
//		out(8);
//		out(9);
//
//		// test bipush, sipoush, ldc(int)
//		out(100);
//		out(1_000);
//		out(1_000_000);
//
//		// test returning an integer
//		out(return99());

		// TODO breaks
		for (int i = -2; i < 10; i++) {

		}

	}

	public static int return99() {
		return 99;
	}

}
