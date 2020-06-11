package beam.events;

import java.util.Map;

import beam.transEnergySim.chargingInfrastructure.management.ChargingSiteSpatialGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

import beam.charging.vehicle.PlugInVehicleAgent;
import beam.transEnergySim.chargingInfrastructure.stationary.ChargingPlug;
import beam.transEnergySim.chargingInfrastructure.stationary.ChargingSite;

public class BeginChargingSessionEvent extends Event implements IdentifiableDecisionEvent {

	private PlugInVehicleAgent agent;
	private ChargingPlug plug;
	private ChargingSite site;
	private int decisionEventId;
	private double chargingKw;
	private double energyNeededForNextTrip;

	public static final String ATTRIBUTE_DECISION_EVENT_ID=DepartureChargingDecisionEvent.ATTRIBUTE_DECISION_EVENT_ID;
	public static final String ATTRIBUTE_PERSON = DepartureChargingDecisionEvent.ATTRIBUTE_PERSON;
	public static final String ATTRIBUTE_PLUG = "plug";
	public static final String ATTRIBUTE_SITE = "site";
	public static final String ATTRIBUTE_ENERGY_NEEDED = "kwhNeeded";

	public BeginChargingSessionEvent(double time, PlugInVehicleAgent agent, ChargingPlug plug, double chargingPowerInW) {
		super(time);
		this.agent = agent;
		this.plug = plug;
		this.site = plug.getChargingSite();
		this.chargingKw = chargingPowerInW/1000.0;
		this.energyNeededForNextTrip = agent.getNextLegTravelDistanceInMeters() - agent.getVehicleWithBattery().getRemainingRangeInMeters();
		this.energyNeededForNextTrip = agent.getVehicleType().equals("PHEV") || this.energyNeededForNextTrip <= 0.0 ? 0.0 :
				agent.getVehicleWithBattery().getRequiredEnergyInkWhToDriveDistance(this.energyNeededForNextTrip);
		this.setDecisionEventId(agent.getCurrentDecisionEventId());
	}

	@Override
	public String getEventType() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_PERSON, agent.getPersonId().toString());
		attributes.put(ATTRIBUTE_PLUG, plug.getId().toString());
		attributes.put(ATTRIBUTE_SITE, site.getId().toString());
		attributes.put(ATTRIBUTE_DECISION_EVENT_ID, Integer.toString(getDecisionEventId()));
		attributes.put(ATTRIBUTE_ENERGY_NEEDED, Double.toString(energyNeededForNextTrip));
		return attributes;
	}

	public ChargingPlug getPlug(){
		return this.plug;
	}

	public Id<Person> getPersonId() {
		return this.agent.getPersonId();
	}

	public int getDecisionEventId() {
		return decisionEventId;
	}

	private void setDecisionEventId(int decisionEventId) {
		this.decisionEventId = decisionEventId;
	}
	
	public double getChargingPowerInKw(){
		return chargingKw;
	}

	public int getNominalChargingLevel(){
		return this.plug.getChargingPlugType().getNominalLevel();
	}

	public ChargingSiteSpatialGroup getChargingSiteSpatialGroup(){
		return this.site.getChargingSiteSpatialGroup();
	}

	public String getSpatialGroup(){
		return this.plug.getChargingSite().getSpatialGroupName();
	}

	public String getSiteType(){
		return this.site.getSiteType();
	}

	public String getPlugType(){
		return this.plug.getChargingPlugType().getPlugTypeName();
	}
}