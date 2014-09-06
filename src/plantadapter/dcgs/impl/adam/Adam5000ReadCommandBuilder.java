package plantadapter.dcgs.impl.adam;

import plantadapter.commands.PortCommand;
import plantadapter.commands.ReadCommand;
import plantadapter.dcgs.AbstractCommandBuilder;
import plantadapter.dcgs.IDeviceCommandBuilder;
import plantmodel.adam.Adam5000Device;

// TODO Per ora gestiti solamente comandi diretti alla porta di controllo, che vengono semplicemente
// rispediti indietro (fattorizzare la cosa per tutti i DCG, a livello pi� alto?).

public class Adam5000ReadCommandBuilder extends AbstractCommandBuilder<Adam5000Device, ReadCommand> implements IDeviceCommandBuilder {

	public Adam5000ReadCommandBuilder(Adam5000Device executorDevice, ReadCommand originalCommand) {
		super(executorDevice, originalCommand);
	}

	@Override
	public PortCommand buildCommand() {
		// TODO Solita necessit� di identificare l'endpoint di controllo...
		if (this.getOriginalCommand().getTargetPort().getID().equalsIgnoreCase("RS232")) {

			return new ReadCommand(super.getOriginalCommand().getCommandID(), super.getOriginalCommand().getTimestamp(), super.getOriginalCommand().getTargetPort(), super.getOriginalCommand().getQuantity() /*returnedQuantity*/);
		}
		throw new UnsupportedOperationException("Adam5000ReadCommandBuilder");
	}
}