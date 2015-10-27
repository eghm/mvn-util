/**
Symlink a localRepository project version duplicate of a maven repository.

-Dmvn.repo.home=/j/m2 -Dmvn.repository.dir=r project 1111 package.one

Existing directories:
/j/m2                    # aka ~/.m2
/j/m2/r                  # aka ~/.m2/repository

Created directories:
/j/m2/project/version    # your project's maven deliverables are here, symlinked from the apporiate location under the next created directoy.
/j/m2/project/m2/version # m2 for your project version's localRepository as defined in ~/.m2/settings-project-version.xml 

Created symlinks:
/j/m2/project/m2/1111/package/one -> /j/m2/project/1111/package/one
/j/m2/project/m2/1111/package -> /j/m2/r/project/1111/package
/j/m2/project/m2/1111 -> /j/m2/r

//TODO:
// handle packages longer than 2 directories
// Update groovy to generate settings-project-version.xml sans sed.
// Support multiple packages. 
// Convert existing standalone repo to mvnLinks (give a list of packages).
// Do above in a non destructive way so it can be run to move and link packages downloaded in a mvnLinks repo (i.e things in org if there is a link in the second part of the linked packages)
// Property file
// Proper object
*/

// mvn default of REPO_HOME would be ~/.m2
def String REPO_HOME = System.getProperty("mvn.repo.home", "~/.m2")
if (REPO_HOME.startsWith("~")) {
    REPO_HOME = System.getProperty("user.home") + REPO_HOME.substring(1, REPO_HOME.length())
}

// mvn default of REPO_HOME would be ~/.m2/repository
def String REPO_DIR = REPO_HOME + "/" + System.getProperty("mvn.repository.dir", "repository")

def String PROJECT = args[0];
def String PVERSION = args[1];

// where the mvn REPO_DIR/project.path deliverables have been copied
def String PROJECT_REPO = REPO_HOME + "/" + PROJECT

// The dir that will be used as m2/repository for the project-version
def String VERSION_REPO = PROJECT_REPO + "/m2/" + PVERSION

if (args.length < 3) {
    System.out.println("Usage: [-Dmvn.repo.home=/j/m2] [-Dmvn.repository.dir=r] <project> <version> <package.1>")
    System.exit(1)
}

// often com, org, net
def String PACKAGE_FIRST = args[2].substring(0, args[2].indexOf("."));

// kuali, apache, etc
def String PACKAGE_SECOND = args[2].substring(PACKAGE_FIRST.length() + 1, args[2].length());


// when setting up for in mvnLinks mode, it seemed the easier way to go was to make the repository
// directory of the packages being linked to a file so if something goes not as expected there will
// be an error about PACKAGE_FIRST/PACKAGE_SECOND not being a directory.  This means something is
// trying to write to REPO_HOME/PACKAGE_FIRST/PACKAGE_SECOND.  We are expecting the path to be
// PROJECT_REPO/PVERSION/PACKAGE_FIRST/PACKAGE_SECOND
def String REPO_FIRST_SECOND = REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND
if (new File(REPO_FIRST_SECOND).isDirectory()) {
    System.out.println(REPO_FIRST_SECOND + " is a directory, not in mvnLinks mode")
    System.exit(1)
}

System.out.println("Creating mvn Links for " + PVERSION)

//if (!new File(PROJECT_REPO).exists()) {
//    System.out.println(PROJECT_REPO + " doesn't exist skipping mvnLinks.sh")
//    System.exit(1)
//}

// recreate linked mvn repository for the passed in version
System.out.println("creating " + VERSION_REPO)
("rm -rf " + VERSION_REPO).execute()
("mkdir -p " + VERSION_REPO).execute()
("touch " + VERSION_REPO + "/" + PROJECT + "." + PVERSION + ".version.repo").execute()

System.out.println("creating " + PROJECT_REPO + "/" + PVERSION)
("mkdir -p " + PROJECT_REPO + "/" + PVERSION).execute()
("touch " + PROJECT_REPO + "/" + PVERSION + "/" + PROJECT + "." + PVERSION + ".project.repo").execute()
new File(REPO_DIR).eachFile() { file->  
    def DIR_BASE = ("basename " + file.getName()).execute().getText().trim()
    def command = "ln -s " + REPO_DIR + "/" + DIR_BASE + " " + VERSION_REPO + "/" + DIR_BASE
    System.out.println(command)
    (command).execute();
}  





// create linked mvn org directory so we can control the kuali dir
// TODO multiple packages will require that all the same PACKAGE_FIRST
// are done before any PACKAGE_SECOND are.
System.out.println("Creating mvn " + PACKAGE_FIRST + " Links for " + VERSION_REPO)
("rm -rf " + VERSION_REPO + "/" + PACKAGE_FIRST).execute()
("mkdir " + VERSION_REPO + "/" + PACKAGE_FIRST).execute()
new File(REPO_DIR + "/" + PACKAGE_FIRST).eachFile() { file->  
    def FILE_BASE = ("basename " + file.getName()).execute().getText().trim()
    def String command = "ln -s " + REPO_DIR + "/" + PACKAGE_FIRST + "/" + FILE_BASE + " " + VERSION_REPO + "/" + PACKAGE_FIRST + "/" + FILE_BASE
    System.out.println(command)
    (command).execute()
}  

// create a link for the PACKAGE_SECOND directory
("rm -rf " + VERSION_REPO + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND).execute()
def secondPackage = "ln -s " + PROJECT_REPO + "/" + PVERSION + " " + VERSION_REPO + "/" + PACKAGE_FIRST+ "/" + PACKAGE_SECOND
System.out.println(secondPackage)
(secondPackage).execute()  



// make sure the repo PACKAGE_FIRST/PACKAGE_SECOND cannot be updated!
def String enabledFile = REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND + ".mvnLinks.enabled"
if (! new File(enabledFile).exists()) {
    ("rm -rf " + REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND).execute()
    ("sudo touch " + REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND).execute()
    ("sudo touch " + REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND + ".mvnLinks.enabled").execute()    
}


if (System.getProperty("MVN_UTIL_HOME") == null) {
    System.out.println("env MVN_UTIL_HOME not set settings xml will not be created")
} else {
    def command = "sed \"s|M2_REPO|" + VERSION_REPO + "|g\" " + System.getProperty("MVN_UTIL_HOME") + "/etc/settings.xml > ~/.m2/settings-" + PROJECT + "-" + PVERSION + ".xml"
    System.out.println(command)
    def sedout = (command).execute().getText()
    System.out.println(sedout)
} 
