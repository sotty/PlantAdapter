package plantadapter.cmdgen;

import plantadapter.CommandLogger;

import plantadapter.cmdgen.adapters.ICommandAdapter;
import plantadapter.cmdgen.adapters.ReadCommandAdapter;
import plantadapter.cmdgen.adapters.TransactionCommandAdapter;
import plantadapter.cmdgen.adapters.WriteCommandAdapter;
import plantadapter.commands.DeviceCommand;
import plantadapter.commands.PortCommand;
import plantadapter.commands.ReadCommand;
import plantadapter.commands.TransactionCommand;
import plantadapter.commands.WriteCommand;

import plantadapter.dcgs.DeviceCommandGeneratorFactory;
import plantadapter.dcgs.IDeviceCommandGenerator;

import plantadapter.excpts.DeviceNotFoundException;
import plantadapter.excpts.InvalidCommandException;
import plantmodel.Endpoint;

public class DeviceCommandGenerationController {

	private final CommandLogger commandLogger;
	
	private final DeviceCommandTreeBuilder deviceCommandTreeBuilder;
	private final DeviceCommandGeneratorFactory dcgFactory;
	
	public DeviceCommandGenerationController(CommandLogger commandLogger) { 
		this.commandLogger = commandLogger;
		this.deviceCommandTreeBuilder = new DeviceCommandTreeBuilder();
		this.dcgFactory = new DeviceCommandGeneratorFactory();
	}

	public IDeviceCommandTree[] getCommandTrees(DeviceCommand[] commands) {
		
		this.deviceCommandTreeBuilder.clear();
		
		// importante adattare anche i primi comandi in modo da gestire il caso
		// di TransactionCommand (richiedono di adattare tutti i comandi interni)
		adaptCommands(commands);
		
		// Ripartizione Comandi in DeviceCommandGenerationList opportune
		DeviceCommandGenerationList[] cmdls = DeviceCommandGenerationList.define(commands);
		
		// Creazione DependenceSolver
		DependenceSolver dependenceSolver = new DependenceSolver(cmdls);
		// Risoluzione Comandi
		while(!ended(cmdls)){
			// Per ogni DeviceCommandGenerationList
			for(DeviceCommandGenerationList cmdl : cmdls){
				// Se DeviceCommandGenerationList da risolvere � privo di Dipendenze
				if(cmdl.getCommandGeneratorDevice() != null && !dependenceSolver.hasDependence(cmdl)){
					// Risolvo DeviceCommandGenerationList
					try 
					{
						// Risoluzione DeviceCommandGenerationList (comandi con Device "risolutore" associato in comune)
						IDeviceCommandGenerator dcg = this.dcgFactory.getDeviceCommandGenerator(cmdl.getCommandGeneratorDevice());
						IAggregatedDeviceCommand[] solvedCommands = dcg.generateCommands(cmdl);
						
						// uso di adapter per garantire il mantenimento dell'identit� tra oggetti e aggiungendo la possibilit� di modificare
						// gli endpoint di destinazione
						adaptAggregatedDeviceCommand(solvedCommands);
						
						// definisce quali DCG gestiranno i singoli comandi ottenuti
						this.updateDestinations(solvedCommands);
						
						// Ampliamento Alberi dei Comandi con i Comandi risolti
						// nota: l'albero contiene TUTTI i comandi generati, gli algoritmi devono tenerne conto 
						// per poter recupare dagli alberi informazioni corrette (es. maschere)
						for(IAggregatedDeviceCommand solvedCommand : solvedCommands) {
							this.deviceCommandTreeBuilder.add(solvedCommand);
						}

						// Aggiornamento DeviceCommandGenerationLists
						cmdls = DeviceCommandGenerationList.update(cmdls, solvedCommands);
						// Risolve le dipendenze (eliminazione delle dipendenze per il dispositivo risolto)
						dependenceSolver.update(cmdl);
					} 
					catch (DeviceNotFoundException e) {
						// TODO
						this.commandLogger.getErrorCallbackReceiver(cmdl.getCommands()[0]).sendError(e); // TODO
						// Permette uscita dal ciclo while()
						cmdls = new DeviceCommandGenerationList[0];
						break;
					} 
					catch (InvalidCommandException e) {
						// TODO
						this.commandLogger.getErrorCallbackReceiver(cmdl.getCommands()[0]).sendError(e); // TODO
						// Permette uscita dal ciclo while()
						cmdls = new DeviceCommandGenerationList[0];
						break;
					}
				}
			}
		}
		return this.deviceCommandTreeBuilder.getTrees();
	}
	
	// Metodo pubblico utilizzato da chiunque abbia la necessit� di accorpare
	// tutti i comandi presenti in un array.
	public static void adaptCommands(DeviceCommand... commands) {
		for (int i = 0; i < commands.length; i++) {
			
			// in questo caso devo creare un NUOVO ADAPTER (in modo da avere due istanze
			// diverse, che contenga lo stesso comando del precedente...
			if (commands[i] instanceof ICommandAdapter) {
				
				PortCommand[] tmp = new PortCommand[] { ((ICommandAdapter)commands[i]).getAdaptedCommand() }; // TODO Sistema bene ipotesi sui tipi...
				// adatto newCmd e memorizzo il nuovo adapter in tmp
				adaptCommands(tmp);
				
				commands[i] = tmp[0];
				continue;
			}
			
			if(commands[i] instanceof WriteCommand)
				commands[i] = new WriteCommandAdapter((WriteCommand)commands[i]);
			else if(commands[i] instanceof ReadCommand)
				commands[i] = new ReadCommandAdapter((ReadCommand)commands[i]);
			else if (commands[i] instanceof TransactionCommand)
				commands[i] = new TransactionCommandAdapter((TransactionCommand)commands[i]);
			else 
				throw new UnsupportedOperationException("DeviceCommandGenerationController.adaptCommands()");
		}
	}
	
	// Nota: siccome i primi comandi possono essere "adattati", � possibile che
	// in ingresso a questo metodo arrivino gi� dei comandi adattati.
	private void adaptAggregatedDeviceCommand(IAggregatedDeviceCommand[] newCmds) {
		for(int i = 0; i < newCmds.length; i++)
		{
			PortCommand newCmd = newCmds[i].getSolvedDeviceCommand();
			
			// in questo caso devo creare un NUOVO ADAPTER (in modo da avere due istanze
			// diverse, che contenga lo stesso comando del precedente...
			if (newCmd instanceof ICommandAdapter) {
				
				PortCommand[] tmp = new PortCommand[] { ((ICommandAdapter) newCmd).getAdaptedCommand() };
				// adatto newCmd e memorizzo il nuovo adapter in tmp
				adaptCommands(tmp);
				adaptCommands(newCmds[i].getAggregatedCommands());
				
				newCmds[i] = new AggregatedDeviceCommandImpl(
						newCmds[i].getAggregatedCommands(),
						tmp[0]
					);
				
				continue;
			}
			
			if(newCmd instanceof WriteCommand)
				newCmds[i] = new AggregatedDeviceCommandImpl(
								newCmds[i].getAggregatedCommands(),
								new WriteCommandAdapter((WriteCommand)newCmd)
							);
			else if(newCmd instanceof ReadCommand)
				newCmds[i] = new AggregatedDeviceCommandImpl(
								newCmds[i].getAggregatedCommands(),
								new ReadCommandAdapter((ReadCommand)newCmd)
							);
			
			else if (newCmd instanceof TransactionCommand) {
				newCmds[i] = new AggregatedDeviceCommandImpl(
						newCmds[i].getAggregatedCommands(),
						new TransactionCommandAdapter((TransactionCommand)newCmd)
						);
			}
			else throw new UnsupportedOperationException("DeviceCommandGenerationController.adaptAggregatedDeviceCommand()");
		}
	}

	/**
	 * Imposta l'endpoint di destinazione (e quindi anche il device) che deve risolvere il comando
	 * @param cmdl
	 */
	/* TODO: prendere in considerazione l'idea di effettuarlo tramite il pattern strategy
	 * inserendo nel DeviceCommandGenerationController una classe esterna adibita al "routing".
	 * 
	 * Il DeviceCommandGenerationController non sarebbe pi� dipendente dai tipi di comandi (WriteCommand, ReadCommand, etc).
	 */
	private void updateDestinations(IAggregatedDeviceCommand[] newCmds) {
		for(int i = 0; i < newCmds.length; i++)
		{
			ICommandAdapter newCmd = (ICommandAdapter)newCmds[i].getSolvedDeviceCommand();
			PortCommand sourcePortCmd = newCmds[i].getSolvedDeviceCommand();
			Endpoint e = sourcePortCmd.getTargetPort();
			// Se � connesso a null significa che non c'� un DCG di livello superiore,
			// dunque lascio l'endpoint invariato (importante per evitare di ritrovari con
			// endpoint/device a null.
			e = e.getConnectionsAsSlave()[0].getMasterEndpoint() == null ? e : e.getConnectionsAsSlave()[0].getMasterEndpoint();
			newCmd.setEndpoint(e);
		}
	}

	/**
	 * Controllo se sono stati gestiti completamente tutti i <code>DeviceCommand</code>.
	 * @param cmds Elenco di tutte le <code>DeviceCommandGenerationList</code> contenenti i <code>DeviceCommand</code> da inviare.
	 * @return true se tutte le richieste sono state gestite (� possibile inviare i comandi generati), false se vi sono ancora <code>DeviceCommandGenerationList</code> da risolvere.
	 */
	private boolean ended(DeviceCommandGenerationList[] cmds) {
		// TODO Controllo che tutti comandi abbiano deviceID == null
		boolean end = true;
		for(DeviceCommandGenerationList d : cmds){
			if(d.getCommandGeneratorDevice() != null) // NOT(nessuna altra operazione da compiere prima di inoltrare i comandi finali)
			{
			 	return false;
			}
		}
		return end;
	}
}