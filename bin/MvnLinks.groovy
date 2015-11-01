/**
Symlink a localRepository project version duplicate of a maven repository.

-Dmvn.repo.home=/j/m2 -Dmvn.repository.dir=r project 1111 package.one

Existing directories:
/j/m2 					 # aka ~/.m2
/j/m2/r  				 # aka ~/.m2/repository

Created directories:
/j/m2/project/version    # your project's maven deliverables are here, symlinked from the apporiate location under the next created directoy.
/j/m2/project/m2/version # m2 for your project version's localRepository as defined in ~/.m2/settings-project-version.xml 

Created symlinks:
/j/m2/project/m2/1111/package/one -> /j/m2/project/1111/package/one
/j/m2/project/m2/1111/package -> /j/m2/r/project/1111/package
/j/m2/project/m2/1111 -> /j/m2/r

//TODO:
// property file
// ignores property (.DS_Store)
// proper object
*/
import org.codehaus.groovy.runtime.InvokerHelper

public class MvnLinks {
//public class MvnLinks extends Script {
    
    def static String REPO_HOME
    def static String REPO_DIR

    def static String PROJECT
    def static String PVERSION

    // where the mvn REPO_DIR/project.path deliverables have been copied
    def static String PROJECT_REPO

    // The dir that will be used as m2/repository for the project-version
    def static String VERSION_M2_REPO

    def static List commands = new LinkedList()

    public static void main(String[] args) {
        // mvn default of REPO_HOME would be ~/.m2
        MvnLinks.REPO_HOME = System.getProperty("mvn.repo.home", "~/.m2")
        if (MvnLinks.REPO_HOME.startsWith("~")) {
            MvnLinks.REPO_HOME = System.getProperty("user.home") + MvnLinks.REPO_HOME.substring(1, MvnLinks.REPO_HOME.length())
        }

        // mvn default of REPO_HOME would be ~/.m2/repository
        MvnLinks.REPO_DIR = MvnLinks.REPO_HOME + "/" + System.getProperty("mvn.repository.dir", "repository")

        MvnLinks.PROJECT = args[0]
        MvnLinks.PVERSION = args[1]

        // where the mvn REPO_DIR/project.path deliverables have been copied
        MvnLinks.PROJECT_REPO = MvnLinks.REPO_HOME + "/" + MvnLinks.PROJECT

        // The dir that will be used as m2/repository for the project-version
        MvnLinks.VERSION_M2_REPO = MvnLinks.PROJECT_REPO + "/m2/" + MvnLinks.PVERSION

        // Usage
        if (args.length < 3) {
            println("Usage: [-Dmvn.repo.home=/j/m2] [-Dmvn.repository.dir=r] <project> <version> <package.one>")
            System.exit(1)
        }

        // often com, org, net
        def String PACKAGE_FIRST = args[2].substring(0, args[2].indexOf("."))

        // kuali, apache, etc
        def String PACKAGE_SECOND = args[2].substring(PACKAGE_FIRST.length() + 1, args[2].length())




        // when setting up for in mvnLinks mode, it seemed the easier way to go was to make the repository
        // directory of the packages being linked to a file so if something goes not as expected there will
        // be an error about PACKAGE_FIRST/PACKAGE_SECOND not being a directory.  This means something is
        // trying to write to REPO_HOME/PACKAGE_FIRST/PACKAGE_SECOND.  We are expecting the path to be
        // PROJECT_REPO/PVERSION/PACKAGE_FIRST/PACKAGE_SECOND
        def String REPO_FIRST_SECOND = MvnLinks.REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND
        if (new File(REPO_FIRST_SECOND).isDirectory()) {
            println(REPO_FIRST_SECOND + " is a directory, not in mvnLinks mode")
            System.exit(1)
        }

        println("Creating mvn Links for " + MvnLinks.PVERSION)

        //if (!new File(PROJECT_REPO).exists()) {
        //    println(PROJECT_REPO + " doesn't exist skipping MvnLinks.sh")
        //    System.exit(1)
        //}

        MvnLinks.recreateVersionM2Repo()

        // create linked repo for VERSION_M2_REPO
        MvnLinks.createVersionM2RepoLinks()

        // For each parent of leaves (with the same parent) 
        // create linked mvn PACKAGE_FIRST directory so we can control the PACKAGE_SECOND dir
        // TODO multiple packages will require that all the same PACKAGE_FIRST are done before
        // any PACKAGE_SECOND are else PACKAGE_SECOND will be deleted by the next PACKAGE_SECOND
        MvnLinks.createParentLinks(PACKAGE_FIRST)

        // create a link for the PACKAGE_SECOND directory
        // create links for the leaves of this parent
        def String versionRepoLeaf = MvnLinks.VERSION_M2_REPO + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND
        MvnLinks.createLeafLink(versionRepoLeaf)

        // make sure the repo PACKAGE_FIRST/PACKAGE_SECOND cannot be updated!
        def String repoLeaf = MvnLinks.REPO_DIR + "/" + PACKAGE_FIRST + "/" + PACKAGE_SECOND
        MvnLinks.lockRepoLeaf(repoLeaf)

        // settings.xml for VERSION_M2_REPO
        MvnLinks.createProjectVersionSettingsXml()

        MvnLinks.printCommands()
//        MvnLinks.executeCommands()

    }

    static void recreateVersionM2Repo() {
        println("recreateVersionM2Repo " + MvnLinks.VERSION_M2_REPO)
        MvnLinks.commands.add("rm -rf " + MvnLinks.VERSION_M2_REPO)
        MvnLinks.commands.add("mkdir -p " + MvnLinks.VERSION_M2_REPO)
        MvnLinks.commands.add("touch " + MvnLinks.VERSION_M2_REPO + "/" + MvnLinks.PROJECT + "." + MvnLinks.PVERSION + ".version.repo")
    }

    static void createVersionM2RepoLinks() {
        println("createVersionM2RepoLinks " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION)
        MvnLinks.commands.add("mkdir -p " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION)
        MvnLinks.commands.add("touch " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION + "/" + MvnLinks.PROJECT + "." + MvnLinks.PVERSION + ".project.repo")
        new File(MvnLinks.REPO_DIR).eachFile() { file->  
            def dirBase = ("basename " + file.getName()).execute().getText().trim()
            def command = "ln -s " + MvnLinks.REPO_DIR + "/" + dirBase + " " + MvnLinks.VERSION_M2_REPO + "/" + dirBase
            println(command)
            MvnLinks.commands.add(command)
        }
    }

    static void createParentLinks(String parentPackage) {
        println("createParentLinks: " + MvnLinks.REPO_DIR + "/" + parentPackage);
        MvnLinks.commands.add("rm -rf " + MvnLinks.VERSION_M2_REPO + "/" + parentPackage)
        MvnLinks.commands.add("mkdir " + MvnLinks.VERSION_M2_REPO + "/" + parentPackage)
        new File(MvnLinks.REPO_DIR + "/" + parentPackage).eachFile() { file->  
            def fileBase = ("basename " + file.getName()).execute().getText().trim()
            def String command = "ln -s " + MvnLinks.REPO_DIR + "/" + parentPackage + "/" + fileBase + " " + MvnLinks.VERSION_M2_REPO + "/" + parentPackage + "/" + fileBase
            println(command)
            MvnLinks.commands.add(command)
        }  
    }

    static void createLeafLink(String versionRepoLeaf) {
        MvnLinks.commands.add("rm -rf " + versionRepoLeaf)
        def leafPackageLinkCommand = "ln -s " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION + " " + versionRepoLeaf;
        println(leafPackageLinkCommand)
        MvnLinks.commands.add(leafPackageLinkCommand)  
    }

    static void lockRepoLeaf(String leaf) {
        def String enabledFile = leaf + ".MvnLinks.enabled"
        if (! new File(enabledFile).exists()) {
            MvnLinks.commands.add("rm -rf " + leaf)
            MvnLinks.commands.add("sudo touch " + leaf)
            MvnLinks.commands.add("sudo touch " + leaf + ".MvnLinks.enabled")  
        }
    }

    static void createProjectVersionSettingsXml() {
        if (System.getProperty("MVN_UTIL_HOME") == null) {
            println("env MVN_UTIL_HOME not set settings xml will not be created")
        } else {
            def command = "sed \"s|M2_REPO|" + MvnLinks.VERSION_M2_REPO + "|g\" " + System.getProperty("MVN_UTIL_HOME") + "/etc/settings.xml > ~/.m2/settings-" + MvnLinks.PROJECT + "-" + MvnLinks.PVERSION + ".xml"
            println(command)
            def sedout = (command).execute().getText()
            println(sedout)        
        }
    }



    static void executeCommands() {
        for (String command: MvnLinks.commands) {
            command.execute()
        }
    }

    static void printCommands() {
        for (String command: MvnLinks.commands) {
            println(command)
        }
    }     
}

