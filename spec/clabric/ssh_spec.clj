(ns clabric.ssh-spec
  (:use [speclj.core]
        [clabric.ssh]
        [clabric.util])
  (:import
   [java.io File]
   [com.jcraft.jsch JSchException]))

(describe "SSH"
  
  (with host "33.33.33.10")
  (with port 22)
  (with user "vagrant")
  (with private_key_path (str (current-user-home) "/.ssh/vagrant_private_key"))

  (context "session"

    (context "with not enough parameters"
      
      (with session-params {:host @host})
      (it "should use default value"
        (let [session (ssh-session @session-params)]
          (should= (current-user) (.getUserName session))
          (should= 22 (.getPort session))))))

  (context "exec"

    (with ssh-exec-default #(ssh-exec %1 {:host @host :user @user :port @port :private_key_path @private_key_path}))

    (it "should be able to execute simple command successfully"
      (let [result (@ssh-exec-default "whoami")
            exit (:exit result)
            output (:out result)
            error (:err result)]
        (should= 0 exit)
        (should= (str @user "\n") output)
        (should= "" error)))

    (it "should be able to execute commands successfully"
      (let [result (@ssh-exec-default "cd /tmp; pwd | wc -c")
            exit (:exit result)
            output (:out result)
            error (:err result)]
        (should= 0 exit)
        (should= "5\n" output)
        (should= "" error)))

    (it "should return error if command execution failed"
      (let [result (@ssh-exec-default "wrong-command")
            exit (:exit result)
            output (:out result)
            error (:err result)]
        (should= 127 exit)
        (should= "" output)
        (should= "bash: wrong-command: command not found\n" error)))

    (it "should timeout after given waiting time")
  
    (context "with wrong key"
      (with private_key_path "non-existing key")
      (it "should throw exception"
        (should-throw JSchException "java.io.FileNotFoundException: non-existing key (No such file or directory)"
          (@ssh-exec-default "ls"))))

    (context "with non-existing host"
      (with host "non-existing host")
      (it "should throw exception"
        (should-throw JSchException "java.net.UnknownHostException: non-existing host"
          (@ssh-exec-default "ls"))))

    (context "with wrong port"
      (with port 55555)
      (it "should throw exception"
        (should-throw JSchException "java.net.ConnectException: Connection refused"
          (@ssh-exec-default "ls"))))

    (context "with wrong user"
      (with user "non-existing-user")
      (it "should throw exception"
        (should-throw JSchException "Auth fail"
          (@ssh-exec-default "ls")))))

  (context "put"

    (it "should be able to upload a file to the server"
      (let [result (ssh-upload "/tmp/a.txt" "/tmp/b.txt" {:user @user :host @host :private_key_path @private_key_path})
            exit (:exit result)
            output (:out result)
            error (:err result)]
        (should= 0 exit)
        (should= "" output)
        (should= "" error)))

    (it "should return error if uploading failed")))