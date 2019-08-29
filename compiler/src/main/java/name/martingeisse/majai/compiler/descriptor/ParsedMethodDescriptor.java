package name.martingeisse.majai.compiler.descriptor;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ParsedMethodDescriptor {

	static final Pattern PATTERN = Pattern.compile("\\((" + ParsedFieldDescriptor.PATTERN + ")*\\)(V|(" + ParsedFieldDescriptor.PATTERN + "))");

	private final ImmutableList<ParsedFieldDescriptor> parameterTypes;
	private final ParsedFieldDescriptor returnType;

	public ParsedMethodDescriptor(String descriptor) {
		if (!PATTERN.matcher(descriptor).matches()) {
			throw new IllegalArgumentException("invalid descriptor: " + descriptor);
		}
		int closingParenthesisIndex = descriptor.indexOf(')');
		List<ParsedFieldDescriptor> parameterTypes = new ArrayList<>();
		int position = 1;
		while (position < closingParenthesisIndex) {
			position = parseFieldType(descriptor, position, s -> parameterTypes.add(new ParsedFieldDescriptor(s)));
		}
		this.parameterTypes = ImmutableList.copyOf(parameterTypes);
		position = closingParenthesisIndex + 1;
		if (descriptor.charAt(position) == 'V') {
			this.returnType = null;
		} else {
			this.returnType = new ParsedFieldDescriptor(descriptor.substring(position));
		}
	}

	private static int parseFieldType(String s, int startPosition, Consumer<String> consumer) {
		int lastPosition = startPosition;
		while (s.charAt(lastPosition) == '[') {
			lastPosition++;
		}
		if (s.charAt(lastPosition) == 'L') {
			while (s.charAt(lastPosition) != ';') {
				lastPosition++;
			}
		}
		int endPosition = lastPosition + 1;
		consumer.accept(s.substring(startPosition, endPosition));
		return endPosition;
	}

	public ImmutableList<ParsedFieldDescriptor> getParameterTypes() {
		return parameterTypes;
	}

	public ParsedFieldDescriptor getReturnType() {
		return returnType;
	}

	public int getParameterWords() {
		int words = 0;
		for (ParsedFieldDescriptor parameterDescriptor : parameterTypes) {
			words += parameterDescriptor.getWords();
		}
		return words;
	}

	public int getReturnWords() {
		return (returnType == null ? 0 : returnType.getWords());
	}

}
