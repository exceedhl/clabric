(ns clabric.dsl
  (:use [clabric.ssh]
        [clojure.string :only (join split)]
        [clabric.task :only (distribute local)])
  (:require [clj-commons-exec :as exec]))

(defn- split-and-filter-cmds [cmds]
  (map #(vec (filter (complement empty?) (split %1 (re-pattern "\\s+"))))
       cmds))

(defn cmd-exec [command options]
  (let [cmds (split command (re-pattern "\\|"))
        cmd-lists (split-and-filter-cmds cmds)]
    (let [result (if (> (count cmds) 1)
                   @(eval `(last (exec/sh-pipe ~@cmd-lists ~options)))
                   @(exec/sh (first cmd-lists) options))
          out (:out result)
          err (:err result)
          exception (:exception result)]
      (assoc result
        :out (or out "")
        :err (or err (if exception (.getMessage exception)) "")))))

(defn cmd [command & options]
  (local (fn [option]
           (let [option (merge option (apply array-map options))]
             (cmd-exec command option)))))

(defn- merge-options-and-distribute [f options]
  #(distribute (fn [option]
                 (let [option (merge option (apply array-map options))]
                   (f option)))))

(defn run [command & options]
  ((merge-options-and-distribute (fn [option] (ssh-exec command option)) options)))

(defn upload [from to & options]
  ((merge-options-and-distribute (fn [option] (ssh-upload from to option)) options)))

(defn put [content to & options]
  ((merge-options-and-distribute (fn [option] (ssh-put content to option)) options)))