package org.raiderrobotics;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;
import org.raiderrobotics.utils.ConfigurationAPI;

public class LiftArmSystem {

    //Talon variables
    CANTalon leftTalon;
    CANTalon rightTalon;

    //Hall effect sensors
    DigitalInput rightSwitch;
    DigitalInput leftSwitch;

    double speed = 0.5; //autonomous move speed

    Joystick xbox;

    int mode = 0;

    public boolean debug;

    //I'll only use it for debugging
    ConfigurationAPI.ConfigurationSection config = ConfigurationAPI.load("/home/lvuser/config.yml");


    //Dynamic state variables
    //This is why you use Command Base .-.
    private boolean rightDoneMoving;
    private boolean leftDoneMoving;

/*	final static double L_LIFTSPEED = 0.8;
	final static double R_LIFTSPEED = 0.8;
	final static double RAMPRATE = 0.3;  //volts per second ?
			
	final static int POSITION_ONE_L = 4000;	//number of encoder ticks to get to position 1
	final static int POSITION_ONE_R = 4000;	//number of encoder ticks to get to position 1
	//FIXME: get correct numbers
	final static int POSITION_TOP_L = 4700;	//number of encoder ticks to get to top position 
	final static int POSITION_TOP_R = 4700;	//number of encoder ticks to get to top position
*/

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
    LiftArmSystem(Joystick xbox) {
        this.xbox = xbox;

        //configure both talons
        this.leftTalon = new CANTalon(RobotMap.TALON3_CAN_ID);
        this.rightTalon = new CANTalon(RobotMap.TALON4_CAN_ID);

        leftTalon.set(0);
        leftTalon.changeControlMode(ControlMode.PercentVbus);
        leftTalon.enableControl();
        //TODO: test this
        //leftTalon.setVoltageRampRate(RAMPRATE);

        rightTalon.set(0);
        rightTalon.changeControlMode(ControlMode.PercentVbus);
        rightTalon.enableControl();
        //TODO: test this
        //rightTalon.setVoltageRampRate(RAMPRATE);

        reset();
    }

    /**
     * Used to reset any variables of the running class.
     * Must be called once on disable or initialisation of the robot.
     */
    public void reset() {
        config.reload();

        leftTalon.setPosition(0);
        rightTalon.setPosition(0);

        speed = config.getDouble("speed");
        if (speed == 0)
            speed = 0.5;

        //This is why you use Command Base .-.
        rightDoneMoving = false;
        leftDoneMoving = false;

        if(debug)
            System.out.println("[Arm Debug] Reset!");
    }

    /**
     * The most important method of the class.
     * It runs the whole logic of the class.
     * It the method is not executed - no output will be produced form the arm control system.
     * Must be called periodically (usually in teleopPeriodic()).
     */
    public void tick() {
        //Check for manual drive
        double move = xbox.getRawAxis(3) - xbox.getRawAxis(2);
        if(Math.abs(move) > 0.15){
            mode = 0; //Reset the current mode

            //Percentage at what the talons will move
            //when one is going faster than the other one
            double rightCut = 0.95;
            double leftCut = 0.95;

            //Determining if one talon is moving faster than the other one
            double right = Math.abs(getRightEncPos()) > Math.abs(getLeftEncPos())
                    ? rightCut
                    : 1;
            double left = Math.abs(getLeftEncPos()) > Math.abs(getRightEncPos())
                    ? leftCut
                    : 1;

            //Move the talons based on their determined speeds
            leftTalon.set(move * left);
            rightTalon.set(move * right);

            return;
        }

        //Check the mode
        //This is why you use Command Base .-.

        //Emergency stop button
        if (xbox.getRawButton(RobotMap.XBOX_BTN_X)) {
            mode = 0;
            if (debug)
                System.out.println("[Arm Debug] Pressed button " + RobotMap.XBOX_BTN_X);
        }

        //Move to rest button
        else if (xbox.getRawButton(RobotMap.XBOX_BTN_A)) {
            mode = RobotMap.XBOX_BTN_A;
            if (debug)
                System.out.println("[Arm Debug] Pressed button " + RobotMap.XBOX_BTN_A);
        }

        //Move to middle button
        else if (xbox.getRawButton(RobotMap.XBOX_BTN_B)) {
            mode = RobotMap.XBOX_BTN_B;
            if (debug)
                System.out.println("[Arm Debug] Pressed button " + RobotMap.XBOX_BTN_B);
        }

        //Move to top button
        else if (xbox.getRawButton(RobotMap.XBOX_BTN_Y)) {
            mode = RobotMap.XBOX_BTN_Y;
            if (debug)
                System.out.println("[Arm Debug] Pressed button " + RobotMap.XBOX_BTN_Y);
        }


        //Check the buttons
        //This is why you use Command Base .-.
        switch (mode) {
            //Move to rest button
            case RobotMap.XBOX_BTN_A:
                //Check if done
                if (!rightSwitch.get() && !leftSwitch.get()) {
                    //Reset
                    rightTalon.set(0);
                    leftTalon.set(0);

                    rightTalon.setPosition(0);
                    leftTalon.setPosition(0);

                    mode = 0;
                }
                //Not done? Move
                else {
                    rightTalon.set(!rightSwitch.get() ? 0 : -speed);
                    leftTalon.set(!leftSwitch.get() ? 0 : -speed);
                }

                break;

            //Move to middle button
            case RobotMap.XBOX_BTN_B:
                //Check if done
                if (rightDoneMoving && leftDoneMoving) {
                    //Reset
                    rightTalon.set(0);
                    leftTalon.set(0);

                    rightDoneMoving = false;
                    leftDoneMoving = false;

                    rightTalon.setPosition(0);
                    leftTalon.setPosition(0);

                    mode = 0;
                }
                //Not done? Move
                else {
                    moveTo(config.getDouble("posMiddle"));
                }
                break;

            //Move to top button
            case RobotMap.XBOX_BTN_Y:
                //Check if done
                if (rightDoneMoving && leftDoneMoving) {
                    //Reset
                    rightTalon.set(0);
                    leftTalon.set(0);

                    rightDoneMoving = false;
                    leftDoneMoving = false;

                    rightTalon.setPosition(0);
                    leftTalon.setPosition(0);

                    mode = 0;
                }
                //Not done? Move
                else {
                    moveTo(config.getDouble("posTop"));
                }
                break;

            //Emergency stop button
            default:
                //Reset stuff
                rightDoneMoving = false;
                leftDoneMoving = false;

                rightTalon.set(0);
                leftTalon.set(0);
        }
    }

    //ONLY used by MOVE_TO_MIDDLE and MOVE_TO_TOP modes
    private void moveTo(double targetPos) {
        double sigR = Math.signum(targetPos - getRightEncPos());
        double sigL = Math.signum(targetPos - getLeftEncPos());

        //Move right
        if (!rightDoneMoving) {
            //If the direction is negative and the arm is mover than the target position - stop
            //Or if the direction if positive and the arm is higher than the target position - stop
            if ((sigR < 0 && getRightEncPos() < targetPos) || (sigR > 0 && getRightEncPos() > targetPos)) {
                rightDoneMoving = true;
                rightTalon.set(0);
            } else
                rightTalon.set(sigR * speed);
        }

        //Move left
        if (!leftDoneMoving) {
            //Same as above
            if ((sigL < 0 && getLeftEncPos() < targetPos) || (sigL > 0 && getLeftEncPos() > targetPos)) {
                leftDoneMoving = true;
                leftTalon.set(0);
            } else
                leftTalon.set(sigL * speed);
        }
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
