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
    final int BALANCE_MODE = 99;
    public boolean debug;

    //I'll only use it for debugging
    ConfigurationAPI.ConfigurationSection config = ConfigurationAPI.load("/home/lvuser/config.yml");


    //Dynamic state variables
    //This is why you use Command Base .-.
    private boolean rightDoneMoving;
    private boolean leftDoneMoving;
    private int balancePosition=0;
    private boolean balanceRight;

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
        
        leftSwitch = new DigitalInput(4);
        rightSwitch = new DigitalInput(5);

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
        
        mode = 0;

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
                if (rightSwitch.get() && leftSwitch.get()) {
                    //Reset
                    rightTalon.set(0);
                    leftTalon.set(0);

                    rightTalon.setPosition(0);
                    leftTalon.setPosition(0);

                    mode = 0;
                }
                //Not done? Move
                else {
                    rightTalon.set(rightSwitch.get() ? 0 : -speed);
                    leftTalon.set(leftSwitch.get() ? 0 : -speed);
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

                    mode = BALANCE_MODE;
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

                    mode = BALANCE_MODE;
                }
                //Not done? Move
                else {
                    moveTo(config.getDouble("posTop"));
                }
                break;

            //Balance mode (run after MOVE_TO_MIDDLE and MOVE_TO_TOP)
            case BALANCE_MODE:
                if(balancePosition == 0){
                    if(getRightEncPos() > getLeftEncPos()){
                        balancePosition = getRightEncPos();
                        balanceRight = true;
                    } else if(getLeftEncPos() > getRightEncPos()){
                        balancePosition = getLeftEncPos();
                        balanceRight = false;
                    }

                    if(debug)
                        System.out.println("Balance position: "+balancePosition+" ; Right: "+balanceRight);
                }

                if(balanceRight){ //If moving the right talon
                    if(getRightEncPos() > balancePosition){ //If not in place yet
                        rightTalon.set(-0.5);
                        break;
                    } else
                        rightTalon.set(0);

                } else { //No? Then we might probably be moving the left talon
                    if(getLeftEncPos() > balancePosition){ //If not in place yet
                        leftTalon.set(-0.5);
                        break;
                    } else
                        leftTalon.set(0);
                }

                mode = 0;

                break;

            //Emergency stop button
            default:
                //Reset stuff
                rightDoneMoving = false;
                leftDoneMoving = false;
                balancePosition = 0;

                rightTalon.set(0);
                leftTalon.set(0);
        }
    }

    //ONLY used by MOVE_TO_MIDDLE and MOVE_TO_TOP modes
    private void moveTo(double targetPos) {
        double disR = targetPos - getRightEncPos();
        double disL = targetPos - getLeftEncPos();
        
        double sigR = Math.signum(disR);
        double sigL = Math.signum(disL);

        double absR = Math.abs(disR);
        double absL = Math.abs(disL);
        
        double slowDown = config.getDouble("slowDown");
                
        //If getting close - slow down
        //I.E.: If position is less than the maximum speed * (100 for example)
        // than move at a speed between maximum and 0.15
        if(absR < speed * slowDown) {
        	double s = disR / slowDown;
        	sigR *= (s < 0.15 ? 0.15 : s);
        } else
        	sigR *= speed;
        
        if(absL <= speed * slowDown) {
        	double s = disL / slowDown;
        	sigL *= (s < 0.15 ? 0.15 : s);
        } else
        	sigL *= speed;

        //Move right
        if (!rightDoneMoving) {
            //If the direction is negative and the arm is lower than the target position - stop
            //Or if the direction if positive and the arm is higher than the target position - stop
            if (absR <= 10) {
                rightDoneMoving = true;
                rightTalon.set(0);
            } else
                rightTalon.set(sigR);
        }

        //Move left
        if (!leftDoneMoving) {
            //Same as above
            if (absL <= 10) {
                leftDoneMoving = true;
                leftTalon.set(0);
            } else
                leftTalon.set(sigL);
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