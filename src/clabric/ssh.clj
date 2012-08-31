(ns clabric.ssh
  (:use [clojure.core.incubator]
        [clabric.util]
        [clojure.java.io]
        [clabric.logging])
  (:import
   [java.io
    File
    FileNotFoundException
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
  (debug (:host options) "Execute command:" command "with options:" options)
  (try
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
            (let [output (.toString out)
                  error (.toString err)]
              (log-host-and-result (:host options) output error)
              {:exit (.getExitStatus exec)
               :out output
               :err error})))))
    (catch Exception e
      {:exit 1 :err (.getMessage e) :out ""})))

(defn- ptimestamp-cmd [filepath]
  (let [file (file filepath)
        lm (/ (.lastModified file) 1000)]
    (str "T " lm " 0" " " lm " 0\n")))

(defn- filesize-and-mode-cmd [filepath mode]
  (let [file (file filepath)
        filesize (.length file)]
    (str "C" mode " " filesize " " (.getName file) "\n")))

(defn- string->out [s out]
  (let [out (DataOutputStream. out)]
    (.writeBytes out s)))

(defn- readline [in]
  (.readLine (BufferedReader. (InputStreamReader. in))))

(defn- check-ack [in]
  (let [b (.read in)]
    (if (not= 0 (int b))
      (throw (Exception. (readline in))))))

(defn- create-scp-channel [session to]
  (let [^ChannelExec exec (.openChannel session "exec")]
    (.setCommand exec (str "scp -p -t " to))
    exec))

(defn- send-ptimestamp-cmd [from in out]
  (let [cmd (ptimestamp-cmd from)]
    (string->out cmd out)
    (.flush out)
    (check-ack in)))

(defn- send-size-and-mode-cmd [from mode in out]
  (let [mode (or mode "0644")
        cmd (filesize-and-mode-cmd from mode)]
    (string->out cmd out)
    (.flush out)
    (check-ack in)))

(defn- send-file-content [from in out]
  (let [fin (input-stream from)]
    (copy fin out)
    (string->out "\0" out) 
    (.close out)
    (check-ack in)))

(defn ssh-upload [from to options]
  (debug (:host options) "Uploading file from:" from "to:" to "with options:" options)
  (try
    (let [session (ssh-session options)]
      (with-connected-session session
        (let [^ChannelExec exec (create-scp-channel session to)]
          (with-connected-channel exec
            (let [in (.getInputStream exec)
                  out (.getOutputStream exec)]
              (check-ack in)
              (send-ptimestamp-cmd from in out)
              (send-size-and-mode-cmd from (:mode options) in out)
              (send-file-content from in out)
              (info (:host options) (str "File " from " transfer complete."))
              {:exit 0 :err "" :out "File has been uploaded to remote server successfully"})))))
    (catch Exception e
      (do
        (log-host-and-result (:host options) nil nil e)
        {:exit 1 :err (.getMessage e)}))))

(defn ssh-put [string to options]
  (let [tempfile (File/createTempFile "clabric-" "-temp")
        filepath (.getCanonicalPath tempfile)]
    (spit filepath string)
    (ssh-upload filepath to options)))