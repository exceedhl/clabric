(ns clabric.ssh
  (:use [clojure.contrib.def]
        [clabric.util])
  (:import
   [java.io
    ByteArrayInputStream ByteArrayOutputStream]
   [com.jcraft.jsch
    JSch Session Channel ChannelShell ChannelExec ChannelSftp JSchException]))

(defn ssh-session [{:keys [host port user private_key_path]
                    :or {port 22
                         user (current-user)
                         private_key_path (str (current-user-home) "/.ssh/id_rsa")}}]
  (JSch/setConfig "StrictHostKeyChecking" "no")
  (let [jsch (JSch.)]
    (.addIdentity jsch private_key_path)
    (.getSession jsch user host port)))

(defmacro- with-connected-session [session & body]
  `(try (.connect ~session)
        ~@body
        (finally (.disconnect ~session))))

(defmacro- with-connected-channel [ch & body]
  `(try (.connect ~ch)
        ~@body
        (finally (.disconnect ~ch))))

(defn ssh-exec [command options]
  (let [session (ssh-session options)]
    (with-connected-session session
      (let [out (ByteArrayOutputStream.)
            err (ByteArrayOutputStream.)
            ^ChannelExec exec (.openChannel session "exec")]
        (doto exec
          (.setOutputStream out)
          (.setErrStream err)
          (.setCommand command))
        (with-connected-channel exec
          (while (.isConnected exec)
            (Thread/sleep 100))
          ;; (if (> (.size out) 0) (info (.toString out)))
          ;; (if (> (.size err) 0) (error (.toString err)))
          {:exit (.getExitStatus exec) :out (.toString out) :err (.toString err)})))))

(defn ssh-upload [from to options]
  )