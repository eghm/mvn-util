/**
Port of mvnLinks.sh

Create a project revision specific duplicate of the maven repository out of symlinks.

Existing directories:
m2
m2/repository       (m2/r)
m2/project          (m2/p)
m2/project/version1 (m2/p/1)
m2/project/version2 (m2/p/2)

Symlinks created:
m2/project/m2/1111/org/kuali -> m2/project/1111/org/kuali
m2/project/m2/1111/org -> m2/repository/org/
m2/project/m2/1111 -> m2/repository
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

def String PACKAGE_FIRST = "org"
def String PACKAGE_SECOND = "kuali"

def String REPO_FIRST_SECOND = REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND
if (new File(REPO_FIRST_SECOND).isDirectory()) {
    System.out.println(REPO_FIRST_SECOND + " is a directory, not in mvnLinks mode")
    System.exit(1)
}

def String VERSION_REPO = REPO_HOME + "/" + PROJECT + "/m2/" + PVERSION

// where the mvn REPO_DIR/project.path deliverables have been copied
def String PROJECT_REPO = REPO_HOME + "/" + PROJECT

if (PROJECT == null) {
    System.out.println("Usage: <m2_repo_home> <project> <version> <package.1>...")
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
    ("sed \"s|M2_REPO|" + VERSION_REPO + "|g\" " + System.getProperty("MVN_UTIL_HOME") + "/etc/settings.xml > ~/.m2/settings-" + PROJECT + "-" + PVERSION + ".xml").execute()    
} 
