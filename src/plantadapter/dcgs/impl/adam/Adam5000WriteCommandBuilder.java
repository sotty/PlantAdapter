package plantadapter.dcgs.impl.adam;

import plantadapter.commands.PortCommand;
import plantadapter.commands.WriteCommand;
import plantadapter.dcgs.AbstractCommandBuilder;
import plantadapter.dcgs.IDeviceCommandBuilder;
import plantadapter.excpts.EndpointNotFoundException;
import plantadapter.utils.DeviceAmountConverter;

import plantmodel.DigitalConversionEndpointInterface;
import plantmodel.DigitalEndpointInterface;
import plantmodel.Endpoint;
import plantmodel.adam.Adam5000Device;
import plantmodel.adam.Adam5000EngineeringUnits;
import plantmodel.datatypes.ASCIIString;

import quantities.DigitalAmount;
import quantities.DigitalQuantity;
import quantities.IAmount;
import quantities.IDigitalQuantity;
import quantities.InformationAmount;

public class Adam5000WriteCommandBuilder extends AbstractCommandBuilder<Adam5000Device, WriteCommand> implements IDeviceCommandBuilder {

	public Adam5000WriteCommandBuilder(Adam5000Device executorDevice, WriteCommand originalCommand) {
		super(executorDevice, originalCommand);
	}

	@Override
	public PortCommand buildCommand() {
		
		try{
			/// Creazione contenuto comando che deve essere ricevuto
			//// Identificazione Endpoint su cui Scrivere
			Endpoint outPort = super.getOriginalCommand().getTargetPort();
			//\\ Identificazione Endpoint su cui Scrivere
			//// Valore contenuto in comando
			IAmount value = null;
			//\\ Valore contenuto in comando
			//// Identificazione Dati HW da Endpoint
			
			// TODO Fattorizzare in un DCG pi� astratto la gestione di comandi diretti a EP di controllo... (?)
			// Nota: appare comunque abbastanza naturale metterlo nel WriteCommandBuilder --> stabilire se si tratta
			// veramente di una politica generale...
			if (super.getOriginalCommand().getTargetPort().getID().equalsIgnoreCase("RS232")) { // TODO Sempre solito problema delle "porte di controllo"...
				// Inoltro Amount
				return new WriteCommand(super.getOriginalCommand().getCommandID(), super.getOriginalCommand().getTargetPort(), super.getOriginalCommand().getValue());
			}
	
			// TODO Active (?)
			
			/*
			if(!super.getExecutorDevice().isActive(outPort))
				throw new IllegalStateException(outPort + " is not active");
			*/
			
			// TODO Per il momento in uso ID endpoint in quanto contenente le stesse info nel formato voluto dall'ADAM
	//		int HWNetworkNumber = this.device.getHWNetworkAddress();
	//		int slotNumber = this.device.getSlotNumber(outPort);
	//		int channelNumber = this.device.getChannelNumber(outPort);
				
			//\\ Identificazione Dati HW da Endpoint
			
			//// Applicazione Scaling SW
			// TODO occorre modificiare Command per permettere il passaggio dei comandi originali da cui deriva
			//\\ Applicazione Scaling SW
			
			//// Gestione Endpoint Digitali
			if(super.getOriginalCommand().getValue().getQuantity().equals(IDigitalQuantity.class)){
				DigitalEndpointInterface ei = (DigitalEndpointInterface) outPort.getEndpointInterfacesForQuantity(IDigitalQuantity.class)[0];
				
				if(ei.getClass().equals(DigitalConversionEndpointInterface.class))
					// effettuo conversione in IAmount analogico
					value = (((DigitalConversionEndpointInterface)ei).convertAmount((DigitalAmount)super.getOriginalCommand().getValue()));
				else
					// endpoint digitali supportati nativamente dal dispositivo
					// CASO NON SUPPORTATO DA ADAM5000
					throw new UnsupportedOperationException();
			}
			//\\ Gestione Endpoint Digitali
			
			//// Determinazione Contenuto Comando
			StringBuilder sb = new StringBuilder();
			sb.append(super.getExecutorDevice().getEndpointDeviceID(outPort));
			
			if(value == null)
				value = super.getOriginalCommand().getValue();
			
			// Valore IAmount Comando
			value = DeviceAmountConverter.toDeviceAmount(outPort, value);
			
			// Conversione in EngineeringUnits (formato standard ADAM)
			sb.append(Adam5000EngineeringUnits.toString(value));
			
			// Adam vuole Ritorno del carrello per terminare il comando
			sb.append("\r");
			//\\ Determinazione Contenuto Comando
			
			// Valore Comando in uscita
			value = new InformationAmount(new ASCIIString(sb.toString()).toByteArray());
			//\ Creazione contenuto comando che deve essere ricevuto
			
			return new WriteCommand(super.getOriginalCommand().getCommandID(), super.getOriginalCommand().getTimestamp(), super.getExecutorDevice().getEndpointById("RS232"), value);
		} 
		catch (EndpointNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}