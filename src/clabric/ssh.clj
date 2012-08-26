(ns clabric.ssh
  (:use [clojure.contrib.def]
        [clabric.util]
        [clojure.java.io]
        [clabric.SSHException])
  (:import
   [java.io
    File
    ByteArrayInputStream
    ByteArrayOutputStream
    DataOutputStream
    BufferedReader
    InputStreamReader]
   [com.jcraft.jsch
    JSch Session Channel
    ChannelShell ChannelExec
    ChannelSftp JSchException]))

(defn- default-private-key []
  (str (current-user-home) "/.ssh/id_rsa"))

(defn ssh-session [{:keys [host port user private_key_path]
                    :or {port 22
                         user (current-user)
                         private_key_path (default-private-key)}}]
  (JSch/setConfig "StrictHostKeyChecking" "no")
  (let [jsch (JSch.)]
    (.addIdentity jsch private_key_path)
    (.getSession jsch user host port)))

(defmacro with-connected-session [session & body]
  `(try (.connect ~session)
        ~@body
        (finally (.disconnect ~session))))

(defmacro with-connected-channel [ch & body]
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
          {:exit (.getExitStatus exec)
           :out (.toString out)
           :err (.toString err)})))))

(defn- ptimestamp-cmd [filepath]
  (let [file (file filepath)
        lm (/ (.lastModified file) 1000)]
    (str "T " lm " 0" " " lm " 0\n")))

(defn- filesize-and-mode-cmd [filepath mode]
  (let [file (file filepath)
        filesize (.length file)]
    (str "C" mode " " filesize " " (.getName file) "\n")))

(defn- in->out [in out]
  (let [o (DataOutputStream. out)
        s (slurp in)]
    (.writeBytes o s)))

(defn- string->out [s out]
  (let [out (DataOutputStream. out)]
    (.writeBytes out s)))

(defn- readline [in]
  (.readLine (BufferedReader. (InputStreamReader. in))))

(defn- check-ack [in]
  (let [b (.read in)]
    (if (not= 0 (int b))
      (throw (clabric.SSHException. (readline in))))))

(defn ssh-upload [from to options]
  (let [session (ssh-session options)]
    (with-connected-session session
      (let [^ChannelExec exec (.openChannel session "exec")]
        (.setCommand exec (str "scp -p -t " to))
        (with-connected-channel exec
          (let [ptimestamp-cmd (ptimestamp-cmd from)
                mode (or (:mode options) "0644")
                size-and-mode-cmd (filesize-and-mode-cmd from mode)
                fin (input-stream from)
                in (.getInputStream exec)
                out (.getOutputStream exec)]
            (check-ack in)
            (string->out ptimestamp-cmd out)
            (.flush out)
            (check-ack in)
            (string->out size-and-mode-cmd out)
            (.flush out)
            (check-ack in)
            (in->out fin out)
            (string->out "\0" out) 
            (.close out)
            (check-ack in)
            {:exit 0 :err ""}))))))

(defn ssh-put [string to options]
  (let [tempfile (File/createTempFile "clabric-" "-temp")
        filepath (.getCanonicalPath tempfile)]
    (spit filepath string)
    (ssh-upload filepath to options)))