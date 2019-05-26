package com.greysphere.internal.cm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.felix.cm.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;

@SuppressWarnings("rawtypes")
public class DelegatingPersistenceManager implements PersistenceManager
{
	private static final Logger log = LoggerFactory.getLogger(DelegatingPersistenceManager.class);
	
	private final PersistenceManager pmPrimary;
	private final PersistenceManager pmWrapped;
	private final Predicate<String> supportedPidPredicate;
	private final boolean persistToWrappedPm;
	private final SyncMode syncMode;
	private static final Set<String> ignoredPropertiesForPrimary = new HashSet<>(Arrays.asList(
		Constants.FELIX_INSTALL_FILENAME,
		Constants.SERVICE_PID));
	
	public DelegatingPersistenceManager(
			final PersistenceManager pmPrimary,
			final PersistenceManager pmWrapped,
			final Predicate<String> supportedPidPredicate,
			final boolean persistToWrappedPm,
			final SyncMode syncMode)
	{
		this.pmPrimary = Objects.requireNonNull(pmPrimary);
		this.pmWrapped = Objects.requireNonNull(pmWrapped);
		this.supportedPidPredicate = Objects.requireNonNull(supportedPidPredicate);
		this.syncMode = Objects.requireNonNull(syncMode);
		this.persistToWrappedPm = persistToWrappedPm;
		
		log.info(
			"Initialized: persistToWrappedPm={}; syncMode={}",
			persistToWrappedPm,
			syncMode); 
	}

	@Override
	public boolean exists(String pid)
	{
		if(supportedPidPredicate.test(pid))
		{
			return pmPrimary.exists(pid);
		}
		
		return pmWrapped.exists(pid);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Dictionary load(String pid) throws IOException
	{
		if(supportedPidPredicate.test(pid))
		{
			Dictionary properties = pmPrimary.load(pid);
			
			properties.put(Constants.SERVICE_PID, pid);
			
			return properties;
		}
		
		return pmWrapped.load(pid);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enumeration getDictionaries() throws IOException
	{
		return Iterators.asEnumeration(
				Iterators.concat(
					Iterators.forEnumeration(pmPrimary.getDictionaries()), 
					Iterators.forEnumeration(pmWrapped.getDictionaries())));
	}
	
	@Override
	public void store(String pid, Dictionary properties) throws IOException
	{
		if(supportedPidPredicate.test(pid))
		{
			Dictionary propertiesClean =
					copyProperties(
						properties,
						ignoredPropertiesForPrimary::contains);
			
			pmPrimary.store(pid, propertiesClean);
			
			if(!persistToWrappedPm) return;
		}
		
		pmWrapped.store(pid, properties);
	}
		

	@Override
	public void delete(String pid) throws IOException
	{
		if(supportedPidPredicate.test(pid))
		{
			pmPrimary.delete(pid);
			
			if(!persistToWrappedPm) return;
		}
		
		pmWrapped.delete(pid);
	}
	
	public void init()
	{
		switch(syncMode)
		{
			case FROM_WRAPPED:
				copyData(pmWrapped, pmPrimary, supportedPidPredicate);
				break;
			case TO_WRAPPED:
				copyData(pmPrimary, pmWrapped, supportedPidPredicate);
				break;
			default: 
				return;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void copyData(
			PersistenceManager source,
			PersistenceManager destination,
			Predicate<String> supportedPidPredicate)
	{
		log.info("copying configs from {} to {}", source, destination);
		
		int copiedCount = 0;
		int failedCount = 0;
		int skippedCount = 0;
		
		Enumeration<Dictionary> existing;
		
		try
		{
			existing = source.getDictionaries();
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
		while(existing.hasMoreElements())
		{
			Dictionary properties = existing.nextElement();
			String pid = (String) properties.get(Constants.SERVICE_PID);
			
			if(!supportedPidPredicate.test(pid))
			{
				skippedCount++;
				log.debug("Skipping unsupported PID: {}", pid);
				continue;
			}
			
			Dictionary propertiesClean =
					copyProperties(
						properties,
						ignoredPropertiesForPrimary::contains);
			
			try
			{
				destination.store(pid, propertiesClean);
			}
			catch(Exception e)
			{
				failedCount++;
				log.error("Dictionary import failed for PID: {}", pid, e);
				continue;
			}
			
			copiedCount++;
			log.info("Imported dictionary for PID: {}", pid);
		}
		
		log.info(
			"Finished copying configs: {} copied, {} skipped, {} failed",
			copiedCount,
			skippedCount,
			failedCount);
	}
	
	@SuppressWarnings("unchecked")
	private static Dictionary copyProperties(Dictionary source, Predicate<Object> excludeIf)
	{
		Dictionary destination = new Hashtable();
		
		Enumeration keys = source.keys();
		
		while(keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			
			if(excludeIf.test(key)) continue;
			
			Object value = source.get(key);
			
			destination.put(key, value);
		}
		
		return destination;
	}
}
