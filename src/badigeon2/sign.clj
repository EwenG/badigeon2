(ns badigeon2.sign
  (:require [clojure.java.io :as io]
            [clojure.tools.build.api :as api]
            [clojure.tools.build.api.specs :as specs]
            [badigeon2.utils :as utils]))

(defn sign-gpg
  "Sign pom and jar using gpg. 

  Options:
  - jar-file: required, path to jar file
  - lib: required, lib symbol
  - class-dir: required, used to find the pom file
  - gpg-key: private key used to sign, default to the first private key found"
  [{:keys [jar-file lib class-dir gpg-key] :as params}]
  (utils/assert-required "sign" params [:lib :jar-file :class-dir])
  (utils/assert-specs "sign" params
                      :lib ::specs/lib
                      :jar-file ::specs/path
                      :class-dir ::specs/path)
  (let [group-id (namespace lib)
        artifact-id (name lib)
        jar-file-file (api/resolve-path jar-file)
        pom-dir (io/file (api/resolve-path class-dir) "META-INF" "maven" group-id artifact-id)
        pom (io/file pom-dir "pom.xml")
        sign-args `["gpg" "--yes" "-ab" "--pinentry-mode" "loopback"
                    ~@(when gpg-key ["--default-key" gpg-key])
                    "--"]
        {:keys [exit] :as process-result} (api/process {:command-args (conj sign-args (str jar-file-file))})
        _ (when-not (= exit 0)
            (throw (ex-info "Error while signing jar file"
                            (assoc params
                                   :pom-file (str pom)
                                   :process-result process-result))))
        {:keys [exit] :as process-result} (api/process {:command-args (conj sign-args (str pom))})]
    (when-not (= exit 0)
      (throw (ex-info "Error while signing pom file"
                      (assoc params
                             :pom-file (str pom)
                             :process-result process-result))))))

(comment
  (api/compile-clj {:basis (api/create-basis)
                    :class-dir "target/classes"})
  
  (api/jar {:class-dir "target/classes"
            :jar-file "target/badigeon.jar"})

  (api/write-pom {:lib 'badigeon/badigeon2
                  :version "1.1.0"
                  :basis (api/create-basis)
                  :class-dir "target/classes"})

  (sign-gpg {:lib 'badigeon/badigeon2
             :jar-file "target/badigeon.jar"
             :class-dir "target/classes"
             :gpg-key "root@eruditorum.org"})
  )
