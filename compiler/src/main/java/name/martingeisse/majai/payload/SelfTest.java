package name.martingeisse.majai.payload;

/**
 *
 */
public class SelfTest {

	public static native void out(int value);
	public static native void out(String value);

	public static void test() {

		// test static native call and small-integer pushing
		out(-2);
		out(-1);
		out(0);
		out(1);
		out(2);
		out(3);
		out(4);
		out(5);
		out(6);
		out(7);
		out(8);
		out(9);

		// test: bipush, sipush, ldc(int)
		out(100);
		out(1_000);
		out(1_000_000);

		// test returning an integer
		out(return99());

		// test: aconst_null, ldc(String), if_acmpeq, if_acmpne, ifnull, ifnonnull
		{
			String x = "abc";
			String y = null;
			String z = "def";
			String x2 = x;
			if (x == null) {
				out(1);
			}
			if (x != null) {
				out(2);
			}
			if (y == null) {
				out(3);
			}
			if (y != null) {
				out(4);
			}
			if (x == z) {
				out(10);
			}
			if (x != z) {
				out(11);
			}
			if (x == x2) {
				out(12);
			}
			if (x != x2) {
				out(13);
			}
		}

		// test constructors, including from sublasses
		{
			ConstructorSub constructorSub = new ConstructorSub(3, 5);
			out(constructorSub.x);
			out(constructorSub.y);

		}

		// test array creation, storing values, loading values, length
		{
			int[] a = new int[3];
			for (int i = 0; i < a.length; i++) {
				a[i] = 2 * i;
			}
			for (int i = 0; i < a.length; i++) {
				out(a[i]);
			}
		}

		// test checkcast
		{
			Object o = "foo";
			out(((String)o).length());
		}



		// TODO: test returning a String
		// out(returnWorld());
	}

	public static int return99() {
		return 99;
	}

	public static String returnWorld() {
		return "World";
	}

}
