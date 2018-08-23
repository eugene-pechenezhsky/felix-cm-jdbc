package com.greysphere.internal.cm;

import java.util.function.Function;

public class PrefixAddFunction implements Function<String, String>
{
	private final String prefix;
	
	public PrefixAddFunction(final String prefix)
	{
		this.prefix = prefix;
	}

	@Override
	public String apply(String t)
	{
		return prefix + t;
	}

}
