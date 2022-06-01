# API

## `badigeon2.sign/sign-gpg`

Options: `([{:keys [jar-file lib class-dir gpg-key], :as params}])`

Sign pom and jar using gpg. 

  Options:
    - jar-file: required, path to jar file
    - lib: required, lib symbol
    - class-dir: required, used to find the pom file
    - gpg-key: private key used to sign, default to the first private key found

## `badigeon2.deploy/deploy`

Options: `([{:keys [basis lib classifier version jar-file class-dir repository credentials], :as params}])`

Deploy pom and jar to remote Maven repo.
  Returns nil.
  
  Options:
    - basis: required, used for :mvn/local-repo
    - lib: required, lib symbol
    - classifier: classifier string, if needed
    - version: required, string version
    - jar-file: required, path to jar file
    - class-dir: required, used to find the pom file
    - repository: A map with an :id and a :url key representing the remote repository where the artifacts are to be deployed. The :id is used to find credentials in the settings.xml file when authenticating to the repository
    - credentials: When authenticating to a repository, the credentials are searched in the maven settings.xml file, using the repository :id, unless the "credentials" parameter is used. credentials must be a map with the following optional keys: :username, :password, :private-key, :passphrase
    - allow-unsigned?: When set to true, allow deploying non-snapshot versions of unsigned artifacts. Default to false.

## `badigeon2.jlink/jlink`

Options: `([{:keys [jlink-dir module-path modules jlink-options]}])`

Creates a custom JRE using the jlink command. To be run, this function requires a JDK >= version 9. 
  
  Options:
    - jlink-dir: required, the folder where the custom JRE is output
    - module-path: the path where the java module are searched for. Default to "JAVA_HOME/jmods".
    - modules: a vector of modules to be used when creating the custom JRE. Default to ["java.base"]
    - jlink-options: the options used when executing the jlink command. Default to ["--strip-debug" "--no-man-pages" "--no-header-files" "--compress=2"]

