(ns clabric.dsl
  (:use [clabric.ssh]
        [clabric.task :only (distribute)]))

(defn run [command & options]
  (distribute (fn [option]
                (let [option (merge option (apply array-map options))]
                  (ssh-exec (ssh-session option) command)))))
