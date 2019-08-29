package name.martingeisse.majai.compiler.descriptor;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 *
 */
public final class ParsedFieldDescriptor {

	static final Pattern PATTERN = Pattern.compile("\\[*([ZBSCIFJD]|L.*;)");

	private final int dimension;
	private final char leafBaseType;
	private final String leafClass;

	public ParsedFieldDescriptor(String originalDescriptor) {
		if (!PATTERN.matcher(originalDescriptor).matches()) {
			throw new IllegalArgumentException("invalid descriptor: " + originalDescriptor);
		}

		String remainder = originalDescriptor;
		int dimension = 0;
		while (remainder.startsWith("[")) {
			dimension++;
			remainder = remainder.substring(1);
		}
		this.dimension = dimension;

		this.leafBaseType = remainder.charAt(0);
		this.leafClass = leafBaseType == 'L' ? remainder.substring(1, remainder.length() - 1) : null;
	}

	public int getDimension() {
		return dimension;
	}

	public char getLeafBaseType() {
		return leafBaseType;
	}

	public String getLeafClass() {
		return leafClass;
	}

	public boolean isLeafReferenceType() {
		return leafClass != null;
	}

	public boolean isArray() {
		return dimension > 0;
	}

	public boolean isReferenceType() {
		return isLeafReferenceType() ||isArray();
	}

	public int getBytes() {
		if (isReferenceType()) {
			return 4;
		} else {
			return getLeafBytes();
		}
	}

	public int getWords() {
		if (isReferenceType()) {
			return 1;
		} else {
			return (leafBaseType == 'J' || leafBaseType == 'D') ? 2 : 1;
		}
	}

	public boolean isSigned() {
		if (isReferenceType()) {
			return false;
		} else {
			return isLeafSigned();
		}
	}

	public int getLeafBytes() {
		switch (leafBaseType) {

			case 'Z':
			case 'B':
				return 1;

			case 'S':
			case 'C':
				return 2;

			case 'I':
			case 'F':
			case 'L':
				return 4;

			case 'J':
			case 'D':
				return 8;

			default:
				throw new RuntimeException();

		}
	}

	public boolean isLeafSigned() {
		return (leafBaseType == 'B' || leafBaseType == 'S' || leafBaseType =='I' || leafBaseType == 'D');
	}

	public String getLoadInstruction() {
		switch (getBytes()) {

			case 1:
				return isSigned() ? "lb" : "lbu";

			case 2:
				return isSigned() ? "lh" : "lhu";

			case 4:
				return "lw";

			default:
				throw new UnsupportedOperationException("no load instruction for " + this);

		}
	}

	public String getStoreInstruction() {
		switch (getBytes()) {

			case 1:
				return "sb";

			case 2:
				return "sh";

			case 4:
				return "sw";

			default:
				throw new UnsupportedOperationException("no store instruction for " + this);

		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof ParsedFieldDescriptor) {
			ParsedFieldDescriptor other = (ParsedFieldDescriptor)o;
			return dimension == other.dimension && leafBaseType == other.leafBaseType && Objects.equals(leafClass, other.leafClass);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(dimension).append(leafBaseType).append(leafClass).toHashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < dimension; i++) {
			builder.append('[');
		}
		builder.append(leafBaseType);
		if (leafClass != null) {
			builder.append(leafClass).append(';');
		}
		return builder.toString();
	}

}
