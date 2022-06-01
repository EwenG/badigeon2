(ns badigeon2-build.deploy
  (:require [badigeon2.utils :as utils]
            [badigeon2.deploy :as deploy]
            [clojure.tools.build.api :as api]))

(defn -main []
  (let [basis (api/create-basis)
        deploy-token (slurp "deploy_token.txt")
        jar-file (format "target/badigeon2-%s.jar" utils/version)]
    (api/delete {:path "target"})
    (api/copy-dir {:src-dirs ["src"]
                   :target-dir "target/classes"})
    (api/write-pom {:basis basis
                    :class-dir "target/classes"
                    :lib 'com.github.eweng/badigeon2
                    :version utils/version
                    :src-dirs ["src"]})
    (api/jar {:class-dir "target/classes"
              :jar-file jar-file})
    (deploy/deploy {:basis basis
                    :class-dir "target/classes"
                    :lib 'com.github.eweng/badigeon2
                    :version utils/version
                    :jar-file jar-file
                    :repository {:id "clojars"
                                 :url "https://repo.clojars.org/"}
                    :credentials {:username "ewen" :password deploy-token}})))


