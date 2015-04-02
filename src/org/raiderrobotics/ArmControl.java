package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
//import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;

import static org.raiderrobotics.RobotMap.*;

//import org.raiderrobotics.utils.ConfigurationAPI;

/* Other to do: standardize which is done first, left or right so that all code is consistent.
 *
 * Temporarily added SmartDashboard printouts so that we can remove the config.get ...
 */

public class ArmControl {

	//Talon objects
	CANTalon leftTalon;
	CANTalon rightTalon;

	//Sensor switches
	//for this switch, false = open = no signal from arm; true = closed. (If the sensor is replaced this may be reversed.)
	//thus: leftSwitch.get() is TRUE if it is closed
	DigitalInput rightSwitch;
	DigitalInput leftSwitch;

	Joystick xbox;

	//(these numbers are also stored in a config file on the roboRIO
	static final double POS_TOP = 6500.0;
	static final double POS_MIDDLE = 4000.0;
	static final double ARMSPEED = 1.0; 		// was 0.8;
	static final double SLOWDOWN_REGION = 500.0;
	static final double LSPEED_MULT = 1.00;
	static final double RSPEED_MULT = 0.93;	//the right motor is a bit faster than the left, so slow it down. 
		//tried 0.90 and 0.95
	static final int RIGHT=1, LEFT=2;		//used for rumble
	
	int rightRumbleCount = 0;
	int leftRumbleCount = 0;
	//this is the base speed that the arms move at.
	//It can is often set to the contant ARMSPEED but then changed to 0.5
	//It is used as a starting point to calculate the slowdown speed.
	double baseSpeed = 0.5; 

	
	//Different control modes. To use from within a different class
	public enum Mode{
		STOP,
		MOVE_TO_REST,
		MOVE_TO_MIDDLE,
		MOVE_TO_TOP,
		PICK_UP_TO_MIDDLE,
		PICK_UP_TO_TOP;
	}
	Mode armMode = Mode.STOP;

	//the next two lines are for debugging by Kirill
	public boolean debug;
	//ConfigurationAPI.ConfigurationSection config = ConfigurationAPI.load("/home/lvuser/config.yml");

	//Dynamic state variables
	private boolean rightDoneMoving;
	private boolean leftDoneMoving;
	//    private int balancePosition=0;
	//    private boolean balanceRight;

	static private ArmControl instance;
	public static ArmControl setupInstance(Joystick joystick){
		if(instance == null)
			instance = new ArmControl(joystick);
		return instance;
	}

	public static ArmControl getInstance(){
		return instance;
	}

	/**
	 * Create the arm lift control system.
	 * Responsible for lifting and lowering the arm.
	 * The only two methods that are required to be called from the main class: {@link #tick()} and {@link #reset()}.
	 *
	 * @param xbox The arm controller reference
	 */
	private ArmControl(Joystick xbox) {
		this.xbox = xbox;

		//configure both talons
		leftTalon = new CANTalon(TALON3_CAN_ID);
		rightTalon = new CANTalon(TALON4_CAN_ID);

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

		leftSwitch = new DigitalInput(LEFT_ARM_SWITCH_PORT);
		rightSwitch = new DigitalInput(RIGHT_ARM_SWITCH_PORT);

		reset();

		//Get values of constants used:
		//SmartDashboard.putString("Config.speed", "" + config.getDouble("speed"));
		//SmartDashboard.putString("Config.posMiddle", "" + config.getDouble("posMiddle"));
		//SmartDashboard.putString("Config.posTop", "" + config.getDouble("posTop"));
		//SmartDashboard.putString("Config.slowDown", "" + config.getDouble("slowDown"));
	}

	/**
	 * Used to reset the talon encoder positions.
	 * Also used to reset the speed
	 * Must be called once on disable or initialisation of the robot.
	 */
	public void reset() {
		//config.reload();

		leftTalon.setPosition(0);
		rightTalon.setPosition(0);

		baseSpeed = ARMSPEED;
		if (Math.abs(baseSpeed) < 0.08)		//which number should we use?
			baseSpeed = 0.5;

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
		isRightRumbleDone();
		isLeftRumbleDone();
		
		//******* Manual Drive Section *******//
		//Check for manual drive
		double move = xbox.getRawAxis(XBOX_R_TRIGER) - xbox.getRawAxis(XBOX_L_TRIGGER);
		if(Math.abs(move) > 0.15){
			armMode = Mode.STOP; //Reset the current mode

			//Percentage at what the talons will move
			//when one is going faster than the other one
			double rightCut = 0.95;
			double leftCut = 0.95;

			//Determining if one talon is moving faster than the other one
			double right = Math.abs(getRightEncPos()) > Math.abs(getLeftEncPos())
					? rightCut : 1.0;
			double left = Math.abs(getLeftEncPos()) > Math.abs(getRightEncPos())
					? leftCut : 1.0;

			//Move the talons based on their determined speeds
			if((move < 0 && !rightSwitch.get())
					|| (move > 0.0 && getRightEncPos() <= POS_TOP)) //Check bottom and top limits
				rightTalon.set(move * right);
			else {
				rightTalon.set(0.0);
			}
			
			if((move < 0 && !leftSwitch.get())
					|| (move > 0.0 && getLeftEncPos() <= POS_TOP)) //Check bottom and stop limits
				leftTalon.set(move * left);
			else {
				leftTalon.set(0.0);
			}
			
			return;
		} 
		//********  End manual arm movement section  *******//

		//Check the mode

		//Emergency stop button
		if (xbox.getRawButton(XBOX_BTN_X)) {
			armMode = Mode.STOP;
			startRumble(LEFT);
		}

		//Move to rest button
		else if (xbox.getRawButton(XBOX_BTN_A)) {
			armMode = Mode.MOVE_TO_REST;
		}

		//Move to middle button
		else if (xbox.getRawButton(XBOX_BTN_B)) {
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.MOVE_TO_MIDDLE;
		}

		//Move to top button
		else if (xbox.getRawButton(XBOX_BTN_Y)) {
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.MOVE_TO_TOP;
		}

		//Pick up and move to middle
		else if (xbox.getRawButton(XBOX_BUMPER_R)){
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.PICK_UP_TO_MIDDLE;
		}

		//Pick up and move to top
		else if (xbox.getRawButton(XBOX_BUMPER_L)){
			rightDoneMoving = false;
			leftDoneMoving = false;
			armMode = Mode.PICK_UP_TO_TOP;
		}


		//Perform arm moving based on which mode was selected by the most recent button push.
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

			//Emergency stop button
		case STOP:
		default:
			//Reset stuff (but don't call "reset()" as that will reset the encoders too) 
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

		//speedL and speedR are used to figure out the speeds of the two arms.
		//Note that they initially start as just being +1 or -1 to get the direction of motion
		//and are later multiplied by a multiplier to get the correct speed.
		double speedR = Math.signum(distR);
		double speedL = Math.signum(distL);

		double absR = Math.abs(distR);
		double absL = Math.abs(distL);

		//double slowDown = SLOWDOWN;

		//If getting close - slow down
		//i.e. If position is less than the maximum speed * (100 for example)
		// than move at a speed between maximum and 0.15

		//if you are in the slowdown region then ...
		if(absR < baseSpeed * SLOWDOWN_REGION) {	//note that slowDown is being scaled by mutiplying it by speed.
			double slowDownSpeed = distR / SLOWDOWN_REGION;	//calculate the slowdown speed
			speedR *= Math.max(slowDownSpeed, 0.15); //do not permit a slowDown speed to be less than 0.15
			//WAS: //speedR *= (slowDownSpeed < 0.15 ? 0.15 : slowDownSpeed);
		} else //not in slowdown region -- move at normal speed
			speedR *= baseSpeed;

		if(absL <= baseSpeed * SLOWDOWN_REGION) {
			double slowDownSpeed = distL / SLOWDOWN_REGION;
			speedL *= Math.max(slowDownSpeed, 0.15);
		} else
			speedL *= baseSpeed;

		//now that speeds have been calculated, adjust for different motors
		speedL *= LSPEED_MULT;
		speedR *= RSPEED_MULT;
		
		
		//Move right
		if (!rightDoneMoving) {
			//Stop when it's closer than 10 encoder distance units
			//Also, idiot proof, in case it moves over negative
			if (absR <= 10.0 || (speedR < 0 && rightSwitch.get())) {
				rightDoneMoving = true;
				rightTalon.set(0.0);
			} else
				rightTalon.set(speedR);
		}

		//Move left
		if (!leftDoneMoving) {
			//Same as above
			if (absL <= 10.0 || (speedL < 0.0 && leftSwitch.get())) {
				leftDoneMoving = true;
				leftTalon.set(0.0);
			} else
				leftTalon.set(speedL);
		}
	}

	boolean moveToRest(){
		boolean rightDone=false;
		boolean leftDone=false;

		//Move right to bottom
		if(rightSwitch.get()){
			rightDone = true;
			rightTalon.set(0.0);
			rightTalon.setPosition(0.0);
		} else 
			rightTalon.set(-baseSpeed);

		//Move left to bottom
		if(leftSwitch.get()){
			leftDone = true;
			leftTalon.set(0.0);
			leftTalon.setPosition(0.0);
		} else 
			leftTalon.set(-baseSpeed);

		if (rightDone && leftDone) startRumble(LEFT);
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

	private void startRumble(int side){
		if(side == RIGHT){
			if(rightRumbleCount == 0){
				rightRumbleCount = 1;
			}
			xbox.setRumble(Joystick.RumbleType.kRightRumble, 1.0f);
			//isRightRumbleDone();
		}else{
			if(leftRumbleCount == 0){
				leftRumbleCount = 1;
			}
			xbox.setRumble(Joystick.RumbleType.kLeftRumble, 1.0f);
		}
	}
	
	private boolean isRightRumbleDone(){ 
		if(rightRumbleCount>0){
			//xbox.setRumble(Joystick.RumbleType.kRightRumble, 1.0f);
			rightRumbleCount--;
			return false;
		}else{ //stop
			xbox.setRumble(Joystick.RumbleType.kRightRumble, 0.0f);
			return true;
		}	
	}
	
	private boolean isLeftRumbleDone(){
		if(leftRumbleCount>0){
			leftRumbleCount--;
			return false;
		}else{ //stop
			xbox.setRumble(Joystick.RumbleType.kLeftRumble, 0.0f);
			return true;
		}
	}
	
	//For some reason inverting the sensor input doesn't work with the talons.
	//So we need to invert it manually
	public int getRightEncPos() {
		return -rightTalon.getEncPosition();
	}

	public int getLeftEncPos() {
		return -leftTalon.getEncPosition();
	}
}
