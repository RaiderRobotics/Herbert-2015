//this is so confusing
package org.raiderrobotics;


import edu.wpi.first.wpilibj.*;
import static org.raiderrobotics.RobotMap.*;
import org.raiderrobotics.utils.CircularOperation;

public class AutoProgram {
	Talon talon1, talon2;
	Gyro gyro;
	Encoder distEncoder;
	int programUsed = AUTO_TOTE; //default
	ArmControl armControl;
	boolean inAutoZone = false;
	double startingAngle = 0.0;

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
	}
	

	void init(){
		inAutoZone = false;
		distEncoder.reset();
		gyro.reset();
		startingAngle = gyro.getAngle();
	}

	void setProgram(int program) {
		programUsed = program;
	}

	void run(){
		switch(programUsed) {
		case AUTO_RECYCLE:
			autoRecycle();
			break;
		case AUTO_TOTE:
			autoTote();
			break;
		case AUTO_MULTITOTE:
			autoMutitote();
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
	
	void autoMove(){
		//drive straight.
		double Kp = 0.5; //0.3 works. Possibly a higher value
         
		double currentAngle = gyro.getAngle();
//		double offset = CircularOperation.offsetZero(currentAngle);
		double rightMotorFactor, leftMotorFactor;
		
		
		// the correction should be based on the size of the error
		//the left motor is not as powerful as the right
		if(currentAngle > startingAngle){
			leftMotorFactor = 1.6 + (currentAngle - startingAngle) * Kp;
			rightMotorFactor = 1.4;
		} else if (currentAngle < startingAngle) {
			leftMotorFactor = 1.6;
			rightMotorFactor = 1.4 + (currentAngle - startingAngle) * Kp;
		} else {
			leftMotorFactor = 1.4;
			rightMotorFactor = 1.4;
		}
	

//		leftMotorFactor = 1.5 - (currentAngle - startingAngle) * Kp;
//		rightMotorFactor = 1.0;
		
		System.out.println("diff: " + (currentAngle - startingAngle) + "\tS=" + startingAngle + " \tC=" + currentAngle);
		//System.out.println("Corr: " + (currentAngle - startingAngle) * Kp);
		
		if(! inAutoZone){
			if(distEncoder.getDistance() > AUTO_ZONE_DISTANCE){
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
	}
	
	void tenKProgram(){
		double currentAngle = gyro.getAngle()%360;
		double offset = CircularOperation.offsetZero(currentAngle);
		double rightMotorFactor, leftMotorFactor;
		
		if(offset > 0){
			leftMotorFactor = 0.8;
			rightMotorFactor = 1.2;
		}else{
			leftMotorFactor = 1.2;
			rightMotorFactor = 0.8;
		}
		if(distEncoder.getDistance() > -1000){
			rampToSpeed(talon1, AUTO_SPEED_FWD * leftMotorFactor);
			rampToSpeed(talon2, -1 * AUTO_SPEED_FWD * rightMotorFactor);
		}else{
			distEncoder.reset();
			talon1.stopMotor();
			talon2.stopMotor();
		}	

	}
	
	
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
