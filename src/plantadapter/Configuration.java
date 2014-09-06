package plantadapter;

// TODO Caricamento da file di configurazione

public final class Configuration {

	public static final String[] COMMANDS_PACKAGES = new String[] { 
		"plantadapter.commands",
		"plantadapter.commands.adam" // TODO Eventualmente fai package per tutti i comandi di questo tipo (StatusCommand)...
	};
	
	public static final String[] QUANTITIES_PACKAGES = new String[] {
		"quantities", 
		"javax.measure.quantity"
	};
	
	public static final String[] DEVICES_PACKAGES = new String[] {
		"plantmodel.adam", 
		"plantmodel.dt80",
		"plantmodel.misc"
	};
	
	private Configuration() {}
}