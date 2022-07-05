(ns badigeon2.utils
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]))

(def ^:const version "1.2.0-SNAPSHOT")

(defn assert-required
  "Check that each key in required coll is a key in params and throw if
  required are missing in params, otherwise return nil."
  [task params required]
  (let [missing (set/difference (set required) (set (keys params)))]
    (when (seq missing)
      (throw (ex-info (format "Missing required params for %s: %s" task (vec (sort missing))) (or params {}))))))

(defn assert-specs
  "Check that key in params satisfies the spec. Throw if it exists and
  does not conform to the spec, otherwise return nil."
  [task params & key-specs]
  (doseq [[key spec] (partition-all 2 key-specs)]
    (let [val (get params key)]
      (when (and val (not (s/valid? spec val)))
        (throw (ex-info (format "Invalid param %s in call to %s: got %s, expected %s" key task (pr-str val) (s/form spec)) {}))))))

