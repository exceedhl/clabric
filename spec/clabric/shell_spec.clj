(ns clabric.shell-spec
  (:use 
    [speclj.core]
    [clabric.shell]
    [clabric.util]))

(describe "Shell"

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
    (should-throw (cmd-exec "pwd | " {}))))