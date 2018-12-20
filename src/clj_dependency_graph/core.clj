(ns clj-dependency-graph.core
  (:require [clojure.zip :as z]
            [me.raynes.fs :as fs]))

(def f "~/Documents/tech/repos/camunda-cli-tool/src/camunda_cli_tool/")

(defn at-require [zp]
  (-> zp z/down z/right z/right z/down))

(defn ns-zipper [fname]
  (->> fname
       fs/expand-home
       slurp
       read-string
       (z/zipper sequential? seq (fn [_ c] c))))

(defn ns-name [fname]
  (-> fname ns-zipper z/down z/right first))

(defn heads
  "Returns the heads of "
  [zipper]
  (loop [zp zipper
         xs []]
    (let [x (first (z/down zp))]
      (if-let [r (z/right zp)]
        (recur r (conj xs x))
        (conj xs x)))))

(defn all-dependencies [fname]
  (filter some? (heads (at-require (ns-zipper fname)))))

(defn prefix? [p s]
  (some? (re-matches (re-pattern (str "^" p ".*")) s)))

(defn internal-dependency? [proj-name dep]
  (prefix? proj-name (name dep)))

(defn project-dependencies [proj-name fname]
  (filter (partial internal-dependency? proj-name) (all-dependencies fname)))

(defn serialize-dependencies! [proj-name fname]
  (spit "test.txt" "")
  (let [ns (ns-name fname)]
    (doseq [dep (project-dependencies proj-name fname)]
      (spit "test.txt" (str ns " -> " dep "\n") :append true))))

(defn dir-files [path]
  (->> path
       fs/expand-home
       fs/iterate-dir
       (map (fn [[base-dir _ files]] (for [file files] (str base-dir "/" file))))))
