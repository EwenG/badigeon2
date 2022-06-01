(ns badigeon2.jlink
  (:require [clojure.string :as string])
  (:import [java.util Optional]
           [java.util.spi ToolProvider]
           [java.io ByteArrayOutputStream PrintStream File]))

(defmacro interface-static-call
  [sym argtypes]
  `(let [m# (.getMethod ~(symbol (namespace sym))
                        ~(name sym)
                        (into-array Class ~argtypes))]
     (fn [& args#]
       (.invoke m# nil (to-array args#)))))

(defn- jlink-command [java-home out-path module-path modules jlink-options]
  (into ["--module-path" module-path
         "--add-modules" modules
         "--output" (str out-path)]
        jlink-options))

(defn jlink
  "Creates a custom JRE using the jlink command. To be run, this function requires a JDK >= version 9. 
  
  Options:
    - jlink-dir: required, the folder where the custom JRE is output
    - module-path: the path where the java module are searched for. Default to \"JAVA_HOME/jmods\".
    - modules: a vector of modules to be used when creating the custom JRE. Default to [\"java.base\"]
    - jlink-options: the options used when executing the jlink command. Default to [\"--strip-debug\" \"--no-man-pages\" \"--no-header-files\" \"--compress=2\"]"
  [{:keys [jlink-dir module-path modules jlink-options]
    :or {modules ["java.base"]
         jlink-options ["--strip-debug" "--no-man-pages"
                        "--no-header-files" "--compress=2"]}}]
  (let [java-home (System/getProperty "java.home")
        module-path (or module-path (str java-home File/separator "jmods"))
        modules (string/join "," modules)
        maybe-jlink ((interface-static-call ToolProvider/findFirst [java.lang.String]) "jlink")
        jlink (.orElse ^Optional maybe-jlink nil)]
    (when (nil? jlink)
      (throw (ex-info "JLink tool not found" {})))
    (let [jlink-out (ByteArrayOutputStream.)
          jlink-err (ByteArrayOutputStream.)]
      (.run ^ToolProvider jlink (PrintStream. jlink-out) (PrintStream. jlink-err)
            ^"[Ljava.lang.String;" (into-array
                                    String
                                    (jlink-command java-home jlink-dir module-path modules
                                                   jlink-options)))
      (print (str jlink-out))
      (print (str jlink-err)))))


(comment
  (jlink {:jlink-dir "target/runtime"})
  )
