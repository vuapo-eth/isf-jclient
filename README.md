ABOUT
=====

Java Command Line Client for the IOTA Spam Fund (www.iotaspam.com). Work in progress.


INSTALLATION (UBUNTU)
=====================	

REQUIREMENTS
------------

Depending on your permissions, you might have to write `sudo` in front of each line.

Please make sure you have the JDK (java Developement Kit) and Maven installed. Here is how to install them:

	$ apt update

	# install Java8
	$ add-apt-repository ppa:webupd8team/java
	$ apt-get update
	$ apt-get install oracle-java8-installer
	$ apt-get install -y maven 

	# set environment variables
	$ apt-get install oracle-java8-set-default

	$ java -version >> /dev/null

The last command should return something similar to this:

	$ java -version >> /dev/null
	java version "1.8.0_144"
	Java(TM) SE Runtime Environment (build 1.8.0_144-b01)
	Java HotSpot(TM) 64-Bit Server VM (build 25.144-b01, mixed mode)

credits to [this guide](https://medium.com/@scott.tudd/an-almost-complete-guide-to-setting-up-a-full-iota-node-d9784dfdc80)

INSTALLATION
------------

Move to the directory where you want to install the project (e.g. '/opt/'). Then create the executable .jar directly from the GitHub source code:

	# move to the directory where you want to install the spammer
	$ cd /opt/
	
	# download and unzip
	$ wget https://github.com/mikrohash/isf-jclient/archive/master.zip
	$ unzip master.zip
	
	# compile it into a .jar file
	$ cd isf-jclient-master
	$ mvn clean compile package

STARTING THE JAR
----------------

Simply start the .jar to run the spamming tool:

	$ java -jar target/isf-jclient-[VERSION].jar

After starting it for the first time, you will be guided through the configuration. You can always stop the spammer using `CTRL+C`.