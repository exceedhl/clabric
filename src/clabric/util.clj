(ns clabric.util)

(defn current-user []
  (System/getProperty "user.name"))

(defn current-user-home []
  (System/getProperty "user.home"))
