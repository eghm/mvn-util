/**
Symlink a localRepository project version duplicate of a maven repository.

-Dmvn.repo.home=/j/m2 -Dmvn.repository.dir=r project version one.package [second.package]

Existing directories:
/j/m2 					 # aka ~/.m2
/j/m2/r  				 # aka ~/.m2/repository

Created directories:
/j/m2/project/version    # your project's maven deliverables are here, symlinked from the apporiate location under the next created directoy.
/j/m2/project/m2/version # m2 for your project version's localRepository as defined in ~/.m2/settings-project-version.xml 

Created symlinks:
/j/m2/project/m2/version/one/package -> /j/m2/project/version/one/package
/j/m2/project/m2/version/one -> /j/m2/r/project/version/one
/j/m2/project/m2/version -> /j/m2/r
/j/m2/project/m2/version/one/package -> /j/m2/project/version/second/package
/j/m2/project/m2/version/one -> /j/m2/r/project/version/second
/j/m2/project/m2/version -> /j/m2/r

//TODO:
// Handle package.one package.two
// Update groovy to generate settings-project-version.xml
// handle packages longer than 2 directories
// Convert existing standalone repo to mvnLinks (give a list of packages).
// Do above in a non destructive way so it can be run to move and link packages downloaded in a mvnLinks repo (i.e things in org if there is a link in the second part of the linked packages)
// property file
// proper object
*/
import java.util.logging.Logger
import java.util.logging.Level

public class MvnLinks {

	def static String USAGE = "Usage: [-Dmvn.repo.home=/j/m2] [-Dmvn.repository.dir=r] <project> <version> <one.package> [<second package>]"
//    def static Level LOGGER_LEVEL = Level.INFO
    def static Level LOGGER_LEVEL = Level.FINE
    def static Logger LOGGER = Logger.getLogger("")

    def static String REPO_HOME
    def static String REPO_DIR

    def static String PROJECT
    def static String PVERSION

    // where the mvn REPO_DIR/project.path deliverables have been copied
    def static String PROJECT_REPO

    // The dir that will be used as m2/repository for the project-version
    def static String VERSION_M2_REPO

    def static List commands = new LinkedList()

    def static Map packages = new HashMap();

    public static void main(String[] args) {
        MvnLinks.configure(args)

        LOGGER.info("Creating mvn Links for " + MvnLinks.PVERSION)

        //if (!new File(PROJECT_REPO).exists()) {
        //    println(PROJECT_REPO + " doesn't exist skipping MvnLinks.sh")
        //    System.exit(1)
        //}

        MvnLinks.recreateVersionM2Repo()

        // create linked repo for VERSION_M2_REPO
        MvnLinks.createVersionM2RepoLinks()



        for (int i = 2; i < args.length; i++) {
        	// kuali, apache, etc
	        def String PACKAGE_LEAF = args[i].substring(args[i].lastIndexOf(".") + 1, args[i].length())

        	// com, org, net, edu
        	def String PACKAGE_PARENT = args[i].substring(0, args[i].length() - PACKAGE_LEAF.length() - 1).replaceAll("\\.", "/")

	        // when setting up for in mvnLinks mode, it seemed the easier way to go was to make the repository
	        // directory of the packages being linked to a file so if something goes not as expected there will
	        // be an error about PACKAGE_PARENT/PACKAGE_LEAF not being a directory.  This means something is
	        // trying to write to REPO_HOME/PACKAGE_PARENT/PACKAGE_LEAF.  We are expecting the path to be
	        // PROJECT_REPO/PVERSION/PACKAGE_PARENT/PACKAGE_LEAF
	        def String REPO_FIRST_SECOND = MvnLinks.REPO_DIR + "/" + PACKAGE_PARENT + "/" + PACKAGE_LEAF
	        if (new File(REPO_FIRST_SECOND).isDirectory()) {
	            println(REPO_FIRST_SECOND + " is a directory, not in mvnLinks mode.  Please manually delete it, sudo touch it, and rerun.")
	            System.exit(1) // exit to avoid incomplete creation
	        } else {
		        MvnLinks.createPackage(PACKAGE_PARENT, PACKAGE_LEAF)        		        	
	        }

        }



        // settings.xml for VERSION_M2_REPO
        MvnLinks.createProjectVersionSettingsXml()        

        MvnLinks.printCommands()
        MvnLinks.executeCommands()

    }

    static configure(String[] args) {
        LOGGER.setLevel(MvnLinks.LOGGER_LEVEL)

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
            println(USAGE)
            System.exit(1)
        }        
    }

    static void recreateVersionM2Repo() {
        LOGGER.info("recreateVersionM2Repo " + MvnLinks.VERSION_M2_REPO)
        MvnLinks.commands.add("rm -rf " + MvnLinks.VERSION_M2_REPO)
        MvnLinks.commands.add("mkdir -p " + MvnLinks.VERSION_M2_REPO)
        MvnLinks.commands.add("touch " + MvnLinks.VERSION_M2_REPO + "/" + MvnLinks.PROJECT + "." + MvnLinks.PVERSION + ".version.repo")
    }

    static void createVersionM2RepoLinks() {
        LOGGER.info("createVersionM2RepoLinks " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION)
        MvnLinks.commands.add("mkdir -p " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION)
        MvnLinks.commands.add("touch " + MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION + "/" + MvnLinks.PROJECT + "." + MvnLinks.PVERSION + ".project.repo")
        new File(MvnLinks.REPO_DIR).eachFile() { file->  
            def dirBase = ("basename " + file.getName()).execute().getText().trim()
            def command = "ln -s " + MvnLinks.REPO_DIR + "/" + dirBase + " " + MvnLinks.VERSION_M2_REPO + "/" + dirBase
            LOGGER.fine(command)
            MvnLinks.commands.add(command)
        }
    }

    static void createPackage(String parentPackage, String packageLeaf) {
        // For each parent of leaves (with the same parent) 
        // create linked mvn parentPackage directory so we can control the packageLeaf dir
        // TODO multiple packages will require that all the same parentPackage are done before
        // any packageLeaf are else packageLeaf will be deleted by the next packageLeaf
        MvnLinks.createParentLinks(parentPackage)

        // create a link for the packageLeaf directory
        // create links for the leaves of this parent
        MvnLinks.createLeafLink(parentPackage + "/" + packageLeaf)

        // make sure the repo parentPackage/packageLeaf cannot be updated!
        def String repoLeaf = parentPackage + "/" + packageLeaf

        MvnLinks.lockRepoLeaf(repoLeaf)
    }

    static void createParentLinks(String parentPackage) {
        LOGGER.info("createParentLinks: " + MvnLinks.REPO_DIR + "/" + parentPackage);
        MvnLinks.commands.add("rm -rf " + MvnLinks.VERSION_M2_REPO + "/" + parentPackage)
        MvnLinks.commands.add("mkdir " + MvnLinks.VERSION_M2_REPO + "/" + parentPackage)
        new File(MvnLinks.REPO_DIR + "/" + parentPackage).eachFile() { file->  
            def fileBase = ("basename " + file.getName()).execute().getText().trim()
            def String command = "ln -s " + MvnLinks.REPO_DIR + "/" + parentPackage + "/" + fileBase + " " + MvnLinks.VERSION_M2_REPO + "/" + parentPackage + "/" + fileBase
            LOGGER.fine(command)
            MvnLinks.commands.add(command)
        }  
    }

    static void createLeafLink(String versionRepoLeaf) {
        def String fullVersionRepoLeaf = MvnLinks.VERSION_M2_REPO + "/" + versionRepoLeaf
        MvnLinks.commands.add("rm -rf " + fullVersionRepoLeaf)
        def String versionRepoLeafLeaf = versionRepoLeaf.substring(versionRepoLeaf.lastIndexOf("/") + 1, versionRepoLeaf.length())
        def String projectLeafPackageRepo = MvnLinks.PROJECT_REPO + "/" + MvnLinks.PVERSION + "/" + versionRepoLeafLeaf
        MvnLinks.commands.add("mkdir -p " + projectLeafPackageRepo)
        MvnLinks.commands.add("touch " + projectLeafPackageRepo + "/" + versionRepoLeafLeaf + "." + MvnLinks.PVERSION + ".package")
        def leafPackageLinkCommand = "ln -s " + projectLeafPackageRepo + " " + fullVersionRepoLeaf;
        LOGGER.fine(leafPackageLinkCommand)
        MvnLinks.commands.add(leafPackageLinkCommand)  
    }

    static void lockRepoLeaf(String leaf) {
        def String fullLeaf = MvnLinks.REPO_DIR + "/" + leaf
        def String enabledFile = fullLeaf + ".MvnLinks.enabled"
        if (! new File(enabledFile).exists()) {
            MvnLinks.commands.add("rm -rf " + fullLeaf)
            MvnLinks.commands.add("sudo touch " + fullLeaf)
            MvnLinks.commands.add("sudo touch " + fullLeaf + ".MvnLinks.enabled")  
        }
    }

    static void createProjectVersionSettingsXml() {
        if (System.getProperty("MVN_UTIL_HOME") == null) {
            println("env MVN_UTIL_HOME not set settings xml will not be created")
        } else {
            def command = "sed \"s|M2_REPO|" + MvnLinks.VERSION_M2_REPO + "|g\" " + System.getProperty("MVN_UTIL_HOME") + "/etc/settings.xml > ~/.m2/settings-" + MvnLinks.PROJECT + "-" + MvnLinks.PVERSION + ".xml"
            commands.add(command)
            LOGGER.info(command)
        }
    }

    static void executeCommands() {
        for (String command: MvnLinks.commands) {
            if (!command.contains(".DS_Store")) {
                LOGGER.fine(command)
                command.execute()
            }
        }
    }

    static void printCommands() {
        println("\nCommands that would be executed:")
        for (String command: MvnLinks.commands) {
            if (!command.contains(".DS_Store")) {
                println(command)
            }
        }
    }     
}

