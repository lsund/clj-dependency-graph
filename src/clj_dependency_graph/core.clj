(ns clj-dependency-graph.core
  (:require [clojure.zip :as z]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]))

(def mock-repo "~/Documents/tech/repos/camunda-cli-tool/")

(defn at-require [zp]
  (-> zp z/down z/right z/right z/down))

(defn ns-zipper [fname]
  (->> fname
       fs/expand-home
       slurp
       read-string
       (z/zipper sequential? seq (fn [_ c] c))))

(defn file-ns-name [fname]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Make graph

(defn serialize-dependencies! [proj-name files dotfile]
  (spit dotfile "digraph {\n")
  (doseq [file files]
    (let [ns (file-ns-name file)]
      (doseq [dep (project-dependencies proj-name file)]
        (spit dotfile (str "\"" ns "\" -> \"" dep "\";\n") :append true))))
  (spit dotfile "}\n" :append true))

(defn make-graph [dotfile outfile]
  (shell/sh "dot" "-Tpng" dotfile "-o" outfile))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; List files

(defn dir-files [repo]
  (->> repo
       fs/expand-home
       fs/iterate-dir
       (map (fn [[base-dir _ files]] (for [file files] (str base-dir "/" file))))))

(defn dir-clj-src-files [repo]
  (println "Guessing root directory...")
  (println "Generating dependency graph from './src'")
  (apply concat (dir-files (str repo "/src"))))

(defn generate [repo]
  (serialize-dependencies! (fs/base-name repo)
                           (dir-clj-src-files repo)
                           "resources/data.dot")
  (make-graph "resources/data.dot" "resources/data.png"))
