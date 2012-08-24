(ns clabric.dsl-spec
  (:use 
    [speclj.core]
    [clabric.dsl]
    [clabric.ssh]
    [clabric.task]
    [clojure.contrib.mock]))

(defn mock-ssh-session [opt]
  opt)

(defn mock-ssh-exec [session command]
  (merge session {:command command}))

(describe "DSL"

  (it "should pass arguments to ssh"
    (deftask t1 ["host1"] "task t1"
      (run "ls"))
    (with-redefs [ssh-session mock-ssh-session
                  ssh-exec mock-ssh-exec]
      (should= {:host "host1" :opt1 "opt1" :command "ls"}
        (first (execute t1 :opt1 "opt1")))))

  (it "should be able to override option in run"
    (deftask t1 ["host1"] "task t1"
      (run "ls" :opt1 "new opt1" :opt2 "opt2"))
    (with-redefs [ssh-session mock-ssh-session
                  ssh-exec mock-ssh-exec]
      (should= {:host "host1" :opt1 "new opt1" :opt2 "opt2" :command "ls"}
        (first (execute t1 :opt1 "opt1"))))))
