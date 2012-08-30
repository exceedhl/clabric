(ns clabric.dsl
  (:use [clabric.ssh]
        [clabric.shell]
        [clabric.util]
        [clabric.task :only (distribute local)]))

(defn- check-result [result]
  (if (not= 0 (:exit result))
    (exit (:exit result))
    result))

(defn- check-and-return-results [results]
  (doseq [result results]
    (check-result result))
  results)

(defn cmd [command & options]
  (let [result (local (fn [option]
                        (let [option (merge option (apply array-map options))]
                          (cmd-exec command option))))]
    (check-result result)))

(defn- merge-options-and-distribute [f options]
  #(distribute (fn [option]
                 (let [option (merge option (apply array-map options))]
                   (f option)))))

(defn run [command & options]
  (check-and-return-results
   ((merge-options-and-distribute
     (fn [option] (ssh-exec command option)) options))))

(defn upload [from to & options]
  (check-and-return-results
   ((merge-options-and-distribute
     (fn [option] (ssh-upload from to option)) options))))

(defn put [content to & options]
  (check-and-return-results
   ((merge-options-and-distribute
     (fn [option] (ssh-put content to option)) options))))