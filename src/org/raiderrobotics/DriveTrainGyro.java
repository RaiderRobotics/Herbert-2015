package org.raiderrobotics;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.RobotDrive;
import static org.raiderrobotics.RobotMap.*;

public class DriveTrainGyro extends Gyro {
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
	void orientZero() {
		targetAngle = 0.0;
	}

	void orient90() {
		targetAngle = 90.0;
	}

	void orient180() {
		targetAngle = 180.0;
	}

	void orient270() {
		targetAngle = 270.0;
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
		double gyroRenorm;
		gyroRenorm = this.getAngle() - gyroStartingAngle;
		gyroRenorm -= targetAngle; //-starting??
		gyroRenorm = gyroRenorm % 360;

		//System.out.print(gyro + " ");
		if (gyroRenorm > 0) {
			if (gyroRenorm < 180) {
				rotate(-1);	//to the left (CCW)
			} else {
				rotate(+1);
			}
		}
		if (gyroRenorm < 0) {
			if (gyroRenorm < -180) {
				rotate(-1);
			} else {
				rotate(+1);
			}
		}
		if (gyroRenorm == 0) isTurning = false;
	}
	void rotate (int dir) {
		if (!isTurning) {
			driveTrain.arcadeDrive(0, 0, false);
			return;
		}
		if (dir == +1) {
			driveTrain.arcadeDrive(0, 0.7, false);
		}
		if (dir == -1) {
			driveTrain.arcadeDrive(0, -0.7, false);
		}

	}

	boolean isTurning() { return isTurning; }
}
