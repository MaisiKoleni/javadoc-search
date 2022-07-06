package net.maisikoleni.javadoc.util.regex;

import static java.lang.Long.rotateLeft;
import static java.lang.Long.rotateRight;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class CompiledRegex implements GradingLongStepMatcher {

	private static final int OPCODE_OFFSET = 30;

	private static final int ARG_MASK = (1 << OPCODE_OFFSET) - 1;
	private static final int OPCODE_MASK = -1 << OPCODE_OFFSET;

	private static final int LITERAL_OC = 0b00;
	private static final int CHAR_CLASS_OC = 0b01;
	private static final int STAR_LITERAL_OC = 0b10;
	private static final int STAR_CHAR_CLASS_OC = 0b11;

	private static final int LITERAL = LITERAL_OC << OPCODE_OFFSET;
	private static final int CHAR_CLASS = CHAR_CLASS_OC << OPCODE_OFFSET;
	private static final int STAR_LITERAL = STAR_LITERAL_OC << OPCODE_OFFSET;
	private static final int STAR_CHAR_CLASS = STAR_CHAR_CLASS_OC << OPCODE_OFFSET;
	private static final int ANY_STAR_MASK = 0b10 << OPCODE_OFFSET;

	private static final int MATCHED_STAR_COUNT_OFFSET = 32;
	private static final long MATCHED_STAR_COUNT_MASK = 0x7F_FF_FF_FF_00_00_00_00L;
	private static final int STAR_MATCHED_OFFSET = 31;
	private static final long STAR_MATCHED_MASK = 1L << STAR_MATCHED_OFFSET;
	private static final long INSTRUCTION_MASK = 0x00_00_00_00_7F_FF_FF_FFL;

	public static final long START_STATE = 0L;
	public static final long NO_MATCH = -1L;

	private final int[] instructions;
	private final CharPredicate[] charClasses;
	private final boolean caseInsensitive;
	private final int matchEnd;
	private final int starCount;

	private CompiledRegex(CharPredicate[] charClasses, int[] instructions, boolean caseInsensitive) {
		this.instructions = instructions;
		this.caseInsensitive = caseInsensitive;
		if (caseInsensitive)
			this.charClasses = Stream.of(charClasses).map(CharPredicate::ignoreCase).toArray(CharPredicate[]::new);
		else
			this.charClasses = charClasses;
		int tempMatchEnd = instructions.length;
		while (tempMatchEnd > 0 && isStar(instructions[tempMatchEnd - 1]))
			tempMatchEnd--;
		this.matchEnd = tempMatchEnd;
		this.starCount = (int) IntStream.of(instructions).filter(CompiledRegex::isStar).count();
	}

	@Override
	public long getStartState() {
		return START_STATE;
	}

	public boolean isCaseInsensitive() {
		return caseInsensitive;
	}

	private static boolean isStar(int instruction) {
		return (instruction & ANY_STAR_MASK) != 0;
	}

	@Override
	public long step(char c, long state) {
		if (state == NO_MATCH)
			return NO_MATCH;
		long currentState = state;
		for (int i = getInstructionIndex(currentState); i < instructions.length; i++) {
			int instruction = instructions[i];
			int argument = instruction & ARG_MASK;
			int opCode = (instruction & OPCODE_MASK) >>> OPCODE_OFFSET;
			switch (opCode) {
				case STAR_LITERAL_OC -> {
					if (charEquals((char) argument, c))
						return newState(currentState, 0, 1);
					currentState = continueAfterStar(currentState);
				}
				case LITERAL_OC -> {
					if (charEquals((char) argument, c))
						return newState(currentState, 1, 0);
					return NO_MATCH;
				}
				case STAR_CHAR_CLASS_OC -> {
					if (charClasses[argument].test(c))
						return newState(currentState, 0, 1);
					currentState = continueAfterStar(currentState);
				}
				case CHAR_CLASS_OC -> {
					if (charClasses[argument].test(c))
						return newState(currentState, 1, 0);
					return NO_MATCH;
				}
				default -> throw new IllegalStateException("Unexpected value");
			}
		}
		return NO_MATCH;
	}

	private boolean charEquals(char a, char b) {
		return a == b || (caseInsensitive && charEqualsIgnoreCase(a, b));
	}

	private static boolean charEqualsIgnoreCase(char a, char b) {
		return Character.toLowerCase(a) == Character.toLowerCase(b)
				|| Character.toUpperCase(a) == Character.toUpperCase(b);
	}

	private static int getInstructionIndex(long state) {
		return (int) (state & INSTRUCTION_MASK);
	}

	private static long continueAfterStar(long state) {
		return rotateLeft(rotateRight( // emptyStarCount += ...
				state & ~STAR_MATCHED_MASK // starMatched = false
				, MATCHED_STAR_COUNT_OFFSET) //
				+ ((state >>> STAR_MATCHED_OFFSET) & 1) // ...starMatched ? 1 : 0
				, MATCHED_STAR_COUNT_OFFSET) //
				+ 1; // instructionIndex++
	}

	private static long newState(long state, int instructionIndexIncrement, int starMatched) {
		return ((state & ~STAR_MATCHED_MASK) | ((long) starMatched << STAR_MATCHED_OFFSET)) + instructionIndexIncrement;
	}

	@Override
	public boolean matches(CharSequence s) {
		long resultState = stepThrough(s, START_STATE);
		return isMatch(resultState);
	}

	@Override
	public long stepThrough(CharSequence s, long startState, int start, int end) {
		long state = startState;
		for (int i = start; i < end; i++) {
			state = step(s.charAt(i), state);
			if (state == NO_MATCH)
				return NO_MATCH;
		}
		return state;
	}

	@Override
	public boolean isMatch(long state) {
		if (state == NO_MATCH)
			return false;
		return (state & INSTRUCTION_MASK) >= matchEnd;
	}

	@Override
	public boolean isOk(long state) {
		return state != NO_MATCH;
	}

	@Override
	public double grade(long state) {
		if (starCount == 0.0)
			return 1.0;
		return (double) getEmptyStarCount(state) / starCount;
	}

	public int getEmptyStarCount(long state) {
		return starCount - (int) (((state & MATCHED_STAR_COUNT_MASK) >>> MATCHED_STAR_COUNT_OFFSET)
				+ ((state >>> STAR_MATCHED_OFFSET) & 1));
	}

	public static CompiledRegex compile(Regex simpleRegex) {
		return compile(simpleRegex, false);
	}

	public static CompiledRegex compile(Regex simpleRegex, boolean caseInsensitive) {
		Objects.requireNonNull(simpleRegex);
		boolean compilable = simpleRegex.accept(new RegexVisitor<>() {
			@Override
			public Boolean visit(Concatenation concatenation) {
				for (var part : concatenation.parts())
					if (!part.accept(this))
						return false;
				return true;
			}

			@Override
			public Boolean visit(Literal literal) {
				return true;
			}

			@Override
			public Boolean visit(Star star) {
				var regex = star.regex();
				if (regex instanceof Literal l)
					return l.chars().length() == 1;
				return regex instanceof CharClass;
			}

			@Override
			public Boolean visit(CharClass charClass) {
				return true;
			}
		});
		if (!compilable)
			throw new IllegalArgumentException("cannot compile regex structure");

		int instructionCount = simpleRegex.accept(new RegexVisitor<>() {
			@Override
			public Integer visit(Concatenation concatenation) {
				return concatenation.parts().stream().mapToInt(r -> r.accept(this)).sum();
			}

			@Override
			public Integer visit(Literal literal) {
				return literal.chars().length();
			}

			@Override
			public Integer visit(Star star) {
				return 1;
			}

			@Override
			public Integer visit(CharClass charClass) {
				return 1;
			}
		});
		var charPredicateMap = simpleRegex.accept(new RegexVisitor<Map<CharPredicate, Integer>>() {
			int nextIndex;
			Map<CharPredicate, Integer> map = new LinkedHashMap<>();

			@Override
			public Map<CharPredicate, Integer> visit(Concatenation concatenation) {
				for (var regex : concatenation.parts())
					regex.accept(this);
				return map;
			}

			@Override
			public Map<CharPredicate, Integer> visit(Literal literal) {
				return map;
			}

			@Override
			public Map<CharPredicate, Integer> visit(Star star) {
				return star.regex().accept(this);
			}

			@Override
			public Map<CharPredicate, Integer> visit(CharClass charClass) {
				map.computeIfAbsent(charClass.predicate(), predicate -> nextIndex++);
				return map;
			}
		});
		var instructions = new int[instructionCount];
		var charClasses = charPredicateMap.keySet().toArray(CharPredicate[]::new);
		int addedInstructions = simpleRegex.accept(new RegexVisitor<>() {

			int nextIndex;

			@Override
			public Integer visit(Concatenation concatenation) {
				for (var regex : concatenation.parts())
					regex.accept(this);
				return nextIndex;
			}

			@Override
			public Integer visit(Literal literal) {
				literal.chars().chars().forEach(c -> addInstruction(LITERAL, c));
				return nextIndex;
			}

			@Override
			public Integer visit(Star star) {
				var regex = star.regex();
				if (regex instanceof Literal l)
					addInstruction(STAR_LITERAL, l.chars().charAt(0));
				else if (regex instanceof CharClass cc)
					addInstruction(STAR_CHAR_CLASS, charPredicateMap.get(cc.predicate()));
				else
					throw new IllegalArgumentException();
				return nextIndex;
			}

			@Override
			public Integer visit(CharClass charClass) {
				addInstruction(CHAR_CLASS, charPredicateMap.get(charClass.predicate()));
				return nextIndex;
			}

			private int addInstruction(int opCode, int arg) {
				if ((opCode & ARG_MASK) != 0 || (arg & OPCODE_MASK) != 0)
					throw new IllegalArgumentException();
				instructions[nextIndex] = opCode | arg;
				return nextIndex++;
			}
		});
		if (addedInstructions != instructions.length)
			throw new IllegalStateException();
		return new CompiledRegex(charClasses, instructions, caseInsensitive);
	}

	@Override
	public String toString() {
		return IntStream.of(instructions).mapToObj(HexFormat.of()::toHexDigits)
				.collect(Collectors.joining(",\n  ", "CompiledRegex[\n  ", "]"));
	}
}