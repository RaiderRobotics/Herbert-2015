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

	//TODO: Note to use ArmControl system do ArmControl.getInstance() to recover its instance
	//      then you can access the non-private functions in it.

	AutoProgram(Talon talonLeft, Talon talonRight, Encoder encoderA){
		talon1 = talonLeft;
		talon2 = talonRight;
		distEncoder = encoderA;
		armControl = ArmControl.getInstance();
		
		gyro = new Gyro( new AnalogInput(0));
		gyro.reset();
		gyro.setSensitivity(0.007);
	}
	

	void init(){
		inAutoZone = false;
		distEncoder.reset();
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
			break;
		case AUTO_MULTITOTE:
			autoMutitote();
			break;
		}
	}
	
	void autoTote(){
		armControl.armMode=armControl.armMode.MOVE_TO_REST;
		armControl.tick();
		tenKProgram();
	}

	void autoRecycle(){
		armControl.armMode=armControl.armMode.MOVE_TO_MIDDLE;
		armControl.tick();
		autoMove();
	}
	
	void autoMove(){
		double currentAngle = gyro.getAngle()%360;
		double offset = CircularOperation.offsetZero(currentAngle);
		double rightMotorFactor, leftMotorFactor;
		
		if(offset > 0){
			leftMotorFactor = 1;
			rightMotorFactor = 1.2;
		}else{
			leftMotorFactor = 1.2;
			rightMotorFactor = 1;
		}
		
		if(! inAutoZone){
			if(distEncoder.getDistance() < AUTO_ZONE_DISTANCE){
				rampToSpeed(talon1, AUTO_SPEED_FWD * leftMotorFactor);
				rampToSpeed(talon2, -1 * AUTO_SPEED_FWD * rightMotorFactor);
			}else{
				inAutoZone = true;
				distEncoder.reset();
				talon1.stopMotor();
				talon2.stopMotor();
			}	
		}else{
			if(distEncoder.getDistance() > AUTO_BACKUP_DISTANCE){
				talon1.set(-1 * AUTO_SPEED_BCK * leftMotorFactor);
				talon2.set(AUTO_SPEED_BCK * rightMotorFactor); 
			}else{
				talon1.stopMotor();
				talon2.stopMotor();	
			}
		}
	}
	
	void tenKProgram(){
		double currentAngle = gyro.getAngle()%360;
		double offset = CircularOperation.offsetZero(currentAngle);
		double rightMotorFactor, leftMotorFactor;
		
		if(offset > 0){
			leftMotorFactor = 1;
			rightMotorFactor = 1.2;
		}else{
			leftMotorFactor = 1.2;
			rightMotorFactor = 1;
		}
		if(distEncoder.getDistance() < 100){
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
