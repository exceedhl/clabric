(ns clabric.ssh-spec
  (:use [speclj.core]
        [clabric.ssh]
        [clabric.util])
  (:import
   [java.io File FileNotFoundException]
   [com.jcraft.jsch JSchException]))

(describe "SSH"
  
  (with host "33.33.33.10")
  (with port 22)
  (with user "vagrant")
  (with private_key_path (str (current-user-home) "/.ssh/vagrant_private_key"))
  (with options {:host @host :user @user :port @port :private_key_path @private_key_path})
  (with ssh-exec-default #(ssh-exec %1 @options))

  (context "session"

    (context "with not enough parameters"
      
      (with session-params {:host @host})
      (it "should use default value"
        (let [session (ssh-session @session-params)]
          (should= (current-user) (.getUserName session))
          (should= 22 (.getPort session))))))

  (context "exec"

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
      (it "should return error"
        (should-not= 0 (:exit (@ssh-exec-default "ls")))
        (should= "java.io.FileNotFoundException: non-existing key (No such file or directory)" (:err (@ssh-exec-default "ls")))))

    (context "with non-existing host"
      (with host "non-existing host")
      (it "should return error"
        (let [result (@ssh-exec-default "ls")]
          (should-not= 0 (:exit result))
          (should= "java.net.UnknownHostException: non-existing host"
            (:err result)))))

    (context "with wrong port"
      (with port 55555)
      (it "should return error"
        (let [result (@ssh-exec-default "ls")]
          (should-not= 0 (:exit result))
          (should= "java.net.ConnectException: Connection refused"
            (:err result)))))

    (context "with wrong user"
      (with user "non-existing-user")
      (it "should return error"
        (let [result (@ssh-exec-default "ls")]
          (should-not= 0 (:exit result))
          (should= "Auth fail"
            (:err result))))))

  (context "upload"

    (with local-file "/tmp/a.txt")
    (with file-content "hello")
    (with remote-file "/tmp/b.txt")
    (with ssh-upload-default #(ssh-upload %1 %2 @options))
    (with remove-remote-file #(@ssh-exec-default (str "rm -rf " @remote-file)))

    (it "should be able to upload a file to the server"
      (spit @local-file @file-content)
      (@remove-remote-file)
      (let [result (@ssh-upload-default @local-file @remote-file)
            exit (:exit result)
            error (:err result)
            output (:out result)]
        (should= 0 exit)
        (should= "" error)
        (should= "File has been uploaded to remote server successfully" output))
      (let [result (@ssh-exec-default (str "cat " @remote-file))]
        (should= @file-content (:out result))))
    
    (context "with mode option"
      (with options {:user @user :host @host :private_key_path @private_key_path :mode "0755"})
      (it "should be able to set remote file mode"
        (spit @local-file @file-content)
        (@remove-remote-file)
        (@ssh-upload-default @local-file @remote-file)
        (let [result (@ssh-exec-default (str "ls -la " @remote-file " | cut -d ' ' -f 1"))]
          (should= "-rwxr-xr-x\n" (:out result)))))

    (context "with wrong user"
      (with user "non-existing-user")
      (it "should return error"
        (let [result (@ssh-upload-default @local-file @remote-file)]
          (should-not= 0 (:exit result))
          (should= "Auth fail"
            (:err result)))))

    (context "when local file not exist"
      (with local-file "non-existing-file")
      (it "should return error"
        (let [result (@ssh-upload-default @local-file @remote-file)]
          (should-not= 0 (:exit result))
          (should= "non-existing-file (No such file or directory)" (:err result)))))

    (context "when remote file is not writable"
      (with remote-file "/etc/passwd")
      (it "should return error"
        (let [result (@ssh-upload-default @local-file @remote-file)]
          (should-not= 0 (:exit result))
          (should= "scp: /etc/passwd: Permission denied" (:err result))))))

  (context "put"

    (with file-content "hello")
    (with remote-file "/tmp/b.txt")
    (with remove-remote-file #(@ssh-exec-default (str "rm -rf " @remote-file)))

    (it "should be able to put string to a remote file"
      (@remove-remote-file)
      (ssh-put @file-content @remote-file @options)
      (let [result (@ssh-exec-default (str "cat " @remote-file))]
        (should= @file-content (:out result))))))