package name.martingeisse.majai;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public final class ParsedMethodDescriptor {

    private final ImmutableList<String> parameterTypes;
    private final String returnType;

    public ParsedMethodDescriptor(String descriptor) {
        if (!descriptor.startsWith("(")) {
            throw new RuntimeException("missing opening parenthesis in method descriptor: " + descriptor);
        }
        int index = descriptor.indexOf(')');
        if (index == -1) {
            throw new RuntimeException("missing closing parenthesis in method descriptor: " + descriptor);
        }
        List<String> parameterTypes = new ArrayList<>();
        int position = 1;
        while (position < index) {
            String fieldDescriptor = parseFieldType(descriptor, position);
            parameterTypes.add(fieldDescriptor);
            position += fieldDescriptor.length();
        }
        this.parameterTypes = ImmutableList.copyOf(parameterTypes);
        this.returnType = descriptor.substring(index + 1);
    }

    private static String parseFieldType(String s, int startPosition) {
        int position = startPosition;
        char c = s.charAt(position);
        while (c == '[') {
            position++;
            c = s.charAt(position);
        }
        if (c == 'B' || c == 'C' || c == 'D' || c == 'F' || c == 'I' || c == 'J' || c == 'S' || c == 'Z') {
            return s.substring(startPosition, position + 1);
        }
        if (c != 'L') {
            throw new RuntimeException("invalid field or method descriptor, found field type starting with " + c);
        }
        return s.substring(startPosition, s.indexOf(';', position) + 1);
    }

    public ImmutableList<String> getParameterTypes() {
        return parameterTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getParameterWords() {
        int words = 0;
        for (String parameterDescriptor : parameterTypes) {
            if (parameterDescriptor.equals("J") || parameterDescriptor.equals("D")) {
                words += 2;
            } else {
                words++;
            }
        }
        return words;
    }

}
