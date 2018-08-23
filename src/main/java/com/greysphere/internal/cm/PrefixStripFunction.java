package com.greysphere.internal.cm;

import java.util.function.Function;

public class PrefixStripFunction implements Function<String, String>
{
	private final String prefix;
	
	public PrefixStripFunction(final String prefix)
	{
		this.prefix = prefix;
	}

	@Override
	public String apply(String t)
	{
		return t.replaceFirst(prefix, "");
	}

}
