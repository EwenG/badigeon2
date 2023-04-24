(ns badigeon2.deploy
  (:require [clojure.tools.deps.util.maven :as maven]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as api]
            [clojure.tools.build.api.specs :as specs]
            [badigeon2.utils :as utils])
  (:import [org.eclipse.aether.artifact DefaultArtifact]
           [org.eclipse.aether.deployment DeployRequest]
           [org.eclipse.aether.repository RemoteRepository$Builder]
           [org.apache.maven.settings DefaultMavenSettingsBuilder]
           [org.apache.maven.settings.building DefaultSettingsBuilderFactory]
           [org.eclipse.aether.util.repository AuthenticationBuilder]))

(defn- set-settings-builder
  [^DefaultMavenSettingsBuilder default-builder settings-builder]
  (doto (.. default-builder getClass (getDeclaredField "settingsBuilder"))
    (.setAccessible true)
    (.set default-builder settings-builder)))

(defn- get-settings
  ^org.apache.maven.settings.Settings []
  (.buildSettings
   (doto (DefaultMavenSettingsBuilder.)
     (set-settings-builder (.newInstance (DefaultSettingsBuilderFactory.))))))

(defn remote-repo [{:keys [id url]} credentials]
  (let [repository (RemoteRepository$Builder. id "default" url)
        ^org.apache.maven.settings.Server server-setting
        (first (filter
                #(.equalsIgnoreCase ^String id (.getId ^org.apache.maven.settings.Server %))
                (.getServers (get-settings))))
        username (or (:username credentials) (when server-setting
                                               (.getUsername server-setting)))
        password (or (:password credentials) (when server-setting
                                               (.getPassword server-setting)))
        private-key (or (:private-key credentials) (when server-setting
                                                     (.getPassword server-setting)))
        passphrase (or (:passphrase credentials) (when server-setting
                                                   (.getPassphrase server-setting)))]
    (-> repository
        (.setAuthentication (-> (AuthenticationBuilder.)
                                (.addUsername username)
                                (.addPassword password)
                                (.addPrivateKey private-key passphrase)
                                (.build)))
        (.build))))

(defn deploy
  "Deploy pom and jar to remote Maven repo.
  Returns nil.
  
  Options:
  - basis: required, used for :mvn/local-repo
  - lib: required, lib symbol
  - classifier: classifier string, if needed
  - version: required, string version
  - jar-file: required, path to jar file
  - jar-signature-file: optional, path to jar signature
  - class-dir: required, used to find the pom file
  - repository: A map with an :id and a :url key representing the remote repository where the artifacts are to be deployed. The :id is used to find credentials in the settings.xml file when authenticating to the repository
  - credentials: When authenticating to a repository, the credentials are searched in the maven settings.xml file, using the repository :id, unless the \"credentials\" parameter is used. credentials must be a map with the following optional keys: :username, :password, :private-key, :passphrase "
  [{:keys [basis lib classifier version jar-file jar-signature-file class-dir repository credentials] :as params}]
  (utils/assert-required "deploy" params [:basis :lib :version :jar-file :class-dir])
  (utils/assert-specs "deploy" params
                      :lib ::specs/lib
                      :jar-file ::specs/path
                      :class-dir ::specs/path)
  (java.lang.System/setProperty "aether.checksums.forSignature" "true")
  (let [{:maven/keys [local-repo]} basis
        group-id (namespace lib)
        artifact-id (name lib)
        jar-file-file (api/resolve-path jar-file)
        jar-signature-file-file (when jar-signature-file (api/resolve-path jar-signature-file))
        pom-dir (io/file (api/resolve-path class-dir) "META-INF" "maven" group-id artifact-id)
        pom (io/file pom-dir "pom.xml")
        pom-signature (when jar-signature-file (io/file pom-dir "pom.xml.asc"))
        system (maven/make-system)
        session (maven/make-session system (or local-repo @maven/cached-local-repo))
        jar-artifact (.setFile (DefaultArtifact. group-id artifact-id classifier "jar" version) jar-file-file)
        artifacts (cond-> [jar-artifact]
                    (and pom-dir (.exists pom))
                    (conj (.setFile (DefaultArtifact. group-id artifact-id classifier "pom" version) pom))
                    (and jar-signature-file-file (.exists jar-signature-file-file))
                    (conj (.setFile (DefaultArtifact. group-id artifact-id classifier "jar.asc" version) jar-signature-file-file))
                    (and pom-signature (.exists pom-signature))
                    (conj (.setFile (DefaultArtifact. group-id artifact-id classifier "pom.asc" version) pom-signature)))
        deploy-request (-> (DeployRequest.)
                           (.setRepository (remote-repo repository credentials))
                           (.setArtifacts artifacts))]
    (.deploy system session deploy-request)))

(comment
  (deploy {:basis (api/create-basis)
           :class-dir "target/classes"
           :lib 'com.github.eweng/badigeon2
           :version "1.1.0"
           :jar-file "target/badigeon2.jar"
           :repository {:id "clojars"
                        :url "https://repo.clojars.org/"}})
  )
