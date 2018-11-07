package MetronomeFederate;

import org.cpswt.config.FederateConfig;
import org.cpswt.config.FederateConfigParser;
import org.cpswt.hla.base.AdvanceTimeRequest;
import org.cpswt.utils.CpswtDefaults;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.cli.*;

/**
 * The Metronome type of federate for the federation designed in WebGME.
 *
 */
public class Metronome extends MetronomeBase {

    private final static Logger log = LogManager.getLogger(Metronome.class);

    private double currentTime = 0;
	private MetronomeConfig configuration;
	private long startTime;
	private long stopTime;
	private double logicalTimeSec;
	private double ignoreTil;
	private String timeZone;

    public Metronome(MetronomeConfig params) throws Exception {
        super(params);
        
        this.configuration = params;
    }
    
    private void sendSimTime(){
        SimTime simTime = create_SimTime();
        simTime.set_unixTimeStart(startTime);
        simTime.set_unixTimeStop(stopTime);
        simTime.set_timeScale(logicalTimeSec);
        simTime.set_ignoreUntil(ignoreTil);
        simTime.set_timeZone(timeZone);
        simTime.sendInteraction(getLRC(), currentTime + getLookAhead());
        
        log.info(
        	"curentTime: " + currentTime
			+ ", startTime: " + startTime 
			+ ", ignoreTil: " + ignoreTil 
			+ ", logicalTimeSec: " + logicalTimeSec 
			+ ", stopTime: " + stopTime
			+ ", timeZone: " + timeZone
			);
    }
    
    private void sendSimTimeRO(){
        SimTime simTime = create_SimTime();
        simTime.set_unixTimeStart(startTime);
        simTime.set_unixTimeStop(stopTime);
        simTime.set_timeScale(logicalTimeSec);
        simTime.set_ignoreUntil(ignoreTil);
        simTime.set_timeZone(timeZone);
        simTime.sendInteraction(getLRC());
        
        log.info(
            "curentTime: " + currentTime
            + ", startTime: " + startTime 
            + ", ignoreTil: " + ignoreTil 
            + ", logicalTimeSec: " + logicalTimeSec 
            + ", stopTime: " + stopTime
            + ", timeZone: " + timeZone
            );
    }

    private void execute() throws Exception {
        if(super.isLateJoiner()) {
            log.info("turning off time regulation (late joiner)");
            currentTime = super.getLBTS() - super.getLookAhead();
            super.disableTimeRegulation();
        }

        /////////////////////////////////////////////
        // TODO perform basic initialization below //
        /////////////////////////////////////////////
    	startTime = configuration.starttime;
    	stopTime = configuration.stoptime;
    	logicalTimeSec = configuration.logicaltimesec;
    	ignoreTil = configuration.ignoretil;
    	timeZone = configuration.timezone;

        AdvanceTimeRequest atr = new AdvanceTimeRequest(currentTime);
        putAdvanceTimeRequest(atr);

        if(!super.isLateJoiner()) {
            log.info("waiting on readyToPopulate...");
            readyToPopulate();
            log.info("...synchronized on readyToPopulate");
        }
        
        sendSimTimeRO();


        ///////////////////////////////////////////////////////////////////////
        // Call CheckReceivedSubscriptions(<message>) here to receive
        // subscriptions published before the first time step.
        ///////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////
        // TODO perform initialization that depends on other federates below //
        ///////////////////////////////////////////////////////////////////////

        if(!super.isLateJoiner()) {
            log.info("waiting on readyToRun...");
            readyToRun();
            log.info("...synchronized on readyToRun");
        }

        startAdvanceTimeThread();
        log.info("started logical time progression");

        while (!exitCondition) {
            atr.requestSyncStart();
            enteredTimeGrantedState();

            sendSimTime();

            ////////////////////////////////////////////////////////////////////////////////////////
            // TODO break here if ready to resign and break out of while loop
            ////////////////////////////////////////////////////////////////////////////////////////


            if (!exitCondition) {
                currentTime += super.getStepSize();
                AdvanceTimeRequest newATR = new AdvanceTimeRequest(currentTime);
                putAdvanceTimeRequest(newATR);
                atr.requestSyncEnd();
                atr = newATR;
            }
        }

        // call exitGracefully to shut down federate
        exitGracefully();

        ////////////////////////////////////////////////////////////////////////////////////////
        // TODO Perform whatever cleanups needed before exiting the app
        ////////////////////////////////////////////////////////////////////////////////////////
    }

    public static void main(String[] args) {
        try {
        	
            FederateConfigParser federateConfigParser = new FederateConfigParser();
            MetronomeConfig federateConfig = federateConfigParser.parseArgs(args, MetronomeConfig.class);
            Metronome federate = new Metronome(federateConfig);
            federate.execute();
            log.info("Done.");
            System.exit(0);
        } catch (Exception e) {
            log.error("There was a problem executing the Metronome federate: {}", e.getMessage());
            log.error(e);
            System.exit(1);
        }
    }
}
