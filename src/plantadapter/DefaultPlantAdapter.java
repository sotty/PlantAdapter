package plantadapter;

import plantadapter.commands.Command;
import plantadapter.communication.ISource;
import plantadapter.communication.impl.Mailbox;

import plantadapter.excpts.CommandFailureException;
import plantadapter.excpts.ConnectionNotFoundException;
import plantadapter.excpts.FullMailboxException;
import plantadapter.excpts.PlantConfigurationException;

import plantadapter.inputs.InputReceptionThread;
import plantadapter.inputs.InputRecognizerFactory;

import plantmodel.Device;

/**
 * <p>Questa implementazione di <code>PlantAdapter</code> prevede che il comando venga
 * depositato in una Maibox dall'utilizzatore, dopodich� la sua esecuzione � demandata
 * ad un thread separato interno al componente stesso.</p>
 * 
 * @author JCC
 *
 */
class DefaultPlantAdapter extends PlantAdapter {

	private final IPlant plant;
	private final Mailbox<Command> cmdMailbox;

	protected DefaultPlantAdapter(IPlant plant) throws PlantConfigurationException {

		this.plant = plant;
		
		// TODO Configuazione Factories...
	
		// Ricezione Risposte
		for (Device rootDevice : plant.getRootDevices()) { // TODO
			
			plant.getRequestMailboxFactory().newInstance(rootDevice); // TODO
			
			// TODO
			
			try {
				ISource<byte[]> inputSource = plant.getCommunicationManager().getInputSource(rootDevice.getHostConnections()[0]);
				new InputReceptionThread(inputSource, new InputRecognizerFactory(this).getInputRecognized(rootDevice)).start();
			}
			catch (ConnectionNotFoundException e) {
				throw new PlantConfigurationException();
			}
		}
		
		// TODO
		
		// Gestione Comandi
		this.cmdMailbox = new Mailbox<Command>();
		new CommandExecutionThread(this.cmdMailbox, new PlantCommandRecognizer(this), plant.getCommandLogger()).start();
		
		// TODO
	}
	
	// Nota: il fatto che alcuni moduli "reintroducano" comandi rende teoricamente possibile un deadlock
	// in caso di MEMORIA PIENA (la coda concettualmente non � limitata in ingresso).
	
	public void sendCommand(Command cmd, IPlantCallbackReceiver observer) throws CommandFailureException {
		/* TODO Gestione in caso in caso di coda piena? Definire quale deve essere
		 * la visione dell'utilizzatore. Definire la semantica di accesso e utilizzare 
		 * oggetto wrapper "CommandMailbox" che esponga solamente quanto necessario.
		 */
		try {
			this.plant.getCommandLogger().log(cmd, observer);
			this.cmdMailbox.put(cmd);
		}
		catch (FullMailboxException e) {
			throw new CommandFailureException(e);
		}
	}

	@Override
	public void sendCommand(Command command) throws CommandFailureException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPlant getPlantModel() {
		return this.plant;
	}

	/*
	@Override
	public void command(String xml, IPlantCallbackReceiver observer) throws CommandFailureException {
		try { this.sendCommand(Command.fromXML(XMLData.fromXML(xml).toDocument()), observer); } 
		catch (Exception e) {
			throw new CommandFailureException(""); // TODO
		}
	}

	@Override
	public void command(Document xml, IPlantCallbackReceiver observer) throws CommandFailureException {
		try { this.sendCommand(Command.fromXML(xml), observer); } 
		catch (Exception e) {
			throw new CommandFailureException(""); // TODO
		}
	}
	*/
}