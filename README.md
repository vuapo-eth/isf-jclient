ABOUT
=====

Java Command Line Client for the IOTA Spam Fund (www.iotaspam.com). Work in progress, bugs should be expected.


SIMPLE INSTALLATION
===================

If you have never heard about Maven, you should stick with this rather simple "installation" (basically a download).

1) download the .jar file from the [releases page](https://github.com/mikrohash/isf-jclient/releases)

2) put the .jar into a folder which is in another folder, for example 'Desktop/[FOLDER 1]/[FOLDER 2]/' (your jar is in [FOLDER 2], but will create files in [FOLDER 1])

3) if you haven't already, now is the time to install the JRE (Java Runtime Environment) or JDK (Java Development Kit, which includes the JRE) so you can execute the .jar file in the next step

4) open your console, `cd` yourself into [FOLDER 2] and run the jar: `java -jar isf-jclient-[VERSION].jar `

MAVEN INSTALLATION (UBUNTU)
===========================

If you prefer to compile the .jar file yourself and know how to use a terminal, use this guide. The process is similar on other OS. Depending on your permissions, you might have to write `sudo` in front of each line.

REQUIREMENTS
------------

Please make sure you have the JDK (java Developement Kit) and Maven installed. Here is how to install them:

	$ apt update

	# install Java8 and Maven
	$ add-apt-repository ppa:webupd8team/java
	$ apt-get update
	$ apt-get install oracle-java8-installer
	$ apt-get install -y maven 
	$ apt-get install oracle-java8-set-default

	$ java -version >> /dev/null

The last command should return something similar to this:

	$ java -version >> /dev/null
	java version "1.8.0_144"
	Java(TM) SE Runtime Environment (build 1.8.0_144-b01)
	Java HotSpot(TM) 64-Bit Server VM (build 25.144-b01, mixed mode)

credits to [this guide](https://medium.com/@scott.tudd/an-almost-complete-guide-to-setting-up-a-full-iota-node-d9784dfdc80).

Now you have to install the official iota.lib.java library (the spammer was built for 0.9.11-SNAPSHOT, if you are using a newer version, you have to modify the pom.xml accordingly):

    $ cd ~
    $ wget https://github.com/iotaledger/iota.lib.java/archive/master.zip
    $ unzip master.zip
    $ cd iota.lib.java-master
    $ mvn install
    $ mvn install:install-file -Dfile=jota/target/jota-0.9.11-SNAPSHOT.jar -DgroupId=com.github.iotaledger -DartifactId=iota.lib.java -Dversion=0.9.11-SNAPSHOT -Dpackaging=jar

INSTALLATION
------------

Move to the directory where you want to install the project (e.g. '/opt/'). Then create the executable .jar directly from the GitHub source code:

    # move to the directory where you want to install the spammer
    $ cd ~
    
    # download and unzip
    $ wget https://github.com/mikrohash/isf-jclient/archive/master.zip
    $ unzip master.zip

    # compile it into a runnable .jar file
    $ cd isf-jclient-master
    $ mvn install

STARTING THE JAR
----------------

Simply start the .jar to run the spamming tool:

	$ java -jar target/isf-jclient-[VERSION].jar

After starting it for the first time, you will be guided through the configuration. You can always stop the spammer using `CTRL+C`.
