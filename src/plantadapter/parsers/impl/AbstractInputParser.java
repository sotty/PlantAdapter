package plantadapter.parsers.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.measure.quantity.DataAmount;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import plantadapter.commands.DeviceCommand;
import plantadapter.commands.ReadCommand;
import plantadapter.commands.dev.IInputMask;
import plantadapter.inputs.MaskFormatException;
import plantadapter.parsers.IInputParser;
import plantadapter.parsers.InputParserFactory;
import plantadapter.results.SingleResult;
import plantadapter.utils.LinearScalingTransformation;
import plantadapter.utils.ScalingTransformation;
import plantmodel.AnalogueEndpointInterface;
import plantmodel.Device;
import plantmodel.Endpoint;
import plantmodel.IDeviceIOData;
import plantmodel.IEndpointInterface;
import quantities.IDigitalQuantity;
import quantities.InformationAmount;

abstract class AbstractInputParser implements IInputParser {

	// Protected Static Methods
	
	protected static Unit<? extends Quantity> getResponseMeasurementUnit(Device currentDevice, ReadCommand rc) { // TODO Metodi con analoga politica...
		// Proprio Endpoint connesso a quello indicato nel comando
		Endpoint myEp = currentDevice.getConnectedEndpoints(rc.getTargetPort())[0]; // TODO Pu� non essere vero in generale (?)
		// Comando relativo all'interfaccia fisica con cui � impostato l'Endpoint
		if (myEp.hasPreferredInterface() &&  myEp.getPreferredInterface().getQuantity().equals(rc.getQuantity())) {
			return myEp.getPreferredInterface().getUnit();
		}
		else {
			IEndpointInterface[] myEis = myEp.getLogicalInterfacesForQuantity(rc.getQuantity());
			// Se non � definita un'interfaccia logica ad hoc, crea uno scaling in base alle impostazioni dell'altro Endpoint
			if (myEis.length == 0) {
				IEndpointInterface[] otherEis = rc.getTargetPort().getEndpointInterfacesForQuantity(rc.getQuantity());
				return otherEis[0].getUnit(); // TODO Ipotizzo sia unica...
			}
			else {
				// Se � definita un'interfaccia, ottengo la giusta unit� di misura...
				return myEis[0].getUnit(); // TODO Ipotizzo sia unica...
			}
		}
	}
	
	protected static ScalingTransformation getScalingTransformation(Device currentDevice, ReadCommand rc) { // TODO Metodi con analoga politica...
		Class<? extends Quantity> q = rc.getQuantity();
		if(q.equals(IDigitalQuantity.class) || q.equals(DataAmount.class))
			throw new UnsupportedOperationException();
		
		// Proprio Endpoint connesso a quello indicato nel comando
		Endpoint myEp = currentDevice.getConnectedEndpoints(rc.getTargetPort())[0]; // TODO Pu� non essere vero in generale (?)
		// Comando relativo all'interfaccia fisica con cui � impostato l'Endpoint
		if (myEp.hasPreferredInterface() && myEp.getPreferredInterface().getQuantity().equals(rc.getQuantity())) { // TODO Prevista possibilit� assenza preferredInterface...
			return LinearScalingTransformation.IDENTITY;
		}
		else {
			IEndpointInterface[] myEis = myEp.getLogicalInterfacesForQuantity(rc.getQuantity());
			// Se non � definita un'interfaccia logica ad hoc, crea uno scaling in base alle impostazioni dell'altro Endpoint
			if (myEis.length == 0) {
				AnalogueEndpointInterface[] otherEis = (AnalogueEndpointInterface[])rc.getTargetPort().getEndpointInterfacesForQuantity(rc.getQuantity()); // CAST possibile in quanto scalingTransformation sono possibli solo per AnalogueQuantities
				return otherEis[0].getScalingTransformation().invert(); // TODO Ipotizzo sia unica...
			}
			else {
				// Se � definita un'interfaccia si presume che il dispositivo sia gi� impostato per far restituire il valore nel giusto formato
				return LinearScalingTransformation.IDENTITY;
			}
		}
	}
	
	// Instance Fields
	
	private Device device;

	public AbstractInputParser(Device device) {
		this.device = device;
	}
	
	// Template Methods
	
	protected abstract IDeviceIOData convertToDeviceIODataType(IDeviceIOData input);
	// In generale il formato di un campo potrebbe dipendere anche dal particolare comando che
	// lo ha generato, dunque � opportuno passare anche la maschera a questo metodo.
	protected abstract IDeviceIOData[] tokenizeInput(IDeviceIOData input, IInputMask mask);
	protected abstract SingleResult parse(ReadCommand cmd, IDeviceIOData token);

	// Accessors
	
	public Device getDevice() {
		return this.device;
	}
	
	// IInputParser
	
	@Override
	public final SingleResult[] parse(IInputMask mask, IDeviceIOData input) throws MaskFormatException {
		if (mask.asMask().getSourceDevice() != this.device)
			throw new IllegalStateException(); // TODO Passato input generato da un Device che non � quello associato!!!
		if (!input.getClass().equals(device.getIODataType()))
			input = this.convertToDeviceIODataType(input);
		// Risultati da restituire
		List<SingleResult> results = new ArrayList<SingleResult>();
		
		IDeviceIOData[] tokens = this.tokenizeInput(input, mask);
		if (tokens.length != mask.asMask().getSubMasksCount()) {
			throw new IllegalStateException(); // TODO La mashera non corrisponde all'input ricevuto!!!	
		}
		int tokenIndex = 0;
		// Esamina tutte le maschere, se si tratta di comandi aggiunge il risultato alla lista, altrimenti delega ad un altro parser...
		for (IInputMask subMask : mask.asMask()) {
			if (subMask.isCommand()) { 
				SingleResult res = this.parse(subMask.asCommand(), tokens[tokenIndex]);
				// TODO Convenzione temporanea: se l risultato � null significa che il parser per qualche motivo ha deciso di ignorarlo...
				if (res != null) results.add(res);
				else {
					// TODO Necessaria add seguita da remove perch� altrimenti al momento della toArray() non carica la classe...
					results.add(new SingleResult(new InformationAmount(new byte[0]), new DeviceCommand("", this.device) {}));
					results.remove(0);
				}
			}
			else 
			{
				results.addAll(Arrays.asList(InputParserFactory.getInputParser(subMask.asMask().getSourceDevice()).parse(subMask, tokens[tokenIndex])));
			}
			tokenIndex++;
		}
		return results.toArray(new SingleResult[0]);
	}
}