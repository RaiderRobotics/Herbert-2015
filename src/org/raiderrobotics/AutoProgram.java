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

		System.out.println("got to the constructor");
	}

	void init(){
		inAutoZone = false;
		distEncoder.reset();
	}

	void setProgram(int program) {
		programUsed = program;
	}

	void run(){
		System.out.println("got to the run method");
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
		System.out.println("got to he autorecycle method");
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
				talon.set (talon.get() + TALONRAMPSPEED); //add 10% each time
			} else {
				talon.set(speed);
			}
		} else { //going negative
			if (speed < talon.get()) {
				talon.set (talon.get() - TALONRAMPSPEED); //subtract 10% each time
			} else {
				talon.set(speed);
			}
		}
	}

}