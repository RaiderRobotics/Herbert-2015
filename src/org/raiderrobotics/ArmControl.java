package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DigitalInput;

public class ArmControl {

	//TODO: may be moved to RobotMap when this class is working
	final static int TALON3_CAN_ID = 3;
	final static int TALON4_CAN_ID = 4;
	//NOTE: positive speeds move the arms up. However, the encoders are recording more and more negative numbers as they go up.
	final static double L_LIFTSPEED = 0.8;
	final static double R_LIFTSPEED = 0.79; //must be slightly slower than left one		
	final static double POSITION_ONE_L = -3500.0;	//number of encoder ticks to get to position 1
	final static double POSITION_ONE_R = -3500.0;	//Note that the encoder goes more negative as it goes up.
	//FIXME: get correct numbers
	final static double POSITION_TOP_L = -5500.0;	//number of encoder ticks to get to top position 
	final static double POSITION_TOP_R = -5500.0;
	
	
	private CANTalon leftTalon = new CANTalon(TALON3_CAN_ID);
	private CANTalon rightTalon = new CANTalon(TALON4_CAN_ID);
	
	//for this switch, false = open = no signal from arm; true = closed. If the sensor is replaced this may be reversed.
	private DigitalInput leftSwitch = new DigitalInput(4); 
	private DigitalInput rightSwitch = new DigitalInput(5); 
	private boolean isMoving = false;
	private boolean isZeroing = false;
	private double leftTargetPos;
	private double rightTargetPos;

	//Constructor -- this should be a singleton pattern so you can't make more than one object
	//TODO: if we use the joystick to control the arms, then we need to pass the joystick object here, and so we might as well pass all of the buttons here.
	//      we'll let the drive team tell us if they want joytick control over the arms
	ArmControl() {

		//configure both talons
		leftTalon.enableControl();
		leftTalon.set(0);
		leftTalon.changeControlMode(ControlMode.PercentVbus);
		//TODO: //leftTalon.setVoltageRampRate(RAMPRATE);	//make into a utility function in RobotMap

		rightTalon.enableControl();
		rightTalon.set(0);
		rightTalon.changeControlMode(ControlMode.PercentVbus);
		//TODO: //rightTalon.setVoltageRampRate(RAMPRATE);
	}

	//instance methods
	
	boolean isMoving() { return isMoving; }
	
	void moveToZero() {
		isMoving = true;
		isZeroing = true;
	}

	void moveToOne() {
		isMoving = true; //use encoders to go somewhere
		isZeroing = false;
		leftTargetPos = POSITION_ONE_L;
		rightTargetPos = POSITION_ONE_R;
	}

	void moveToTop() {
		isMoving = true;
		isZeroing = false;
		leftTargetPos = POSITION_TOP_L;
		rightTargetPos = POSITION_TOP_R;
	}

	void stop() {
		isMoving = false;
		isZeroing = false;
		leftTalon.set(0.0);
		rightTalon.set(0.0);
	}

	/* All that this method does is to call the correct arm moving method 
	 * depending on whether we are moving to zero or to a set position.
	 */
	void continueMoving() {
		if (isZeroing) {
			zeroAndCalibrate();
			return;
		}
		moveToPosition();
	}
	
	/* This method moves the arms down until they "hit" the sensor (come close enough to trigger it)
	 * The speed of -0.4 is a bit slow, so they are now moving at -0.6.
	 * It might be best to move faster and then slow down when we're within 500 ticks of zero. 
	 * This wouldn't work the first time though as the encoders could be reading anything.
	 * 
	 * So: implement a state machine (using enum?) instead of a lot of booleans
	 */
	private void zeroAndCalibrate() {
		//Move one arm down until it hits the sensor
		if (!leftSwitch.get()) leftTalon.set(-0.6);	//was -0.4 
		else leftTalon.set(0.0);
		
		if (!rightSwitch.get()) rightTalon.set(-0.6);
		else rightTalon.set(0.0);
		
		if (rightSwitch.get() && leftSwitch.get()) {
			isZeroing = false;
			isMoving = false;
			leftTalon.setPosition(0);
			rightTalon.setPosition(0);
		}
		
	}
	
	//TODO: if we increase the arm speed above the current speed of 0.8, 
	//		then we may need to increase the range in which the test shows that they are in position (right now difference < 100) 	
	private void moveToPosition() {
		//SmartDashboard.putString("Left switch", "" + leftSwitch.get() );		
		SmartDashboard.putString("Left position", "" + leftTalon.getEncPosition() );		
		SmartDashboard.putString("Right position", "" + rightTalon.getEncPosition() );
		
		//all distances start from 0 and go negative upwards
		double leftEncPos =leftTalon.getEncPosition();
		double rightEncPos =rightTalon.getEncPosition();
		boolean leftInPosition = false;
		boolean rightInPosition = false;
		
		if (Math.abs(leftEncPos - leftTargetPos) < 100) {  //arm has reached target when within 100 pulses
			leftTalon.set(0.0);
			leftInPosition = true;
		} else {
			if (leftEncPos > leftTargetPos)   //the encoder is below the target, e.g. -500 > -4000
				leftTalon.set(L_LIFTSPEED);	//move up
			else 
				leftTalon.set(-1.0 * L_LIFTSPEED); // move down from top
		}
		
		if (Math.abs(rightEncPos - rightTargetPos) < 100) {
			rightTalon.set(0.0);
			rightInPosition = true; 
		} else {
			if (rightEncPos > rightTargetPos)   //the encoder is below the target, e.g. -500 > -4000
				rightTalon.set(R_LIFTSPEED);	//move up
			else 
				rightTalon.set(-1.0 * R_LIFTSPEED); // move down from top
		}
		
		if (leftInPosition && rightInPosition) {
			isMoving = false;
			//extra safety feature: set both speeds to zero if isMoving = false;
			leftTalon.set(0.0);
			rightTalon.set(0.0);			
		}
		
		/*ERROR condition:
		 *  This should NEVER happen. This method should never move to the zero position 
		 *  ... unless someone really screws things up by changing the signs of LIFTSPEED or the target positions.
		 */
		if  (leftSwitch.get()) {
			isMoving = false;
			leftTalon.set(0.0);
			leftTalon.setPosition(0);
			//TODO set rumble to pulse 3 times for 0.3 seconds at 75%
		}
		if  (rightSwitch.get()) {
			isMoving = false;
			rightTalon.set(0.0);		
			rightTalon.setPosition(0);
			//TODO set rumble to pulse 3 times for 0.3 seconds at 75%
		}
		
	} // end of method moveToPosition

}
