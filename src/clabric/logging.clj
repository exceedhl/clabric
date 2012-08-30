(ns clabric.logging
  (:use [clj-logging-config.log4j])
  (:require [clojure.tools.logging :as ctl]))

(set-loggers! :root
              {:pattern "%c %p %m %n" :level :info}
              "clabric"
              {:pattern "[%X{host}] %p %m %n" :level :debug}
              "clabric.util"
              {:pattern "%p %m %n" :level :info})

(defmacro info [host & message]
  `(with-logging-context {:host ~host}
    (ctl/info ~@message)))

(defmacro debug [host & message]
  `(with-logging-context {:host ~host}
     (ctl/debug ~@message)))

(defmacro debug-local [& message]
  `(debug "localhost" ~@message))

(defmacro error [host & message]
  `(with-logging-context {:host ~host}
    (ctl/error ~@message)))

(defn log-host-and-result [host out err & exception]
  (with-logging-context {:host host}
    (and (not (empty? out)) (ctl/info out))
    (and (not (empty? err)) (ctl/error err))
    (and (first exception) (ctl/error (.getMessage (first exception))))))

(defmacro log-localhost-and-result [out err & exception]
  `(log-host-and-result "localhost" ~out ~err ~@exception))
