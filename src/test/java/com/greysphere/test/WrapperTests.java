package com.greysphere.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.felix.cm.PersistenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.greysphere.internal.cm.SyncMode;
import com.greysphere.internal.cm.Constants;
import com.greysphere.internal.cm.DelegatingPersistenceManager;

@RunWith(MockitoJUnitRunner.class)
public class WrapperTests
{
	DelegatingPersistenceManager dpm;
	
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	PersistenceManager pmPrimary;
	
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	PersistenceManager pmWrapped;
	
	@Before
	public void init()
	{
		when(pmPrimary.toString()).thenReturn("PRIMARY");
		when(pmWrapped.toString()).thenReturn("WRAPPED");
		
		dpm = new DelegatingPersistenceManager(
				pmPrimary, 
				pmWrapped, 
				x -> true, 
				true, 
				SyncMode.FROM_WRAPPED);
	}
	
	@Test
	public void canBootstrap() throws Exception
	{
		Vector<Dictionary> dictionaries = new Vector<Dictionary>();
		
		dictionaries.add(new Hashtable(ImmutableMap.of(
				Constants.SERVICE_PID, "pid.1", "prop1", "a", "prop2", "b")));
		
		dictionaries.add(new Hashtable(ImmutableMap.of(
				Constants.SERVICE_PID, "pid.2", "prop1", "c")));
		
		when(pmWrapped.getDictionaries()).thenReturn(dictionaries.elements());
		
		dpm.init();
		
		verify(pmPrimary).store(eq("pid.1"), any());
		verify(pmPrimary).store(eq("pid.2"), any());
	}
}
