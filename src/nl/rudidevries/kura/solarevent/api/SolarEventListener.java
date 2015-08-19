package nl.rudidevries.kura.solarevent.api;

public interface SolarEventListener {
	void sunrise(SolarEvent e);
	void sunset(SolarEvent e);
}
