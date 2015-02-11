package org.raiderrobotics;

import edu.wpi.first.wpilibj.*;
import static org.raiderrobotics.RobotMap.*;

public class AutoProgram {
	Talon talon1, talon2;
	Encoder distEncoder;
	int programUsed = AUTO_RECYCLE; //default

	boolean inAutoZone = false;

	AutoProgram(Talon talonLeft, Talon talonRight, Encoder encoderA){
		talon1 = talonLeft;
		talon2 = talonRight;
		distEncoder = encoderA;
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
			break;
		}
	}


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

	//TODO: test this 
	public void rampToSpeed(Talon talon, double speed){
		if (speed >= 0.0) {	//ramp up to positive speed
			if (talon.get() < speed) {
				//TODO: rewrite using multiplication factor; 
				//talon.set(talon.get() * 1.1); //It's a bit faster & better for low values. However, it needs a zero.
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