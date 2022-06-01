(ns badigeon2.doc
  (:require [badigeon2.sign :as sign]
            [badigeon2.deploy :as deploy]
            [badigeon2.jlink :as jlink]))

(def header "# API\n\n")
(def template
  (str
   "## `%s`\n\n"
   "Options: `%s`\n\n"
   "%s"))

(defn escape-chars [^String s]
  (let [sb (StringBuilder.)]
    (dotimes [n (count s)]
      (let [c (.charAt s n)]
        (case c
          \* (do (.append sb \\ ) (.append sb \*))
          (.append sb c))))
    (str sb)))

(def vars [#'sign/sign-gpg
           #'deploy/deploy
           #'jlink/jlink])

(defn var-sym [^clojure.lang.Var v]
  (symbol (str (.-ns v)) (str (.-sym v))))

(defn arglists-remove-default-vals [arg]
  (cond (map? arg)
        (into (or (empty arg) []) (map arglists-remove-default-vals) (dissoc arg :or))
        (coll? arg)
        (into (or (empty arg) []) (map arglists-remove-default-vals) arg)
        :else arg))

(defn doc-entry [template v-sym v-arglists doc]
  (str (format template
               (pr-str v-sym)
               (binding [*print-length* nil
                         *print-level* nil]
                 (pr-str v-arglists))
               doc) "\n\n"))

(defn v->doc [v]
  (let [{:keys [arglists doc]} (meta v)
        arglists (map arglists-remove-default-vals arglists)]
    (doc-entry template (var-sym v) arglists doc)))

(defn gen-doc [vars]
  (escape-chars (apply str header (map v->doc vars))))

(defn -main []
  (spit "API.md" (gen-doc vars)))

(comment
  (-main)
  )
