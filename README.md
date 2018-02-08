ABOUT
=====

Java Command Line Client for the IOTA Spam Fund (www.iotaspam.com). Work in progress, bugs should be expected.


SIMPLE INSTALLATION
===================

If you have never heard about Maven, you should stick with this rather simple "installation" (basically a download).

1) download the .jar file from the [releases page](https://github.com/mikrohash/isf-jclient/releases)
2) put the .jar into a folder which is in another folder, for example 'Desktop/[FOLDER 1]/[FOLDER 2]/' (your jar is in the folder [FOLDER 2], but will create files in [FOLDER 1])
3) if you haven't already, now is the time to install the JRE (Java Runtime Environment) or JDK (Java Development Kit, which includes the JRE) so you can execute the .jar file in the next step
4) open your console, `cd` yourself into [FOLDER 2] and run the jar: `java -jar isf-jclient-[VERSION].jar `

MAVEN INSTALLATION (UBUNTU)
===========================

If you prefer to compile the .jar file yourself and know how to use a terminal, use this guide. The process is similar on other OS.

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
	$ mvn install

STARTING THE JAR
----------------

Simply start the .jar to run the spamming tool:

	$ java -jar target/isf-jclient-[VERSION].jar

After starting it for the first time, you will be guided through the configuration. You can always stop the spammer using `CTRL+C`.