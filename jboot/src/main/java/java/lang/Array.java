package java.lang;

/**
 * "Real" arrays use this class as a template for their header field layout and also store it as the internal VM class.
 */
abstract class Array {

    final int length;

    Array(int length) {
        this.length = length;
    }

}
