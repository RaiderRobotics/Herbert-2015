package org.raiderrobotics;

import edu.wpi.first.wpilibj.*;
import static org.raiderrobotics.RobotMap.*;

public class AutoProgram {
	Talon talon1, talon2;
	Encoder distEncoder;
	Gyro gyro;
	HallArmControl hallArmControl = null;
	BinArmSystem binArmSystem = null;
	
	int programUsed = AUTO_TOTE; //default
	boolean inAutoZone = false;
	double startingAngle = 0.0;
	long startingTime = 0L;
	
	// These are for use with the bin arm current sensor
	// The array size is how many samples we are taking, so 10 would be for the 
	//	last 10 samples (if 1 us per sample, then the last 10 us)
	final static int MAXSAMPLESARRAYSIZE = 1000;
	final static int DEFAULTSAMPLESARRAYSIZE = 25;			  // 25*20ms = 500  ms
	private int m_samplesArraySize = DEFAULTSAMPLESARRAYSIZE;
	private double[] m_currentSamplesArray;
	private int m_nextSampleIndex = 0;

	double startingCurrent = 0.0;
	CurrentMonitorStatus currentMonStatus = CurrentMonitorStatus.WAITING_TO_START;
	
	private enum CurrentMonitorStatus {
		WAITING_TO_START, MONITOR_CURRENT, BIN_ATTACHED
	}
	
	// A very basic timer for various timing things. 
	private long m_startTime = 0;
	// Saves the current time since boot
	void ResetTimer() {
		this.m_startTime = System.currentTimeMillis();
		return;
	}
	
	// Returns the elasped time (ms) since ResetTimer() was called
	long getElapsedMS() {
		long elapsedTime = System.currentTimeMillis() - this.m_startTime;
		// If you didn't call ResetTimer(), this value might be negative, so fix that
		if ( elapsedTime < 0 )
		{	// Oops. Didn't call reset
			this.ResetTimer();
			elapsedTime = 0;	// Return zero rather than a negative time
		}
		return elapsedTime;
	}
	// End of: very basic timer
	
	
	
	//TODO: Note to use ArmControl system do ArmControl.getInstance() to recover its instance
	//      then you can access the non-private functions in it.

	AutoProgram(Talon talonLeft, Talon talonRight, Encoder encoderA, Gyro gyroInput){
		talon1 = talonLeft;
		talon2 = talonRight;
		distEncoder = encoderA;
		hallArmControl = HallArmControl.getInstance();
		binArmSystem = BinArmSystem.getInstance();
		gyro = gyroInput;
		gyro.reset();
		gyro.setSensitivity(0.007);
		
		// Init array for low pass filter of current
		this.initSamplesArray(DEFAULTSAMPLESARRAYSIZE);
	}

	void init(){
		inAutoZone = false;
		distEncoder.reset();
		gyro.reset();
		startingAngle = gyro.getAngle();
		startingTime = System.currentTimeMillis();
	}

	void setProgram(int program) {
		programUsed = program;
	}

	/***** called repeatedly by autonomousPeriodic() ******/
	void run(){
		
		if (currentMonStatus != CurrentMonitorStatus.BIN_ATTACHED) measureCurrent();
		//System.out.println("II=" + binArmSystem.talonTwister.getOutputCurrent());
		
		switch(programUsed) {
		case AUTO_RECYCLE:
			autoRecycle();
			break;
		case AUTO_TOTE:
			autoTote();
			break;
		case AUTO_MULTITOTE:
			break;
		}
	}

	void autoTote(){
		//armControl.armMode=armControl.armMode.MOVE_TO_REST;
		//armControl.tick();
		autoMove();
	}
	
	void autoRecycle(){
		//armControl.armMode=armControl.armMode.MOVE_TO_MIDDLE;
		//armControl.tick();
		autoMove();
	}
	
	/* Drive Straight */
	void autoMove(){
		
		double Kp = 0.5; //0.3 works. Possibly a higher value 
		double currentAngle = gyro.getAngle();
//		double offset = CircularOperation.offsetZero(currentAngle);
		double rightMotorFactor, leftMotorFactor;
		
		/* use Gyro to drive straight. WOW This is NOT being used! */
		// the correction should be based on the size of the error
		//the left motor is not as powerful as the right
/*		if(currentAngle > startingAngle){
			leftMotorFactor = 1.6 + (currentAngle - startingAngle) * Kp;
			rightMotorFactor = 1.4;
		} else if (currentAngle < startingAngle) {
			leftMotorFactor = 1.6;
			rightMotorFactor = 1.4 + (currentAngle - startingAngle) * Kp;
		} else {
			leftMotorFactor = 1.4;
			rightMotorFactor = 1.4;
		}
*/
		
		/* use encoder to determine location of robot; when to stop */
		//TODO: the two speed factors have jsut been set to 1 and 0.8 by trial and error.
		if(! inAutoZone){
			if(distEncoder.getDistance() < AUTO_ZONE_DISTANCE){
				//rampToSpeed(talon1, AUTO_SPEED_FWD * leftMotorFactor);
				//rampToSpeed(talon2, -1 * AUTO_SPEED_FWD * rightMotorFactor);
				rampToSpeed(talon1, AUTO_SPEED_FWD * 1.0);
				rampToSpeed(talon2, -1 * AUTO_SPEED_FWD * 0.8);
				
			}else{
				inAutoZone = true;
				distEncoder.reset();
				talon1.stopMotor();
				talon2.stopMotor();
			}	
/*		}else{
			if(distEncoder.getDistance() > AUTO_BACKUP_DISTANCE){
				talon1.set(-1 * AUTO_SPEED_BCK * leftMotorFactor);
				talon2.set(AUTO_SPEED_BCK * rightMotorFactor); 
			}else{
				talon1.stopMotor();
				talon2.stopMotor();	
			}
*/		}
		switch (currentMonStatus) {
		case BIN_ATTACHED:
			//binArmSystem.talonTwister.set(0.0);
			binArmSystem.stopRotation();
			//TODO: raiseBIN
			//binArmSystem.talonPulley.set(0.20);
			binArmSystem.autoLiftToTop(0.50);
			break;
		default:
			//start the bin motor after WAITTIME
			if (System.currentTimeMillis() - startingTime > AUTO_WAITTIME) {
				binArmSystem.talonTwister.set(1.0);
			}
		}
		
		
	}

/*	
	void autoRecycle(){
		if(! inAutoZone){
			if(distEncoder.getDistance() < AUTO_ZONE_DISTANCE){
				rampToSpeed(talon1, AUTO_SPEED_FWD);
				rampToSpeed(talon2, -1 * AUTO_SPEED_FWD);
			}else{
				inAutoZone = true;
				distEncoder.reset();
				talon1.stopMotor();
				talon2.stopMotor();
			}	

		}else{
			if(distEncoder.getDistance() > AUTO_BACKUP_DISTANCE){
				talon1.set(-1 * AUTO_SPEED_BCK);
				talon2.set(AUTO_SPEED_BCK); 
			}else{
				talon1.stopMotor();
				talon2.stopMotor();	
			}
		}
	}
*/
	void autoMutitote(){ 
	}

/* We intended to rewrite using multiplication factor; 
* talon.set(talon.get() * 1.1); //It's a bit faster & better for low values. However, it needs a correction for speed=0.0
* We tried the mulplication method. It didn't work as intended. This one does. So let's leave it like this for now.
* The mutiplication code is underneath this, commented out.
*/
	public void rampToSpeed(Talon talon, double speed){
		if (speed >= 0.0) {	//ramp up to positive speed
			if (talon.get() < speed) {
				
				talon.set (talon.get() + TALONRAMPINCREMENT); //add 10% each time (now 5%)
			} else {
				talon.set(speed);
			}
		} else { //going negative
			if (speed < talon.get()) {
				talon.set (talon.get() - TALONRAMPINCREMENT); //subtract 10% each time (now 5%)
			} else {
				talon.set(speed);
			}
		}
	}
	
/*	
	//There are some problems with this code. The code above works.
	public void rampToSpeed(Talon talon, double speed) {
		if (speed >= 0.0) { //ramp up to positive speed
			if (talon.get() < speed)
				talon.set(talon.get() == 0 ? 0.1 : talon.get() * TALONRAMPSPEED); //multiply 10% each time (exponentially - 25*20ms)
			else
				talon.set(speed);
		} else { //going negative
			if (talon.get() > speed)
				talon.set(talon.get() == 0 ? -0.1 : talon.get() * TALONRAMPSPEED); //divide by 10% each time (exponentially - 25*20ms)
			else
				talon.set(speed);
		}
	}
	*/

	// This allocates and initilizes (or re-allocates, etc.) the samples array
	void initSamplesArray( int newArraySize ) {
		// check for stupid array size value (too big or too small)
		if ( ( newArraySize < 0 ) || ( newArraySize > MAXSAMPLESARRAYSIZE ) ) {
			newArraySize = DEFAULTSAMPLESARRAYSIZE;
		}	
		// The array size is sensible, now, so allocate (or re-allocate) the array
		this.m_samplesArraySize = newArraySize;
		this.m_currentSamplesArray = new double[this.m_samplesArraySize];
		
		// Does Java automatically place zeros (0.0) in these arrays? Only the Shadow knows...
		for ( int index = 0; index != this.m_samplesArraySize; index++ ) {
			this.m_currentSamplesArray[index] = 0.0;
		}
		// All done. Array is filled with zeros.
		// Set current "next add" index value to the start of the array
		this.m_nextSampleIndex = 0;
		return;
	}
	
	double getAverageCurrent() {
		double average = 0.0;
		for ( int index = 0; index != this.m_samplesArraySize; index++ ) {
			average += m_currentSamplesArray[index];
		}
		average = average / (double)this.m_samplesArraySize;
		return average;
	}
	
	void addSampleCurrent( double currentSample ) {
		this.m_currentSamplesArray[this.m_nextSampleIndex] = currentSample;
		// Move to next location in array
		this.m_nextSampleIndex++;
		// To far?
		if ( this.m_nextSampleIndex >= this.m_samplesArraySize ) {
			// Yup. So reset to start of array...
			this.m_nextSampleIndex = 0;
		}
		return;
	}
	
	
//	double curr1 = 0.0;
//	double curr2 = 0.0;
//	double curr3 = 0.0;
	// This is how long we wait until we start to monitor the current (ms)
	final long CURRENTMONITORSTARTDELAY = 500;	// in milliseconds
	// The threshold the bin motor current needs to be to assume it's engaged the recycle bin
	final double CURRENTCHANGETHRESHOLD = 1.25;  //in amps. This could also be changed to %
	// This is how long the current must be over the threshold before the bin arm engages. 
	final double CURRENTOVERTHRESHOLDWAITPERIOD = 500;	// in milliseconds
	
	// If the current drops below threshold, this is reset to the current time (resetting the timer, effectively)
	// If detla between this and current time is > CURRENTOVERTHRESHOLDWAITPERIOD, we start the bin
	// This is called all the time in auto. 
	// Each time, it will add an average current to the array (low pass filter)
	// If the average current (over that period) is more than some set value (i.e. the current has spiked 
	//	because the robot has 'caught' the recycle bin and is turning it on the ground), then a timer is started.
	// (Note: this 'timer' is really just a time stamp thing, but will return the elapsed time since reset)
	// If the current goes BELOW this value (which is very likely to happen), then the time is 'reset to zero'.
	// (Note: i.e. the current time is reset)
	// If the current stays ABOVE the threshold for more than some fixed time (the time we're going to wait to
	//	be 'sure' the motor has caught the recycle bin), then we do something sexy:
	//  -- start the bin arm lifting?
	//	-- turn off the bin arm twist motor?
	void measureCurrent() {
		long deltaTimeSinceBoot = System.currentTimeMillis() - startingTime;
		double motorCurrent = binArmSystem.talonTwister.getOutputCurrent();
		//DEBUG
		System.out.print("I=" + motorCurrent );
		System.out.println("\n AVE=" + this.getAverageCurrent());
		
		switch (currentMonStatus) {
		case WAITING_TO_START:
			// Waited long enough?
			if ( deltaTimeSinceBoot > CURRENTMONITORSTARTDELAY ) {
				// Change state to monitor current
				this.currentMonStatus = CurrentMonitorStatus.MONITOR_CURRENT;
				// Start the timer (used in the MONITOR_CURRENT part)
				this.ResetTimer();
			}
//			if (deltaT > 300 && deltaT <= 600) {	//0.3 to 0.6 sec
//				curr1 = motorCurrent;
//				
//			}
//			if (deltaT > 600 && deltaT <= 900) {	//0.6 to 0.9 sec
//				curr2 = motorCurrent; 
//			}
//			if (deltaT > 300 && deltaT <= 600) {	//0.9 to 01.2 sec
//				curr3 = motorCurrent;
//				
//				startingCurrent = (curr1 + curr2 + curr3) / 3.0;
//				currentMonStatus = Status.MONITOR_CURRENT;		
//			}
			break;
			
		case MONITOR_CURRENT:
			// Add current sample
			this.addSampleCurrent(motorCurrent);
			
			// Current high enough?
			if ( this.getAverageCurrent() < CURRENTCHANGETHRESHOLD )
			{	// No, so reset the timer to zero
				this.ResetTimer();
			}
			else
			{	// Yes current IS high enough, but have we waited long enough?
				if ( this.getElapsedMS() > CURRENTOVERTHRESHOLDWAITPERIOD )
				{	// Yes, so start the bin motor rising... (boom goes the dynamite!)
					// ***********************************************************
					this.currentMonStatus = CurrentMonitorStatus.BIN_ATTACHED;
					
					// Insert supah sexy bin lift code here
					// ***********************************************************
				}
			}
			
			//TODO: do we want three measurements here too?
//			if ( motorCurrent - startingCurrent > currentChangeThresh) {
//				currentMonStatus = Status.ATTACHED;
//			}
			break;
		default: 
			//nothing
			break;
		}
		return;
	}
}