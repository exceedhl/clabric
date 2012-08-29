(ns clabric.util)

(defn current-user []
  (System/getProperty "user.name"))

(defn current-user-home []
  (System/getProperty "user.home"))

(defn exit [exit-code message]
  (println message)
  (System/exit exit-code))