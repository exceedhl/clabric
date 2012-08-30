(ns clabric.helper
  (:use [clj-logging-config.log4j]))

(set-loggers! :root
              {:pattern "%c %p %m %n" :level :fatal}
              "clabric"
              {:pattern "[%X{host}] %p %m %n" :level :fatal})