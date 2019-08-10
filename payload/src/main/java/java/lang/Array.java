package java.lang;

/**
 * "Real" arrays use this class as a template for their header field layout and also store it as the internal VM class.
 */
final class Array {

    public final int length;

    public Array(int length) {
        this.length = length;
    }

}
