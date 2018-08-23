package com.greysphere.internal.cm;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ServicePidFilterFactory
{
	public enum ServicePidFilterType
	{
		REGEX,
		PREFIX
	}
	
	public Predicate<String> create(ServicePidFilterType type, String arg)
	{
		switch(type)
		{
			case PREFIX:
				return x -> x != null && x.startsWith(arg);
			case REGEX:
				return Pattern.compile(arg).asPredicate();
			default:
				throw new UnsupportedOperationException("Unknown factory: " + type);
		}
	}
}
