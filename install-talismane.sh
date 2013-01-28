#!/usr/bin/env bash

#Shell scipt that
# * clones talismane form github.com
# * compiles using ant
# * installs jars to the local maven repo
#
# Those Steps are required before you the stanbol-talismane can be compiled

# ceck if talismane is already present
if [[ ( ! -f "talismane/build.xml" ) ]]
then
    #clone from Git
    git -c diff.mnemonicprefix=false -c core.quotepath=false clone \
        --recursive https://github.com/urieli/talismane.git ./talismane
fi
#compile using ant
ant -f talismane/talismane_utils/build.xml jar
ant -f talismane/talismane_machine_learning/build.xml jar
ant -f talismane/talismane_core/build.xml jar
ant -f talismane/talismane_lefff/build.xml jar
ant -f talismane/talismane_ftb/build.xml jar
ant -f talismane/talismane_ftbDep/build.xml jar
ant -f talismane/talismane_fr/build.xml jar
# now the required jar should be in 'talismane/dist'
# and we can install them to the local mvm repository
mvn install:install-file \
    -Dfile=talismane/dist/talismane-core-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-core \
    -Dversion=1.3.3b \
    -Dpackaging=jar
mvn install:install-file \
    -Dfile=talismane/dist/talismane-fr-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-fr \
    -Dversion=1.3.3b \
    -Dpackaging=jar
mvn install:install-file \
    -Dfile=talismane/dist/talismane-ftb-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-ftb \
    -Dversion=1.3.3b \
    -Dpackaging=jar
mvn install:install-file \
    -Dfile=talismane/dist/talismane-ftbDep-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-ftbDep \
    -Dversion=1.3.3b \
    -Dpackaging=jar
mvn install:install-file \
    -Dfile=talismane/dist/talismane-lefff-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-lefff \
    -Dversion=1.3.3b \
    -Dpackaging=jar
mvn install:install-file \
    -Dfile=talismane/dist/talismane-machineLearning-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-machineLearning \
    -Dversion=1.3.3b \
    -Dpackaging=jar
mvn install:install-file \
    -Dfile=talismane/dist/talismane-utils-1.3.3b.jar \
    -DgroupId=com.joliciel.talismane \
    -DartifactId=talismane-utils \
    -Dversion=1.3.3b \
    -Dpackaging=jar

