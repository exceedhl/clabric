(ns clabric.test
  (:use
   [clabric.task]
   [clabric.dsl]))

(deftask t2 ["33.33.33.10"] "task t2"
  (run "ps aux")
  (upload "/Users/huangliang/Downloads/Skype_5.8.0.1027.dmg" "/tmp/a.dmg")
  ;; (cmd "adsfadsf")
  ;; (run "whoami" :user "root")
  (cmd "ls -la")
  (put "adsfasdfads\n" "/tmp/c.txt")
  (run "sudo cat /etc/passwd"))

(execute t2 :user "vagrant" :private_key_path "/Users/huangliang/.ssh/vagrant_private_key")

(deftask t3 ["localhost"] "task t3"
  (cmd "uname -a")
  ;; (throw (Exception. "hello, error happened"))
  ;; (cmd "adsfadsf")
  (cmd "ls -la")
  (execute t2 :user "vagrant" :private_key_path "/Users/huangliang/.ssh/vagrant_private_key"))

(execute t3)

(deftask t1 [] "task t1"
  (run "ps"))

(execute t1)

