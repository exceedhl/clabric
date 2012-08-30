(ns clabric.shell
  (:use [clojure.string :only (join split)]
        [clabric.logging])
  (:require [clj-commons-exec :as exec]))

(defn- split-and-filter-cmds [cmds]
  (map #(vec (filter (complement empty?) (split %1 (re-pattern "\\s+"))))
       cmds))

(defn cmd-exec [command options]
  (debug-local "Execute command:" command "with options:" options)
  (let [cmds (split command (re-pattern "\\|"))
        cmd-lists (split-and-filter-cmds cmds)]
    (let [result (if (> (count cmds) 1)
                   @(eval `(last (exec/sh-pipe ~@cmd-lists ~options)))
                   @(exec/sh (first cmd-lists) options))
          out (:out result)
          err (:err result)
          exception (:exception result)]
      (log-localhost-and-result out err exception)
      (assoc result
        :out (or out "")
        :err (or err (if exception (.getMessage exception)) "")))))