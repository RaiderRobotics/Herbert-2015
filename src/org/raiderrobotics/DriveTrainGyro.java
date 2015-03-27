package org.raiderrobotics;

import org.raiderrobotics.utils.CircularOperation;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.RobotDrive;
import static org.raiderrobotics.RobotMap.*;

public class DriveTrainGyro extends Gyro {
	private boolean isTurning = false;
	private double targetAngle = 0.0;
	
	RobotDrive driveTrain;
	

	//TODO: this should probably be fixed so that you can never make a second MyGyro and have two of them trying to control the same drive train.
	//make it a singleton method
	DriveTrainGyro(RobotDrive driveTrain, int channel) {
		super(channel);
		this.setSensitivity(GYRO_SENSITIVITY);
		this.driveTrain = driveTrain;
	}

	DriveTrainGyro(RobotDrive driveTrain, AnalogInput channel) {
		super(channel);
	}

	void orientXAxis() {
		//TODO: add working code
		/*
		 isTurning = true;
		 targetAngle = ... //the result of some complex calculation (that must, nevertheless, be easy to understand)
		 */
		double currentAngle = this.getAngle();
		isTurning = true;
		
		//if in Quadrant 1 or 3 
		if(CircularOperation.getQuadrant(currentAngle) == 1 || CircularOperation.getQuadrant(currentAngle) == 4){
			targetAngle = 270;
		}
		else if(CircularOperation.getQuadrant(currentAngle) == 2 || CircularOperation.getQuadrant(currentAngle) == 3){
			targetAngle = 90;
		}else{
			if(currentAngle == 270){
				targetAngle = 90;
			}else if(currentAngle == 90){
				targetAngle = 270;
			}else{
				isTurning = false;
			}
		}
		continueTurning();
	}

	void orientYAxis() {
		//TODO: add working code
		/*
		isTurning = true;
		targetAngle = ... //the result of some complex calculation (that must, nevertheless, be easy to understand)
		 */
		double currentAngle = this.getAngle();
		isTurning = true;
		
		//if in Quadrant 1 or 3 
		if(CircularOperation.getQuadrant(currentAngle) == 1 || CircularOperation.getQuadrant(currentAngle) == 2){
			targetAngle = 0;
		}
		else if(CircularOperation.getQuadrant(currentAngle) == 3 || CircularOperation.getQuadrant(currentAngle) == 4){
			targetAngle = 180;
		}else{
			if(currentAngle == 0){
				targetAngle = 180;
			}else if(currentAngle == 180){
				targetAngle = 0;
			}else{
				isTurning = false;
			}
		}
		
		continueTurning();
	}

	void turnPlus45() {
		isTurning = true;
		targetAngle = this.getAngle() + 45.0;	
		continueTurning();
	}

	void turnMinus45() {
		isTurning = true;
		targetAngle = this.getAngle() - 45.0;
		continueTurning();
	}

	void cancelTurning() {
		isTurning = false;
	}

	void continueTurning() {
		//1. set motors to turn speed
		//driveTrain. (  ) 
		//TODO: do we use arcadeDrive() or drive() ? or set the motors ourselves?
		//TODO: do we need to ramp up the motors if they were stopped? 

		//2. check if desired angle is reached
		//if so, set isTurning = false;
		
		if(!isTurning){ //cancelled 
			return;
		}
		
		double currentAngle = this.getAngle();
    	double offset = CircularOperation.offsetCostume(currentAngle, targetAngle);
    	//turnSpeed = (-joystick.getThrottle() + 1) / 2; //The throttle goes from -1 to 1, so we need to make it go from 0 to 1
    	
    	if(Math.abs(offset) < 5){
    		driveTrain.drive(0, 0);
    		isTurning = false; 
    	}else{
    		 if(offset > 0){
    			 driveTrain.drive(-1*0.5, 1); //turn right (aka clockwise)
    		 }else{
    			 driveTrain.drive(1*0.5, 1); //turn left (aka counter clockwise)
    		 }
    	}

	}

	boolean isTurning() { return isTurning; }
}
