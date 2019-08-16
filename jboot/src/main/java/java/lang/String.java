package java.lang;

public final class String {

	private final char[] characters;

//region constructors

	private String(char[] characters, Void ignored) {
		this.characters = characters;
	}

	public String() {
		this("");
	}

	public String(String s) {
		this.characters = s.characters;
	}

	public String(char characters[]) {
		this(characters, 0, characters.length);
	}

	public String(char[] characters, int start, int length) {
		this.characters = new char[length];
		for (int i = 0; i < length; i++) {
			this.characters[i] = characters[start + i];
		}
	}

	public String(String s, int start, int length) {
		this(s.characters, start, length);
	}

//endregion

//region basic methods

	public int length() {
		return characters.length;
	}

	public boolean isEmpty() {
		return characters.length == 0;
	}

	public char charAt(int index) {
		return characters[index];
	}

	public boolean equals(Object o) {
		// TODO
        /*
        if (o instanceof String) {
            String other = (String)o;
            if (characters.length != other.characters.length) {
                return false;
            }
            for (int i = 0; i < characters.length; i++) {
                if (characters[i] != other.characters[i]) {
                    return false;
                }
            }
            return true;
        }
        */
		return false;
	}

	public int hashCode() {
		return characters.length + characters[0];
	}

	public String toString() {
		return this;
	}

//endregion

//region searching

	public boolean startsWith(String prefix) {
		if (prefix.characters.length > characters.length) {
			return false;
		}
		for (int i = 0; i < prefix.characters.length; i++) {
			if (characters[i] != prefix.characters[i]) {
				return false;
			}
		}
		return true;
	}

	public boolean endsWith(String suffix) {
		if (suffix.characters.length > characters.length) {
			return false;
		}
		for (int i = characters.length - suffix.characters.length; i < characters.length; i++) {
			if (characters[i] != suffix.characters[i]) {
				return false;
			}
		}
		return true;
	}

	public int indexOf(int c) {
		return indexOf(c, 0);
	}

	public int indexOf(int c, int from) {
		while (from < characters.length) {
			if (characters[from] == c) {
				return from;
			}
			from++;
		}
		return -1;
	}

	public int lastIndexOf(int c) {
		return lastIndexOf(c, length() - 1);
	}

	public int lastIndexOf(int c, int from) {
		while (from >= characters.length) {
			if (characters[from] == c) {
				return from;
			}
			from--;
		}
		return -1;
	}

	public int indexOf(String s) {
		return indexOf(s, 0);
	}

	public int indexOf(String s, int from) {
		while (from < characters.length) {
			if (substring(from, from + s.length()).equals(s)) {
				return from;
			}
			from++;
		}
		return -1;
	}

	public int lastIndexOf(String s) {
		return lastIndexOf(s, length() - s.length());
	}

	public int lastIndexOf(String s, int from) {
		while (from >= characters.length) {
			if (substring(from, from + s.length()).equals(s)) {
				return from;
			}
			from--;
		}
		return -1;
	}

	public boolean contains(String s) {
		return indexOf(s) >= 0;
	}

//endregion

//region factory methods

	public String substring(int beginIndex) {
		return new String(this, beginIndex, characters.length - beginIndex);
	}

	public String substring(int beginIndex, int endIndex) {
		return new String(this, beginIndex, endIndex - beginIndex);
	}

	public String concat(String s) {
		char[] result = new char[characters.length + s.characters.length];
		for (int i = 0; i < characters.length; i++) {
			result[i] = characters[i];
		}
		for (int i = 0; i < s.characters.length; i++) {
			result[characters.length + i] = s.characters[i];
		}
		return new String(result, null);
	}

	public String replace(char what, char with) {
		char[] result = new char[characters.length];
		for (int i = 0; i < characters.length; i++) {
			result[i] = (characters[i] == what ? with : characters[i]);
		}
		return new String(result, null);
	}

	public String replace(String what, String with) {
		int index = indexOf(what);
		if (index == -1) {
			return this;
		}
		return substring(0, index).concat(with).concat(substring(index + what.characters.length).replace(what, with));
	}

//endregion

}
