(ns clabric.dsl-spec
  (:use 
    [speclj.core]
    [clabric.dsl]
    [clabric.util]
    [clabric.ssh]
    [clabric.task]
    [clojure.contrib.mock]))

(defn mock-ssh-exec [command options]
  (merge options {:command command :exit 0}))

(defn error-mock-ssh-exec [command options]
  (merge options {:command command :exit 1 :err "error happened"}))

(defn mock-ssh-upload [from to options]
  (merge options {:from from :to to :exit 0}))

(defn error-mock-ssh-upload [from to options]
  (merge options {:from from :to to :exit 1 :err "error happened"}))

(defn mock-ssh-put [content to options]
  (merge options {:content content :to to :exit 0}))

(defn error-mock-ssh-put [content to options]
  (merge options {:content content :to to :exit 1 :err "error happened"}))


(describe "DSL"

  (context "run command"

    (it "should pass arguments to ssh-exec"
      (deftask t1 ["host1"] "task t1"
        (run "ls"))
      (with-redefs [ssh-exec mock-ssh-exec]
        (should= {:exit 0 :host "host1" :opt1 "opt1" :command "ls"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should be able to override option"
      (deftask t1 ["host1"] "task t1"
        (run "ls" :opt1 "new opt1" :opt2 "opt2"))
      (with-redefs [ssh-exec mock-ssh-exec]
        (should= {:exit 0 :host "host1" :opt1 "new opt1" :opt2 "opt2" :command "ls"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should run on multiple hosts"
      (deftask t1 ["host1" "host2"] "task t1"
        (run "ls"))
      (with-redefs [ssh-exec mock-ssh-exec]
        (let [result (execute t1 :opt1 "opt1")]
          (should= 2 (count result))
          (should= {:exit 0 :host "host1" :opt1 "opt1" :command "ls"}
            (first result))
          (should= {:exit 0 :host "host2" :opt1 "opt1" :command "ls"}
            (second result)))))

    (it "should throw exception if there is an error on some host"
      (deftask t1 ["host1" "host2"] "task t1"
        (run "ls"))
      (with-redefs [ssh-exec error-mock-ssh-exec]
        (should-throw Exception "error happened" (execute t1)))))

  (context "upload command"

    (it "should pass arguments to ssh-upload"
      (deftask t1 ["host1"] "task t1"
        (upload "from" "to"))
      (with-redefs [ssh-upload mock-ssh-upload]
        (should= {:exit 0 :host "host1" :opt1 "opt1" :from "from" :to "to"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should be able to override option"
      (deftask t1 ["host1"] "task t1"
        (upload "from" "to" :opt1 "new opt1" :opt2 "opt2"))
      (with-redefs [ssh-upload mock-ssh-upload]
        (should= {:exit 0 :host "host1" :opt1 "new opt1" :opt2 "opt2" :from "from" :to "to"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should run on multiple hosts"
      (deftask t1 ["host1" "host2"] "task t1"
        (upload "from" "to"))
      (with-redefs [ssh-upload mock-ssh-upload]
        (let [result (execute t1 :opt1 "opt1")]
          (should= 2 (count result))
          (should= {:exit 0 :host "host1" :opt1 "opt1" :from "from" :to "to"}
            (first result))
          (should= {:exit 0 :host "host2" :opt1 "opt1" :from "from" :to "to"}
            (second result)))))

    (it "should throw exception if there is an error on some host"
      (deftask t1 ["host1" "host2"] "task t1"
        (upload "from" "to"))
      (with-redefs [ssh-upload error-mock-ssh-upload]
        (should-throw Exception "error happened" (execute t1)))))

  (context "put command"

    (it "should pass arguments to ssh-put"
      (deftask t1 ["host1"] "task t1"
        (put "something" "to"))
      (with-redefs [ssh-put mock-ssh-put]
        (should= {:exit 0 :host "host1" :opt1 "opt1" :content "something" :to "to"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should be able to override option"
      (deftask t1 ["host1"] "task t1"
        (put "something" "to" :opt1 "new opt1" :opt2 "opt2"))
      (with-redefs [ssh-put mock-ssh-put]
        (should= {:exit 0 :host "host1" :opt1 "new opt1" :opt2 "opt2" :content "something" :to "to"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should run on multiple hosts"
      (deftask t1 ["host1" "host2"] "task t1"
        (put "something" "to"))
      (with-redefs [ssh-put mock-ssh-put]
        (let [result (execute t1 :opt1 "opt1")]
          (should= 2 (count result))
          (should= {:exit 0 :host "host1" :opt1 "opt1" :content "something" :to "to"}
            (first result))
          (should= {:exit 0 :host "host2" :opt1 "opt1" :content "something" :to "to"}
            (second result)))))

    (it "should throw exception if there is an error on some host"
      (deftask t1 ["host1" "host2"] "task t1"
        (put "something" "to"))
      (with-redefs [ssh-put error-mock-ssh-put]
        (should-throw Exception "error happened" (execute t1)))))

  (context "cmd command"

    (it "should be able to execute commands with options"
      (defn cmd-exec-test [command options expected-exit-code expected-output expected-error]
        (let [result (cmd-exec command options)]
          (if (fn? expected-exit-code)
            (expected-exit-code (:exit result))
            (should= expected-exit-code (:exit result)))
          (if (fn? expected-output)
            (expected-output (:out result))
            (should= expected-output (:out result)))
          (if (fn? expected-error)
            (expected-error (:err result))
            (should= expected-error (:err result)))))
      
      (cmd-exec-test "ls -la" {} 0 #(should (re-find #"drwxr-xr-x" %1)) "")
      (cmd-exec-test "whoami" {} 0 (str (current-user) "\n") "")
      (cmd-exec-test "pwd" {:dir "/usr"} 0 "/usr\n" "")
      (cmd-exec-test "wrong-command" {} #(should-not= 0 %1) "" #(should (re-find #"No such file or directory" %1)))
      (cmd-exec-test "find /usr -name bin -d 1" {} 0 "/usr/bin\n" "")
      (cmd-exec-test "pwd | wc -c | sed -e s/^[[:space:]]*//" {:dir "/usr"} 0 "5\n" "")
      (should-throw (cmd-exec "" {}))
      (should-throw (cmd-exec "pwd | " {})))
    
    (it "should pass option to cmd"
      (deftask t1 ["host1"] "task t1"
        (cmd "pwd"))
      (let [result (execute t1 :dir "/usr")]
        (should= 0 (:exit result))
        (should= "/usr\n" (:out result))))

    (it "should be able to override option in cmd"
      (deftask t1 ["host1"] "task t1"
        (cmd "pwd" :dir "/usr"))
      (let [result (execute t1 :dir "/tmp")]
        (should= 0 (:exit result))
        (should= "/usr\n" (:out result))))

    (it "should only run on localhost"
      (deftask t1 ["host1" "host2"] "task t1"
        (cmd "pwd"))
      (let [result (execute t1 :dir "/usr")]
        (should= 0 (:exit result))
        (should= "/usr\n" (:out result))))

    (it "should throw exception if execution failed"
      (deftask t1 ["host1" "host2"] "task t1"
        (cmd "wrong-command"))
      (should-throw Exception (execute t1)))))