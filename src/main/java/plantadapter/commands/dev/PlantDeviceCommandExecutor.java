package plantadapter.commands.dev;

import plantadapter.IPlant;
import plantadapter.PlantAdapter;

import plantadapter.commands.Command;
import plantadapter.commands.DeviceCommand;

import plantadapter.communication.ISink;
import plantadapter.communication.impl.Mailbox;

import plantadapter.excpts.FullMailboxException;

public class PlantDeviceCommandExecutor implements ISink<Command> {
	
	private Mailbox<DeviceCommand> cmdMailbox;
	
	public PlantDeviceCommandExecutor(PlantAdapter plant) {
		this.cmdMailbox = new Mailbox<DeviceCommand>();
		new DeviceCommandExecutionThread(this.cmdMailbox, new DeviceCommandExecutorImpl(plant.getPlantModel())).start();
	}

	@Override
	public void put(Command cmd) throws Exception {
		
		if (!(cmd instanceof DeviceCommand))
			throw new IllegalArgumentException(); // TODO
		
		DeviceCommand devCmd = (DeviceCommand)cmd;
		
		// Inserisce il dato nella Mailbox
		try {
			this.cmdMailbox.put(devCmd);
		} 
		catch (FullMailboxException e) {
			// TODO Invio errore all'utilizzatore Nota: necessario CommandLogger...
		}
	}
}