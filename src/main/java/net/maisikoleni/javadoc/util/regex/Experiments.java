package net.maisikoleni.javadoc.util.regex;

import java.util.Scanner;
import java.util.regex.Pattern;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.jdk18.Jdk18SearchSerivce;

public class Experiments {

	private static final int LOOP = 100;

	public static void main(String[] args) {
		var lower = new CharClass(Character::isLowerCase, "\\p{javaLowerCase}");
		var nonDiv = new CharClass(c -> c != '.' && c != '/', "[^./]");
		var anyLower = new Star(lower);
		var r = new Concatenation(new Literal("jav"), anyLower, new Literal(".base"), anyLower, new Literal("/j"),
				anyLower, new Literal("."), anyLower, new Literal(".Set"), new Star(nonDiv));
		System.out.println(r);
		var cr = CompiledRegex.compile(r, false);
		var p = Pattern.compile(r.toString());
		var index = JavadocIndex.loadAsResources(Jdk18SearchSerivce.class);
		try (var s = new Scanner(System.in)) {
			s.nextLine();
		}
		long t1 = 0;
		long t2 = 0;
		long t3 = 0;
		String[] names = index.stream().map(SearchableEntity::qualifiedName).toArray(String[]::new);
		for (int i = 0; i < LOOP; i++) {
			for (var qn : names) {
				t1 -= System.nanoTime();
				boolean b1 = r.matches(qn);
				t1 += System.nanoTime();
				t2 -= System.nanoTime();
				boolean b2 = p.matcher(qn).matches();
				t2 += System.nanoTime();
				t3 -= System.nanoTime();
				long esgc = cr.stepThrough(qn);
				boolean b3 = cr.isMatch(esgc);
				t3 += System.nanoTime();
				if (b1 != b2 || b2 != b3)
					System.out.format("'%s' -> %s, %s, %s%n", qn, b1, b2, b3);
				if (b3)
					System.out.println(qn + " : " + cr.getEmptyStarCount(esgc));
			}
		}
		System.out.println(LOOP * names.length);
		System.out.println(t1);
		System.out.println(t2);
		System.out.println(t3);
	}
}
