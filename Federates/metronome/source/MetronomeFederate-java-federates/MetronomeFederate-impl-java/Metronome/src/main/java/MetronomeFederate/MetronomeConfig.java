package MetronomeFederate;

import org.cpswt.config.FederateConfig;
import org.cpswt.config.FederateParameter;

public class MetronomeConfig extends FederateConfig {
	@FederateParameter
	public long starttime;
	
	@FederateParameter
	public long stoptime;
	
	@FederateParameter
	public double logicaltimesec;
	
	@FederateParameter
	public double ignoretil;

    @FederateParameter
    public String timezone;
}
