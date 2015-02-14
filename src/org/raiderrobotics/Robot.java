package org.raiderrobotics;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.Joystick.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import static org.raiderrobotics.RobotMap.*;

import com.ni.vision.NIVision;
//import com.ni.vision.NIVision.DrawMode;
import com.ni.vision.NIVision.Image;
//import com.ni.vision.NIVision.ShapeMode;


public class Robot extends IterativeRobot {
	//Create object references
	Joystick xbox360drive, xbox360arm;
	RobotDrive driveTrain1;
	Talon talon1, talon2;
	CANTalon talonPulley, talonTwister;
	DriveTrainGyro gyro1;
	Encoder encodeDriveL, encodeDriveR;
	ArmControl armControl;
	AutoProgram autoProgram;
    DigitalInput limitSwitch; //Which limit switch is this?
	int cameraSession;
	Image imageFrame;
	
	public void robotInit() {
		talon1 = new Talon(TALON_1_PORT);
		talon2 = new Talon(TALON_2_PORT);
		talonPulley = new CANTalon(TALON_PULLEY_CAN_ID);
		talonTwister = new CANTalon(TALON_TWISTER_CAN_ID);
		//this is supposed to shut off the motors when joystick is at zero to save power.  Does it work only on Jaguars?
		talon1.enableDeadbandElimination(true);
		talon2.enableDeadbandElimination(true);

		//reversing 1,2 and 3,4 will switch front and back in arcade mode.
		driveTrain1 = new RobotDrive(talon1, talon2);

		//this works to fix arcade joystick
		driveTrain1.setInvertedMotor(RobotDrive.MotorType.kFrontLeft,true);
		driveTrain1.setInvertedMotor(RobotDrive.MotorType.kRearLeft,true);
		driveTrain1.setInvertedMotor(RobotDrive.MotorType.kFrontRight,true);
		driveTrain1.setInvertedMotor(RobotDrive.MotorType.kRearRight,true);

		gyro1 = new DriveTrainGyro(driveTrain1, GYRO1_PORT);
		
		limitSwitch = new DigitalInput(LIMIT_SWITCH_PORT);
		xbox360drive = new Joystick(XBOX0_PORT);
		xbox360arm = new Joystick(XBOX1_PORT);

        armControl = new ArmControl(xbox360arm);
		armControl.debug = true;
		
        encodeDriveL = new Encoder(1,0,false,Encoder.EncodingType.k4X); //parameters taken from Toropov023 branch (Robot.java)
		encodeDriveL.setDistancePerPulse(ENCODER_DIST_PER_PULSE); //Not sure parameter contents. A guess from Toropov023
		encodeDriveL.reset();
		autoProgram = new AutoProgram(talon1, talon2, encodeDriveL);
		autoProgram.setProgram(AUTO_RECYCLE);
		
		setUpSmartDashboard();
		setUpCamera();

	}

	public void disabledInit() {
        armControl.reset();
		talon1.stopMotor();
		talon2.stopMotor();		
		NIVision.IMAQdxStopAcquisition(cameraSession);
	}

	public void autonomousInit() {
		//this will set the Gyro so that zero is the direction that it is pointing when Autonomous begins.
		gyro1.reset();
		autoProgram.init();
	}

	public void autonomousPeriodic() {
		autoProgram.run();
	}

	public void teleopInit() {
		NIVision.IMAQdxStartAcquisition(cameraSession);
	}

	/* This method contains all buttons and joysticks.
	 * It then calls the appropriate method in the correct class.
	 * Exceptions: joystick driving and speed boost is in normalDrive()
	 * 	If the arm needs to be controlled by joystick too, then the arm buttons may be moved to ArmControl class
	 */
	public void teleopPeriodic() {
		
		//TODO: what happens if two buttons are pressed at the same time on the same joystick?!
		//TODO: make sure that it is logical how one button can cancel a previous task.
		//detect buttons
		if (xbox360drive.getRawButton(XBOX_BTN_A)) gyro1.turnPlus45();
		if (xbox360drive.getRawButton(XBOX_BTN_B)) gyro1.turnMinus45();		
		if (xbox360drive.getRawButton(XBOX_BTN_X)) gyro1.orientXAxis();
		if (xbox360drive.getRawButton(XBOX_BTN_Y)) gyro1.orientYAxis();
        
        normalDrive();

        armControl.tick();

        if (gyro1.isTurning()) gyro1.continueTurning();
		
		//pulley system. !limitSwitch.get() -- this means that the switch is still open.
		if(!limitSwitch.get()){	
			talonPulley.set(xbox360arm.getY()*-0.3);
		}else{ // bin motor has hit switch at top
			talonPulley.set(-0.2);
			armControl.stop();
			//TODO: set 3 short rumbles!!!  Also when it hits the bottom.
		}
		//twister system
		talonTwister.set(xbox360arm.getRawAxis(4)*0.8);
		
 		//update camera image
		NIVision.IMAQdxGrab(cameraSession, imageFrame, 1);
		//NIVision.imaqDrawShapeOnImage(imageFrame, imageFrame, new NIVision.Rect(10, 10, 100, 100) , DrawMode.DRAW_VALUE, ShapeMode.SHAPE_OVAL, 0.0f);
		CameraServer.getInstance().setImage(imageFrame);
	}

	// Drive the robot normally, apply speed boost if needed
	private void normalDrive() {
		//Change "L" to "R" in the following two lines if drivers want to use the right xbox joystick for driving
		double stickX = xbox360drive.getRawAxis(XBOX_L_XAXIS); 
		double stickY = xbox360drive.getRawAxis(XBOX_L_YAXIS); 
		double stickMove = stickX * stickX + stickY * stickY;

		if (stickMove > 0.05) gyro1.cancelTurning();

		if (xbox360drive.getRawButton(XBOX_BUMPER_R)) {//high speed mode
			double x2max = xbox360drive.getX() * (MAXSPEED / 100.0);
			double y2max = xbox360drive.getY() * (MAXSPEED / 100.0);
			driveTrain1.arcadeDrive(y2max, x2max, true); //use squared inputs
		} else {
			double x2norm = xbox360drive.getX() * (NORMSPEED / 100.0);
			double y2norm = xbox360drive.getY() * (NORMSPEED / 100.0);
			driveTrain1.arcadeDrive(y2norm, x2norm, true);
		}
	}

	public void testInit() {
		//System.out.println("Default IterativeRobot.testInit() method... Overload me!");
	}

	/* This function is called periodically during test mode */
	/*** Run only one side of robot drive - based on logitech buttons****/
	public void testPeriodic() {
		if (xbox360drive.getRawButton(XBOX_BTN_A) ) {
			talon1.set(xbox360drive.getY());
			talon2.stopMotor();
		} else if (xbox360drive.getRawButton(XBOX_BTN_B) ) {
			talon2.set(xbox360drive.getY());
			talon1.stopMotor();
		} else {
			talon1.stopMotor();
			talon2.stopMotor();
		}
	}

	void setUpSmartDashboard() {
		//hopefully will put a "current status" button on the smart dashboard
		SmartDashboard.putString("Current Status", "fine");
		SmartDashboard.getString("Current Status");

		//Sendable chooser should create a list of selectable objects(they do nothing)
		SendableChooser chooser1 = new SendableChooser();

		chooser1.addDefault("Default", "x");
		chooser1.addObject("Option 1", "y");
		chooser1.addObject("Option 2", "z");
		SmartDashboard.putData("Chooser", chooser1);
	}

	void setUpCamera() {
		imageFrame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
		// the camera name (ex "cam0") can be found through the roborio web interface
		cameraSession = NIVision.IMAQdxOpenCamera("cam0", NIVision.IMAQdxCameraControlMode.CameraControlModeController);
		NIVision.IMAQdxConfigureGrab(cameraSession);
	}

}

