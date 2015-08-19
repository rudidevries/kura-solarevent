package nl.rudidevries.kura.solarevent.impl;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import nl.rudidevries.kura.solarevent.api.SolarEvent;
import nl.rudidevries.kura.solarevent.api.SolarEventListener;
import nl.rudidevries.kura.solarevent.api.SolarEventService;
import nl.rudidevries.kura.solarevent.api.SolarEventType;

public class SolarEventServiceImpl implements ConfigurableComponent, SolarEventService {

	private static final Logger s_logger = LoggerFactory.getLogger(SolarEventServiceImpl.class);
	

	private static final String APP_ID = "solarevent";
	static final String PROPERTY_LATITUDE = "solar.latitude";
	static final String PROPERTY_LONGITUDE = "solar.longitude";
	static final String PROPERTY_TIMEZONE = "solar.timezone";
	static final String PROPERTY_ACCURACY = "solar.accuracy";
	
	private ScheduledExecutorService worker;
	private ScheduledFuture<?> threadHandle;
	
	/**
	 * Defines within what number of seconds from the event the event
	 * should be fired. Higher accuracy means more thread activity, thus
	 * a higher load. 
	 */
	private int accuracy;
	private Calendar lastChecked;
	
	private Set<SolarEventListener> listeners = new HashSet<>();
	private SunriseSunsetCalculator sunriseCalculator;
	private Calendar nextSunrise;
	private Calendar nextSunset;
	
	/**
	 * Constructor
	 */
	public SolarEventServiceImpl() {
		worker = Executors.newSingleThreadScheduledExecutor();
	}
	
	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		updated(properties);
		s_logger.info("Activating {}... Done.", APP_ID);
	}
	
	protected void deactivate(ComponentContext componentContext) {
		// Cancel previously started task
		if (threadHandle != null) {
			threadHandle.cancel(true);
		}
	}
	
	/**
	 * Update component configuration.
	 * @param properties
	 */
	public void updated(Map<String,Object> properties) {
		// location, latitude & longitude
		Location location = new Location(
					(String) properties.get(PROPERTY_LATITUDE),
					(String) properties.get(PROPERTY_LONGITUDE)
				);
		// timezone
		sunriseCalculator = new SunriseSunsetCalculator(
					location, 
					(String) properties.get(PROPERTY_TIMEZONE)
				);
		// accuracy 
		accuracy = (Integer) properties.get(PROPERTY_ACCURACY);
		
		// (re)initialze
		initialize();
	}
	
	@Override
	public void addEventListener(SolarEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeEventListener(SolarEventListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Initialize service by starting a new thread which 
	 * will fire events.
	 */
	private void initialize() {
		// Cancel previously started task
		if (threadHandle != null) {
			threadHandle.cancel(true);
		}
		
		// start worker immediately and repeat based on accuracy setting.
		threadHandle = worker.scheduleAtFixedRate(
					new SolarEventWatcher(), 0, accuracy, TimeUnit.SECONDS
				);
	}
	
	/**
	 * Reset sunrise & sunset dates.
	 * @param date Date to base calculations on.
	 */
	private void resetSunriseSunset(Calendar date) {
		nextSunrise = sunriseCalculator.getOfficialSunriseCalendarForDate(date);
		s_logger.info("Next sunrise calculated: {}", nextSunrise.getTime());
		nextSunset = sunriseCalculator.getOfficialSunsetCalendarForDate(date);
		s_logger.info("Next sunset calculated: {}", nextSunset.getTime());
	}

	/**
	 * Fire event to all listeners.
	 * @param time time of the event.
	 * @param solarEventType type of event.
	 */
	private void fireEvent(Calendar time, SolarEventType solarEventType) {
		SolarEvent e = new SolarEventImpl(time.getTime());
		for (SolarEventListener l: listeners) {
			switch (solarEventType) {
				case SUNRISE:
					l.sunrise(e);
					break;
				case SUNSET:
					l.sunset(e);
					break;
			}
		}
	}
	
	/**
	 * SolarEventWatcher.
	 * Periodically checks if a sunset or sunrise has occurred.
	 */
	private class SolarEventWatcher implements Runnable {
		@Override
		public void run() {
			Calendar now = Calendar.getInstance();

			s_logger.info("Running check {}, {}.", now.getTime().toString(), APP_ID);
			
			// If sunrise/sunset was not checked before, set last checked to now
			if (lastChecked == null) {
				lastChecked = now;
				resetSunriseSunset(lastChecked);
			}
			// if it is a new day, calculate sunrise/sunset for this day.
			else if (now.get(Calendar.DATE) != lastChecked.get(Calendar.DATE)) {
				resetSunriseSunset(now);
			}
			
			// If sunrise/sunset is between last checked and now, trigger an event.
			if (nextSunrise.after(lastChecked) && nextSunrise.before(now)) {
				fireEvent(nextSunrise, SolarEventType.SUNRISE);
			}
			else if (nextSunset.after(lastChecked) && nextSunset.before(now)) {
				fireEvent(nextSunset, SolarEventType.SUNSET);
			}
			
			// Update last checked to just now.
			lastChecked = now;
		}
		
	}
}
