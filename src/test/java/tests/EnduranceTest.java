package tests;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import javax.measure.quantity.Quantity;
import javax.measure.quantity.Temperature;

import plantadapter.IPlantCallbackReceiver;
import plantadapter.PlantAdapter;
import plantadapter.commands.CommandsDiagnosticCommand;
import plantadapter.commands.DeviceCommand;
import plantadapter.commands.ReadCommand;
import plantadapter.commands.ScheduleSubscriptionCommand;
import plantadapter.commands.WriteCommand;
import plantadapter.excpts.CommandFailureException;
import plantadapter.excpts.PlantAdapterException;
import plantadapter.excpts.PlantConfigurationException;
import plantadapter.results.CommandsDiagnosticResult;
import plantadapter.results.ImmediateResult;
import plantadapter.results.Result;
import plantadapter.results.ScheduledResult;
import plantadapter.schedules.SystemScheduleDefinition;
import plantmodel.Endpoint;
import plants.MulePlant;
import quantities.DO;
import quantities.Flow;
import quantities.InformationAmount;
import quantities.NH4;
import quantities.NO3;
import quantities.ORP;
import quantities.PH;
import quantities.TSS;

import testbench.IPServerActivationThread;
import tests.model.ModelTest;

public class EnduranceTest {

	// DataTaker
	private static final String ip = "192.168.5.9";
	private static final int port = 7700;
	
	// Output Files
	private static final String errorFileName = "error.txt";
	private static final String diagnosticFileName = "log.txt";
	private static final String scheduleFileName = "sched.txt";
	private static final String immediateFileName = "cmd.txt";
	
	private static void setupSystem(PlantAdapter p) throws CommandFailureException {
		
		/* TODO Occorre dare la configurazione corretta anche A TUTTI I CANALI DELL'ADAM.
		 * Il comando � il seguente: 2SERIAL("\e{$01S0C3A3110\013}") (credo imposti la
		 * slew rate corretta, a 9600 - "31", e la corrente come formato dei dati - "10").
		 */
		
		// TODO Controllare i terminatori (forse gli \n sono di troppo).
		
		p.sendCommand
		(
				new WriteCommand
				(
						UUID.randomUUID().toString(),
						Endpoint.fromID("dt80", "ETHERNET_PORT"),
						new InformationAmount("/H\r\n".getBytes())
				)
		);
		
		// Impostazioni da dare alla porta seriale per il corretto funzionamento con l'ADAM
		// (NOTA: si suppone di usare la HOST_PORT - porta RS232 sul lato sinistro).
		
		p.sendCommand // PROFILE"HOST_PORT" "BPS"="9600"
		(
				new WriteCommand
				(
						UUID.randomUUID().toString(),
						Endpoint.fromID("dt80", "ETHERNET_PORT"),
						new InformationAmount("PROFILE\"HOST_PORT\" \"BPS\"=\"9600\"\r\n".getBytes())
				)
		);
		p.sendCommand // PROFILE"HOST_PORT" "DATA_BITS"="8"
		(
				new WriteCommand
				(
						UUID.randomUUID().toString(),
						Endpoint.fromID("dt80", "ETHERNET_PORT"),
						new InformationAmount("PROFILE\"HOST_PORT\" \"DATA_BITS\"=\"8\"\r\n".getBytes())
				)
		);
		p.sendCommand // PROFILE"HOST_PORT" "STOP_BITS"="1"
		(
				new WriteCommand
				(
						UUID.randomUUID().toString(),
						Endpoint.fromID("dt80", "ETHERNET_PORT"),
						new InformationAmount("PROFILE\"HOST_PORT\" \"STOP_BITS\"=\"1\"\r\n".getBytes())
				)
		);
		p.sendCommand // PROFILE"HOST_PORT" "PARITY"="NONE"
		(
				new WriteCommand
				(
						UUID.randomUUID().toString(),
						Endpoint.fromID("dt80", "ETHERNET_PORT"),
						new InformationAmount("PROFILE\"HOST_PORT\" \"PARITY\"=\"NONE\"\r\n".getBytes())
				)
		);
		p.sendCommand // PROFILE"HOST_PORT" "FUNCTION"="SERIAL"
		(
				new WriteCommand
				(
						UUID.randomUUID().toString(),
						Endpoint.fromID("dt80", "ETHERNET_PORT"),
						new InformationAmount("PROFILE\"HOST_PORT\" \"FUNCTION\"=\"SERIAL\"\r\n".getBytes())
				)
		);
	}
	
	public static ReadCommand[] setupScheduleCommands() {
		ReadCommand[] commands = new ReadCommand[12];
		commands[0] = new ReadCommand("ORP_Anox",
				Endpoint.fromID("ORP_Anox", "source"), ORP.class);
		commands[1] = new ReadCommand("pH_Anox",
				Endpoint.fromID("pH_Anox", "source"), PH.class);
		commands[2] = new ReadCommand("NH4_Anox",
				Endpoint.fromID("NH4_Anox", "source"), NH4.class);
		commands[3] = new ReadCommand("Temperature",
				Endpoint.fromID("Temperature", "source"), Temperature.class);
		commands[4] = new ReadCommand("NO3_Anox",
				Endpoint.fromID("NO3_Anox", "source"), NO3.class);
		commands[5] = new ReadCommand("ORP_Ox",
				Endpoint.fromID("ORP_Ox", "source"), ORP.class);
		commands[6] = new ReadCommand("Oxigen",
				Endpoint.fromID("Oxigen", "source"), DO.class);
		commands[7] = new ReadCommand("pH_Ox",
				Endpoint.fromID("pH_Ox", "source"), PH.class);
		commands[8] = new ReadCommand("TSS",
				Endpoint.fromID("TSS", "source"), TSS.class);
		commands[9] = new ReadCommand("NO3_Ox",
				Endpoint.fromID("NO3_Ox", "source"), NO3.class);
		commands[10] = new ReadCommand("Flow",
				Endpoint.fromID("Flow", "source"), Flow.class);
		commands[11] = new ReadCommand("NH4_Ox",
				Endpoint.fromID("NH4_Ox", "source"), NH4.class);
		return commands;
	}
	
	private static void pause(long timeout, long timespan) throws InterruptedException {
		for (long count = 0; count < timeout; count += timespan) {
			Thread.sleep(timespan);
		}
	}
	
	private static void setupSchedule(PlantAdapter p) throws CommandFailureException {
		
		List<DeviceCommand> scheduledCommands = new ArrayList<DeviceCommand>();
		
//		ReadCommand rdCmd = new ReadCommand(UUID.randomUUID().toString(), 
//				Device.fromID("oxigen"), Endpoint.fromID("oxigen", "source"), DO.class);
//		
//		scheduledCommands.add(rdCmd);
		
		scheduledCommands.addAll(Arrays.asList(setupScheduleCommands()));
		
		// TODO Verificare perch� non va nel caso seguente e correggere...
		
		/*
		// Definizione della Schedule
		p.sendCommand(new ScheduleDefinitionCommand(UUID.randomUUID().toString(), 
				"Martina", scheduledCommands.toArray(new DeviceCommand[0]), 
				new Date(), 5000));
		// Sottoscrizione della Schedule
		p.sendCommand(new ScheduleSubscriptionCommand(UUID.randomUUID().toString(), "Martina"));
		*/
		
		// TODO Verificare perch� non va nel caso precedente e correggere...
		
		// Definizione Schedule
		SystemScheduleDefinition schedule = ModelTest.createSchedule(); // TODO Invio comando definizione...
		
		// Sottoscrizione alla Schedule...
		ScheduleSubscriptionCommand subscription = new ScheduleSubscriptionCommand(UUID.randomUUID().toString(), schedule.getScheduleId());
		p.sendCommand(subscription);
	}
	
	private static void sendReadCommand(PlantAdapter p, Endpoint e, Class<? extends Quantity> quantity) throws CommandFailureException {
		p.sendCommand(
				new ReadCommand(
						UUID.randomUUID().toString(),
						e,
						quantity
				)
		);
	}

	private static void sendCommandsDiagnosticCommand(PlantAdapter p) throws CommandFailureException {
		p.sendCommand(
				new CommandsDiagnosticCommand(
						UUID.randomUUID().toString()
				)
		);
	}
	
	/**
	 * Sintassi di invocazione:
	 * 
	 * endurance-test ? per avere l'help
	 * 
	 * endurance-test [C] [D] [-M m] [-P port]
	 * 
	 * C control enable (default false)
	 * D debug enable (default false)
	 * n minuti (default 1)
	 * p porta di controllo (default 12345)
	 * 
	 * @param args
	 * @throws PlantConfigurationException 
	 */
	public static void main(String[] args) throws PlantConfigurationException, IOException {

		// TODO: rendere riutilizzabile con passaggio di nomeFile e Subject
		DiagnosticObserver observer = new DiagnosticObserver(errorFileName, diagnosticFileName, scheduleFileName, immediateFileName);
		
		boolean debug = true;
		boolean ctrl = true;
		
		int mins = 1;
		// porta di controllo dell'applicazione
		int ctrlPort = 12345;
		
		for (int i = 0; i < args.length; i++) {
			
			String arg =  args[i].trim().toUpperCase();
			
			if (arg.equals("?")) {
				System.out.println("Sintassi di invocazione:\n\n" +
						"endurance-test ? per avere l'help\n\n" +
						"endurance-test [C] [D] [-M m] [-P port]\n\n" +
						"C control enable (default false)" +
						"D debug enable (default false)" +
						"n minuti (default 1)" +
						"p porta di controllo (default 12345)");
				System.exit(0);
			}
			else if (arg.equals("D"))
				debug = true;
			else if (arg.startsWith("-"))
			{
				if (arg.equals("-M") || arg.equals("-P")) {
					try { 
						int tmp = Integer.parseInt(args[i+1]);
						
						if (arg.equals("-M"))
							mins = tmp;
						if (arg.equals("-P"))
							ctrlPort = tmp;
					}
					catch (Exception e) {
						System.out.println("Errore parsing valore intero " + args[i+1] + ", procedo con default.");
					}
				}
			}
		}
		
		/* Istanzio il server di controllo */
		
		ServerSocket ctrlSrv = new ServerSocket(ctrlPort);
		// la prima volta attendo a oltranza
		while(ctrl) {
			Socket cnnSock = ctrlSrv.accept();
			String command = new DataInputStream(cnnSock.getInputStream()).readUTF().trim().toUpperCase();
			if (command.equals("START")) {
				ctrlSrv.setSoTimeout(1000);
				cnnSock.close();
				break;
			}
			else if (command.equals("PAUSE")) {
				// attende nuova CONNESSIONE
			}
			else if (command.equals("STOP")) {
				cnnSock.close();
				System.exit(0);
			}
		}
		
		MulePlant model = null;
		PlantAdapter p = null;
		
		IPServerActivationThread s = null;
		
		//// NON � possibile utilizzare entrambi a causa delle collezioni statiche di Device ed Endpoint
		
		
		
		// DEBUG
		if (debug) {
			// esecuzione server su localhost:
			s = new IPServerActivationThread();
			s.start();
			model = new MulePlant(s.getServerInetAddres().getHostName(), s.getServerLocalPort());
		}
		else {
			// REMOTE
			model = new MulePlant(ip, port);
		}
		
		p = PlantAdapter.newInstance("Mule", model.getDevices(), observer);
		
		try {
			setupSystem(p);
			setupSchedule(p);
			
			Endpoint eDO = Endpoint.fromID("oxigen", "source");
			Class<? extends Quantity> qDO = DO.class;
			
			// Command Loop
			while(true) {
				sendReadCommand(p, eDO, qDO);
				
				// DEBUG
				if (debug) {
					// Invio risultati fittizi
					new PrintStream(s.getOutputStream()).print(
							"D,089401,\"29_03_12\",2012/05/19,14:07:00,0.000366,0;;*;0,0;063;4D46\r\n"
							+ "D,081044,\"ModelTest_2\",2012/05/19,14:07:00,0.000366,0;A,0,1,2,3,4,5,6,7,8,9,10,11,12;0151;4D46\r\n"
					);
				}
				
				sendCommandsDiagnosticCommand(p);

				try {
					for (long count = 0; count < mins * 60000; count += 60000) {
						Thread.sleep(60000);
						Socket cnnSock = ctrlSrv.accept();
						String command = new DataInputStream(cnnSock.getInputStream()).readUTF().trim().toUpperCase();
						if (command.equals("START")) {
							// continua
						}
						else if (command.equals("PAUSE")) {
							System.out.println("PAUSED");
						}
						else if (command.equals("STOP")) {
							cnnSock.close();
							System.exit(0);
						}
					}
					//Thread.sleep(mins * 60000);
				} catch (InterruptedException e) { }
				//observer.flush();
			}
		}
		catch (CommandFailureException e) {
			observer.sendError(e);
			e.printStackTrace();
		}
	}

	/*
	public class ScheduleObserver implements IInputCallbackReceiver {

		private String destination;
		
		
		
		private void wakeup() {
			
		}
		
		public void standby() {
			
		}
		
		@Override
		public void sendInput(Result result) {
			// TODO Auto-generated method stub
			
		}
	}
	*/
	
	public static class DiagnosticObserver implements IPlantCallbackReceiver {

		private static final int MAX_BUFFER_SIZE = 2;
		
		private String errorFileName;
		private String diagnosticFileName;
		private String scheduleFileName;
		private String immediateFileName;
		
		private Queue<CommandsDiagnosticResult> diagnosticResults = new LinkedList<CommandsDiagnosticResult>();
		private Queue<ImmediateResult> immediateResults = new LinkedList<ImmediateResult>();
		private Queue<ScheduledResult> scheduleResults = new LinkedList<ScheduledResult>();
		
		public DiagnosticObserver(
				String errorFileName,
				String diagnosticFileName,
				String scheduleFileName,
				String immediateFileName
		) {
			this.errorFileName = errorFileName;
			this.diagnosticFileName = diagnosticFileName;
			this.scheduleFileName = scheduleFileName;
			this.immediateFileName = immediateFileName;
		}
		
		/**
		 * Scrive sui file tutto ci� che � nei buffer
		 */
		public void flush() {
			for(Subject s : Subject.values())
				if(s != Subject.ERROR)
					flush(s);
		}
		
		/**
		 * Scrive su file tutto ci� che � nel buffer in base al soggetto passato
		 * @param subject
		 */
		private void flush(Subject subject) {
			try {
				BufferedWriter bw = getBufferedWriter(subject);
				Queue<? extends Result> q = getQueue(subject);
				
				PrintWriter pw = new PrintWriter(bw);
			
				System.err.println("Scrivendo file...");
				
				flush(q, pw);
				
				pw.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void flush(Queue<? extends Result> queue, PrintWriter out) {
			while(!queue.isEmpty()) {
				String s = queue.poll().toString();
				out.println(s);
				System.err.println("Scritto " + s);
			}
		}
		
		private BufferedWriter getBufferedWriter(Subject subject) throws IOException {
			switch (subject) {
				case IMMEDIATE_COMMAND: {
					FileWriter f = new FileWriter(immediateFileName, true);
					
					return new BufferedWriter(f);
				}
				case SCHEDULE: {
					FileWriter f = new FileWriter(scheduleFileName, true);
					
					return new BufferedWriter(f);
				}
				case DIAGNOSTIC_COMMAND: {
					FileWriter f = new FileWriter(diagnosticFileName, true);
					
					return new BufferedWriter(f);
				}
				case ERROR: {
					FileWriter f = new FileWriter(errorFileName, true);
					
					return new BufferedWriter(f);
				}
				default:
					throw new IllegalArgumentException();
			}
		}
		
		private Queue<? extends Result> getQueue(Subject subject) {
			switch (subject) {
				case IMMEDIATE_COMMAND:
					return immediateResults;
				case SCHEDULE:
					return scheduleResults;
				case DIAGNOSTIC_COMMAND:
					return diagnosticResults;
				default:
					throw new IllegalArgumentException();
			}
		}
		
		private Subject getSubject(Result result) {
			if(result instanceof ImmediateResult)
				return Subject.IMMEDIATE_COMMAND;
			if(result instanceof ScheduledResult)
				return Subject.SCHEDULE;
			if(result instanceof CommandsDiagnosticResult) // TODO: non omogeneo, modellare gerarchia DiagnosticCommand anche dalla parte dei risultati (controllo solo se � un DiagnosticCommand, in base al suo soggetto so cosa contiene)
				return Subject.DIAGNOSTIC_COMMAND;
			
			throw new UnsupportedOperationException();
		}
		
		@SuppressWarnings("unchecked") // checked in getSubject
		@Override
		public void sendInput(Result result) {
			Subject resultSubject = getSubject(result);
			Queue<?> q = getQueue(resultSubject);
			
			// se coda piena
			if(q.size() > MAX_BUFFER_SIZE) {
				flush(resultSubject);
			}
			
			// inserimento result in coda appropriata
			switch(resultSubject) {
				case DIAGNOSTIC_COMMAND: ((Queue<CommandsDiagnosticResult>)q).add((CommandsDiagnosticResult)result); break;
				case SCHEDULE: ((Queue<ScheduledResult>)q).add((ScheduledResult)result); break;
				case IMMEDIATE_COMMAND: ((Queue<ImmediateResult>)q).add((ImmediateResult)result); break;
			}
		}

		@Override
		public void sendError(PlantAdapterException exception) {
			// (si assume che gli errori siano sporadici, il file viene pertanto aperto e chiuso ogni volta)
			// apertura file errore
			try {
				BufferedWriter bw = getBufferedWriter(Subject.ERROR);
			
				PrintWriter pw = new PrintWriter(bw);

				// scrittura su log errore
				pw.println("Exception @ " + new Date());
				pw.println(exception);
				pw.println(exception.getStackTrace());
				pw.println("-----------------------------------------------------");
			
				// chiusura file errore
				pw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public enum Subject {
			DIAGNOSTIC_COMMAND,
			SCHEDULE,
			IMMEDIATE_COMMAND,
			ERROR
		}
	}
}
