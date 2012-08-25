(ns clabric.dsl
  (:use [clabric.ssh]
        [clojure.string :only (join split)]
        [clabric.task :only (distribute local)])
  (:require [clj-commons-exec :as exec]))

(defn run [command & options]
  (distribute (fn [option]
                (let [option (merge option (apply array-map options))]
                  (ssh-exec (ssh-session option) command)))))

(defn cmd-exec [command options]
  (let [cmd-list (split command (re-pattern "\\s+"))]
    (let [result @(exec/sh cmd-list options)
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