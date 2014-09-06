package plantadapter.communication;

public abstract class ExecutionThread<T> extends Thread {
	
	private ISource<T> src;
	private ISink<T> sink;
	
	public ExecutionThread(ISource<T> src, ISink<T> sink) {
		this.src = src;
		this.sink = sink;
	}
	
	/**
	 * 
	 * @param e L'eccezione sollevata
	 * @param item L'oggetto ricevuto dalla sorgente (null se l'eccezione � stata scatenata da ISource)
	 */
	protected abstract void exceptionsHandler(Exception e, T item);
	
	@Override
	public void run() {
		while (true) 
		{	
			T message = null;	
			try 
			{
				message = this.src.get();
				this.sink.put(message);
			}
			catch (Exception e) {
				this.exceptionsHandler(e, message);
				// TODO Dovrebbe farlo la sottoclasse
				System.err.println(e.getMessage() + " " + message);
				e.printStackTrace();
			}
		}
	}
}