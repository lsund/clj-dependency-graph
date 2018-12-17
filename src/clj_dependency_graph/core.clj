(ns clj-dependency-graph.core
  (:require [clojure.zip :as z]))

(def f "/home/lsund/Temp/test/src/test/core.clj")

(defn at-require [zp]
  (-> zp z/down z/right z/right z/down))

(def ns-zipper (->> f
                    slurp
                    read-string
                    (z/zipper sequential? seq (fn [_ c] c))))

(defn heads
  "Returns the heads of "
  [zipper]
  (loop [zp zipper
         xs []]
    (let [x (first (z/down zp))]
      (if-let [r (z/right zp)]
        (recur r (conj xs x))
        (conj xs x)))))

(defn all-dependencies []
  (filter some? (heads (at-require ns-zipper))))

(defn prefix? [p s]
  (some? (re-matches (re-pattern (str "^" p ".*")) s)))

(defn internal-dependency? [proj-name dep]
  (prefix? proj-name (name dep)))

(defn project-dependencies [proj-name deps]
  (filter (partial internal-dependency? proj-name) deps))
