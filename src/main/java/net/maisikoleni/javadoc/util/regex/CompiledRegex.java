package net.maisikoleni.javadoc.util.regex;

import static java.lang.Long.*;

import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
	private final CharPredicate[] charPredicates;
	private final boolean caseInsensitive;
	private final int matchEnd;
	private final int starCount;

	private CompiledRegex(CharPredicate[] charPredicates, int[] instructions, boolean caseInsensitive) {
		this.instructions = instructions;
		this.caseInsensitive = caseInsensitive;
		if (caseInsensitive)
			this.charPredicates = Stream.of(charPredicates).map(CharPredicate::ignoreCase)
					.toArray(CharPredicate[]::new);
		else
			this.charPredicates = charPredicates;
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
					if (charPredicates[argument].test(c))
						return newState(currentState, 0, 1);
					currentState = continueAfterStar(currentState);
				}
				case CHAR_CLASS_OC -> {
					if (charPredicates[argument].test(c))
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

	@Override
	public String toString() {
		return IntStream.of(instructions).mapToObj(HexFormat.of()::toHexDigits)
				.collect(Collectors.joining(",\n  ", "CompiledRegex[\n  ", "]"));
	}

	public static CompiledRegex compile(Regex simpleRegex) {
		return compile(simpleRegex, false);
	}

	public static CompiledRegex compile(Regex simpleRegex, boolean caseInsensitive) {
		Objects.requireNonNull(simpleRegex);
		if (!isCompatible(simpleRegex))
			throw new IllegalArgumentException("cannot compile regex structure");

		var charPredicates = collectCharPredicates(simpleRegex).distinct().toArray(CharPredicate[]::new);
		var charPredicateMap = createCharPredicateIndexMap(charPredicates);
		var instructions = composeInstructionArray(simpleRegex, charPredicateMap);
		return new CompiledRegex(charPredicates, instructions, caseInsensitive);
	}

	private static int[] composeInstructionArray(Regex simpleRegex, Map<CharPredicate, Integer> charPredicateMap) {
		return instructionStreamFor(simpleRegex, charPredicateMap).toArray();
	}

	private static IntStream instructionStreamFor(Regex simpleRegex, Map<CharPredicate, Integer> charPredicateMap) {
		return switch (simpleRegex) {
			case Literal(var charSequence) -> charSequence.chars().map(c -> composeInstruction(LITERAL, c));
			case CharClass(var predicate, var __) -> IntStream
					.of(composeInstruction(CHAR_CLASS, charPredicateMap.get(predicate)));
			case Star(var innerRegex) -> switch (innerRegex) {
				case Literal(var chars) when chars.length() == 1 -> IntStream
						.of(composeInstruction(STAR_LITERAL, chars.charAt(0)));
				case CharClass(var predicate, var __) -> IntStream
						.of(composeInstruction(STAR_CHAR_CLASS, charPredicateMap.get(predicate)));
				default -> throw new IllegalArgumentException();
			};
			case Concatenation(var parts) -> parts.stream()
					.flatMapToInt(part -> instructionStreamFor(part, charPredicateMap));
		};
	}

	private static int composeInstruction(int opCode, int arg) {
		if ((opCode & ARG_MASK) != 0 || (arg & OPCODE_MASK) != 0)
			throw new IllegalArgumentException();
		return opCode | arg;
	}

	private static Map<CharPredicate, Integer> createCharPredicateIndexMap(CharPredicate[] charPredicates) {
		return IntStream.range(0, charPredicates.length).boxed()
				.collect(Collectors.toUnmodifiableMap(i -> charPredicates[i], Function.identity()));
	}

	private static Stream<CharPredicate> collectCharPredicates(Regex simpleRegex) {
		return switch (simpleRegex) {
			case Literal __ -> Stream.of();
			case CharClass(var predicate, var __) -> Stream.of(predicate);
			case Star(var innerRegex) -> collectCharPredicates(innerRegex);
			case Concatenation(var parts) -> parts.stream().flatMap(CompiledRegex::collectCharPredicates);
		};
	}

	private static boolean isCompatible(Regex simpleRegex) {
		return switch (simpleRegex) {
			case Literal __ -> true;
			case CharClass __ -> true;
			case Star(var innerRegex) -> (innerRegex instanceof Literal l && l.chars().length() == 1)
					|| innerRegex instanceof CharClass;
			case Concatenation(var parts) -> parts.stream().allMatch(CompiledRegex::isCompatible);
		};
	}
}