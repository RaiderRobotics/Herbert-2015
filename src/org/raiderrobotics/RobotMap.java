package org.raiderrobotics;

/* This class is a list of constants used in the main program. All must be static.
 * Should this class be abstract too?
*/

/****************************
 * Updates: 
 * Feb 8: MAXSPEED set to 100%
 * 
 * 
 * ***************************/
public final class RobotMap {

	/* FINAL constants */
	//final static int ARCADE = 1;
	//final static int TANK = 2;
	final static int NORMSPEED = 75;	//% speed of normal driving
	final static int MAXSPEED = 100;	
	
	/* joysticks and buttons */
	//Logitech joystick
	final static int LOGITECH_TRIGGER = 1;
	final static int LOGITECH_BTN2 = 2;
	final static int LOGITECH_BTN3 = 3;
	final static int LOGITECH_BTN4 = 4;
	final static int LOGITECH_BTN5 = 5;
	final static int LOGITECH_BTN6 = 6;
	//xbox 360 controller buttons
	final static int XBOX_BTN_A = 1;
	final static int XBOX_BTN_B = 2;
	final static int XBOX_BTN_X = 3;
	final static int XBOX_BTN_Y = 4;
	final static int XBOX_BUMPER_L = 5;
	final static int XBOX_BUMPER_R = 6;
	final static int XBOX_BTN_BACK = 7;
	final static int XBOX_BTN_START = 8;
	//Xbox 360 controller joysticks
	final static int XBOX_L_XAXIS = 0;
	final static int XBOX_L_YAXIS = 1;
	final static int XBOX_L_TRIGGER = 2;
	final static int XBOX_R_TRIGER = 3;
	final static int XBOX_R_XAXIS = 4;
	final static int XBOX_R_YAXIS = 5;
	
	/* Port allocation */
	//Joystick ports
	//final static int LOGITECH_PORT = 0;
	final static int XBOX0_PORT = 0;
	final static int XBOX1_PORT = 1;
	
	//motor controller ports
	final static int TALON_1_PORT = 1;
	final static int TALON_2_PORT = 2;
	//talons 3 and 4 are only in the ArmControl (and don't have to be here)
	final static int TALON3_CAN_ID = 3;
	final static int TALON4_CAN_ID = 4;	
	//talons 5 and 6 are only in the BinControl (and don't need to be here)
	final static int TALON_TWISTER_CAN_ID = 5;
	final static int TALON_PULLEY_CAN_ID = 6; 
	
	//sensor ports
	final static int GYRO1_PORT = 0;
	final static int LEFT_ARM_SWITCH_PORT = 4;	//the TR Electronics sensors
	final static int RIGHT_ARM_SWITCH_PORT = 5;
	final static int TOP_LIMIT_SWITCH_PORT = 6;

	
	//sensor config values
	final static double ENCODER_DIST_PER_PULSE = 0.1665; //Note: this will not work at high speeds. The robot will overshoot.
	final static double GYRO_SENSITIVITY = 0.007; // "Our gyro is ADRSX622 , with a sensitivity of 7 mV/degree/sec" --Mr.Harwood
	
	//autonomous mode constants
	//TODO: make the programs into ENUM
	final static int AUTO_RECYCLE = 0;
	final static int AUTO_TOTE = 1;
	final static int AUTO_MULTITOTE = 2;
	
	//NOTE!!! THe following 4 settings are for HERBERT V1, not the second one that we built.
	final static double AUTO_ZONE_DISTANCE = -3000.0;	// originally +3000.0 and - 500.0
	final static double AUTO_BACKUP_DISTANCE = +500.0;
	final static double AUTO_SPEED_FWD = 1.0; //was 0.8
	final static double AUTO_SPEED_BCK = 0.4;  //TODO if this is 0.5 or higher, the autoprogram must use rampToSpeed() or the talons and battery get damaged.
	
	final static double TALONRAMPINCREMENT = 0.1; 
	/* NOTE: make a separate constant if you are using the multiply method for ramping the Talons. 
	 * This is designed and intended for the addition method.
	 * Do NOT set this to 1.1 . Make a new constant.
	 */

	/*** location of HAll effect sensor ports */ 
	final static int HALL_L_MID_PORT = 10;
	final static int HALL_R_MID_PORT = 11;
	final static int HALL_L_TOP_PORT = 12;
	final static int HALL_R_TOP_PORT = 13;
	
}
