# SpareTimeMiner
The SpareTimerMiner (STM) is a Java program that controls the launching and closing of Nimiq Miners as deemed necessary based on Key and Mouse input.

Based on the frequency set in the config file, the program will stop and choose whether to end the current miner and start a more or less intensive one based on the number of interactions in the time frame.  Every 15 seconds stats are logged to the console, and a counter is increased.  When this counter comes above the set frequency (default of 20 counts, or 5 minutes) the amount of interactions since the last check is compared to the average interactions.  If it's less than 3/4 of the average then the light miner command will be used and if it's greater than 3/4 the tiny miner command will be used.  The heavy miner command is only used when there have been less than 1 interaction every 15 seconds on average.

## Tracking Mouse and Key Input
Using the Java Native Hook library, key and mouse presses can be listened to even when the Java application isn't the active window.  These key presses are not recorded or sent anywhere and the frequency is all that is recorded.  Average interactions (a key press or mouse click) are recorded along with the number of clicks done in a certain timeframe.

## The Config File
A sample config file is included in this repository without the light, heavy, or tiny commands present.  If these commands are not present the program will fail to start.  The command used can be anything, whether a batch file, shell file, or a single command that runs the NodeJS miner for whichever pool you are using.
