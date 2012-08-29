(ns clabric.dsl-spec
  (:use 
    [speclj.core]
    [clabric.dsl]
    [clabric.util]
    [clabric.ssh]
    [clabric.task]))

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

(defn create-mock-exit [expected-exit-code expected-message]
  (fn [exit-code message]
    (should= expected-exit-code exit-code)
    (should (re-find (re-pattern expected-message) message))))


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
      (with-redefs [ssh-exec error-mock-ssh-exec
                    exit (create-mock-exit 1 "error happened")]
        (execute t1))))

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
      (with-redefs [ssh-upload error-mock-ssh-upload
                    exit (create-mock-exit 1 "error happened")]
        (execute t1))))

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
      (with-redefs [ssh-put error-mock-ssh-put
                    exit (create-mock-exit 1 "error happened")]
        (execute t1))))

  (context "cmd command"

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
      (with-redefs [exit (create-mock-exit -559038737 "Cannot run program \"wrong-command\"")]
        (execute t1)))))