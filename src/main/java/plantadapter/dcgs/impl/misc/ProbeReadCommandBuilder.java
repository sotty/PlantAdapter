package plantadapter.dcgs.impl.misc;

import plantadapter.commands.PortCommand;
import plantadapter.commands.ReadCommand;
import plantadapter.dcgs.AbstractCommandBuilder;
import plantadapter.dcgs.IDeviceCommandBuilder;

import plantmodel.Endpoint;
import plantmodel.AnalogueEndpointInterface;
import plantmodel.IEndpointInterface;

import plantmodel.misc.ProbeDevice;

public class ProbeReadCommandBuilder extends AbstractCommandBuilder<ProbeDevice, ReadCommand> implements IDeviceCommandBuilder {

	public ProbeReadCommandBuilder(ProbeDevice executorDevice,
			ReadCommand originalCommand) {
		super(executorDevice, originalCommand);
	}

	@Override
	public PortCommand buildCommand() {
		
		// TODO Anche questo genere di controlli possono essere fattorizzati...
		boolean found = false;
		for (IEndpointInterface ei : super.getOriginalCommand().getTargetPort().getInterfaces()) {
			if (ei.getQuantity().equals(super.getOriginalCommand().getQuantity())) {
				found = true;
				break;
			}
		}
		
		if (!found) throw new IllegalStateException(); // TODO
		
		// Endpoint su cui dovr� essere inviato il comando
		Endpoint outPort = super.getExecutorDevice().getEndpointById("SOURCE");
		
		// Quantity gestita dal Device nativamente
		// Nota: La conversione viene effettuata nella parte di risposta rispetto al tipo del comando originale.
		if(!outPort.hasPreferredInterface())
			throw new IllegalStateException("Physical interface NOT defined");
		//Class<? extends Quantity> returnedQuantity = outPort.getPreferredInterface().getQuantity();
	
		// Creazione ReadCommand utilizzando i parametri definiti
		return new ReadCommand(super.getOriginalCommand().getCommandID(), super.getOriginalCommand().getTimestamp(), outPort, super.getOriginalCommand().getQuantity() /*returnedQuantity*/);
	}
}