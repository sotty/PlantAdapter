package plantadapter;

import java.util.HashMap;
import java.util.Map;

import plantadapter.CommandLogger.CommandInfo.CommandState;
import plantadapter.commands.Command;
import plantadapter.excpts.PlantAdapterException;
import plantadapter.results.Result;

public class CommandLogger {
	
	private Map<Command, LoggedCommandInfo> log;
	
	public CommandLogger() {
		this.log = new HashMap<Command, LoggedCommandInfo>();
	}
	
	// Public Instance Methods
	
	public synchronized void log(Command cmd, ICallbackReceiver... observers) {
		if (getUserCommand(cmd) != null)
			throw new IllegalArgumentException("Comando gi� presente."); // TODO
		this.log.put(cmd, new LoggedCommandInfo(observers));
	}
	
	public synchronized void unlog(Command cmd) {
		Command unlog = null;
		for(Command c : this.log.keySet())
			if(c.getCommandID().equals(cmd.getCommandID()))
				unlog = c;
		
		if(unlog != null)
			this.log.remove(unlog);
	}
	
	public Command[] getCommands() {
		return log.keySet().toArray(new Command[0]);
	}
	
	// Nota: importantissimo ricordare che questa associazione � al momento
	// basata sull'ID del comando (!).
	public Command getUserCommand(Command cmd) {
		for (Command c : this.log.keySet()) {
			if (c.getCommandID().equals(cmd.getCommandID())) {
				return c;
			}
		}
		return null;
	}
	
	public LoggedCommandInfo getCommandInfo(Command cmd) {
		
		LoggedCommandInfo info = this.log.get(this.getUserCommand(cmd));
		
		if (info == null)
			throw new IllegalArgumentException(); // TODO
		
		return info;
	}
	
	public IInputCallbackReceiver getInputCallbackReceiver(Command cmd) {
		return this.getCommandInfo(cmd).getInputCallbackReceiver();
	}
	
	public IErrorCallbackReceiver getErrorCallbackReceiver(Command cmd) {
		return this.getCommandInfo(cmd).getErrorCallbackReceiver();
	}
	
	public IPlantCallbackReceiver getPlantCallBackReceiver(Command cmd) {
		return this.getCommandInfo(cmd).getPlantCallbackReceiver();
	}
	
	public CommandState getState(Command c) { // TODO: rimuovere? ricavarsi CommandInfo per poi leggerne lo stato
		return log.get(c).getState();
	}
	
	public void setState(Command cmd, CommandInfo.CommandState state) { // TODO: rimuovere? ricavarsi CommandInfo per poi modificarne lo stato
		// TODO Eccezione se lo stato non � sensato per il tipo di comando indicato (usare enumerativi differenti?)
	}
	
	public static class CommandInfo {
		// TODO Notare che alcune cose hanno senso solo per alcuni tipi di comandi...
		public enum CommandState {
			/**
			 * Indica che il comando � stato riconosciuto come corretto ed aggiunto al log.
			 */
			ACCEPTED
			// TODO
		}
	}
	
	public class LoggedCommandInfo {
		
		private IInputCallbackReceiver inputReceiver;
		private IErrorCallbackReceiver errorReceiver;
		private ILogCallbackReceiver logReceiver;
		
		// TODO
		private IPlantCallbackReceiver plantReceiver;
		
		private CommandInfo.CommandState state; // TODO
		
		public CommandInfo.CommandState getState() {
			return state;
		}
		
		public void setState(CommandInfo.CommandState state) { // TODO
			this.state = state;
		}
		
		public LoggedCommandInfo(ICallbackReceiver[] receivers) {
			
			for (final ICallbackReceiver rec : receivers) {
				// TODO
				if (rec instanceof IPlantCallbackReceiver) {
					
					// TODO
					this.plantReceiver = (IPlantCallbackReceiver)rec;
					
					// Crea input e error receiver fittizi
					this.inputReceiver = new IInputCallbackReceiver() {
							@Override
							public void sendInput(Result result) {
								((IPlantCallbackReceiver)rec).sendInput(result);
							}
						};
					this.errorReceiver = new IErrorCallbackReceiver() {
						@Override
						public void sendError(PlantAdapterException exception) {
							((IPlantCallbackReceiver)rec).sendError(exception);
						}
					};
				}
				else if (rec instanceof IInputCallbackReceiver) {
					this.inputReceiver = (IInputCallbackReceiver)rec;
				}
				else if (rec instanceof IErrorCallbackReceiver) {
					this.errorReceiver = (IErrorCallbackReceiver)rec;
				}
				else if (rec instanceof ILogCallbackReceiver) {
					this.logReceiver = (ILogCallbackReceiver)rec;
				}
					
			}
			
			this.state = CommandInfo.CommandState.ACCEPTED;
		}
		
		// TODO
		public IPlantCallbackReceiver getPlantCallbackReceiver() {
			return this.plantReceiver;
		}
		
		public IInputCallbackReceiver getInputCallbackReceiver() {
			return this.inputReceiver;
		}

		public IErrorCallbackReceiver getErrorCallbackReceiver() {
			return this.errorReceiver;
		}
		
		public ILogCallbackReceiver getLogCallbackReceiver() {
			return this.logReceiver;
		}
	}
}