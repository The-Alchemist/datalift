						COMPILATION AND INSTALLATION OF RDF-XML-CONVERSION MODULE

Please follow the steps to compile and use this module

Requirements(additional for this module):
Maven

1. Installing Maven
Maven is required to compile and install this module. To install maven, download maven from the website, http://maven.apache.org/download.html. To avoid any unforseen problems, maven v2.2.1 is recommended. For installing instructions, please refer: http://maven.apache.org/download.html#Installation

To check if maven is correctly installed, try executing "mvn -version" in a command prompt. You should be able to see maven's version along with java version and your system's architecture information.

2. Compile the sources
a. Go to the home folder of the module. You should be able to see "pom.xml" file among others
b. Maven pulls all the dependencies(which are declared in the pom.xml file) from its central repository(an internet connection is needed!). Those dependecies not available in the central repository should be installed locally. To install local dependencies, execute the follwing two commands from the module's home folder

mvn install:install-file -Dfile=lib/sesame-toolkit-6.8.1-SNAPSHOT.jar -Dpackaging=jar -DgroupId=com.mondeca.share -artifactId=sesame-toolkit -Dversion=6.8.1-SNAPSHOT

mvn install:install-file -Dfile=lib/rdf-transform-6.8.1-SNAPSHOT-onejar.jar -Dpackaging=jar -DgroupId=com.mondeca.clientapps -DartifactId=rdf-transform -Dversion=6.8.1-SNAPSHOT


c. Compile the sources with this command "mvn clean install". This command will compile, install the compiled artifact(jar) in your local repository and copies the same to the modules folder(you should make sure that $DATALIFT_HOME is already set)

d. (Re)Start your datalift server and you should be able to access the conversion module as soon as corresponding source files are uploaded.

3. Recompiling the sources
If you need to recompile the sources, simply execute "mvn clean install" command in the home folder of the module.
