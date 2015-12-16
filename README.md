## Recycle Rush - Final repository

## >>> Dec 2015: Nothing was ever done with this <<< 

### This is a temporary branch that is being used to test two things:
### 1. autonomous mode selection via the DIP switches.
### 2. using the gyro to orient the robot

------------

#### What's been done: 

1. Autonomous mode kind of works using the dip switches on the extra board that we made (MXP port).  However, I think that the wiring or pins are numbered incorrectly since only 1 or 2 of the 4 actually work. I'll check the pin numbers and wiring in the next few days (Dec 16). But *from a proof of concept point of view, IT WORKS!*

2. The Gyro code works. I still need to do exhaustive testing. I'm using the LCD display to print out the angle (which helped me troubleshoot things to find that the gyro was actually not plugged in! It wasn't my code, it was no connections!).

 * Buttons: Y = turn to 0 degrees
 * X = turn to -90 degrees
 * B = turn to +90 degrees
 * A = turn to 180 degrees
 
Right now it is turning slowly for troubleshooting purposes. Ideally, it would use "rampToSpeed" code o get to 100% and then PID to slow down as it approached the target angle. The gyro turning is disabled as soon as the joystick is moved more than 5%.

To fix: in conjunction with the above paragraph, fix this:  right now, it stops when it is within 8 degrees of target angle.

