package nl.rudidevries.kura.solarevent.api;

public interface SolarEventService {
	/**
	 * Add an event listener to the service
	 * @param listener
	 */
	void addEventListener(SolarEventListener listener);
	
	/**
	 * Remove an event listener from the service
	 * @param listener
	 */
	void removeEventListener(SolarEventListener listener);
}
