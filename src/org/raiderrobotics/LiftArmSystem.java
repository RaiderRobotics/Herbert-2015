package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;

public class LiftArmSystem {

	//TODO: constants maybe?? should be moved to RobotMap when this class is working 
	final static int TALON3_CAN_ID = 3;
	final static int TALON4_CAN_ID = 4;
	final static double L_LIFTSPEED = 0.8;
	final static double R_LIFTSPEED = 0.8;
	final static double RAMPRATE = 0.3;  //volts per second ?
			
	final static int POSITION_ONE_L = 4000;	//number of encoder ticks to get to position 1
	final static int POSITION_ONE_R = 4000;	//number of encoder ticks to get to position 1
	//FIXME: get correct numbers
	final static int POSITION_TOP_L = 4700;	//number of encoder ticks to get to top position 
	final static int POSITION_TOP_R = 4700;	//number of encoder ticks to get to top position 
	
	
	private CANTalon leftTalon = new CANTalon(TALON3_CAN_ID);
	private CANTalon rightTalon = new CANTalon(TALON4_CAN_ID);

	private boolean isMoving = false;
	private int leftTargetPos;
	private int rightTargetPos;

	//constructor -- this should be a singleton pattern so you can't make more than one object
	LiftArmSystem() {

		//configure both talons
		leftTalon.enableControl();
		leftTalon.set(0);
		leftTalon.changeControlMode(ControlMode.PercentVbus);
//TODO: test this
		//leftTalon.setVoltageRampRate(RAMPRATE);

		rightTalon.enableControl();
		rightTalon.set(0);
		rightTalon.changeControlMode(ControlMode.PercentVbus);
//TODO: test this
		//rightTalon.setVoltageRampRate(RAMPRATE);

	}

	//instance methods
	void moveToZero() {
		isMoving = true; //will not work
		//TODO: need to go down until hit switches
	}

	void moveToOne() {
		isMoving = true; //use encoders to go somewhere
		leftTargetPos = POSITION_ONE_L;
		rightTargetPos = POSITION_ONE_R;
	}

	void moveToTop() {
		isMoving = true;
		leftTargetPos = POSITION_TOP_L;
		rightTargetPos = POSITION_TOP_R;
	}

	void stop() {
		isMoving = false;
		leftTalon.set(0.0);
		rightTalon.set(0.0);
	}

	void continueMoving() {
		
		double dl = leftTargetPos - leftTalon.getEncPosition(); //NOTE: this implies that the encoder is plugged into the TalonSRX
		double dr = rightTargetPos - rightTalon.getEncPosition();
		
		// Assume we are still moving, so they aren't at the end point, yet
		boolean bLeftInPosition = false;
		boolean bRightInPosition = false;
		
//		//check how close to final position both are.
//		if (Math.abs(dl) < 5.0 && Math.abs(dr) < 5.0) {
//			isMoving = false;
//			return;
//		}
		//TODO see if it wobbles.  It it wobbles the imbalance increases
		if (dl < 15.0) {
			//set a boolean to true
			leftTalon.set(0.0);
			bLeftInPosition = true;
		} else {
			leftTalon.set(L_LIFTSPEED);		
		}
		
		if (dr < 15.0) {
			rightTalon.set(0.0);
			bRightInPosition = true;
		} else {
			rightTalon.set(R_LIFTSPEED);
		}
		
		// If both arms are in position, then they aren't moving
		if ( bLeftInPosition && bRightInPosition )
		{
			isMoving = false;
		}
		
		//Old code. Do not compare to zero. Check to see if motors are stopped.
//		if (leftTalon.get() == 0.0 && rightTalon.get() == 0.0) {
//			isMoving = false;
//		}
	
	}

	boolean isMoving() { return isMoving; }
}
