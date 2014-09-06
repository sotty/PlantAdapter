package utils;

import java.io.PrintStream;
import java.io.PrintWriter;

import plantadapter.IPlantCallbackReceiver;
import plantadapter.excpts.PlantAdapterException;
import plantadapter.results.Result;

/**
 * Semplice callback receiver che stampa i a video i risultati restituiti
 * o la stack trace di eventuali eccezioni.
 * @author JCC
 *
 */
public class PlantObserver implements IPlantCallbackReceiver
{
	private PrintWriter output;
	private PrintWriter error;
	
	public PlantObserver(PrintStream output, PrintStream error) {
		this.output = new PrintWriter(output);
		this.error = new PrintWriter(error);
	}
	
	@Override
	public synchronized void sendInput(Result result) {
		this.output.println(result.toString());
		this.output.flush();
	}

	@Override
	public synchronized void sendError(PlantAdapterException exception) {
		exception.printStackTrace(this.error);
		this.error.flush();
	}
}