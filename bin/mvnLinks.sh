#!/bin/bash

# Create a project revision specific duplicate of the maven repository out of symlinks.
#
# Existing directories:
# m2
# m2/repository       (m2/r)
# m2/project          (m2/p)
# m2/project/version1 (m2/p/1)
# m2/project/version2 (m2/p/2)
#
# Symlinks created:
# m2/project/m2/1111/org/kuali -> m2/project/1111/org/kuali
# m2/project/m2/1111/org -> m2/repository/org/
# m2/project/m2/1111 -> m2/repository

# mvn default of REPO_HOME would be ~/.m2
export REPO_HOME=/java/m3

# mvn default of REPO_HOME would be ~/.m2/repository
export REPO_DIR=$REPO_HOME/r

# if repository/PACKAGE_FIRST/PACKAGE_SECOND is a directory, then assume we are not in mvnLinks mode
export PACKAGE_FIRST=org
export PACKAGE_SECOND=kuali
if [ -d "$REPO_DIR/$PACKAGE_FIRST/$PACKAGE_SECOND" ]; then
  echo "$REPO_DIR/$PACKAGE_FIRST/$PACKAGE_SECOND is a directory, not in mvnLinks mode"
  exit 1
fi

echo -e "\n\nmvnLinks.sh uses $REPO_HOME as M2_REPO\n\n"

export PROJECT=$1
export PVERSION=$2
export VERSION_REPO=$REPO_HOME/$PROJECT/m2/$PVERSION

# where the mvn REPO_DIR/project.path deliverables have been copied
export PROJECT_REPO=$REPO_HOME/$PROJECT

if [ "$1" = "" ]
then
	echo "Usage: $0 <project to create repository maven links for>"
	exit 1
fi
echo "Creating mvn Links for $1"

if [ ! -e $PROJECT_REPO ]
then
  echo "$PROJECT_REPO doesn't exist skipping mvnLinks.sh"
  exit 1
fi


# recreate linked mvn repository for the passed in version
echo "creating $VERSION_REPO"
rm -rf   $VERSION_REPO
mkdir -p $VERSION_REPO
touch    $VERSION_REPO/$PROJECT.$PVERSION.version.repo

echo "creating $PROJECT_REPO/$PVERSION"
mkdir -p $PROJECT_REPO/$PVERSION
touch    $PROJECT_REPO/$PVERSION/$PROJECT.$PVERSION.project.repo
for dir in $REPO_DIR/*
do
  export DIR_BASE=`basename $dir`
  echo "ln -s $REPO_DIR/$DIR_BASE $VERSION_REPO/$DIR_BASE"
  ln -s $REPO_DIR/$DIR_BASE $VERSION_REPO/$DIR_BASE
done



# create linked mvn org directory so we can control the kuali dir
# TODO multiple packages will require that all the same PACKAGE_FIRST
# are done before any PACKAGE_SECOND are.
echo "Creating mvn $PACKAGE_FIRST Links for $VERSION_REPO"
rm -rf $VERSION_REPO/$PACKAGE_FIRST
mkdir  $VERSION_REPO/$PACKAGE_FIRST
for file in $REPO_DIR/$PACKAGE_FIRST/*
do
  export FILE_BASE=`basename $file`
  echo "ln -s $REPO_DIR/$PACKAGE_FIRST/$FILE_BASE $VERSION_REPO/$PACKAGE_FIRST/$FILE_BASE"
  ln -s $REPO_DIR/$PACKAGE_FIRST/$FILE_BASE $VERSION_REPO/$PACKAGE_FIRST/$FILE_BASE 
done

# create a link for the $PACKAGE_SECOND directory
rm -rf $VERSION_REPO/$PACKAGE_FIRST/$PACKAGE_SECOND
echo "ln -s $PROJECT_REPO/$PVERSION $VERSION_REPO/$PACKAGE_FIRST/$PACKAGE_SECOND"
ln -s  $PROJECT_REPO/$PVERSION $VERSION_REPO/$PACKAGE_FIRST/$PACKAGE_SECOND  

# make sure the repo $PACKAGE_FIRST/$PACKAGE_SECOND cannot be updated!
if [ ! -e $REPO_DIR/$PACKAGE_FIRST/$PACKAGE_SECOND.mvnLinks.enabled ]
then
    rm -rf $REPO_DIR/$PACKAGE_FIRST/$PACKAGE_SECOND
    sudo touch $REPO_DIR/$PACKAGE_FIRST/$PACKAGE_SECOND
    sudo touch $REPO_DIR/$PACKAGE_FIRST/$PACKAGE_SECOND.mvnLinks.enabled
fi



sed "s|M2_REPO|$VERSION_REPO|g" $MVN_UTIL_HOME/etc/settings.xml > ~/.m2/settings-$PROJECT-$PVERSION.xml
