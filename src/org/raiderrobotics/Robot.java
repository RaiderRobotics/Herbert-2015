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
	DriveTrainGyro gyro1;
	Encoder encodeDriveL, encodeDriveR;
	ArmControl armControl;
	BinArmSystem binArmSystem;
	AutoProgram autoProgram;
	int cameraSession;
	Image imageFrame;
	
	public void robotInit() {
		talon1 = new Talon(TALON_1_PORT);
		talon2 = new Talon(TALON_2_PORT);
		/* deatBandElimination is supposed to shut off the motors when joystick is at zero to save power.  
		 * It is part of the PWM motor controller class. It does not exist in CAN mode.		 */
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
		
		xbox360drive = new Joystick(XBOX0_PORT);
		xbox360arm = new Joystick(XBOX1_PORT);

        armControl = ArmControl.setupInstance(xbox360arm);
		//armControl.debug = true;
		
		//make an ArmControl instance in BinArmSystem in order to access armControl
		binArmSystem = new BinArmSystem(xbox360arm);
		
        encodeDriveL = new Encoder(1,0,false,Encoder.EncodingType.k4X); //parameters taken from Toropov023 branch (Robot.java)
		encodeDriveL.setDistancePerPulse(ENCODER_DIST_PER_PULSE); //Not sure parameter contents. A guess from Toropov023
		encodeDriveL.reset();
		
		autoProgram = new AutoProgram(talon1, talon2, encodeDriveL, gyro1);
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
        
        	binArmSystem.tick();

        	if (gyro1.isTurning()) gyro1.continueTurning();
		
 		//update camera image
		NIVision.IMAQdxGrab(cameraSession, imageFrame, 1);
		//NIVision.imaqDrawShapeOnImage(imageFrame, imageFrame, new NIVision.Rect(10, 10, 100, 100) , DrawMode.DRAW_VALUE, ShapeMode.SHAPE_OVAL, 0.0f);
		CameraServer.getInstance().setImage(imageFrame);
	}

	// Drive the robot normally, apply speed boost if needed
	private void normalDrive() {
		//Change "L" to "R" in the following two lines if drivers want to use the right xbox joystick for driving
		double stickX = xbox360drive.getRawAxis(XBOX_L_XAXIS); 
		double stickY = xbox360drive.getRawAxis(XBOX_L_YAXIS) - 0.01; 
		double stickMove = stickX * stickX + stickY * stickY;
		
		if (stickMove > 0.05) gyro1.cancelTurning();
		
		if (xbox360drive.getRawButton(XBOX_BUMPER_R)) {//high speed mode
//			double x2max = xbox360drive.getX() * (MAXSPEED / 100.0) - 0.05;
//			double y2max = (xbox360drive.getY() - 0.01) * (MAXSPEED / 100.0) - 0.05;
			double x2max = stickX * (MAXSPEED / 100.0); //Do not amplify X motions: + Math.signum(stickX)*0.05;
			double y2max = stickY * (MAXSPEED / 100.0) + Math.signum(stickY)*0.05;
			driveTrain1.arcadeDrive(y2max, x2max, false); //use squared inputs. Herbert#2: set to false
		} else {
				
			double x2norm = stickX * (NORMSPEED / 100.0); // + Math.signum(stickX)*0.05;
			double y2norm = stickY * (NORMSPEED / 100.0) + Math.signum(stickY)*0.05;
			driveTrain1.arcadeDrive(y2norm, x2norm, false);
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

