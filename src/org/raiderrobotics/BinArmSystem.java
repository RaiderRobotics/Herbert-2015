package org.raiderrobotics;

import static org.raiderrobotics.RobotMap.*;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;

public class BinArmSystem {
	CANTalon talonPulley, talonTwister;
	Joystick xbox;
    DigitalInput limitSwitch;
	double pulleyPower;
	boolean backPushed = false;
	boolean topReached = false;
	ArmControl armControl;
	
	BinArmSystem(Joystick xbox){
		this.xbox = xbox;
		talonPulley = new CANTalon(TALON_PULLEY_CAN_ID);
		talonTwister = new CANTalon(TALON_TWISTER_CAN_ID);
		limitSwitch = new DigitalInput(TOP_LIMIT_SWITCH_PORT);
		armControl = ArmControl.getInstance();
	}
	
	public void tick(){
		
		if(xbox.getRawButton(XBOX_BTN_BACK)){
			backPushed = true;
		}
				
		//pulley system. !limitSwitch.get() -- this means that the switch is still open.
		if(!limitSwitch.get()){
			//up and down have different speeds because of gravity 
			pulleyPower = xbox.getRawAxis(XBOX_L_YAXIS);
			if(pulleyPower<-0.10){ //going up
				talonPulley.set(pulleyPower*-0.5);
				topReached = false;
				backPushed = false;
			}else if(pulleyPower>0.10){
				talonPulley.set(pulleyPower*-0.3); //going down
				topReached = false;
				backPushed = false;
			}else{

				
				
				if(backPushed){ //only when the back button is pushed to run this code
					if(topReached){ 
						talonPulley.set(0.15); //try to hold the bin there. This number will need to be adjusted
												//by trial and error.
					}else{
						talonPulley.set(0.5);
					}
				}else{
					talonPulley.set(0.0);
				}
			}
			
		}else{ // bin motor has hit switch at top
			if(backPushed) topReached = true;
			talonPulley.set(-0.2);	//maintain 20% power.
			//armControl.stop();  //this does not work for all cases reliably
		}
		//twister system
		talonTwister.set(xbox.getRawAxis(XBOX_R_XAXIS));
		
	}
	
	void stopRotation() {
		talonTwister.set(0);
	}
}
