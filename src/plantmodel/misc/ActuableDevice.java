package plantmodel.misc;

import plantadapter.annotations.DeviceInfo;
import plantadapter.dcgs.impl.ActuableDeviceCommandGenerator;
import plantmodel.Device;
import plantmodel.Endpoint;
import plantmodel.AnalogueEndpointInterface;

/**
 * Generico dispositivo (mono-endpoint) su cui � possibile compiere attuazioni
 */
@DeviceInfo
(
	deviceCommandGeneratorClass = ActuableDeviceCommandGenerator.class
)
public class ActuableDevice extends Device {

	private static Endpoint[] setup(AnalogueEndpointInterface physicalQuantityInterface, AnalogueEndpointInterface logicalQuantityInterface) {
		Endpoint source = new Endpoint("CMD_PORT", physicalQuantityInterface);
		source.addLogicalInterface(logicalQuantityInterface);
		return new Endpoint[] { source };
	}
	
	private static Endpoint[] setup(AnalogueEndpointInterface physicalQuantityInterface) {
		Endpoint source = new Endpoint("CMD_PORT", physicalQuantityInterface);
		return new Endpoint[] { source };
	}
	
	public ActuableDevice(String id, String manufacturer, String model, String series, String serialNumber, AnalogueEndpointInterface physicalQuantityInterface, AnalogueEndpointInterface logicalQuantityInterface) {
		super(id, "Actuable", manufacturer, model, series, serialNumber, setup(physicalQuantityInterface, logicalQuantityInterface), null);
		// TODO Auto-generated constructor stub
	}
	
	public ActuableDevice(String id, String manufacturer, String model, String series, String serialNumber, AnalogueEndpointInterface physicalQuantityInterface) {
		super(id, "Actuable", manufacturer, model, series, serialNumber, setup(physicalQuantityInterface), null);
		// TODO Auto-generated constructor stub
	}
	
}
