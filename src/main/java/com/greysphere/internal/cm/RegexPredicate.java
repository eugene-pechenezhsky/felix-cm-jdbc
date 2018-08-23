package com.greysphere.internal.cm;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RegexPredicate implements Predicate<String>
{
	private Pattern p;
	
	@Override
	public boolean test(String t)
	{
		return p.matcher(t).matches();
	}

}
