package org.raiderrobotics;

import static org.raiderrobotics.RobotMap.*;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Joystick;

public class BinArmSystem {
	CANTalon talonPulley, talonTwister;
	Joystick xbox;
    DigitalInput limitSwitch;
	
	BinArmSystem(Joystick xbox){
		this.xbox = xbox;
		talonPulley = new CANTalon(TALON_PULLEY_CAN_ID);
		talonTwister = new CANTalon(TALON_TWISTER_CAN_ID);
		limitSwitch = new DigitalInput(LIMIT_SWITCH_PORT);
	}
	
	public void tick(){
		//pulley system. !limitSwitch.get() -- this means that the switch is still open.
		if(!limitSwitch.get()){	
			talonPulley.set(xbox.getRawAxis(XBOX_L_YAXIS)*-0.3);
		}else{ // bin motor has hit switch at top
			talonPulley.set(-0.2);
		}
		//twister system
		talonTwister.set(xbox.getRawAxis(XBOX_R_XAXIS)*0.8);
		
	}
}
