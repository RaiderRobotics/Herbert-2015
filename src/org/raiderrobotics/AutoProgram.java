package org.raiderrobotics;

import edu.wpi.first.wpilibj.*;
import static org.raiderrobotics.RobotMap.*;

public class AutoProgram {
	Talon talon1, talon2;
	Encoder distEncoder;
	Gyro gyro;
	ArmControl armControl;

	int programUsed = AUTO_TOTE; //default
	boolean inAutoZone = false;
	double startingAngle = 0.0;
	long startTime = 0;
	CANTalon talonTwister; //create this again here. It is also in BinArmSystem.

	/* Sample code for DIP switches and LEDs to control autonomous program   */
	DigitalInput MXP10 = new DigitalInput(10);
	DigitalInput MXP11 = new DigitalInput(11);
	DigitalInput MXP12 = new DigitalInput(12);
	DigitalInput MXP13 = new DigitalInput(13);

	DigitalOutput MXP14 = new DigitalOutput (14);
	DigitalOutput MXP15 = new DigitalOutput (15);
	DigitalOutput MXP16 = new DigitalOutput (16);
	DigitalOutput MXP17 = new DigitalOutput (17);

	//TODO: Note to use ArmControl system do ArmControl.getInstance() to recover its instance
	//      then you can access the non-private functions in it.

	AutoProgram(Talon talonLeft, Talon talonRight, Encoder encoderA, Gyro gyroInput){
		talon1 = talonLeft;
		talon2 = talonRight;
		distEncoder = encoderA;
		armControl = ArmControl.getInstance();
		gyro = gyroInput;
		gyro.reset();
		gyro.setSensitivity(0.007);
		talonTwister = new CANTalon(TALON_TWISTER_CAN_ID);
	}

	void init(){

		inAutoZone = false;
		distEncoder.reset();
		gyro.reset();
		startingAngle = gyro.getAngle();
		startTime = System.currentTimeMillis();
		/* testing multiple program selection via MXP port  */
		//turn off LEDs
		MXP14.set(false);
		MXP15.set(false);
		MXP16.set(false);
		MXP17.set(false);
		//turn on LED corresponding to DIP switch
		if (!MXP10.get())  {
			MXP14.set(true);
			//autoprogram.set(……);
		} else if (!MXP11.get())  {
			MXP15.set(true);
			//autoprogram.set(……);
		} else if (!MXP12.get())  {
			MXP16.set(true);
			//autoprogram.set(……);
		} else if (!MXP13.get())  {			
			MXP17.set(true);
			//autoprogram.set(……);
		} 

	}

	void setProgram(int program) {
		programUsed = program;
	}

	void run(){
		switch(programUsed) {
		case AUTO_RECYCLE:
			//autoRecycle();  DISABLE FOR DEBUG
			break;
		case AUTO_TOTE:
			//autoTote();  DEBUG
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

		//start the bin motor after WAITTIME
		if (System.currentTimeMillis() - startTime > AUTO_WAITTIME) {
			talonTwister.set(1.0);
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
}
