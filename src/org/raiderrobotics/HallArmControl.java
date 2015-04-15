package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;

import static org.raiderrobotics.RobotMap.*;

//import org.raiderrobotics.utils.ConfigurationAPI;

/* 
 * Temporarily added SmartDashboard printouts so that we can remove the config.get ...
 */

public class HallArmControl {

	Joystick xbox;
	
	//Talon objects
	CANTalon leftTalon;
	CANTalon rightTalon;

	//Sensor switches
	//for this switch, false = open = no signal from arm; true = closed. (If the sensor is replaced this may be reversed.)
	//thus: leftSwitch.get() is TRUE if it is closed	
	DigitalInput leftBottomSwitch, rightBottomSwitch;
	DigitalInput leftMiddleSensor, rightMiddleSensor;
	DigitalInput leftTopSensor, rightTopSensor;

	//multipliers to handle non-identical motors - in both directions. Find values by trial and error.
	static final double L_UP_SPEED_MULT = 1.00;
	static final double R_UP_SPEED_MULT = 0.95;
	static final double L_DOWN_SPEED_MULT = 1.00;
	static final double R_DOWN_SPEED_MULT = 1.00;

	//general speed of the arms
	static final double ARMSPEED = 1.0; 
		
	//used for rumble
	//static final int	RIGHTRUMBLE = 1,
	//					LEFTRUMBLE = 2; 		
	enum RumbleSide {LEFT, RIGHT;}	 
	
	int rightRumbleCount = 0;
	int leftRumbleCount = 0;


	//Different control modes. To use from within a different class
	enum Mode{
		STOP,
		MOVE_TO_BOTTOM,
		MOVE_TO_MIDDLE,
		MOVE_TO_TOP,
		PICK_UP_TO_MIDDLE,
		PICK_UP_TO_TOP;
	}
	Mode armMode = Mode.STOP;

	enum Position {
		BOTTOM, LOWER, MIDDLE, UPPER, TOP, TOO_HIGH;
	}
	
	//Position position = Position.MIDDLE;	//assume that it starts here
	Position Lposition = Position.MIDDLE;	//assume that it starts here
	Position Rposition = Position.MIDDLE;	//assume that it starts here
	
	enum Moving {UP, DOWN, STOPPED;}
	Moving moveMode = Moving.STOPPED;
	
	
	//Dynamic state variables
	private boolean rightDoneMoving;
	private boolean leftDoneMoving;
	//    private int balancePosition=0;
	//    private boolean balanceRight;

	static private HallArmControl instance;
	public static HallArmControl setupInstance(Joystick joystick){
		if(instance == null)
			instance = new HallArmControl(joystick);
		return instance;
	}

	public static HallArmControl getInstance(){
		return instance;
	}

	/**
	 * Create the arm lift control system.
	 * Responsible for lifting and lowering the arm.
	 * The only two methods that are required to be called from the main class: {@link #tick()} and {@link #reset()}.
	 *
	 * @param xbox The arm controller reference
	 */
	private HallArmControl(Joystick xbox) {
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

		leftBottomSwitch = new DigitalInput(LEFT_ARM_SWITCH_PORT);
		rightBottomSwitch = new DigitalInput(RIGHT_ARM_SWITCH_PORT);
		leftMiddleSensor = new DigitalInput(HALL_L_MID_PORT);
		rightMiddleSensor = new DigitalInput(HALL_R_MID_PORT);
		leftTopSensor = new DigitalInput(HALL_L_TOP_PORT);
		rightTopSensor = new DigitalInput(HALL_R_TOP_PORT);
		
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

//		if (Math.abs(baseSpeed) < 0.08)		//which number should we use?
//			baseSpeed = 0.5;

		rightDoneMoving = false;
		leftDoneMoving = false;

		armMode = Mode.STOP;
		moveMode = Moving.STOPPED;

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
		/*System.out.print(rumbleCounter);
		System.err.print(rumbleCounter);
		*/
		
		
		//******* Manual Drive Section *******//
		//Check for manual drive
/*
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
			if((move < 0 && !rightBottomSwitch.get())
					|| (move > 0.0 && getRightEncPos() <= POS_TOP)) //Check bottom and top limits
				rightTalon.set(move * right);
			else{
				rightTalon.set(0.0);
			}


			if((move < 0 && !leftBottomSwitch.get())
					|| (move > 0.0 && getLeftEncPos() <= POS_TOP)) //Check bottom and stop limits
				leftTalon.set(move * left);
			else{
				leftTalon.set(0.0);
			}

			return;
		} 
*/
		//********  End manual arm movement section  *******//
		
		
		//Check the mode

		//Emergency stop button
		if (xbox.getRawButton(XBOX_BTN_X)) {
			armMode = Mode.STOP;
			moveMode = Moving.STOPPED;
			startRumble(RumbleSide.LEFT);
		}

		//Move to rest button
		else if (xbox.getRawButton(XBOX_BTN_A)) {
			armMode = Mode.MOVE_TO_BOTTOM;
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
		case MOVE_TO_BOTTOM:
			//Run "moveToRest". If it is done (returns TRUE), then stop arm.
			if(moveToBottom()) armMode = Mode.STOP;      	
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
				moveToMiddle();
			}
			break;

		case MOVE_TO_TOP:
			//Run "moveToTop". If it is done (returns TRUE), then stop arm.
			if(moveToTop()) armMode = Mode.STOP; 			
			break;

		case PICK_UP_TO_MIDDLE:
			//run moveToRest() and when it is completed, set mode to Move_To_Middle
			if(moveToBottom()) 
				armMode = Mode.MOVE_TO_MIDDLE;
			break;

		case PICK_UP_TO_TOP:
			//run moveToBottom() and when it is completed, set mode to Move_To_Top
			if(moveToBottom()) 
				armMode = Mode.MOVE_TO_TOP;
			break;
			
			//Emergency stop button
		case STOP:
		default:
			//Reset stuff (but don't call "reset()" as that will reset the encoders too) 
			rightDoneMoving = false;
			leftDoneMoving = false;
			moveMode = Moving.STOPPED;
			rightTalon.set(0.0);
			leftTalon.set(0.0);

			//for testing only. Move to where it detects the bottom limit switch.
			
		}
	}

	
	private boolean moveToMiddle() {
		
		//First check if it is already at the middle.
		if(!leftMiddleSensor.get()){
			Lposition = Position.MIDDLE;
			leftTalon.set(0.0);				
		}
		
		if(!rightMiddleSensor.get()){
			Rposition = Position.MIDDLE;
			rightTalon.set(0.0);				
		} 
			
		if (Lposition == Position.MIDDLE && Rposition == Position.MIDDLE) {			
			moveMode = Moving.STOPPED;
			return true;
		}
		
		/* Moving is determined only by the position of the LEFT ARM! I think that this is okay, 
		since it is a very rough position, and the moving direction doesn't get changed until both are at rest.  */ 
		if (Lposition == Position.LOWER || Lposition == Position.BOTTOM) {
			moveMode = Moving.UP;
		}
		if (Lposition == Position.UPPER || Lposition == Position.TOP || Lposition == Position.TOO_HIGH) {
			moveMode = Moving.DOWN;
		}
		
		//stop if you hit top sensor
		if (moveMode == Moving.UP) {
			if(!leftTopSensor.get()){
				Lposition = Position.TOP;
				leftTalon.set(0.0);				
			}
			
			if(!rightTopSensor.get()){
				Rposition = Position.TOP;
				rightTalon.set(0.0);				
			} 
		}

		if (moveMode == Moving.DOWN) {
			leftTalon.set(-ARMSPEED * L_DOWN_SPEED_MULT);
			rightTalon.set(-ARMSPEED * R_DOWN_SPEED_MULT);
		}
		
		if (moveMode == Moving.UP) {
			leftTalon.set(+ARMSPEED * L_UP_SPEED_MULT);
			rightTalon.set(+ARMSPEED * R_UP_SPEED_MULT);
		}
		
		return false;

	}

	/**** Move both arms to the top position ***/
	boolean moveToTop(){		
				
		//Move left to top
		if(!leftTopSensor.get()){
			Lposition = Position.TOP;
			leftTalon.set(0.0);				
		} else {
			leftTalon.set(+ARMSPEED * L_UP_SPEED_MULT);
		}
		//Move right to top
		if(!rightTopSensor.get()){
			Rposition = Position.TOP;
			rightTalon.set(0.0);				
		} else {
			rightTalon.set(+ARMSPEED * R_UP_SPEED_MULT);
		}
			
		
		if (Lposition == Position.BOTTOM)  {
			Lposition = Position.LOWER;
		} else 	if (!leftMiddleSensor.get()) {
			Lposition = Position.MIDDLE;
		} else if (Lposition == Position.MIDDLE) {
			Lposition = Position.UPPER; 
		}
		
		if (Rposition == Position.BOTTOM) {
			Rposition = Position.LOWER;
		} else if (!rightMiddleSensor.get()) {
			Rposition = Position.MIDDLE;
		} else if (Rposition == Position.MIDDLE) {
			Rposition = Position.UPPER;
		}
		
		//check if BOTH are at the top
		if (Lposition == Position.TOP && Rposition == Position.TOP) {		
			moveMode = Moving.STOPPED;
			return true;
		}
		
		return false;
	}
		
	//move both arms to the bottom position
	//TODO: track the position. If you're in the lower section, then move at 75% of speed.
	boolean moveToBottom(){		
		
		//Move left to bottom
		if(leftBottomSwitch.get()){
			Lposition = Position.BOTTOM;
			leftTalon.set(0.0);
			leftTalon.setPosition(0.0);
		} else { 
			leftTalon.set(-ARMSPEED * L_DOWN_SPEED_MULT);
		}
		//Move right to bottom
		if(rightBottomSwitch.get()){
			Rposition = Position.BOTTOM;
			rightTalon.set(0.0);
			rightTalon.setPosition(0.0);
		} else { 
			rightTalon.set(-ARMSPEED * R_DOWN_SPEED_MULT);
		}
		
		//update position. ** ONLY USES THE LEFT ARM!!
//		if (Lposition == Position.TOP) Lposition = Position.UPPER;
//		if (Rposition == Position.TOP) Rposition = Position.UPPER;
//		if (leftMiddleSensor.get()) Lposition = Position.MIDDLE;
//		if (rightMiddleSensor.get()) Rposition = Position.MIDDLE;
//		if (Lposition == Position.MIDDLE) Lposition = Position.LOWER;
//		if (Rposition == Position.MIDDLE) Rposition = Position.LOWER;
		
		if (Lposition == Position.TOP)  {
			Lposition = Position.UPPER;
		} else 	if (!leftMiddleSensor.get()) {
			Lposition = Position.MIDDLE;
		} else if (Lposition == Position.MIDDLE) {
			Lposition = Position.LOWER; 
		}
		
		if (Rposition == Position.TOP) {
			Rposition = Position.UPPER;
		} else if (!rightMiddleSensor.get()) {
			Rposition = Position.MIDDLE;
		} else if (Rposition == Position.MIDDLE) {
			Rposition = Position.LOWER;
		}

		
		if (Lposition == Position.BOTTOM && Rposition == Position.BOTTOM) {		
			moveMode = Moving.STOPPED;
			startRumble(RumbleSide.LEFT);
			return true;
		}
		
		return false;
	}

	//stop arm moving -- called from Robot.java (limit switch)
	public void stop() {
		armMode = Mode.STOP;
		moveMode = Moving.STOPPED;
	}

	public void setMode(Mode mode){
		this.armMode = mode;
	}
	public Mode getMode(){
		return armMode;
	}

	
	/*
	private void startRumble() {
		xbox.setRumble(Joystick.RumbleType.kRightRumble, 0.4f);
		//xbox.setRumble(Joystick.RumbleType.kLeftRumble, 0.4f);
		rumbleCounter = 50;
	}
*/
	private void startRumble(RumbleSide side){
		
		if(side == RumbleSide.RIGHT){
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
	
}
