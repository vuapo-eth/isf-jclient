# ABOUT

Java Command Line Client for the IOTA Spam Fund (www.iotaspam.com). Work in progress, bugs should be expected.


# SIMPLE INSTALLATION

If you have never heard about Maven, you should stick with this rather simple "installation" (basically a download).

1. download the .jar file from the [releases page](https://github.com/mikrohash/isf-jclient/releases) and put it somewhere where it has write access (e.g. into `Desktop/isf-jclient/`)

2. if you haven't already, now is the time to install the JRE (Java Runtime Environment) or JDK (Java Development Kit, which includes the JRE) so you can execute the .jar file in the next step

3. open your console, `cd` yourself into the jar's directory and simply start it: `java -jar isf-jclient-[VERSION].jar `


# INSTALLATION VIA MAVEN

If you prefer to compile the .jar file yourself and know how to use a terminal, use this guide. Just follow through the steps.

## 1. GET THE REQUIREMENTS (JRE & MAVEN)

Please make sure you have the **JRE** (Java Runtime Environment) and **Maven** installed. Here is how to do that:

### UBUNTU

Depending on your permissions, you might have to write `sudo` in front of each line.

	$ apt update
	$ add-apt-repository ppa:webupd8team/java
	$ apt-get update
	$ apt-get install oracle-java8-installer
	$ apt-get install -y maven 
	$ apt-get install oracle-java8-set-default

To check whether the installations went successfully, execute these commands:

	$ java -version
	$ mvn --version

credits to [this guide](https://medium.com/@scott.tudd/an-almost-complete-guide-to-setting-up-a-full-iota-node-d9784dfdc80).

### WINDOWS

Please install the **JRE** from [oracle.com](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html). You can check whether it works by executing `java -version` in your console. After that, proceed to install **Maven**:

1. download the latest version of Maven from [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)

2. unzip it into the folder where you want to install Maven (e.g. C:\Program Files\Apache\maven)

3. add both `M2_HOME` and `MAVEN_HOME` variables in the Windows system enviroment and point it to the maven folder (C:\Program Files\Apache\maven)

4. update the `PATH` variable, append Maven bin folder: %M2_HOME%\bin (now you can run maven commands everywhere)

5. to test whether it works, execute `mvn --version` in cmd, this should show you details about the installed version, directory etc.

## 2. INSTALL THE DEPENDENCIES

Our spammer makes use of the official IOTA java library **JOTA** ([iota.lib.java](https://github.com/iotaledger/iota.lib.java)). You need to install it locally before you can run our spammer. There are several ways to do that:

### METHOD A: DOWNLOAD VIA WGET

	$ cd ~/somewhere-over-the-rainbow/
	$ wget https://github.com/iotaledger/iota.lib.java/archive/master.zip
	$ unzip master.zip
	$ cd iota.lib.java-master
	$ mvn install
	
### METHOD B: DOWNLOAD VIA GIT

For this you will need the GIT plugin (installation on UBUNTU via: `sudo apt-get install git`)

	$ cd ~/somewhere-over-the-rainbow/
	$ git clone https://github.com/iotaledger/iota.lib.java
	$ cd iota.lib.java
	$ mvn install
	
### METHOD C: MANUAL DOWNLOAD

1. go to [github.com/iotaledger/iota.lib.java](https://github.com/iotaledger/iota.lib.java)

2. click on `Clone or download` and select `Download ZIP`

3. unzip the downloaded master.zip file

4. open you console (CMD on windows) and `cd` yourself into the directory `iota.lib.java-master/` you just unzipped

5. execute `mvn install` to locally install the iota.lib.java repository

## 3. CREATE THE JAR FILE

Simply create the executable .jar directly from the GitHub source code. Again, there are several ways to do that:

### METHOD A: DOWNLOAD VIA WGET

	$ cd ~/my-favorite-directory/
	$ wget https://github.com/mikrohash/isf-jclient/archive/master.zip
	$ unzip master.zip
	$ cd isf-jclient-master
	$ mvn versions:use-latest-versions -DallowSnapshots=true -DexcludeReactor=false
	$ mvn install

### METHOD B: DOWNLOAD VIA GIT

For this you will need the GIT plugin (installation on UBUNTU via: `sudo apt-get install git`)
	
	$ cd ~/my-favorite-directory/
	$ git clone https://github.com/mikrohash/isf-jclient
	$ cd isf-jclient
	$ mvn versions:use-latest-versions -DallowSnapshots=true -DexcludeReactor=false
	$ mvn install
	
### METHOD C: MANUAL DOWNLOAD

1. go to [github.com/mikrohash/isf-jclient](https://github.com/mikrohash/isf-jclient)

2. click on `Clone or download` and select `Download ZIP`

3. unzip the downloaded master.zip file

4. open you console (CMD on windows) and `cd` yourself into the directory `isf-jclient-master/` you just unzipped

5. run `mvn versions:use-latest-versions -DallowSnapshots=true -DexcludeReactor=false` to update all dependencies to the newest versions

5. execute `mvn install` to locally install the iota.lib.java repository

## 4. RUN THE JAR FILE

Simply start the .jar to run the spamming tool:

	$ java -jar isf-jclient-[VERSION].jar

After starting it for the first time, you will be guided through the configuration. You can always stop the spammer using `CTRL+C`.
