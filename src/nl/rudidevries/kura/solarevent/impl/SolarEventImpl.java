package nl.rudidevries.kura.solarevent.impl;

import java.util.Date;

import nl.rudidevries.kura.solarevent.api.SolarEvent;

class SolarEventImpl implements SolarEvent {

	private Date time;
	
	/**
	 * Constructor
	 * @param time
	 */
	SolarEventImpl(Date time) {
		this.time = time;
	}
	
	@Override
	public Date getTime() {
		return time;
	}
}
