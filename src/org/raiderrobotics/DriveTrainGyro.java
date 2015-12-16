package org.raiderrobotics;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.RobotDrive;
import static org.raiderrobotics.RobotMap.*;

public class DriveTrainGyro extends Gyro {
	
	private final double GYROAUTOSPEED = 0.2;  //this is the turning speed of the robot when usign the gyro to reorient itself
											//it should later be increased to a large number (0.9?) and then use PID
	private boolean isTurning = false;
	private double gyroStartingAngle = 0.0;  //there is another startingAngle variable in AutoProgram.java. Just ignore it for testing this code.
	private double targetAngle = 0.0;  //relative to starting angle which is set when the gyro is reset
	private RobotDrive driveTrain;

	//TODO: this should probably be fixed so that you can never make a second MyGyro and have two of them trying to control the same drive train.
	//make it a singleton method ... UNLESS ... you want TWO gyros and average them to get better results.

	DriveTrainGyro(RobotDrive driveTrain, int channel) {
		super(channel);
		this.driveTrain = driveTrain;
		this.setSensitivity(GYRO_SENSITIVITY);
	}

	DriveTrainGyro(RobotDrive driveTrain, AnalogInput channel) {
		super(channel);
		this.driveTrain = driveTrain;
	}

	//set robot to turn to 0 (straight ahead)
	void orient(double angle) {
		targetAngle = angle;
		isTurning = true;
	}

	/*
	 void turnPlus45() {
		isTurning = true;
		targetAngle = this.getAngle() + 45.0;		
	}

	void turnMinus45() {
		isTurning = true;
		targetAngle = this.getAngle() - 45.0;
	}
	 */

	void cancelTurning() {
		isTurning = false;
	}

	void continueTurning() {
		//1. set motors to turn speed
		//driveTrain. (  ) 
		//TODO: do we use arcadeDrive() or drive() ? or set the motors ourselves?
		//TODO: do we need to ramp up the motors if they were stopped? 
		double angleToTurnTo;
		angleToTurnTo = this.getAngle() - gyroStartingAngle;
		angleToTurnTo = angleToTurnTo - targetAngle; //-starting??
		angleToTurnTo = angleToTurnTo % 360;

		//System.out.print(gyro + " ");
		if (angleToTurnTo > 0) {
			if (angleToTurnTo < 180) {
				rotate(-1);	//to the left (CCW)
			} else {
				rotate(+1);
			}
		}
		if (angleToTurnTo < 0) {
			if (angleToTurnTo < -180) {
				rotate(-1);	
			} else {
				rotate(+1);
			}
		}
		//stop when within 5 degrees of target
		//TODO: use PID to slow to angle		
		if (Math.abs(angleToTurnTo) < 8) isTurning = false;
		
	}
	
	void rotate (int dir) {
		if (!isTurning) {
			driveTrain.arcadeDrive(0, 0, false);
			return;
		}
		
		if (dir == +1) {
			driveTrain.arcadeDrive(0, GYROAUTOSPEED, false);
		}
		if (dir == -1) {
			driveTrain.arcadeDrive(0, -GYROAUTOSPEED, false);
		}

	}

	boolean isTurning() { return isTurning; }
}
