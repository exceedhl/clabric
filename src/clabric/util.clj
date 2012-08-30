(ns clabric.util
  (:use [clabric.logging])
  (:require [clojure.tools.logging :as ctl]))

(defn current-user []
  (System/getProperty "user.name"))

(defn current-user-home []
  (System/getProperty "user.home"))

(defn exit [exit-code]
  (ctl/error "Clabric exit because some error happened!")
  (System/exit exit-code))