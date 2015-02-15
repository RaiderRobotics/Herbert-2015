package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;

import org.raiderrobotics.utils.ConfigurationAPI;

/* ****** REFACTORING ********
 * Date: 9pm, Feb 13th, 2015.
 * Done by: M. Harwood to make it easier to read and fix bugs
 * editorial comments and extraneous comment blocks removed
 * enum Mode: removed id numbers
 * 
 * variable names refactored: mode -> armMode
 * 			disR -> distR
 * 			disL -> distL 
 * 			some others still need fixing
 * 
 * The signum code is too complex to attempt modifying. 
 * The logic of it needs explaining and it needs to be rewritten.
 * 
 * The freezing of one arm may be due to 
 * (i) a divide by zero error (for example, if slowDown = 0.0) which results in NaN or infinity.
 * (ii) the line checking to see if speed == 0.0 (rounding errors will make this rarely equal
 * (iii) some other logic problem
 *  
 * Other to do: standardize which is done first, left or right so that all code is consistent.
 *
 * Temporarily added SmartDashboard printouts so that we can remove the config.get ...
 */
public class ArmControl {

	//Talon variables
	CANTalon leftTalon;
	CANTalon rightTalon;

	//Sensor switches
	//for this switch, false = open = no signal from arm; true = closed. (If the sensor is replaced this may be reversed.)
	//thus: leftSwitch.get() is TRUE if it is closed
	DigitalInput rightSwitch;
	DigitalInput leftSwitch;

	//fill these in from the config file values once we figure out what they are.
	static final double POS_TOP = 6500.0;
	static final double POS_MIDDLE = 4000.0;
	static final double ARMSPEED = 0.8;
	static final double SLOWDOWN = 500.0;

	double autonomousSpeed = 0.5; //autonomous move speed

	Joystick xbox;

	Mode armMode = Mode.STOP;
	public boolean debug;

	//I'll only use it for debugging
	//ConfigurationAPI.ConfigurationSection config = ConfigurationAPI.load("/home/lvuser/config.yml");

	//Different control modes. To use from within a different class
	public enum Mode{
		STOP,
		MOVE_TO_REST,
		MOVE_TO_MIDDLE,
		MOVE_TO_TOP,
		PICK_UP_TO_MIDDLE,
		PICK_UP_TO_TOP;
	}


	//Dynamic state variables
	//This is why you use Command Base .-.
	private boolean rightDoneMoving;
	private boolean leftDoneMoving;
	//    private int balancePosition=0;
	//    private boolean balanceRight;

	//constructor -- this should be a singleton pattern so you can't make more than one object
	//Why would you? I can make it static and have static reference to it from within the Robot class - but that's a pain.
	//Unless one messes with it and creates a bunch of objects, it's fine as it is.

	/**
	 * Create the arm lift control system.
	 * Responsible for lifting and lowering the arm.
	 * The only two methods that are required to be called from the main class: {@link #tick()} and {@link #reset()}.
	 *
	 * @param xbox The arm controller reference
	 */
	ArmControl(Joystick xbox) {
		this.xbox = xbox;

		//configure both talons
		this.leftTalon = new CANTalon(RobotMap.TALON3_CAN_ID);
		this.rightTalon = new CANTalon(RobotMap.TALON4_CAN_ID);

		leftSwitch = new DigitalInput(4);
		rightSwitch = new DigitalInput(5);

		leftTalon.set(0.0);
		leftTalon.changeControlMode(ControlMode.PercentVbus);
		leftTalon.enableControl();
		//TODO: test this
		//leftTalon.setVoltageRampRate(RAMPRATE);

		rightTalon.set(0.0);
		rightTalon.changeControlMode(ControlMode.PercentVbus);
		rightTalon.enableControl();
		//TODO: test this
		//rightTalon.setVoltageRampRate(RAMPRATE);

		reset();

		//Get values of constants used:
		//SmartDashboard.putString("Config.speed", "" + config.getDouble("speed"));
		//SmartDashboard.putString("Config.posMiddle", "" + config.getDouble("posMiddle"));
		//SmartDashboard.putString("Config.posTop", "" + config.getDouble("posTop"));
		//SmartDashboard.putString("Config.slowDown", "" + config.getDouble("slowDown"));

	}

	/**
	 * Used to reset any variables of the running class.
	 * Must be called once on disable or initialisation of the robot.
	 */
	public void reset() {
		//config.reload();

		leftTalon.setPosition(0);
		rightTalon.setPosition(0);

		autonomousSpeed = ARMSPEED;
		//fix: NEVER compare a double to any other number using ==
		//if (speed == 0.0)
		if (Math.abs(autonomousSpeed) < 0.08)		//which number should we use?
			autonomousSpeed = 0.5;

		rightDoneMoving = false;
		leftDoneMoving = false;

		armMode = Mode.STOP;

	}

	/**
	 * The most important method of the class.
	 * It runs the whole logic of the class.
	 * It the method is not executed - no output will be produced from the arm control system.
	 * Must be called periodically (usually in teleopPeriodic()).
	 */
	public void tick() {
		//Check for manual drive
		double move = xbox.getRawAxis(RobotMap.XBOX_R_TRIGER) - xbox.getRawAxis(RobotMap.XBOX_L_TRIGGER);
		if(Math.abs(move) > 0.15){
			armMode = Mode.STOP; //Reset the current mode

			//Percentage at what the talons will move
			//when one is going faster than the other one
			//0.95 might not be enough to balance
			double rightCut = 0.80;
			double leftCut = 0.80;
			
			double right, left;
			
			double rightHeight = Math.abs(getRightEncPos()); //the distance of right arm to bottom
			double leftHeight = Math.abs(getLeftEncPos());

			//Determining if one talon is moving faster than the other one
			if(move>0){ //moving up, the longer the distance, the faster the arm should go	
				
				right = rightHeight > leftHeight ? rightCut : 1.0;
				left = leftHeight > rightHeight ? leftCut : 1.0;
				
			}else{ //moving down, the longer the distance, the slower the arm should go
				
				right = rightHeight < leftHeight ? rightCut : 1.0;
				left = leftHeight < rightHeight ? leftCut : 1.0;
				
			}

			//Move the talons based on their determined speeds
			if((move < 0 && !rightSwitch.get())
					|| (move > 0.0 && getRightEncPos() <= POS_TOP)) //Check bottom and top limits
				rightTalon.set(move * right);
			else
				rightTalon.set(0.0);

			if((move < 0 && !leftSwitch.get())
					|| (move > 0.0 && getLeftEncPos() <= POS_TOP)) //Check bottom and stop limits
				leftTalon.set(move * left);
			else
				leftTalon.set(0.0);

			return;
		} //end manual arm movement section

		//Check the mode

		//Emergency stop button
		if (xbox.getRawButton(RobotMap.XBOX_BTN_X)) {
			armMode = Mode.STOP;
		}

		//Move to rest button
		else if (xbox.getRawButton(RobotMap.XBOX_BTN_A)) {
			armMode = Mode.MOVE_TO_REST;
		}

		//Move to middle button
		else if (xbox.getRawButton(RobotMap.XBOX_BTN_B)) {
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.MOVE_TO_MIDDLE;
		}

		//Move to top button
		else if (xbox.getRawButton(RobotMap.XBOX_BTN_Y)) {
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.MOVE_TO_TOP;
		}

		//Pick up and move to middle
		else if (xbox.getRawButton(RobotMap.XBOX_BUMPER_R)){
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.PICK_UP_TO_MIDDLE;
		}

		//Pick up and move to top
		else if (xbox.getRawButton(RobotMap.XBOX_BUMPER_L)){
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.PICK_UP_TO_TOP;
		}


		//Check the buttons
		switch (armMode) {
		case MOVE_TO_REST:
			//Run "moveToRest". If it is done (returns TRUE), then stop arm.
			if(moveToRest()) armMode = Mode.STOP;      	
			break;

		case MOVE_TO_MIDDLE:
			//check if done
			if (rightDoneMoving && leftDoneMoving) {
				rightTalon.set(0.0);
				leftTalon.set(0.0);

				rightDoneMoving = false;
				leftDoneMoving = false;

				armMode = Mode.STOP;
			}
			//Not done? continue to move to middle
			else {
				moveTo(POS_MIDDLE);
			}
			break;

		case MOVE_TO_TOP:
			//Check if done
			if (rightDoneMoving && leftDoneMoving) {
				//Reset
				rightTalon.set(0.0);
				leftTalon.set(0.0);

				rightDoneMoving = false;
				leftDoneMoving = false;

				armMode = Mode.STOP;
			}
			//Not done? continue to move to top
			else {
				moveTo(POS_TOP);
			}
			break;

		case PICK_UP_TO_MIDDLE:
			//run moveToRest() and when it is completed, set mode to Move_To_Middle
			if(moveToRest()) 
				armMode = Mode.MOVE_TO_MIDDLE;
			break;

		case PICK_UP_TO_TOP:
			//run moveToRest() and when it is completed, set mode to Move_To_Top
			if(moveToRest()) 
				armMode = Mode.MOVE_TO_TOP;
			break;

			//Emergency stop button?
		case STOP:
		default:
			//Reset stuff
			rightDoneMoving = false;
			leftDoneMoving = false;
			rightTalon.set(0.0);
			leftTalon.set(0.0);
		}
	}

	//ONLY used by MOVE_TO_MIDDLE and MOVE_TO_TOP modes
	private void moveTo(double targetPos) {
		double distR = targetPos - getRightEncPos();
		double distL = targetPos - getLeftEncPos();

		double dirR = Math.signum(distR);
		double dirL = Math.signum(distL);

		double absR = Math.abs(distR);
		double absL = Math.abs(distL);
		
		double 	speedR = autonomousSpeed,
				speedL = autonomousSpeed;
		

		double slowDown = SLOWDOWN; //SLOWDOWN = 500.0;

		//If getting close - slow down
		//I.E.: If position is less than the maximum speed * (100 for example)
		// than move at a speed between maximum and 0.15

		/*** This whole section needs rewriting so that it is understandable. 
		 *** If we're getting signum, why are we multiplying it by something? ***/
		
		
		//oh wow, what a massive weird chunck of code!
		//but don't worry. It's not that hard to understand! :D
		//decelerationThreshold always has the range from 0 to autonomousSpeed.
		
		if(absR < autonomousSpeed * slowDown) { //first, it looks after brake. If the absR is very small, meaning there isn't a great distance between the target and the arm, therefore, the armm needs to move slower inorder to brake.
			double decelerationThreshold = absR / slowDown;	// If the decelerationThreshold is a number that detects if the motor needs to stop decelerating. If the absR is very big, meaning there's NOT a lot of space between the arm and the target, so no need to stop decelerating :D 
			speedR = dirR * (decelerationThreshold < 0.15 ? 0.15 : decelerationThreshold); //The threshold itself is a speed factor unevenly goes from maximum speed, 0.5 to 0. The closer it is to the target, the lower the value. Since we don't want the arm to go too slow, the value can't be lower than 0.15.
			speedR = absR > absL ? dirR*0.10 : speedR; //balance speed: so yeah, quite straight forward. If one is going too fast, the arm speed will drop to 0.1
		} else
			speedR = dirR*(absR > absL ? (autonomousSpeed - 0.15) : autonomousSpeed); //balance speed, if one arm is going faster than the other, the the speed will drop 1.5. If not, it goes normal speed.

		if(absL < autonomousSpeed * slowDown) {
			double decelerationThreshold = absL / slowDown;
			speedL = dirL * (decelerationThreshold < 0.15 ? 0.15 : decelerationThreshold);
			speedL = absL > absR ? dirL*0.10 : speedL; //balance speed
		} else
			speedL = dirL*(absL > absR ? (autonomousSpeed - 0.15) : autonomousSpeed); //balance speed

		
		//Move right
		if (!rightDoneMoving) {
			//Stop when it's closer than 10 encoder distance units
			//Also, idiot proof, in case it moves over negative
			if (absR <= 10.0 || (dirR < 0.0 && rightSwitch.get())) {
				rightDoneMoving = true;
				rightTalon.set(0.0);
			} else
				rightTalon.set(speedR);
		}

		//Move left
		if (!leftDoneMoving) {
			//Same as above
			if (absL <= 10.0 || (dirL < 0.0 && leftSwitch.get())) {
				leftDoneMoving = true;
				leftTalon.set(0.0);
			} else
				leftTalon.set(speedL);
		}
	}

	boolean moveToRest(){
		boolean rightDone=false;
		boolean leftDone=false;
		
		double  rightCut = 0.8,
				leftCut = 0.8;
		
		double right, left;
		
		double 	rightHeight = getRightEncPos(),
				leftHeight = getLeftEncPos();
		
		//balance speed
		if(rightHeight < leftHeight){ //the right arm is closer to the target
			right = rightCut * autonomousSpeed;
		}else{
			right = autonomousSpeed;
		}
		
		if(leftHeight < rightHeight){ //the right arm is closer to the target
			left = leftCut * autonomousSpeed;
		}else{
			left = autonomousSpeed;
		}
		
		//Move right to bottom
		if(rightSwitch.get()){
			rightDone = true;
			rightTalon.set(0.0);
			rightTalon.setPosition(0.0);
		} else 
			rightTalon.set(-right);

		//Move left to bottom
		if(leftSwitch.get()){
			leftDone = true;
			leftTalon.set(0.0);
			leftTalon.setPosition(0.0);
		} else 
			leftTalon.set(-left);


		return rightDone && leftDone;
	}

	//stop arm moving -- called from Robot.java (limit switch)
	public void stop() {
		armMode = Mode.STOP;
	}

	public void setMode(Mode mode){
		this.armMode = mode;
	}
	public Mode getMode(){
		return armMode;
	}

	//Utils
	//For some reason inverting the sensor input doesn't work with the talons.
	//So we need to inverse it manually
	public int getRightEncPos() {
		return -rightTalon.getEncPosition();
	}

	public int getLeftEncPos() {
		return -leftTalon.getEncPosition();
	}
}