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
	ArmControl armControl;

	BinArmSystem(Joystick xbox){
		this.xbox = xbox;
		talonPulley = new CANTalon(TALON_PULLEY_CAN_ID);
		talonTwister = new CANTalon(TALON_TWISTER_CAN_ID);
		limitSwitch = new DigitalInput(TOP_LIMIT_SWITCH_PORT);
		armControl = ArmControl.getInstance();
	}

	public void tick(){
		//pulley system. !limitSwitch.get() -- this means that the switch is still open.
		if(!limitSwitch.get()){
			//up and down have different speeds because of gravity
			pulleyPower = xbox.getRawAxis(XBOX_L_YAXIS);
			
			if(pulleyPower<0.0){ //going up
				talonPulley.set(pulleyPower*-0.5);
			}
			else if(pulleyPower>0.0){
				talonPulley.set(pulleyPower*-0.3); //going down
			}
			else{
				if(xbox.getRawButton(XBOX_BTN_BACK)){
					talonPulley.set(0.05);//maintain power
				}else{
					talonPulley.set(0.0);
				}
			}
		}else{ // bin motor has hit switch at top
			talonPulley.set(-0.2);	//maintain 20% power.
			//armControl.stop();  //this does not work for all cases reliably
		}
		//twister system
		talonTwister.set(xbox.getRawAxis(XBOX_R_XAXIS)*0.8);
	}
	
	
}
