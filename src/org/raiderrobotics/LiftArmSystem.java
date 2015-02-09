package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;

public class LiftArmSystem {

	//TODO: constants maybe?? should be moved to RobotMap when this class is working 
	final static int TALON3_CAN_ID = 3;
	final static int TALON4_CAN_ID = 4;
	final static double LIFTSPEED = 0.8;
	final static double RAMPRATE = 0.3;  //volts per second ?
			
	final static int POSITION_ONE_L = 500;	//number of encoder ticks to get to position 1
	final static int POSITION_ONE_R = 550;	//number of encoder ticks to get to position 1
	final static int POSITION_TOP_L = 700;	//number of encoder ticks to get to top position 
	final static int POSITION_TOP_R = 700;	//number of encoder ticks to get to top position 
	
	
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
		
		rightTalon.reverseOutput(true);
		leftTalon.reverseOutput(false);

	}

	//instance methods
	void moveToZero() {
		isMoving = true;
		//TODO: need to go down until hit switches
	}

	void moveToOne() {
		isMoving = true;
		leftTargetPos = POSITION_ONE_L;
		rightTargetPos = POSITION_ONE_R;
	}

	void moveToTop() {
		isMoving = true;
		leftTargetPos = POSITION_TOP_L;
		rightTargetPos = POSITION_TOP_R;
	}

	void stop() {
		leftTalon.disable(); //stopMotor() is deprecated and replaced by "disable()"
		rightTalon.disable();
	}

	void continueMoving() {
		
/*** I'm not sure what this is for ***		
		//Percentage at what the talons will move 
		//when one is going faster than the other one
		double rightCut = 0.95;
		double leftCut = 0.95;

		//Determining if one talon is moving faster than the other one 
		double rightMultiplier = Math.abs(rightTalon.getEncPosition()) > Math.abs(leftTalon.getEncPosition()) 
				? rightCut : 1;
		double leftMultiplier = Math.abs(leftTalon.getEncPosition()) > Math.abs(rightTalon.getEncPosition()) 
				? leftCut : 1;

		//Move the talons based on their determined speeds
		leftTalon.set(LIFTSPEED * leftMultiplier);
		rightTalon.set(LIFTSPEED * rightMultiplier);
****/
		
		double dl = leftTargetPos - leftTalon.getEncPosition(); //NOTE: this implies that the encoder is plugged into the TalonSRX
		double dr = rightTargetPos - rightTalon.getEncPosition();

		if (dl < 5.0) {
			leftTalon.set(0.0);
		} else {
			leftTalon.set(LIFTSPEED);		
		}
		
		if (dr < 5.0) {
			rightTalon.set(0.0);
		} else {
			rightTalon.set(LIFTSPEED);
		}

		if (leftTalon.get() == 0.0 && rightTalon.get() == 0.0) {
			isMoving = false;
		}
	}

	boolean isMoving() { return isMoving; }
}
