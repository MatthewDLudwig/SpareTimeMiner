# SpareTimeMiner
The SpareTimerMiner (STM) is a Java program that controls the launching and closing of Nimiq Miners as deemed necessary based on Key and Mouse input.  Three possible modes can be used each ideally using more resources; tiny, light, and heavy.  The commands to run for each mode are provided in the config file and so different miners can be used, or other settings past thread count can be changed to provide optimum performance in each of the three situations.

## The 3 Modes
### Tiny Mode
Tiny mode is turned on when the user has interacted with the computer more than the average.  The command for tiny mode should use the least amount of resources compared to the other two commands.  Tiny mode will always be the result of a mode recheck (manual or automatic) if movie mode is on.
### Light Mode
Light mode is turned on when the user has interacted with the computer less than the average, but more than once every 15 seconds.  The command for light mode should allow for your computer to run normally without affecting your normal use and while still providing a decent hash rate.  The final decision for what the command should do is up to the end user though.
### Heavy Mode
Heavy mode is turned on when the user has interacted with the computer less than once every 15 seconds.  This mode is mainly meant for computers that don't go to sleep after prolonged lack of use so that the miner can use more resources.  The main reason STM was created was for this exact reason as I had grown tired of changing the miner program manually before bed and when waking up.

## The Config File
A sample config file is included in this repository without the light, heavy, or tiny commands present.  If these commands are not present the program will fail to start while printing an error.  A description of each config field is provided below:

### tiny
* This field is where the command to be run in tiny mode should go.
* Quotes need not be escaped and spaces within the command are fine.
* The only disallowed string is "=:=" but I'd be suprised if that's absolutely necessary for a command.
* The command to be run can be a batch file, shell file, or a single command.
### light
* This field is the same as "tiny" except the command is used for light mode.
### heavy
* This field is the same as "tiny" except the command is used for heavy mode.
### frequency
* This field controls how often STM will consider changing miner modes.
* Every 15 seconds a counter is ticked up and once it exceeds the value of "frequency" a change is considered.
* A standard value of 20 will have STM checking every 5 minutes.
### exit
* This field specifies the sequence of "F_" keys to be pressed to tell the program to exit.
* Any "F_" key can be used but only 1-12 have been tested.
* The list of numbers (without the F) should be comma separated.
  * Spacing does not matter as each number is trimmed before being parsed.
### movie
* This field is the same as "exit" except it tells the program to switch the movie mode.
* Movie mode is meant to allow the miner to continue running while watching a movie or playing a game.
* While watching a movie:
  * There will most likely be little interaction with the computer.
  * This would normally lead to a switch to "heavy" mode.
  * This could have the potential to slow down the movie which is undesirable.
* While playing a game:
  * There will most likely be a large number of interactions with the computer.
  * This would normally lead to a switch to "tiny" mode.
  * Unfortunately this switch will only occur every 5 minutes and could be reevaluated mid game.
    * If the stars align, "heavy" mode could be switched on mid game.
  * This could also lead to a skew in the average interactions with the computer.
* Any rechecks whether based on frequency or forced will always result in tiny mode.
* While in movie mode the interaction counter will be reset normally each check, but the average won't be updated.
* There is currently no way to know whether movie mode was turned on or off.
  * Changing the tone was considered and may be added later, but memorizing tones isn't ideal.
* A switch to movie mode still requires a force recheck if you want the change to occur immediately.
### force
* This field is the same as "exit" except it tells the program to force a recheck of which mode to be in.
* Average interactions is updated when a force occurs and frequent forces may skew the average.
### initial
* This field allows the user to specify the inital mode.
* Possible values are "tiny", "light", and "heavy".
  * Numeric values are allowed as well and are 0, 1, and 2 respectively.
### bias
* This field allows the user to specify a bias on the interaction average.
* This is useful so that restarts don't cause a loss of the average.
* The value should be 2 numbers comma separated.
  * The first number is the bias you want set.
  * The second number is the strength of the bias.
* A value from 5-10 for bias strength is recommended.
  * Higher values will make the average interactions slower to adapt.
  * Lower values will lead to the bias making little difference.
* Bias values are dependent on the user but some recommendations are made below:
  * 200 is good for average use and slow typing.
    * This is a good bias for programming involving long bouts of though inbetween typing.
  * 600 is good for typing large amounts quickly.
    * Prior to writing the updated version of this readme I was using 200 as my bias.
    * I was in "tiny" mode the whole time and would have preferred "light" as typing text isn't incredibly intensive.
    * By the end my average was around 600 and "tiny" mode was still the mode of choice for the program.
      * For those writing text often a larger value around 800 may be better.
### beeps
* This field is similar to a logging mode field.
* Possible values are "none", "onoff", "some", and "all".
  * Numeric values are allowed and are 0, 1, 2, and 3 respectively.
* Value: "none"
  * No tones will be made to alert the user of anything.
  * This mode is not recommended but I can't put beeps into a program without a way to turn them off.
* Value: "onoff"
  * Tones will only be made when the miner turns on and off.
  * This is the minimum recommended value as the actual miner may take time to output to the console.
    * As this program is meant to be ran and forgot about, knowing that it started succesfully is important.
* Value: "some"
  * Tones will be made when the miner turns on and off, as well as when commands are given to the program.
    * Commands are those specified in the config file such as "exit" and "force".
  * This is the default mode if no value is specified in the config file.
  * Not being able to tell when your "exit" command goes through is a bit annoying.
    * Alternatively giving the command multiple times gets the job done without tones.
* Value: "all"
  * Tones will be made like in previous modes, but includes different tones for when the mode switches.
    * A lower frequency tone will be played for "heavy".
    * A higher frequency tone will be played for "tiny".
    * The tone for "light" is at 500 Hz and may be recognized from the start up / shut down tones.
  * This mode is my personal favorite as I like to know when I switch modes.
    * While still finding a good value for bias, it's useful to know when you switch from light to tiny.

## Miscellaneous Information
* This program will record the current hash rate and print statistics to the console every 15 seconds.  
* Upon exitting, the program will print the final average number of interactions.
  * The average hash rate over the whole time the program was running will also be printed.
  * The average hash rate is printed since the rates the 3 modes give should be different, and the overall rate is a useful statistic.
### Tracking Mouse and Key Input
Using the Java Native Hook library, key and mouse presses can be listened to even when the Java application isn't the active window.  These key presses are not recorded or sent anywhere and the frequency is all that is recorded.  Average interactions (a key press or mouse click) are recorded along with the number of clicks done in a certain timeframe.
