(ns clabric.dsl-spec
  (:use 
    [speclj.core]
    [clabric.dsl]
    [clabric.util]
    [clabric.ssh]
    [clabric.task]
    [clojure.contrib.mock]))

(defn mock-ssh-session [opt]
  opt)

(defn mock-ssh-exec [session command]
  (merge session {:command command}))

(describe "DSL"

  (context "run command"

    (it "should pass arguments to ssh"
      (deftask t1 ["host1"] "task t1"
        (run "ls"))
      (with-redefs [ssh-session mock-ssh-session
                    ssh-exec mock-ssh-exec]
        (should= {:host "host1" :opt1 "opt1" :command "ls"}
          (first (execute t1 :opt1 "opt1")))))

    (it "should be able to override option"
      (deftask t1 ["host1"] "task t1"
        (run "ls" :opt1 "new opt1" :opt2 "opt2"))
      (with-redefs [ssh-session mock-ssh-session
                    ssh-exec mock-ssh-exec]
        (should= {:host "host1" :opt1 "new opt1" :opt2 "opt2" :command "ls"}
          (first (execute t1 :opt1 "opt1"))))))

  (context "cmd command"

    (it "should be able to execute commands with options"
      (defn cmd-test [command options expected-exit-code expected-output expected-error]
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
      
      (cmd-test "ls -la" {} 0 #(should (re-find #"drwxr-xr-x" %1)) "")
      (cmd-test "whoami" {} 0 (str (current-user) "\n") "")
      (cmd-test "pwd" {:dir "/usr"} 0 "/usr\n" "")
      (cmd-test "wrong-command" {} #(should-not= 1 %1) "" #(should (re-find #"No such file or directory" %1)))
      (cmd-test "find /usr -name bin -d 1" {} 0 "/usr/bin\n" ""))
    
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
        (should= "/usr\n" (:out result))))))
