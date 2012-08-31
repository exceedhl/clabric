(defproject clabric "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.incubator "0.1.1"]
                 [com.jcraft/jsch "0.1.48"]
                 [jsch-agent-proxy "0.0.4"]
                 [jsch-agent-proxy/jsch-agent-proxy-jna "0.0.4"
                  :exclusions [com.jcraft/jsch-agent-proxy]]
                 [speclj "2.1.2"]
                 [clj-logging-config "1.9.8"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojars.hozumi/clj-commons-exec "1.0.6"]]
  :plugins [[lein-swank "1.4.4"]
            [speclj "2.1.2"]]
  :test-paths ["spec/"])
