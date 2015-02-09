package org.raiderrobotics;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.RobotDrive;
import static org.raiderrobotics.RobotMap.*;

public class DriveTrainGyro extends Gyro {
	private boolean isTurning = false;
	private double targetAngle = 0.0;

	//TODO: this should probably be fixed so that you can never make a second MyGyro and have two of them trying to control the same drive train.
	//make it a singleton method
	DriveTrainGyro(RobotDrive driveTrain, int channel) {
		super(channel);
		this.setSensitivity(GYRO_SENSITIVITY);
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
	}

	void orientYAxis() {
		//TODO: add working code
		/*
		isTurning = true;
		targetAngle = ... //the result of some complex calculation (that must, nevertheless, be easy to understand)
		 */
	}

	void turnPlus45() {
		isTurning = true;
		targetAngle = this.getAngle() + 45.0;		
	}

	void turnMinus45() {
		isTurning = true;
		targetAngle = this.getAngle() + 45.0;
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

	}

	boolean isTurning() { return isTurning; }
}
