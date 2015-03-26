package org.raiderrobotics.utils;

public class CircularOperation {
	public static double getFundimentalAngle(double angle){
		return angle%360;
	}
	
	
	public static int getQuadrant(double angle){ 
		angle = getFundimentalAngle(angle);
		if(angle >= 270 && angle < 360){
			return 1;
		}else if(angle >= 0 && angle < 90){
			return 2;
		}else if(angle >= 90 && angle < 180){
			return 3;
		}else if(angle >= 180 && angle < 270){
			return 4;
		}else{
			return 0;
		}
		
	}

	public static double offsetZero(double angle){
		angle = getFundimentalAngle(angle);
		if(angle < 180){
			return angle;
		}else{
			return angle-360;
		}
	}
	
	
	public static double offsetCostume(double orginAngle, double offsetAngle){
		orginAngle = getFundimentalAngle(orginAngle);
		offsetAngle = getFundimentalAngle(offsetAngle);
		offsetAngle = offsetAngle - orginAngle;
		
		return offsetZero(offsetAngle);
	}
	
	
}
