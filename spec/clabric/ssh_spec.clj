(ns clabric.ssh-spec
  (:use [speclj.core]
        [clabric.ssh]
        [clabric.util])
  (:import
   [java.io File]
   [com.jcraft.jsch JSchException]))

(describe "SSH session"
  (with host "33.33.33.10")
  (with port 22)
  (with user "vagrant")
  (with private_key_path (str (current-user-home) "/.ssh/vagrant_private_key"))
  (with session-params {:host @host :port @port :user @user
                        :private_key_path @private_key_path})
  (with session
    (ssh-session @session-params))

  (it "should execute command successfully"
    (let [result (ssh-exec @session "whoami")
          exit (:exit result)
          output (:out result)
          error (:err result)]
      (should= 0 exit)
      (should= (str @user "\n") output)
      (should= "" error)))

  (it "should return error if command execution failed"
    (let [result (ssh-exec @session "wrong-command")
          exit (:exit result)
          output (:out result)
          error (:err result)]
      (should= 127 exit)
      (should= "" output)
      (should= "bash: wrong-command: command not found\n" error)))

  (it "should timeout after given waiting time")

  (context "with not enough parameters"
    (with session-params {:host @host})
    (it "should use default value"
      (should= (current-user) (.getUserName @session))
      (should= 22 (.getPort @session))))
  
  (context "with wrong key"
    (with private_key_path "non-existing key")
    (it "should throw exception"
      (should-throw JSchException "java.io.FileNotFoundException: non-existing key (No such file or directory)"
        (ssh-exec @session "ls"))))

  (context "with non-existing host"
    (with host "non-existing host")
    (it "should throw exception"
      (should-throw JSchException "java.net.UnknownHostException: non-existing host"
        (ssh-exec @session "ls"))))

  (context "with wrong port"
    (with port 55555)
    (it "should throw exception"
      (should-throw JSchException "java.net.ConnectException: Connection refused"
        (ssh-exec @session "ls"))))

  (context "with wrong user"
    (with user "non-existing-user")
    (it "should throw exception"
      (should-throw JSchException "Auth fail"
        (ssh-exec @session "ls")))))