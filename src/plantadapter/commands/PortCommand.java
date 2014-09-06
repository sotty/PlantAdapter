package plantadapter.commands;

import java.util.Date;

import plantmodel.Device;
import plantmodel.Endpoint;

public abstract class PortCommand extends DeviceCommand {

	public static final String targetPointIdTagName = "tgtPortID";
	
	// Instance Fields
	
	// private final Endpoint targetPort;
	private Endpoint targetPort;
	
	// Public Constructors
	
	public PortCommand(String commandID, Date timestamp, Endpoint targetPort) {
		super(commandID, timestamp, getDevice(targetPort));
		
		this.targetPort = targetPort;
	}
	
	public PortCommand(String commandID, Endpoint targetPort) {
		this(commandID, new Date(), targetPort);
	}
	
	private static Device getDevice(Endpoint targetPort) {
		if(targetPort != null)
			return targetPort.getDevice();
		return null;
	}

	// TODO IPortCommand
	
	public Endpoint getTargetPort() {
		return this.targetPort;
	}
	
	// Object
	
	@Override
	public String toString() {
		return this.getTargetPort() + " " + super.toString();
	}
	
	@Override
	public boolean equals(Object o){
		PortCommand command = (PortCommand) o;
		if(super.equals(command))
			return false;
		if(!this.targetPort.equals(command.getTargetPort()))
			return false;
		return true;
	}
}