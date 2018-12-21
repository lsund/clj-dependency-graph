(ns clj-dependency-graph.core
  (:require [clojure.zip :as z]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [me.raynes.fs :as fs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Find dependencies

(defn find-require [zipper]
  (loop [zp (z/down zipper)]
    (if (= (first (z/down zp)) :require)
      (z/down zp)
      (recur (z/right zp)))))

(defn heads
  "Returns the heads of "
  [zipper]
  (loop [zp zipper
         xs []]
    (let [x (first (z/down zp))]
      (if-let [r (z/right zp)]
        (recur r (conj xs x))
        (conj xs x)))))

(defn ns-zipper [fname]
  (->> fname
       fs/expand-home
       slurp
       read-string
       (z/zipper sequential? seq (fn [_ c] c))))

(defn all-dependencies [fname]
  (filter some? (heads (find-require (ns-zipper fname)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Manipulate ns-names

(defn namespace-name [ns part]
  (let [split (-> ns name (string/split #"\."))]
    (case part
      :absolute ns
      :base (first split)
      :relative (last split))))

(defn file-namespace-name [fname part]
  (namespace-name (-> fname ns-zipper z/down z/right first) part))

(defn prefix? [p s]
  (some? (re-matches (re-pattern (str "^" p ".*")) s)))

(defn internal-dependency? [proj-name dep]
  (prefix? proj-name (name dep)))

(defn project-dependencies [proj-name fname]
  (filter (partial internal-dependency? proj-name) (all-dependencies fname)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Make graph

(defn serialize-dependencies! [files dotfile]
  (spit dotfile "digraph {\n")
  (doseq [file files]
    (let [relative-ns (file-namespace-name file :relative)]
      (doseq [dep (project-dependencies (file-namespace-name file :base) file)]
        (spit dotfile (str "\"" relative-ns "\" -> \"" (namespace-name  dep :relative) "\";\n")
              :append true))))
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
  (apply concat (dir-files repo)))

(defn generate [repo]
  (fs/mkdir "resources")
  (serialize-dependencies! (dir-clj-src-files repo) "resources/data.dot")
  (make-graph "resources/data.dot" "resources/data.png"))
